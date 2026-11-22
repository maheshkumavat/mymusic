# Automated Build & Release Script for MyMusic
# This script automates bumping version numbers, compiling the release APK, creating Git tags,
# pushing to GitHub, and publishing a new release with the APK attached.

$ErrorActionPreference = "Stop"

# Step 1: Resolve GitHub Owner & Repository from UpdateManager.kt
$updateManagerPath = "app/src/main/java/com/personal/mymusic/data/network/UpdateManager.kt"
if (!(Test-Path $updateManagerPath)) {
    Write-Error "Cannot find UpdateManager.kt at $updateManagerPath"
}

$updateManagerContent = Get-Content $updateManagerPath -Raw
$githubOwner = $null
$githubRepo = $null

if ($updateManagerContent -match 'const val GITHUB_OWNER = "([^"]+)"') {
    $githubOwner = $Matches[1]
}
if ($updateManagerContent -match 'const val GITHUB_REPO = "([^"]+)"') {
    $githubRepo = $Matches[1]
}

if ($githubOwner -eq "mahesh-git" -or [string]::IsNullOrEmpty($githubOwner)) {
    Write-Host "`n[!] WARNING: GitHub owner in UpdateManager.kt is still set to the default placeholder '$githubOwner'." -ForegroundColor Yellow
    $userInputOwner = Read-Host "Please enter your GitHub Username"
    if (![string]::IsNullOrEmpty($userInputOwner)) {
        $githubOwner = $userInputOwner.Trim()
        # Update the file automatically!
        $updateManagerContent = $updateManagerContent -replace 'const val GITHUB_OWNER = "[^"]+"', "const val GITHUB_OWNER = `"$githubOwner`""
        Set-Content $updateManagerPath $updateManagerContent
        Write-Host "[*] Updated GITHUB_OWNER in UpdateManager.kt to '$githubOwner'." -ForegroundColor Green
    } else {
        Write-Error "GitHub Username cannot be empty."
    }
}

Write-Host "`nUsing GitHub Target: $githubOwner/$githubRepo" -ForegroundColor Cyan

# Step 2: Get GitHub API token (GITHUB_TOKEN)
$githubToken = $env:GITHUB_TOKEN
if ([string]::IsNullOrEmpty($githubToken)) {
    Write-Host "[?] GITHUB_TOKEN environment variable not found." -ForegroundColor Yellow
    $githubToken = Read-Host "Please enter your GitHub Personal Access Token (PAT)"
    if ([string]::IsNullOrEmpty($githubToken)) {
        Write-Error "GitHub PAT is required to publish a release."
    }
    $githubToken = $githubToken.Trim()
}

# Step 3: Parse and Increment versions in app/build.gradle
$gradlePath = "app/build.gradle"
if (!(Test-Path $gradlePath)) {
    Write-Error "Cannot find app/build.gradle"
}

$gradleContent = Get-Content $gradlePath -Raw
$currentCode = $null
$currentName = $null

if ($gradleContent -match 'versionCode\s+(\d+)') {
    $currentCode = [int]$Matches[1]
}
if ($gradleContent -match 'versionName\s+"([^"]+)"') {
    $currentName = $Matches[1]
}

if ($null -eq $currentCode -or $null -eq $currentName) {
    Write-Error "Failed to parse versionCode or versionName from app/build.gradle"
}

if ($currentName -eq "1.0.20") {
    $newCode = 21
    $newName = "1.0.20"
} elseif ($currentName -eq "1.0.19") {
    $newCode = 21
    $newName = "1.0.20"
} else {
    $newCode = $currentCode + 1
    $parts = $currentName.Split('.')
    if ($parts.Length -eq 2) {
        $newName = "$($parts[0]).$([int]$parts[1] + 1)"
    } elseif ($parts.Length -eq 3) {
        $newName = "$($parts[0]).$($parts[1]).$([int]$parts[2] + 1)"
    } else {
        $newName = "$currentName.1"
    }
}

Write-Host "Releasing App Version: $newName (Code: $newCode)" -ForegroundColor Cyan

# Save new version to app/build.gradle
$gradleContent = $gradleContent -replace "versionCode\s+$currentCode", "versionCode $newCode"
$gradleContent = $gradleContent -replace "versionName\s+`"$currentName`"", "versionName `"$newName`""
Set-Content $gradlePath $gradleContent

# Step 4: Build the Signed Release APK
Write-Host "`n[*] Compiling Signed Release APK..." -ForegroundColor Cyan

# Validate Keystore setup
if (!(Test-Path "keystore.properties") -and !(Test-Path "local.properties") -and [string]::IsNullOrEmpty($env:KEYSTORE_PASSWORD)) {
    Write-Host "[!] WARNING: keystore.properties not found and KEYSTORE_PASSWORD environment variable not set." -ForegroundColor Yellow
}

# Resolve Java & Android environment if not already set
if ([string]::IsNullOrEmpty($env:JAVA_HOME) -or !(Test-Path $env:JAVA_HOME)) {
    if (Test-Path "c:\Users\mahes\OneDrive\Desktop\mymusic\.tools\jdk") {
        $env:JAVA_HOME = "c:\Users\mahes\OneDrive\Desktop\mymusic\.tools\jdk"
    }
}
if ([string]::IsNullOrEmpty($env:ANDROID_HOME) -or !(Test-Path $env:ANDROID_HOME)) {
    if (Test-Path "c:\Users\mahes\OneDrive\Desktop\mymusic\.tools\android-sdk") {
        $env:ANDROID_HOME = "c:\Users\mahes\OneDrive\Desktop\mymusic\.tools\android-sdk"
    } elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
        $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    }
}
$env:ANDROID_SDK_HOME = ""

# Run Gradle Build
.\gradlew.bat assembleRelease
if ($LASTEXITCODE -ne 0) {
    # Revert gradle version changes on failure
    Set-Content $gradlePath ((Get-Content $gradlePath -Raw) -replace "versionCode $newCode", "versionCode $currentCode" -replace "versionName `"$newName`"", "versionName `"$currentName`"")
    Write-Error "Gradle build failed with exit code $LASTEXITCODE"
}

$apkPath = "app/build/outputs/apk/release/app-release.apk"
if (!(Test-Path $apkPath)) {
    Write-Error "Could not find built APK at $apkPath"
}
Write-Host "[+] APK Compiled Successfully!" -ForegroundColor Green

# Step 5: Git Commit and Tagging
Write-Host "`n[*] Creating Git Commit and Tag..." -ForegroundColor Cyan
# Initialize git configuration if not set
$gitUser = git config user.name
if ([string]::IsNullOrEmpty($gitUser)) {
    git config user.name "mymusic-builder"
    git config user.email "mymusic-builder@local"
}

git add -A
$gitStatus = git status --porcelain
if (![string]::IsNullOrEmpty($gitStatus)) {
    git commit -m "Bump version to v$newName"
} else {
    Write-Host "[*] Working directory is clean, no new commit needed." -ForegroundColor Green
}

if (!(git tag -l "v$newName")) {
    git tag "v$newName"
    Write-Host "[+] Created tag v$newName" -ForegroundColor Green
} else {
    Write-Host "[*] Tag v$newName already exists." -ForegroundColor Yellow
}

# Configure clean remote URL for origin (no token saved locally)
try {
    git remote remove origin 2>$null
    git remote add origin "https://github.com/$githubOwner/$githubRepo.git"
} catch {}

# Attempt to push to remote securely (never force-push)
Write-Host "`n[*] Pushing to GitHub..." -ForegroundColor Cyan
try {
    $pushUrl = "https://${githubToken}@github.com/$githubOwner/$githubRepo.git"
    git push $pushUrl main
    git push $pushUrl "v$newName"
    Write-Host "[+] Pushed successfully to GitHub remote." -ForegroundColor Green
} catch {
    Write-Host "[!] Could not push to origin: $_" -ForegroundColor Yellow
}

# Step 6: Create GitHub Release and Upload APK Asset
Write-Host "`n[*] Publishing GitHub Release v$newName..." -ForegroundColor Cyan

$releaseBody = @{
    tag_name = "v$newName"
    name = "Release v$newName"
    body = "Automated release version $newName built on $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss').`n`n### Changes:`n- Automatic build version bump`n- Security and performance optimizations"
    draft = $false
    prerelease = $false
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $githubToken"
    "Accept" = "application/vnd.github.v3+json"
}

# Create Release
$releaseResponse = $null
try {
    $releaseResponse = Invoke-RestMethod -Uri "https://api.github.com/repos/$githubOwner/$githubRepo/releases" -Method Post -Body $releaseBody -Headers $headers -ContentType "application/json"
    Write-Host "[+] Created GitHub Release: v$newName" -ForegroundColor Green
} catch {
    Write-Error "Failed to create GitHub release: $_"
}

# Upload APK Asset
Write-Host "`n[*] Uploading release APK asset..." -ForegroundColor Cyan
$uploadUrl = "https://uploads.github.com/repos/$githubOwner/$githubRepo/releases/$($releaseResponse.id)/assets?name=mymusic.apk"
$fileBytes = [System.IO.File]::ReadAllBytes($apkPath)

try {
    $uploadResponse = Invoke-RestMethod -Uri $uploadUrl -Method Post -Body $fileBytes -Headers $headers -ContentType "application/vnd.android.package-archive"
    Write-Host "[+] APK Uploaded Successfully as Release Asset!" -ForegroundColor Green
} catch {
    Write-Error "Failed to upload APK to release: $_"
}

# Step 7: Done!
Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "RELEASE PUBLISHED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "Version Name: v$newName" -ForegroundColor Green
Write-Host "Version Code: $newCode" -ForegroundColor Green
Write-Host "Release Page: https://github.com/$githubOwner/$githubRepo/releases/tag/v$newName" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green

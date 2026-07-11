$toolsDir = "c:\Users\mahes\OneDrive\Desktop\mymusic\.tools"
if (!(Test-Path $toolsDir)) { 
    New-Item -ItemType Directory -Path $toolsDir | Out-Null
}

$jdkUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
$jdkZip = "$toolsDir\jdk.zip"
$jdkDir = "$toolsDir\jdk"

if (!(Test-Path $jdkDir)) {
    Write-Host "[1/4] Downloading JDK 17..."
    curl.exe -L -o $jdkZip $jdkUrl
    Write-Host "[2/4] Extracting JDK 17..."
    tar.exe -xf $jdkZip -C $toolsDir
    Remove-Item $jdkZip
    $extracted = Get-ChildItem $toolsDir -Directory | Where-Object { $_.Name -like "jdk-17*" }
    if ($extracted) {
        Rename-Item $extracted[0].FullName "jdk"
    } else {
        Write-Error "Failed to find extracted JDK directory"
        exit 1
    }
} else {
    Write-Host "[1/4] JDK 17 already present."
}

$sdkUrl = "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip"
$sdkZip = "$toolsDir\sdk.zip"
$sdkDir = "$toolsDir\android-sdk"
$cmdlineLatestDir = "$sdkDir\cmdline-tools\latest"

if (!(Test-Path $cmdlineLatestDir)) {
    Write-Host "[3/4] Downloading Android SDK Command Line Tools..."
    curl.exe -L -o $sdkZip $sdkUrl
    Write-Host "[4/4] Extracting Android SDK Command Line Tools..."
    $cmdlineDir = "$sdkDir\cmdline-tools"
    if (!(Test-Path $cmdlineDir)) { 
        New-Item -ItemType Directory -Path $cmdlineDir | Out-Null
    }
    tar.exe -xf $sdkZip -C $cmdlineDir
    Remove-Item $sdkZip
    
    $extracted = Join-Path $cmdlineDir "cmdline-tools"
    if (Test-Path $extracted) {
        Rename-Item $extracted "latest"
    } else {
        Write-Error "Failed to find extracted cmdline-tools directory"
        exit 1
    }
} else {
    Write-Host "[3/4] Android SDK Command Line Tools already present."
}

# Set environment variables for this script run
$env:JAVA_HOME = $jdkDir
$env:ANDROID_HOME = $sdkDir
$env:PATH = "$jdkDir\bin;$cmdlineLatestDir\bin;$sdkDir\platform-tools;$env:PATH"

Write-Host "Accepting Android SDK licenses..."
1..30 | % { "y" } | & "$cmdlineLatestDir\bin\sdkmanager.bat" --licenses

Write-Host "Installing Android SDK packages..."
& "$cmdlineLatestDir\bin\sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator" "system-images;android-34;google_apis;x86_64"

Write-Host "Tools setup complete!"

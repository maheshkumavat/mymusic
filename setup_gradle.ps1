$toolsDir = "c:\Users\mahes\OneDrive\Desktop\mymusic\.tools"
$gradleUrl = "https://services.gradle.org/distributions/gradle-8.4-bin.zip"
$gradleZip = "$toolsDir\gradle.zip"
$gradleDir = "$toolsDir\gradle-8.4"

if (!(Test-Path $toolsDir)) {
    New-Item -ItemType Directory -Path $toolsDir | Out-Null
}

if (!(Test-Path $gradleDir)) {
    Write-Host "Downloading Gradle 8.4..."
    curl.exe -L -o $gradleZip $gradleUrl
    Write-Host "Extracting Gradle 8.4..."
    tar.exe -xf $gradleZip -C $toolsDir
    Remove-Item $gradleZip
} else {
    Write-Host "Gradle 8.4 already present."
}

# Set environment variables
$env:JAVA_HOME = "$toolsDir\jdk"
$env:ANDROID_HOME = "$toolsDir\android-sdk"
$env:PATH = "$env:JAVA_HOME\bin;$toolsDir\gradle-8.4\bin;$env:PATH"

# Run gradle to generate wrapper
Write-Host "Initializing Gradle Wrapper..."
gradle.bat wrapper

Write-Host "Gradle setup complete!"

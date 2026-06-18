# setup_tools.ps1
# This script downloads and configures JDK 17 and Android SDK Command Line Tools locally.

$ToolsDir = "c:\xampp\htdocs\quran_android\tools"
$SdkDir = "$ToolsDir\android-sdk"
$JdkDir = "$ToolsDir\jdk17"

# Create directories
Write-Host "Creating directories..."
New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
New-Item -ItemType Directory -Force -Path $SdkDir | Out-Null

# Download URLs
$CmdLineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip"
$JdkUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"

$CmdLineZip = "$ToolsDir\commandlinetools.zip"
$JdkZip = "$ToolsDir\jdk17.zip"

# Download Android SDK Command Line Tools
if (-not (Test-Path $CmdLineZip)) {
    Write-Host "Downloading Android SDK Command Line Tools (~120MB)..."
    Invoke-WebRequest -Uri $CmdLineToolsUrl -OutFile $CmdLineZip
} else {
    Write-Host "Android SDK ZIP already exists, skipping download."
}

# Download JDK 17
if (-not (Test-Path $JdkZip)) {
    Write-Host "Downloading Eclipse Temurin JDK 17 (~180MB)..."
    Invoke-WebRequest -Uri $JdkUrl -OutFile $JdkZip
} else {
    Write-Host "JDK ZIP already exists, skipping download."
}

# Extract JDK 17
if (-not (Test-Path $JdkDir)) {
    Write-Host "Extracting JDK 17..."
    $JdkTemp = "$ToolsDir\jdk_temp"
    New-Item -ItemType Directory -Force -Path $JdkTemp | Out-Null
    Expand-Archive -Path $JdkZip -DestinationPath $JdkTemp
    
    # Locate extracted folder and move it to jdk17
    $ExtractedJdk = Get-ChildItem -Path $JdkTemp -Directory | Select-Object -First 1
    if ($ExtractedJdk) {
        Move-Item -Path $ExtractedJdk.FullName -Destination $JdkDir
    }
    Remove-Item -Path $JdkTemp -Recurse -Force | Out-Null
    Write-Host "JDK 17 successfully set up at: $JdkDir"
} else {
    Write-Host "JDK 17 is already extracted."
}

# Extract Android Command Line Tools
$LatestCmdLine = "$SdkDir\cmdline-tools\latest"
if (-not (Test-Path $LatestCmdLine)) {
    Write-Host "Extracting Android Command Line Tools..."
    $SdkTemp = "$ToolsDir\sdk_temp"
    New-Item -ItemType Directory -Force -Path $SdkTemp | Out-Null
    Expand-Archive -Path $CmdLineZip -DestinationPath $SdkTemp
    
    # Structure must be: cmdline-tools\latest\bin, lib, etc.
    New-Item -ItemType Directory -Force -Path "$SdkDir\cmdline-tools" | Out-Null
    Move-Item -Path "$SdkTemp\cmdline-tools" -Destination $LatestCmdLine
    Remove-Item -Path $SdkTemp -Recurse -Force | Out-Null
    Write-Host "Android Command Line Tools successfully set up at: $LatestCmdLine"
} else {
    Write-Host "Android Command Line Tools are already extracted."
}

# Clean up ZIP files to save space
Write-Host "Cleaning up ZIP files..."
if (Test-Path $CmdLineZip) { Remove-Item -Path $CmdLineZip -Force }
if (Test-Path $JdkZip) { Remove-Item -Path $JdkZip -Force }

Write-Host "Setup Completed successfully!"

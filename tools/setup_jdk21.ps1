# setup_jdk21.ps1
$ToolsDir = "c:\xampp\htdocs\quran_android\tools"
$Jdk21Dir = "$ToolsDir\jdk21"
$Jdk21Zip = "$ToolsDir\jdk21.zip"
$JdkUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"

# Download JDK 21
if (-not (Test-Path $Jdk21Zip)) {
    Write-Host "Downloading Eclipse Temurin JDK 21 (~200MB)..."
    Invoke-WebRequest -Uri $JdkUrl -OutFile $Jdk21Zip
} else {
    Write-Host "JDK 21 ZIP already exists, skipping download."
}

# Extract JDK 21
if (-not (Test-Path $Jdk21Dir)) {
    Write-Host "Extracting JDK 21..."
    $JdkTemp = "$ToolsDir\jdk21_temp"
    New-Item -ItemType Directory -Force -Path $JdkTemp | Out-Null
    Expand-Archive -Path $Jdk21Zip -DestinationPath $JdkTemp
    
    # Locate extracted folder and move it to jdk21
    $ExtractedJdk = Get-ChildItem -Path $JdkTemp -Directory | Select-Object -First 1
    if ($ExtractedJdk) {
        Move-Item -Path $ExtractedJdk.FullName -Destination $Jdk21Dir
    }
    Remove-Item -Path $JdkTemp -Recurse -Force | Out-Null
    Write-Host "JDK 21 successfully set up at: $Jdk21Dir"
} else {
    Write-Host "JDK 21 is already extracted."
}

# Clean up ZIP file
if (Test-Path $Jdk21Zip) { Remove-Item -Path $Jdk21Zip -Force }
Write-Host "JDK 21 Setup Completed successfully!"

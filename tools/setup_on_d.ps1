# setup_on_d.ps1
$SourceTools = "c:\xampp\htdocs\quran_android\tools"
$DestTools = "D:\quran_android_tools"

# 1. Buat folder tujuan di D:
Write-Host "Membuat direktori baru di D:\quran_android_tools..."
New-Item -ItemType Directory -Force -Path $DestTools | Out-Null

# 2. Pindahkan JDK 21 ke D:
if (Test-Path "$SourceTools\jdk21") {
    Write-Host "Memindahkan JDK 21 ke D:..."
    if (Test-Path "$DestTools\jdk21") { Remove-Item -Path "$DestTools\jdk21" -Recurse -Force }
    Move-Item -Path "$SourceTools\jdk21" -Destination "$DestTools"
}

# 3. Pindahkan Android SDK ke D:
if (Test-Path "$SourceTools\android-sdk") {
    Write-Host "Memindahkan Android SDK ke D:..."
    if (Test-Path "$DestTools\android-sdk") { Remove-Item -Path "$DestTools\android-sdk" -Recurse -Force }
    Move-Item -Path "$SourceTools\android-sdk" -Destination "$DestTools"
}

# 4. Atur ulang local.properties agar menunjuk ke D:
Write-Host "Memperbarui local.properties..."
$LocalPropertiesPath = "c:\xampp\htdocs\quran_android\local.properties"
"sdk.dir=D:/quran_android_tools/android-sdk" | Out-File -FilePath $LocalPropertiesPath -Encoding utf8

# 5. Setujui lisensi SDK di D:
Write-Host "Menyetujui lisensi SDK di drive D:..."
$env:JAVA_HOME = "$DestTools\jdk21"
$SdkManager = "$DestTools\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat"
$SdkRoot = "$DestTools\android-sdk"
@("y") * 15 | & $SdkManager --sdk_root=$SdkRoot --licenses

# 6. Install SDK Components (Platform 37 & Build-Tools 36) di D:
Write-Host "Mengunduh & memasang komponen SDK di drive D:..."
@("y") * 15 | & $SdkManager --sdk_root=$SdkRoot "build-tools;36.0.0" "platforms;android-37.0"

Write-Host "Konfigurasi di Drive D selesai dengan sukses!"

# install_sdk_components.ps1
$env:JAVA_HOME = "c:\xampp\htdocs\quran_android\tools\jdk21"
$SdkManager = "c:\xampp\htdocs\quran_android\tools\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat"
$SdkRoot = "c:\xampp\htdocs\quran_android\tools\android-sdk"

Write-Host "Mengunduh dan memasang SDK components..."
@("y") * 15 | & $SdkManager --sdk_root=$SdkRoot "build-tools;36.0.0" "platforms;android-37.0"
Write-Host "Pemasangan selesai!"

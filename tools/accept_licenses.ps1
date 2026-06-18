# accept_licenses.ps1
$env:JAVA_HOME = "c:\xampp\htdocs\quran_android\tools\jdk17"
$SdkManager = "c:\xampp\htdocs\quran_android\tools\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat"
$SdkRoot = "c:\xampp\htdocs\quran_android\tools\android-sdk"

Write-Host "Accepting Android SDK licenses..."
@("y") * 15 | & $SdkManager --sdk_root=$SdkRoot --licenses
Write-Host "License process completed!"

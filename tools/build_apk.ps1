# build_apk.ps1
$env:JAVA_HOME = "D:\quran_android_tools\jdk21"
$env:ANDROID_HOME = "D:\quran_android_tools\android-sdk"
$env:GRADLE_USER_HOME = "D:\.gradle"

Write-Host "Mulai membangun APK debug dengan cache di Drive D..."
.\gradlew.bat assembleMadaniDebug

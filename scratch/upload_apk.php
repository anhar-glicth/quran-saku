<?php
$user = 'u290212134';
$pass = 'f1D022013.';
$host = '153.92.10.174';
$port = 65002;

$localFile = 'c:/xampp/htdocs/quran_android/app-madani-release.apk';
$remotePath = '/home/u290212134/domains/pondokquranmahasiswaadtin.com/public_html/app-madani-release.apk';

echo "Mengunggah APK terbaru ke $remotePath...\n";
$url = "sftp://{$user}:" . urlencode($pass) . "@{$host}:{$port}{$remotePath}";

$ch = curl_init();
$fp = fopen($localFile, 'r');

curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_UPLOAD, true);
curl_setopt($ch, CURLOPT_INFILE, $fp);
curl_setopt($ch, CURLOPT_INFILESIZE, filesize($localFile));
curl_setopt($ch, CURLOPT_SSH_AUTH_TYPES, CURLSSH_AUTH_PASSWORD);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 30);

$result = curl_exec($ch);
$error = curl_error($ch);
curl_close($ch);
fclose($fp);

if ($result) {
    echo "BERHASIL: APK dipublikasikan ke web!\n";
    echo "URL Download APK: https://pondokquranmahasiswaadtin.com/app-madani-release.apk\n";
} else {
    echo "ERROR uploading APK: $error\n";
}
?>

<?php
$user = 'u290212134';
$pass = 'f1D022013.';
$host = '153.92.10.174';
$port = 65002;

$localFile = 'c:/xampp/htdocs/quran_android/web/privacy_policy.html';
$remotePath = '/home/u290212134/domains/pondokquranmahasiswaadtin.com/public_html/privacy_policy.html';

$url = "sftp://{$user}:" . urlencode($pass) . "@{$host}:{$port}{$remotePath}";

echo "Mengunggah privacy_policy.html ke $remotePath...\n";

$ch = curl_init();
$fp = fopen($localFile, 'r');

curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_UPLOAD, true);
curl_setopt($ch, CURLOPT_INFILE, $fp);
curl_setopt($ch, CURLOPT_INFILESIZE, filesize($localFile));
curl_setopt($ch, CURLOPT_SSH_AUTH_TYPES, CURLSSH_AUTH_PASSWORD);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);

$result = curl_exec($ch);
$error = curl_error($ch);
curl_close($ch);
fclose($fp);

if ($result) {
    echo "BERHASIL: File privacy_policy.html dipublikasikan!\n";
    echo "URL: https://pondokquranmahasiswaadtin.com/privacy_policy.html\n";
} else {
    echo "ERROR: $error\n";
}
?>

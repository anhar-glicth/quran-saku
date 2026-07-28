<?php
$user = 'u290212134';
$pass = 'f1D022013.';
$host = '153.92.10.174';
$port = 65002;

$filesToUpload = [
    'c:/xampp/htdocs/quran_android/web/delete_account.html' => '/home/u290212134/domains/pondokquranmahasiswaadtin.com/public_html/delete_account.html',
    'c:/xampp/htdocs/quran_android/web/privacy_policy.html' => '/home/u290212134/domains/pondokquranmahasiswaadtin.com/public_html/privacy_policy.html',
    'c:/xampp/htdocs/quran_android/web/child_safety.html'   => '/home/u290212134/domains/pondokquranmahasiswaadtin.com/public_html/child_safety.html',
];

foreach ($filesToUpload as $localFile => $remotePath) {
    echo "Mengunggah " . basename($localFile) . " ke $remotePath...\n";
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
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 15);

    $result = curl_exec($ch);
    $error = curl_error($ch);
    curl_close($ch);
    fclose($fp);

    if ($result) {
        echo "BERHASIL: " . basename($localFile) . " berhasil dipublikasikan ke Hostinger!\n";
    } else {
        echo "ERROR uploading " . basename($localFile) . ": $error\n";
    }
}
?>

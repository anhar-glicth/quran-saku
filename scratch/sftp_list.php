<?php
$user = 'u290212134';
$pass = 'f1D022013.';
$host = '153.92.10.174';
$port = 65002;

$paths = [
    '/home/',
    '/home/u290212134/',
    '/home/u290212134/domains/',
    '/home/u290212134/domains/pondokquranmahasiswaadtin.com/',
    '/home/u290212134/domains/pondokquranmahasiswaadtin.com/public_html/',
];

foreach ($paths as $path) {
    $url = "sftp://{$user}:" . urlencode($pass) . "@{$host}:{$port}{$path}";
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSH_AUTH_TYPES, CURLSSH_AUTH_PASSWORD);
    $out = curl_exec($ch);
    $err = curl_error($ch);
    curl_close($ch);
    echo "Path [$path]: " . ($out ? "\n" . $out : "Error: $err\n");
}
?>

<?php
$user = 'u290212134';
$pass = 'f1D022013.';
$host = '153.92.10.174';
$port = 65002;

$localWebDir = 'c:/xampp/htdocs/quran_android/web';
$remoteWebDir = '/home/u290212134/domains/pondokquranmahasiswaadtin.com/public_html/web';

function uploadDirSFTP($localDir, $remoteDir, $user, $pass, $host, $port) {
    $files = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($localDir, RecursiveDirectoryIterator::SKIP_DOTS),
        RecursiveIteratorIterator::SELF_FIRST
    );

    foreach ($files as $file) {
        $relativePath = str_replace('\\', '/', substr($file->getPathname(), strlen($localDir) + 1));
        $targetRemote = $remoteDir . '/' . $relativePath;

        if ($file->isDir()) {
            // Create dir via SFTP
            $url = "sftp://{$user}:" . urlencode($pass) . "@{$host}:{$port}{$targetRemote}";
            $ch = curl_init();
            curl_setopt($ch, CURLOPT_URL, $url);
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($ch, CURLOPT_SSH_AUTH_TYPES, CURLSSH_AUTH_PASSWORD);
            @curl_exec($ch);
            curl_close($ch);
        } else {
            // Upload file
            $url = "sftp://{$user}:" . urlencode($pass) . "@{$host}:{$port}{$targetRemote}";
            $ch = curl_init();
            $fp = fopen($file->getPathname(), 'r');
            curl_setopt($ch, CURLOPT_URL, $url);
            curl_setopt($ch, CURLOPT_UPLOAD, true);
            curl_setopt($ch, CURLOPT_INFILE, $fp);
            curl_setopt($ch, CURLOPT_INFILESIZE, filesize($file->getPathname()));
            curl_setopt($ch, CURLOPT_SSH_AUTH_TYPES, CURLSSH_AUTH_PASSWORD);
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
            curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
            $res = curl_exec($ch);
            $err = curl_error($ch);
            curl_close($ch);
            fclose($fp);
            if ($res) {
                echo "Uploaded: $relativePath\n";
            } else {
                echo "Error uploading $relativePath: $err\n";
            }
        }
    }
}

echo "Deploying web files to $remoteWebDir...\n";
uploadDirSFTP($localWebDir, $remoteWebDir, $user, $pass, $host, $port);
echo "Deploy web finished!\n";
?>

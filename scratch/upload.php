<?php
// Test FTP / SSH Upload Script

$host = '153.92.10.174';
$port = 21;
$user = 'u290212134';
$pass = 'f1D022013.';

echo "Connecting to FTP $host:$port...\n";
$conn = @ftp_connect($host, $port, 10);
if ($conn) {
    if (@ftp_login($conn, $user, $pass)) {
        echo "FTP Login SUCCESS!\n";
        ftp_pasv($conn, true);
        
        $localFile = 'c:/xampp/htdocs/quran_android/web/store_listing_assets.html';
        $remoteFile = 'public_html/web/store_listing_assets.html';
        
        if (ftp_put($conn, $remoteFile, $localFile, FTP_BINARY)) {
            echo "Uploaded successfully to $remoteFile!\n";
        } else {
            echo "FTP put failed. Trying alternative remote paths...\n";
            $nlist = ftp_nlist($conn, '.');
            print_r($nlist);
        }
    } else {
        echo "FTP Login Failed.\n";
    }
    ftp_close($conn);
} else {
    echo "FTP Connect Failed.\n";
}

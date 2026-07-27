<?php
$host = '153.92.10.174';
$user = 'u290212134';
$pass = 'f1D022013.';

echo "Mencoba koneksi FTP ke $host...\n";
$conn = @ftp_connect($host, 21, 10);

if (!$conn) {
    echo "Gagal koneksi FTP port 21. Mencoba FTPS (SSL)...\n";
    $conn = @ftp_ssl_connect($host, 21, 10);
}

if (!$conn) {
    die("Error: Tidak dapat terhubung ke FTP server $host\n");
}

echo "Terhubung ke FTP. Melakukan login...\n";
$login = @ftp_login($conn, $user, $pass);

if (!$login) {
    die("Error: Login FTP gagal dengan username $user\n");
}

echo "Login FTP BERHASIL!\n";
ftp_pasv($conn, true);

$pwd = ftp_pwd($conn);
echo "Current directory: $pwd\n";

$nlist = ftp_nlist($conn, ".");
echo "Contents:\n";
print_r($nlist);

// Upload child_safety.html
$localFile = 'c:/xampp/htdocs/quran_android/web/child_safety.html';
$remoteDir = 'public_html';

if (in_array('public_html', $nlist) || in_array('./public_html', $nlist)) {
    $remoteFile = 'public_html/child_safety.html';
} else {
    $remoteFile = 'child_safety.html';
}

echo "Mengunggah $localFile -> $remoteFile...\n";
if (ftp_put($conn, $remoteFile, $localFile, FTP_BINARY)) {
    echo "SUCCESS: $remoteFile berhasil diunggah!\n";
} else {
    echo "ERROR: Gagal mengunggah $remoteFile\n";
}

ftp_close($conn);
?>

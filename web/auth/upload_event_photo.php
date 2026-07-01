<?php
// upload_event_photo.php
// Menerima upload foto event dari aplikasi Android dan menyimpannya ke folder uploads/events/

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'message' => 'Method tidak diizinkan']);
    exit;
}

// Pastikan folder upload ada
$uploadDir = __DIR__ . '/../uploads/events/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

if (!isset($_FILES['photo']) || $_FILES['photo']['error'] !== UPLOAD_ERR_OK) {
    echo json_encode(['success' => false, 'message' => 'Tidak ada file yang diunggah']);
    exit;
}

$file = $_FILES['photo'];

// Validasi tipe file
$allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
$mimeType = mime_content_type($file['tmp_name']);

if (!in_array($mimeType, $allowedTypes)) {
    echo json_encode(['success' => false, 'message' => 'Tipe file tidak didukung. Gunakan JPEG, PNG, GIF atau WebP.']);
    exit;
}

// Validasi ukuran (max 5MB)
if ($file['size'] > 5 * 1024 * 1024) {
    echo json_encode(['success' => false, 'message' => 'Ukuran file melebihi batas 5MB']);
    exit;
}

// Generate nama file unik
$ext = pathinfo($file['name'], PATHINFO_EXTENSION) ?: 'jpg';
$filename = 'event_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
$destPath = $uploadDir . $filename;

if (!move_uploaded_file($file['tmp_name'], $destPath)) {
    echo json_encode(['success' => false, 'message' => 'Gagal menyimpan file']);
    exit;
}

// Build public URL
$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$baseDir = dirname(dirname($_SERVER['SCRIPT_NAME'])); // /quran_android
$publicUrl = $protocol . '://' . $host . $baseDir . '/uploads/events/' . $filename;

echo json_encode([
    'success' => true,
    'url'     => $publicUrl,
    'message' => 'Foto berhasil diunggah'
]);
?>

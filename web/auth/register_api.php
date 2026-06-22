<?php
// ============================================
// Quran Saku - Mobile Register API (tanpa CSRF)
// ============================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/functions.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$name    = sanitize($_POST['name']             ?? '');
$email   = sanitize($_POST['email']            ?? '');
$password = $_POST['password']                 ?? '';
$confirm  = $_POST['password_confirm']         ?? '';

// Validasi
if (empty($name) || empty($email) || empty($password) || empty($confirm)) {
    echo json_encode(['success' => false, 'message' => 'Semua field wajib diisi.']);
    exit;
}
if (strlen($name) < 3) {
    echo json_encode(['success' => false, 'message' => 'Nama minimal 3 karakter.']);
    exit;
}
if (!isValidEmail($email)) {
    echo json_encode(['success' => false, 'message' => 'Format email tidak valid.']);
    exit;
}
if (!isValidPassword($password)) {
    echo json_encode(['success' => false, 'message' => 'Password min 8 karakter, huruf besar + angka.']);
    exit;
}
if ($password !== $confirm) {
    echo json_encode(['success' => false, 'message' => 'Konfirmasi password tidak cocok.']);
    exit;
}

try {
    $db = getDB();

    if (isEmailTaken($email)) {
        echo json_encode(['success' => false, 'message' => 'Email sudah terdaftar.']);
        exit;
    }

    $stmt = $db->prepare("INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'user')");
    $stmt->execute([$name, $email, hashPassword($password)]);
    $newId = (int) $db->lastInsertId();

    $newUser = $db->prepare("SELECT * FROM users WHERE id = ?");
    $newUser->execute([$newId]);
    $user = $newUser->fetch();

    // Update last login
    $db->prepare("UPDATE users SET last_login = NOW() WHERE id = ?")->execute([$newId]);

    echo json_encode([
        'success' => true,
        'message' => 'Registrasi berhasil!',
        'user'    => [
            'id'         => $user['id'],
            'name'       => $user['name'],
            'email'      => $user['email'],
            'role'       => $user['role'],
            'avatar'     => $user['avatar'],
            'created_at' => $user['created_at'],
        ]
    ]);

} catch (Exception $e) {
    error_log($e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Server error.']);
}

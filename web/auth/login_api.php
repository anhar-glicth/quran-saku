<?php
// ============================================
// Quran Saku - Mobile Login API (tanpa CSRF)
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

$email    = sanitize($_POST['email']    ?? '');
$password = $_POST['password']          ?? '';

if (empty($email) || empty($password)) {
    echo json_encode(['success' => false, 'message' => 'Email dan password wajib diisi.']);
    exit;
}

if (!isValidEmail($email)) {
    echo json_encode(['success' => false, 'message' => 'Format email tidak valid.']);
    exit;
}

try {
    $db   = getDB();
    $stmt = $db->prepare("SELECT * FROM users WHERE email = ? LIMIT 1");
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if (!$user || !verifyPassword($password, $user['password'])) {
        echo json_encode(['success' => false, 'message' => 'Email atau password salah.']);
        exit;
    }

    if (!$user['is_active']) {
        echo json_encode(['success' => false, 'message' => 'Akun dinonaktifkan. Hubungi admin.']);
        exit;
    }

    // Update last login
    $db->prepare("UPDATE users SET last_login = NOW() WHERE id = ?")
       ->execute([$user['id']]);

    echo json_encode([
        'success' => true,
        'message' => 'Login berhasil.',
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

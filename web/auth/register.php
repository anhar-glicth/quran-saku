<?php
// ============================================
// Quran Saku - Proses Registrasi
// ============================================

require_once __DIR__ . '/../includes/session.php';
require_once __DIR__ . '/../includes/functions.php';

header('Content-Type: application/json');

// Hanya terima POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method tidak diizinkan.']);
    exit;
}

// Ambil & sanitasi input
$name     = sanitize($_POST['name']           ?? '');
$email    = sanitize($_POST['email']          ?? '');
$password = $_POST['password']                ?? '';
$confirm  = $_POST['password_confirm']        ?? '';
$csrf     = $_POST['csrf_token']              ?? '';

// Validasi CSRF
if (!verifyCsrfToken($csrf)) {
    echo json_encode(['success' => false, 'message' => 'Token keamanan tidak valid. Muat ulang halaman.']);
    exit;
}

// Validasi input kosong
if (empty($name) || empty($email) || empty($password) || empty($confirm)) {
    echo json_encode(['success' => false, 'message' => 'Semua field wajib diisi.']);
    exit;
}

// Validasi nama
if (strlen($name) < 3 || strlen($name) > 100) {
    echo json_encode(['success' => false, 'message' => 'Nama harus antara 3-100 karakter.']);
    exit;
}

// Validasi email
if (!isValidEmail($email)) {
    echo json_encode(['success' => false, 'message' => 'Format email tidak valid.']);
    exit;
}

// Validasi password
if (!isValidPassword($password)) {
    echo json_encode([
        'success' => false,
        'message' => 'Password minimal 8 karakter, mengandung huruf besar, huruf kecil, dan angka.',
    ]);
    exit;
}

// Validasi konfirmasi password
if ($password !== $confirm) {
    echo json_encode(['success' => false, 'message' => 'Konfirmasi password tidak cocok.']);
    exit;
}

try {
    $db = getDB();

    // Cek email sudah dipakai
    if (isEmailTaken($email)) {
        echo json_encode(['success' => false, 'message' => 'Email sudah terdaftar. Gunakan email lain atau login.']);
        exit;
    }

    // Hash password & simpan user
    $hashedPassword = hashPassword($password);
    $stmt = $db->prepare(
        "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'user')"
    );
    $stmt->execute([$name, $email, $hashedPassword]);
    $newUserId = (int) $db->lastInsertId();

    // Log aktivitas
    logActivity($newUserId, 'register', "Akun baru terdaftar: $email");

    // Auto login setelah register
    $userStmt = $db->prepare("SELECT * FROM users WHERE id = ?");
    $userStmt->execute([$newUserId]);
    $newUser = $userStmt->fetch();
    setUserSession($newUser);

    // Update last login
    $db->prepare("UPDATE users SET last_login = NOW() WHERE id = ?")
       ->execute([$newUserId]);

    echo json_encode([
        'success'  => true,
        'message'  => 'Registrasi berhasil! Selamat datang di Strava Quran, ' . htmlspecialchars($name) . '.',
        'redirect' => APP_URL . '/user/dashboard.php',
    ]);

} catch (Exception $e) {
    error_log('Register error: ' . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Terjadi kesalahan server. Coba lagi.']);
}

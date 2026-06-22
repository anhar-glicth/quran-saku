<?php
// ============================================
// Quran Saku - Proses Login
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
$email    = sanitize($_POST['email']    ?? '');
$password = $_POST['password']          ?? '';
$csrf     = $_POST['csrf_token']        ?? '';
$remember = isset($_POST['remember']);

// Validasi CSRF
if (!verifyCsrfToken($csrf)) {
    echo json_encode(['success' => false, 'message' => 'Token keamanan tidak valid. Muat ulang halaman.']);
    exit;
}

// Validasi input kosong
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

    // Cari user berdasarkan email
    $stmt = $db->prepare("SELECT * FROM users WHERE email = ? LIMIT 1");
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    // Cek user ada & password cocok & aktif
    if (!$user || !verifyPassword($password, $user['password'])) {
        logActivity(null, 'login_failed', "Percobaan login gagal untuk email: $email");
        echo json_encode(['success' => false, 'message' => 'Email atau password salah.']);
        exit;
    }

    if (!$user['is_active']) {
        echo json_encode(['success' => false, 'message' => 'Akun Anda telah dinonaktifkan. Hubungi admin.']);
        exit;
    }

    // Set session
    setUserSession($user);

    // Update last login
    $db->prepare("UPDATE users SET last_login = NOW() WHERE id = ?")
       ->execute([$user['id']]);

    // Log aktivitas
    logActivity($user['id'], 'login_success', 'Login berhasil dari ' . ($_SERVER['REMOTE_ADDR'] ?? 'unknown'));

    // Tentukan redirect berdasarkan role
    // User biasa → onboarding dulu (jika belum pernah)
    if ($user['role'] === 'admin') {
        $redirectUrl = APP_URL . '/admin/dashboard.php';
    } else {
        // Cek apakah session onboarding sudah selesai
        $redirectUrl = empty($_SESSION['onboarding_done'])
            ? APP_URL . '/user/onboarding.php'
            : APP_URL . '/user/dashboard.php';
    }

    echo json_encode([
        'success'  => true,
        'message'  => 'Login berhasil! Selamat datang, ' . htmlspecialchars($user['name']) . '.',
        'redirect' => $redirectUrl,
        'role'     => $user['role'],
    ]);

} catch (Exception $e) {
    error_log('Login error: ' . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Terjadi kesalahan server. Coba lagi.']);
}

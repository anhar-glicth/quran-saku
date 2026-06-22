<?php
// ============================================
// Quran Saku - Session & Auth Helper
// ============================================

require_once __DIR__ . '/../config/database.php';

session_set_cookie_params([
    'lifetime' => SESSION_LIFETIME,
    'path'     => '/',
    'secure'   => false,
    'httponly' => true,
    'samesite' => 'Lax',
]);

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

/**
 * Cek apakah user sudah login
 */
function isLoggedIn(): bool {
    return isset($_SESSION['user_id']) && !empty($_SESSION['user_id']);
}

/**
 * Cek apakah user adalah admin
 */
function isAdmin(): bool {
    return isLoggedIn() && isset($_SESSION['role']) && $_SESSION['role'] === 'admin';
}

/**
 * Ambil data user yang sedang login
 */
function getCurrentUser(): ?array {
    if (!isLoggedIn()) return null;
    try {
        $db   = getDB();
        $stmt = $db->prepare("SELECT id, name, email, role, avatar, last_login, created_at FROM users WHERE id = ? AND is_active = 1");
        $stmt->execute([$_SESSION['user_id']]);
        return $stmt->fetch() ?: null;
    } catch (Exception $e) {
        return null;
    }
}

/**
 * Redirect ke halaman tertentu
 */
function redirect(string $url): void {
    header("Location: $url");
    exit;
}

/**
 * Wajib login - redirect jika belum login
 */
function requireLogin(): void {
    if (!isLoggedIn()) {
        redirect(APP_URL . '/index.php?msg=login_required');
    }
}

/**
 * Wajib admin - redirect jika bukan admin
 */
function requireAdmin(): void {
    requireLogin();
    if (!isAdmin()) {
        redirect(APP_URL . '/user/dashboard.php?msg=access_denied');
    }
}

/**
 * Set session user setelah login berhasil
 */
function setUserSession(array $user): void {
    session_regenerate_id(true);
    $_SESSION['user_id'] = $user['id'];
    $_SESSION['name']    = $user['name'];
    $_SESSION['email']   = $user['email'];
    $_SESSION['role']    = $user['role'];
    $_SESSION['avatar']  = $user['avatar'];
}

/**
 * Hapus session (logout)
 */
function destroySession(): void {
    $_SESSION = [];
    if (ini_get("session.use_cookies")) {
        $p = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000, $p['path'], $p['domain'], $p['secure'], $p['httponly']);
    }
    session_destroy();
}

/**
 * Log aktivitas user
 */
function logActivity(int $userId = null, string $action = '', string $description = ''): void {
    try {
        $db   = getDB();
        $stmt = $db->prepare("INSERT INTO activity_log (user_id, action, description, ip_address) VALUES (?, ?, ?, ?)");
        $stmt->execute([$userId, $action, $description, $_SERVER['REMOTE_ADDR'] ?? null]);
    } catch (Exception $e) {
        // silent fail
    }
}

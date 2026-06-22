<?php
// ============================================
// Quran Saku - Functions Helper
// ============================================

/**
 * Sanitasi input dari user
 */
function sanitize(string $input): string {
    return htmlspecialchars(strip_tags(trim($input)), ENT_QUOTES, 'UTF-8');
}

/**
 * Validasi format email
 */
function isValidEmail(string $email): bool {
    return filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
}

/**
 * Validasi kekuatan password
 * Min 8 karakter, ada huruf besar, huruf kecil, angka
 */
function isValidPassword(string $password): bool {
    return strlen($password) >= 8
        && preg_match('/[A-Z]/', $password)
        && preg_match('/[a-z]/', $password)
        && preg_match('/[0-9]/', $password);
}

/**
 * Hash password
 */
function hashPassword(string $password): string {
    return password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);
}

/**
 * Verifikasi password
 */
function verifyPassword(string $password, string $hash): bool {
    return password_verify($password, $hash);
}

/**
 * Generate avatar inisial nama
 */
function getAvatarInitials(string $name): string {
    $words    = explode(' ', trim($name));
    $initials = '';
    foreach (array_slice($words, 0, 2) as $word) {
        $initials .= strtoupper(mb_substr($word, 0, 1));
    }
    return $initials ?: 'U';
}

/**
 * Format tanggal ke bahasa Indonesia
 */
function formatDateID(string $date): string {
    $bulan = [
        '01' => 'Januari', '02' => 'Februari', '03' => 'Maret',
        '04' => 'April',   '05' => 'Mei',       '06' => 'Juni',
        '07' => 'Juli',    '08' => 'Agustus',   '09' => 'September',
        '10' => 'Oktober', '11' => 'November',  '12' => 'Desember',
    ];
    $ts  = strtotime($date);
    $m   = date('m', $ts);
    return date('d', $ts) . ' ' . $bulan[$m] . ' ' . date('Y', $ts);
}

/**
 * Generate CSRF token
 */
function generateCsrfToken(): string {
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}

/**
 * Verifikasi CSRF token
 */
function verifyCsrfToken(string $token): bool {
    return isset($_SESSION['csrf_token']) && hash_equals($_SESSION['csrf_token'], $token);
}

/**
 * Flash message setter
 */
function setFlash(string $type, string $message): void {
    $_SESSION['flash'] = ['type' => $type, 'message' => $message];
}

/**
 * Flash message getter & hapus setelah dibaca
 */
function getFlash(): ?array {
    if (!empty($_SESSION['flash'])) {
        $flash = $_SESSION['flash'];
        unset($_SESSION['flash']);
        return $flash;
    }
    return null;
}

/**
 * Ambil total bookmark user
 */
function getUserBookmarkCount(int $userId): int {
    try {
        $db   = getDB();
        $stmt = $db->prepare("SELECT COUNT(*) FROM bookmarks WHERE user_id = ?");
        $stmt->execute([$userId]);
        return (int) $stmt->fetchColumn();
    } catch (Exception $e) {
        return 0;
    }
}

/**
 * Ambil progress terakhir membaca user
 */
function getLastReadingProgress(int $userId): ?array {
    try {
        $db   = getDB();
        $stmt = $db->prepare("SELECT * FROM reading_progress WHERE user_id = ? ORDER BY last_read DESC LIMIT 1");
        $stmt->execute([$userId]);
        return $stmt->fetch() ?: null;
    } catch (Exception $e) {
        return null;
    }
}

/**
 * Cek apakah email sudah terdaftar
 */
function isEmailTaken(string $email): bool {
    try {
        $db   = getDB();
        $stmt = $db->prepare("SELECT id FROM users WHERE email = ?");
        $stmt->execute([$email]);
        return $stmt->fetch() !== false;
    } catch (Exception $e) {
        return false;
    }
}

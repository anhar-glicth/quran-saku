<?php
// ============================================
// Quran Saku - Konfigurasi Database
// ============================================

define('DB_HOST',     'localhost');
define('DB_PORT',     '3306');
define('DB_NAME',     'quran_saku');
define('DB_USER',     'root');
define('DB_PASS',     '');
define('DB_CHARSET',  'utf8mb4');

// App Settings
define('APP_NAME',    'Quran Saku');
define('APP_URL',     'http://localhost/quran_android/web');
define('APP_VERSION', '1.0.0');
define('SESSION_LIFETIME', 86400); // 24 jam

/**
 * Buat koneksi PDO ke database
 */
function getDB(): PDO {
    static $pdo = null;
    if ($pdo === null) {
        $dsn = sprintf(
            'mysql:host=%s;port=%s;dbname=%s;charset=%s',
            DB_HOST, DB_PORT, DB_NAME, DB_CHARSET
        );
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ];
        try {
            $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        } catch (PDOException $e) {
            http_response_code(500);
            die(json_encode([
                'success' => false,
                'message' => 'Koneksi database gagal: ' . $e->getMessage()
            ]));
        }
    }
    return $pdo;
}

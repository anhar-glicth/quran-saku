<?php
// ============================================
// Quran Saku - Auto Setup Database
// Jalankan SEKALI: http://localhost/quran_android/web/setup.php
// ============================================

define('DB_HOST',    'localhost');
define('DB_USER',    'u290212134_quran_Saku');
define('DB_PASS',    'f1D022013.');
define('DB_CHARSET', 'utf8mb4');
define('DB_NAME',    'u290212134_quran_Saku');

$errors   = [];
$success  = [];

try {
    // Koneksi tanpa database dulu
    $pdo = new PDO(
        "mysql:host=" . DB_HOST . ";charset=" . DB_CHARSET,
        DB_USER, DB_PASS,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );

    // Buat database
    $pdo->exec("CREATE DATABASE IF NOT EXISTS `" . DB_NAME . "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    $pdo->exec("USE `" . DB_NAME . "`");
    $success[] = "Database '" . DB_NAME . "' berhasil dibuat / sudah ada.";

    // Buat tabel users
    $pdo->exec("CREATE TABLE IF NOT EXISTS `users` (
        `id`          INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
        `name`        VARCHAR(100) NOT NULL,
        `email`       VARCHAR(150) NOT NULL UNIQUE,
        `password`    VARCHAR(255) NOT NULL,
        `role`        ENUM('user','admin') NOT NULL DEFAULT 'user',
        `avatar`      VARCHAR(255) DEFAULT NULL,
        `is_active`   TINYINT(1) NOT NULL DEFAULT 1,
        `last_login`  DATETIME DEFAULT NULL,
        `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        INDEX `idx_email` (`email`),
        INDEX `idx_role` (`role`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    $success[] = "Tabel <code>users</code> siap.";

    // Buat tabel bookmarks
    $pdo->exec("CREATE TABLE IF NOT EXISTS `bookmarks` (
        `id`           INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
        `user_id`      INT(11) UNSIGNED NOT NULL,
        `surah_number` INT(3) NOT NULL,
        `surah_name`   VARCHAR(100) NOT NULL,
        `ayah_number`  INT(3) NOT NULL,
        `note`         TEXT DEFAULT NULL,
        `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
        INDEX `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    $success[] = "Tabel <code>bookmarks</code> siap.";

    // Buat tabel reading_progress
    $pdo->exec("CREATE TABLE IF NOT EXISTS `reading_progress` (
        `id`           INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
        `user_id`      INT(11) UNSIGNED NOT NULL,
        `surah_number` INT(3) NOT NULL,
        `surah_name`   VARCHAR(100) NOT NULL,
        `ayah_number`  INT(3) NOT NULL,
        `page_number`  INT(3) DEFAULT NULL,
        `last_read`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        UNIQUE KEY `unique_user_surah` (`user_id`, `surah_number`),
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    $success[] = "Tabel <code>reading_progress</code> siap.";

    // Buat tabel activity_log
    $pdo->exec("CREATE TABLE IF NOT EXISTS `activity_log` (
        `id`         INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
        `user_id`    INT(11) UNSIGNED DEFAULT NULL,
        `action`     VARCHAR(100) NOT NULL,
        `description` TEXT DEFAULT NULL,
        `ip_address` VARCHAR(45) DEFAULT NULL,
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
        INDEX `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    $success[] = "Tabel <code>activity_log</code> siap.";

    // Seed admin default jika belum ada
    $check = $pdo->query("SELECT id FROM users WHERE email = 'admin@quransaku.com'")->fetch();
    if (!$check) {
        $hashAdmin = password_hash('Admin@123', PASSWORD_BCRYPT, ['cost' => 12]);
        $pdo->prepare("INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'admin')")
            ->execute(['Administrator', 'admin@quransaku.com', $hashAdmin]);
        $success[] = "Akun Admin default dibuat: <strong>admin@quransaku.com</strong> / <strong>Admin@123</strong>";
    } else {
        $success[] = "Akun Admin sudah ada, skip seeding.";
    }

    // Seed demo user jika belum ada
    $checkUser = $pdo->query("SELECT id FROM users WHERE email = 'user@quransaku.com'")->fetch();
    if (!$checkUser) {
        $hashUser = password_hash('User@123', PASSWORD_BCRYPT, ['cost' => 12]);
        $pdo->prepare("INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'user')")
            ->execute(['Demo User', 'user@quransaku.com', $hashUser]);
        $success[] = "Akun Demo User dibuat: <strong>user@quransaku.com</strong> / <strong>User@123</strong>";
    } else {
        $success[] = "Akun Demo User sudah ada, skip seeding.";
    }

} catch (PDOException $e) {
    $errors[] = "Koneksi database gagal: " . htmlspecialchars($e->getMessage());
} catch (Exception $e) {
    $errors[] = "Error: " . htmlspecialchars($e->getMessage());
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quran Saku - Setup Database</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
            background: #FAF9F6;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .card {
            background: #fff;
            border-radius: 16px;
            padding: 40px;
            max-width: 580px;
            width: 100%;
            box-shadow: 0 4px 30px rgba(255, 152, 0, 0.1);
            border: 1px solid rgba(255, 152, 0, 0.1);
        }
        .logo {
            font-size: 28px;
            font-weight: 800;
            color: #FF9800;
            text-align: center;
            margin-bottom: 8px;
        }
        .logo span { color: #2D3748; }
        h2 {
            text-align: center;
            font-size: 16px;
            font-weight: 600;
            color: #718096;
            margin-bottom: 30px;
        }
        .item {
            display: flex;
            align-items: flex-start;
            gap: 12px;
            padding: 12px 0;
            border-bottom: 1px solid #F7FAFC;
            font-size: 14px;
            color: #2D3748;
        }
        .item i.success { color: #38A169; }
        .item i.error   { color: #E53E3E; }
        .actions {
            display: flex;
            gap: 12px;
            margin-top: 30px;
        }
        a.btn {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            flex: 1;
            padding: 13px;
            border-radius: 12px;
            font-size: 14px;
            font-weight: 700;
            text-decoration: none;
            transition: all 0.25s ease;
        }
        .btn-primary {
            background-color: #FF9800;
            color: #fff;
            box-shadow: 0 4px 6px rgba(255, 152, 0, 0.2);
        }
        .btn-primary:hover { background-color: #F57C00; }
        .btn-secondary {
            background-color: #F7FAFC;
            color: #2D3748;
            border: 1px solid #E2E8F0;
        }
        .btn-secondary:hover { background-color: #E2E8F0; }
        .warning {
            margin-top: 20px;
            padding: 12px 16px;
            border-radius: 10px;
            background-color: #FFFBEB;
            color: #92400E;
            border: 1px solid #FDE68A;
            font-size: 13px;
            display: flex;
            gap: 10px;
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="logo">Quran <span>saku</span></div>
        <h2>Setup & Inisialisasi Database</h2>

        <?php foreach ($success as $msg): ?>
            <div class="item"><i class="fa-solid fa-circle-check success"></i> <span><?php echo $msg; ?></span></div>
        <?php endforeach; ?>

        <?php foreach ($errors as $msg): ?>
            <div class="item"><i class="fa-solid fa-circle-xmark error"></i> <span><?php echo $msg; ?></span></div>
        <?php endforeach; ?>

        <?php if (empty($errors)): ?>
            <div class="actions">
                <a href="index.php" class="btn btn-primary"><i class="fa-solid fa-right-to-bracket"></i> Ke Halaman Login</a>
                <a href="admin/dashboard.php" class="btn btn-secondary"><i class="fa-solid fa-gauge-high"></i> Admin Panel</a>
            </div>
            <div class="warning">
                <i class="fa-solid fa-triangle-exclamation"></i>
                <span><strong>Penting:</strong> Hapus file <code>setup.php</code> setelah instalasi berhasil untuk keamanan.</span>
            </div>
        <?php else: ?>
            <div class="actions">
                <a href="setup.php" class="btn btn-primary"><i class="fa-solid fa-rotate-right"></i> Coba Lagi</a>
            </div>
        <?php endif; ?>
    </div>
</body>
</html>

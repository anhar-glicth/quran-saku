-- =============================================
-- Migration: Tambah tabel Grup Ngaji
-- Jalankan di phpMyAdmin atau MySQL CLI
-- =============================================

USE `quran_saku`;

-- Tabel Grup Ngaji
CREATE TABLE IF NOT EXISTS `ngaji_groups` (
  `id`              INT(11) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `group_code`      VARCHAR(8) UNIQUE NOT NULL,  -- kode unik 8 char untuk join
  `name`            VARCHAR(100) NOT NULL,
  `photo_url`       VARCHAR(255) DEFAULT NULL,
  `creator_id`      INT(11) UNSIGNED NOT NULL,   -- admin grup
  `target_khatam`   INT(11) NOT NULL DEFAULT 1,  -- target berapa kali khatam
  `duration_days`   INT(11) NOT NULL DEFAULT 30, -- durasi dalam hari
  `start_date`      DATE NOT NULL,
  `end_date`        DATE NOT NULL,
  `is_active`       TINYINT(1) DEFAULT 1,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`creator_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabel Anggota Grup
CREATE TABLE IF NOT EXISTS `ngaji_group_members` (
  `id`          INT(11) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `group_id`    INT(11) UNSIGNED NOT NULL,
  `user_id`     INT(11) UNSIGNED NOT NULL,
  `role`        ENUM('admin','member') DEFAULT 'member',
  `status`      ENUM('pending','active','rejected') DEFAULT 'pending',
  `joined_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `unique_member` (`group_id`, `user_id`),
  FOREIGN KEY (`group_id`) REFERENCES `ngaji_groups`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`)  REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabel Relay Bacaan (siapa terakhir baca sampai mana)
CREATE TABLE IF NOT EXISTS `ngaji_reading_relay` (
  `id`            INT(11) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `group_id`      INT(11) UNSIGNED NOT NULL,
  `user_id`       INT(11) UNSIGNED NOT NULL,
  `surah_number`  INT(11) NOT NULL,
  `surah_name`    VARCHAR(100),
  `ayah_number`   INT(11) NOT NULL,
  `page_number`   INT(11) DEFAULT NULL,
  `read_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`group_id`) REFERENCES `ngaji_groups`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`)  REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

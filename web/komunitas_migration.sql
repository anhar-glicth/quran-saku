-- =============================================
-- Migration: Tambah tabel Komunitas
-- Jalankan di phpMyAdmin atau MySQL CLI
-- =============================================

USE `quran_saku`;

-- =====================
-- Tabel: community_prayers (Titip Doa)
-- =====================
CREATE TABLE IF NOT EXISTS `community_prayers` (
  `id`            INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       INT(11) UNSIGNED NOT NULL,
  `arabic_text`   TEXT             DEFAULT NULL,
  `latin_text`    TEXT             NOT NULL,
  `like_count`    INT(11)          NOT NULL DEFAULT 0,
  `aamiin_count`  INT(11)          NOT NULL DEFAULT 0,
  `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Tabel: prayer_reactions (Like & Aamiin)
-- =====================
CREATE TABLE IF NOT EXISTS `prayer_reactions` (
  `id`              INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`         INT(11) UNSIGNED NOT NULL,
  `prayer_id`       INT(11) UNSIGNED NOT NULL,
  `reaction_type`   ENUM('like','aamiin') NOT NULL,
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_prayer_reaction` (`user_id`, `prayer_id`, `reaction_type`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`prayer_id`) REFERENCES `community_prayers`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Tabel: strava_activities (Jika belum ada dari sebelumnya)
-- =====================
CREATE TABLE IF NOT EXISTS `strava_activities` (
  `id`               INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`          INT(11) UNSIGNED NOT NULL,
  `activity_date`    DATE NOT NULL,
  `duration_seconds` INT(11) NOT NULL DEFAULT 0,
  `pages_count`      INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_date` (`user_id`, `activity_date`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

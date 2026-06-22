-- ============================================
-- Quran Saku - Database Schema
-- ============================================

CREATE DATABASE IF NOT EXISTS `quran_saku` 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `quran_saku`;

-- =====================
-- Tabel: users
-- =====================
CREATE TABLE IF NOT EXISTS `users` (
  `id`            INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(100)     NOT NULL,
  `email`         VARCHAR(150)     NOT NULL UNIQUE,
  `password`      VARCHAR(255)     NOT NULL,
  `role`          ENUM('user','admin') NOT NULL DEFAULT 'user',
  `avatar`        VARCHAR(255)     DEFAULT NULL,
  `is_active`     TINYINT(1)       NOT NULL DEFAULT 1,
  `last_login`    DATETIME         DEFAULT NULL,
  `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_email` (`email`),
  INDEX `idx_role`  (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Tabel: user_sessions
-- =====================
CREATE TABLE IF NOT EXISTS `user_sessions` (
  `id`            INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       INT(11) UNSIGNED NOT NULL,
  `session_token` VARCHAR(255)     NOT NULL UNIQUE,
  `ip_address`    VARCHAR(45)      DEFAULT NULL,
  `user_agent`    TEXT             DEFAULT NULL,
  `expires_at`    DATETIME         NOT NULL,
  `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  INDEX `idx_token` (`session_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Tabel: bookmarks
-- =====================
CREATE TABLE IF NOT EXISTS `bookmarks` (
  `id`            INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       INT(11) UNSIGNED NOT NULL,
  `surah_number`  INT(3)           NOT NULL,
  `surah_name`    VARCHAR(100)     NOT NULL,
  `ayah_number`   INT(3)           NOT NULL,
  `note`          TEXT             DEFAULT NULL,
  `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Tabel: reading_progress
-- =====================
CREATE TABLE IF NOT EXISTS `reading_progress` (
  `id`            INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       INT(11) UNSIGNED NOT NULL,
  `surah_number`  INT(3)           NOT NULL,
  `surah_name`    VARCHAR(100)     NOT NULL,
  `ayah_number`   INT(3)           NOT NULL,
  `page_number`   INT(3)           DEFAULT NULL,
  `last_read`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_surah` (`user_id`, `surah_number`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Tabel: activity_log
-- =====================
CREATE TABLE IF NOT EXISTS `activity_log` (
  `id`            INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       INT(11) UNSIGNED DEFAULT NULL,
  `action`        VARCHAR(100)     NOT NULL,
  `description`   TEXT             DEFAULT NULL,
  `ip_address`    VARCHAR(45)      DEFAULT NULL,
  `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Default Admin Account
-- Password: Admin@123
-- ============================================
INSERT INTO `users` (`name`, `email`, `password`, `role`) VALUES
('Administrator', 'admin@quransaku.com', '$2y$12$YKqfVFLBiOKOBFDFYCRITuZpQbqxGmqNFLWbfNJw6Sne8QkSiakQy', 'admin');

-- ============================================
-- Default User Demo Account  
-- Password: User@123
-- ============================================
INSERT INTO `users` (`name`, `email`, `password`, `role`) VALUES
('Demo User', 'user@quransaku.com', '$2y$12$TgY4Iy5ZfBiT3lCn6UB1MuiE6LHl47pElV0GZRPA25MH6XMeYTBKi', 'user');

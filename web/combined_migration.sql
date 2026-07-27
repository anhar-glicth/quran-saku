-- ============================================================
-- SQL MIGRATION LENGKAP: DONASI, EVENT, CAMPAIGN & GRUP NGAJI
-- ============================================================

-- 1. TABEL: DONATIONS (Live Ticker Donasi)
CREATE TABLE IF NOT EXISTS `donations` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_name` VARCHAR(100) NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `campaign_title` VARCHAR(150) DEFAULT 'Donasi Umum',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sampel Data Donasi Awal
INSERT INTO `donations` (`user_name`, `amount`, `campaign_title`, `created_at`) VALUES
('Ahmad Fauzi', 100000.00, 'Sedekah Mushaf Al-Qur\'an', NOW() - INTERVAL 5 MINUTE),
('Hamba Allah', 50000.00, 'Infaq Operasional Dakwah', NOW() - INTERVAL 15 MINUTE),
('Siti Nurhaliza', 250000.00, 'Program Beasiswa Tahfidz', NOW() - INTERVAL 45 MINUTE),
('Rahmat Hidayat', 75000.00, 'Zakat Maal', NOW() - INTERVAL 2 HOUR),
('Budi Santoso', 150000.00, 'Sedekah Subuh Pejuang Quran', NOW() - INTERVAL 4 HOUR);

-- 2. TABEL: CAMPAIGNS (Campaign Donasi)
CREATE TABLE IF NOT EXISTS `campaigns` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(150) NOT NULL,
  `description` TEXT NOT NULL,
  `image_url` VARCHAR(255) DEFAULT '',
  `donate_url` VARCHAR(255) DEFAULT '',
  `target_amount` DECIMAL(12,2) DEFAULT 0.00,
  `collected_amount` DECIMAL(12,2) DEFAULT 0.00,
  `is_active` TINYINT(1) DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. TABEL: PARTNERS (Pejuang Kebaikan)
CREATE TABLE IF NOT EXISTS `partners` (
  `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `category_id` VARCHAR(50) NOT NULL,
  `logo_text` VARCHAR(10) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT NOT NULL,
  `bg_color` VARCHAR(10) NOT NULL DEFAULT '#E0F2F1',
  `text_color` VARCHAR(10) NOT NULL DEFAULT '#004D40',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Partners (jika belum ada)
INSERT INTO `partners` (`category_id`, `logo_text`, `name`, `description`, `bg_color`, `text_color`) VALUES
('mitra_utama', 'YPQ', 'Yayasan Pejuang Quran', 'Penyedia utama program distribusi mushaf digital.', '#E0F2F1', '#004D40'),
('mitra_utama', 'RZ', 'Rumah Zakat', 'Mendukung penyebaran program dakwah Al-Quran di pedalaman.', '#FFF3E0', '#E65100'),
('mitra_utama', 'DD', 'Dompet Dhuafa', 'Fasilitator pendistribusian program Al-Quran untuk kaum dhuafa.', '#E8F5E9', '#1B5E20'),
('pendukung_resmi', 'BMM', 'Baitul Maal Muamalat', 'Donatur utama penyediaan sarana dan operasional server.', '#FFE0B2', '#E65100'),
('pendukung_resmi', 'LMS', 'Lazismu', 'Penyokong finansial program dakwah Al-Quran Saku.', '#F3E5F5', '#4A148C'),
('pendukung_resmi', 'LSN', 'LAZISNU', 'Mitra resmi pendanaan dan penyebaran program syiar Quran.', '#E0F7FA', '#006064'),
('mitra_distribusi', 'MAI', 'Masjid Istiqlal', 'Penyalur resmi program aplikasi langsung ke jamaah Masjid Istiqlal.', '#E8EAF6', '#1A237E'),
('mitra_distribusi', 'MAA', 'Masjid Agung Al-Azhar', 'Penyalur program kajian dan aplikasi ke jamaah sekolah & masjid.', '#FCE4EC', '#880E4F'),
('mitra_distribusi', 'DMI', 'Dewan Masjid Indonesia', 'Jaringan distribusi aplikasi untuk takmir masjid nasional.', '#FFFDE7', '#F57F17'),
('mitra_edukasi', 'PPDQ', 'Daarul Qur\'an', 'Mitra pembinaan hafalan, tilawah, dan pemahaman ayat santri.', '#FCE4EC', '#880E4F'),
('mitra_edukasi', 'CQF', 'Cinta Quran Foundation', 'Penyedia materi edukasi dakwah dan pembinaan baca tulis Quran.', '#E0F2F1', '#004D40'),
('mitra_edukasi', 'RTI', 'Rumah Tahfidz Indonesia', 'Penyelenggara bimbingan tahfidz Quran berbasis kurikulum digital.', '#FFF3E0', '#E65100')
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 4. TABEL: EVENTS & REGISTRATIONS (Kalender & Event)
CREATE TABLE IF NOT EXISTS `events` (
  `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(150) NOT NULL,
  `category` VARCHAR(50) NOT NULL,
  `description` TEXT NOT NULL,
  `event_date` DATE NOT NULL,
  `time_range` VARCHAR(50) NOT NULL,
  `speaker` VARCHAR(100) NOT NULL,
  `location` VARCHAR(150) NOT NULL DEFAULT 'Online Zoom',
  `is_featured` TINYINT(1) NOT NULL DEFAULT 0,
  `image_url` VARCHAR(255) DEFAULT '',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Events
INSERT INTO `events` (`title`, `category`, `description`, `event_date`, `time_range`, `speaker`, `location`, `is_featured`) VALUES
('Meaningful Life in Islam', 'Kajian', 'Kajian mendalam tentang cara menemukan arti dan kedamaian hidup dalam naungan syariat Islam di era modern.', CURDATE(), '09:00 - 11:30 WIB', 'Ustadz Adi Hidayat', 'Masjid Istiqlal / Online Zoom', 1),
('Youth Spiritual Circle', 'Webinar', 'Diskusi interaktif pemuda muslim seputar tantangan iman, pergaulan sehat, dan tips menjaga istiqamah di sekolah/kampus.', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '19:30 - 21:00 WIB', 'Ustadz Hanan Attaki', 'Online Zoom Meeting', 0),
('Social Care Weekend', 'Sosial', 'Aksi sosial pembagian mushaf Al-Qur\'an gratis dan paket sembako untuk masyarakat pra-sejahtera di bantaran sungai.', DATE_ADD(CURDATE(), INTERVAL 4 DAY), '08:00 - 12:00 WIB', 'Tim Relawan Quran Saku', 'Kawasan Manggarai, Jakarta Selatan', 0),
('Fiqh of Daily Life', 'Workshop', 'Workshop praktis membahas hukum-hukum fiqh ibadah harian, muamalah jual beli online, dan tanya jawab kasus sehari-hari.', DATE_ADD(CURDATE(), INTERVAL 6 DAY), '13:00 - 15:30 WIB', 'Ustadz Abdul Somad', 'Aula Rabbani Bandung', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

CREATE TABLE IF NOT EXISTS `event_registrations` (
  `id` INT(11) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `event_id` INT(11) UNSIGNED NOT NULL,
  `user_id` INT(11) UNSIGNED DEFAULT NULL,
  `name` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `notes` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`event_id`) REFERENCES `events`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. TABEL: NGAJI_GROUPS & MEMBERS (Fitur Grup Ngaji)
CREATE TABLE IF NOT EXISTS `ngaji_groups` (
  `id` INT(11) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `group_code` VARCHAR(8) UNIQUE NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `photo_url` VARCHAR(255) DEFAULT NULL,
  `creator_id` INT(11) UNSIGNED NOT NULL,
  `admin_user_id` INT(11) UNSIGNED DEFAULT NULL,
  `target_khatam` INT(11) NOT NULL DEFAULT 1,
  `duration_days` INT(11) NOT NULL DEFAULT 30,
  `current_page` INT(11) NOT NULL DEFAULT 1,
  `start_date` DATE DEFAULT NULL,
  `end_date` DATE DEFAULT NULL,
  `is_active` TINYINT(1) DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`creator_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ngaji_group_members` (
  `id` INT(11) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `group_id` INT(11) UNSIGNED NOT NULL,
  `user_id` INT(11) UNSIGNED NOT NULL,
  `role` ENUM('admin','member') DEFAULT 'member',
  `status` ENUM('pending','active','rejected') DEFAULT 'pending',
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `unique_member` (`group_id`, `user_id`),
  FOREIGN KEY (`group_id`) REFERENCES `ngaji_groups`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ngaji_reading_relay` (
  `id` INT(11) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `group_id` INT(11) UNSIGNED NOT NULL,
  `user_id` INT(11) UNSIGNED NOT NULL,
  `surah_number` INT(11) NOT NULL,
  `surah_name` VARCHAR(100),
  `ayah_number` INT(11) NOT NULL,
  `page_number` INT(11) DEFAULT NULL,
  `read_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`group_id`) REFERENCES `ngaji_groups`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

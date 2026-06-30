-- =============================================
-- Migration V2: Tambah tabel Mitra, Event, dan Grup Ngaji
-- =============================================

USE `quran_saku`;

-- 1. Tabel: partners (Pejuang Kebaikan)
CREATE TABLE IF NOT EXISTS `partners` (
  `id`           INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id`  VARCHAR(50) NOT NULL, -- 'mitra_utama', 'pendukung_resmi', 'mitra_distribusi', 'mitra_edukasi'
  `logo_text`    VARCHAR(10) NOT NULL,
  `name`         VARCHAR(100) NOT NULL,
  `description`  TEXT NOT NULL,
  `bg_color`     VARCHAR(10) NOT NULL DEFAULT '#E0F2F1',
  `text_color`    VARCHAR(10) NOT NULL DEFAULT '#004D40',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabel: events (Event & Kalender)
CREATE TABLE IF NOT EXISTS `events` (
  `id`           INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`        VARCHAR(150) NOT NULL,
  `category`     VARCHAR(50) NOT NULL, -- 'Webinar', 'Kajian', 'Workshop', 'Sosial'
  `description`  TEXT NOT NULL,
  `event_date`   DATE NOT NULL, -- YYYY-MM-DD
  `time_range`   VARCHAR(50) NOT NULL, -- e.g., '09:00 - 11:30 WIB'
  `speaker`      VARCHAR(100) NOT NULL,
  `location`     VARCHAR(150) NOT NULL DEFAULT 'Online Zoom',
  `is_featured`  TINYINT(1) NOT NULL DEFAULT 0,
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tabel: ngaji_groups (Grup Ngaji)
CREATE TABLE IF NOT EXISTS `ngaji_groups` (
  `id`              INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `group_code`      VARCHAR(8) NOT NULL UNIQUE,
  `name`            VARCHAR(100) NOT NULL,
  `description`     TEXT NOT NULL,
  `photo_url`       VARCHAR(255) DEFAULT NULL,
  `admin_user_id`   INT(11) UNSIGNED NOT NULL,
  `khatam_target`   INT(11) NOT NULL DEFAULT 1,
  `duration_days`   INT(11) NOT NULL DEFAULT 30,
  `current_page`    INT(11) NOT NULL DEFAULT 1,
  `last_reader_id`  INT(11) UNSIGNED DEFAULT NULL,
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`admin_user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`last_reader_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tabel: group_members (Anggota Grup)
CREATE TABLE IF NOT EXISTS `group_members` (
  `id`              INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `group_id`        INT(11) UNSIGNED NOT NULL,
  `user_id`         INT(11) UNSIGNED NOT NULL,
  `status`          ENUM('pending','active','rejected') NOT NULL DEFAULT 'pending',
  `last_page_read`  INT(11) NOT NULL DEFAULT 0,
  `joined_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_group_member` (`group_id`, `user_id`),
  FOREIGN KEY (`group_id`) REFERENCES `ngaji_groups`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================
-- Seed Initial Data
-- =============================================

-- Seed Partners (jika kosong)
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

-- Seed Events (jika kosong)
INSERT INTO `events` (`title`, `category`, `description`, `event_date`, `time_range`, `speaker`, `location`, `is_featured`) VALUES
('Meaningful Life in Islam', 'Kajian', 'Kajian mendalam tentang cara menemukan arti dan kedamaian hidup dalam naungan syariat Islam di era modern.', CURDATE(), '09:00 - 11:30 WIB', 'Ustadz Adi Hidayat', 'Masjid Istiqlal / Online Zoom', 1),
('Youth Spiritual Circle', 'Webinar', 'Diskusi interaktif pemuda muslim seputar tantangan iman, pergaulan sehat, dan tips menjaga istiqamah di sekolah/kampus.', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '19:30 - 21:00 WIB', 'Ustadz Hanan Attaki', 'Online Zoom Meeting', 0),
('Social Care Weekend', 'Sosial', 'Aksi sosial pembagian mushaf Al-Qur\'an gratis dan paket sembako untuk masyarakat pra-sejahtera di bantaran sungai.', DATE_ADD(CURDATE(), INTERVAL 4 DAY), '08:00 - 12:00 WIB', 'Tim Relawan Quran Saku', 'Kawasan Manggarai, Jakarta Selatan', 0),
('Fiqh of Daily Life', 'Workshop', 'Workshop praktis membahas hukum-hukum fiqh ibadah harian, muamalah jual beli online, dan tanya jawab kasus sehari-hari.', DATE_ADD(CURDATE(), INTERVAL 6 DAY), '13:00 - 15:30 WIB', 'Ustadz Abdul Somad', 'Aula Rabbani Bandung', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

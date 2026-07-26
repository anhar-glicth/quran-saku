-- ============================================================
-- donations_migration.sql
-- Table schema and sample data for Live Donation Ticker
-- ============================================================

CREATE TABLE IF NOT EXISTS `donations` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_name` VARCHAR(100) NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `campaign_title` VARCHAR(150) DEFAULT 'Donasi Umum',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample initial donations
INSERT INTO `donations` (`user_name`, `amount`, `campaign_title`, `created_at`) VALUES
('Ahmad Fauzi', 100000.00, 'Sedekah Mushaf Al-Qur\'an', NOW() - INTERVAL 5 MINUTE),
('Hamba Allah', 50000.00, 'Infaq Operasional Dakwah', NOW() - INTERVAL 15 MINUTE),
('Siti Nurhaliza', 250000.00, 'Program Beasiswa Tahfidz', NOW() - INTERVAL 45 MINUTE),
('Rahmat Hidayat', 75000.00, 'Zakat Maal', NOW() - INTERVAL 2 HOUR),
('Budi Santoso', 150000.00, 'Sedekah Subuh Pejuang Quran', NOW() - INTERVAL 4 HOUR);

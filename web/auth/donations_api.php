<?php
// ============================================================
// donations_api.php
// API Endpoint untuk Donasi Ticker (Running / Rotator Donasi)
// ============================================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');

require_once __DIR__ . '/../config/database.php';

try {
    $db = getDB();
    
    // Pastikan tabel donations ada
    $db->exec("
        CREATE TABLE IF NOT EXISTS `donations` (
          `id` INT AUTO_INCREMENT PRIMARY KEY,
          `user_name` VARCHAR(100) NOT NULL,
          `amount` DECIMAL(12,2) NOT NULL,
          `campaign_title` VARCHAR(150) DEFAULT 'Donasi Umum',
          `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    ");

    // Jika tabel kosong, tambahkan data awal sampel
    $countStmt = $db->query("SELECT COUNT(*) FROM donations");
    if ($countStmt->fetchColumn() == 0) {
        $db->exec("
            INSERT INTO `donations` (`user_name`, `amount`, `campaign_title`, `created_at`) VALUES
            ('Ahmad Fauzi', 100000.00, 'Sedekah Mushaf Al-Qur\'an', NOW() - INTERVAL 5 MINUTE),
            ('Hamba Allah', 50000.00, 'Infaq Operasional Dakwah', NOW() - INTERVAL 15 MINUTE),
            ('Siti Nurhaliza', 250000.00, 'Program Beasiswa Tahfidz', NOW() - INTERVAL 45 MINUTE),
            ('Rahmat Hidayat', 75000.00, 'Zakat Maal', NOW() - INTERVAL 2 HOUR),
            ('Budi Santoso', 150000.00, 'Sedekah Subuh Pejuang Quran', NOW() - INTERVAL 4 HOUR);
        ");
    }

    $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 20;

    $stmt = $db->prepare("
        SELECT id, user_name, amount, campaign_title, created_at 
        FROM donations 
        ORDER BY id DESC 
        LIMIT :limit
    ");
    $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
    $stmt.execute();
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $formattedData = [];
    foreach ($rows as $row) {
        $amount = (float)$row['amount'];
        $formattedAmount = 'Rp ' . number_format($amount, 0, ',', '.');
        
        // Formatter relatif waktu sederhadana
        $timeAgo = 'Baru saja';
        if (!empty($row['created_at'])) {
            $timestamp = strtotime($row['created_at']);
            $diff = time() - $timestamp;
            if ($diff < 60) {
                $timeAgo = 'Baru saja';
            } elseif ($diff < 3600) {
                $mins = floor($diff / 60);
                $timeAgo = $mins . ' mnt lalu';
            } elseif ($diff < 86400) {
                $hours = floor($diff / 3600);
                $timeAgo = $hours . ' jam lalu';
            } else {
                $days = floor($diff / 86400);
                $timeAgo = $days . ' hr lalu';
            }
        }

        $formattedData[] = [
            'id'               => (int)$row['id'],
            'user_name'        => $row['user_name'],
            'amount'           => $amount,
            'formatted_amount' => $formattedAmount,
            'campaign'         => $row['campaign_title'],
            'time_ago'         => $timeAgo
        ];
    }

    echo json_encode([
        'success' => true,
        'data'    => $formattedData
    ]);

} catch (Exception $e) {
    // Fallback data sampel jika ada error database
    echo json_encode([
        'success' => false,
        'error'   => $e->getMessage(),
        'data'    => [
            ['id' => 1, 'user_name' => 'Ahmad Fauzi', 'amount' => 100000, 'formatted_amount' => 'Rp 100.000', 'campaign' => 'Sedekah Mushaf', 'time_ago' => '5 mnt lalu'],
            ['id' => 2, 'user_name' => 'Hamba Allah', 'amount' => 50000, 'formatted_amount' => 'Rp 50.000', 'campaign' => 'Infaq Dakwah', 'time_ago' => '15 mnt lalu'],
            ['id' => 3, 'user_name' => 'Siti Nurhaliza', 'amount' => 250000, 'formatted_amount' => 'Rp 250.000', 'campaign' => 'Beasiswa Tahfidz', 'time_ago' => '45 mnt lalu']
        ]
    ]);
}

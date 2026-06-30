<?php
// ============================================================
// leaderboard_api.php
// API untuk papan peringkat komunitas (mingguan & bulanan)
// Berdasarkan data strava_activities yang disimpan dari app
// ============================================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');

require_once __DIR__ . '/../config/database.php';

$db = getDB();

$period = $_GET['period'] ?? 'weekly'; // 'weekly' atau 'monthly'
$limit  = isset($_GET['limit']) ? intval($_GET['limit']) : 10;

if ($period === 'weekly') {
    // Minggu ini: Senin sampai hari ini
    $startDate = date('Y-m-d', strtotime('monday this week'));
    $endDate   = date('Y-m-d');
} else {
    // Bulan ini: tanggal 1 sampai hari ini
    $startDate = date('Y-m-01');
    $endDate   = date('Y-m-d');
}

$stmt = $db->prepare("
    SELECT 
        u.id AS user_id,
        u.name AS user_name,
        SUM(sa.duration_seconds) AS total_duration_seconds,
        SUM(sa.pages_count) AS total_pages,
        COUNT(DISTINCT sa.activity_date) AS active_days
    FROM strava_activities sa
    JOIN users u ON u.id = sa.user_id
    WHERE sa.activity_date BETWEEN :start_date AND :end_date
    GROUP BY u.id, u.name
    ORDER BY total_duration_seconds DESC
    LIMIT :limit
");

$stmt->execute([
    ':start_date' => $startDate,
    ':end_date'   => $endDate,
    ':limit'      => $limit
]);

$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

$leaderboard = array_map(function($row, $index) {
    $durationSec  = (int)$row['total_duration_seconds'];
    $durationMin  = (int)ceil($durationSec / 60);
    $totalPages   = (int)$row['total_pages'];
    $activeDays   = (int)$row['active_days'];

    // Generate initials dari nama
    $nameParts = explode(' ', trim($row['user_name']));
    $initials  = '';
    foreach (array_slice($nameParts, 0, 2) as $part) {
        $initials .= strtoupper(mb_substr($part, 0, 1));
    }

    return [
        'rank'             => $index + 1,
        'user_id'          => (int)$row['user_id'],
        'user_name'        => $row['user_name'],
        'initials'         => $initials,
        'total_minutes'    => $durationMin,
        'total_pages'      => $totalPages,
        'active_days'      => $activeDays,
        'display_score'    => number_format($durationMin * 10 + $totalPages * 5) // Skor gabungan
    ];
}, $rows, array_keys($rows));

// Cari peringkat user sendiri jika user_id dikirim
$myRankData = null;
$myUserId = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
if ($myUserId > 0) {
    // Ambil seluruh ranking tanpa limit untuk mencari rank user_id
    $stmtAll = $db->prepare("
        SELECT 
            u.id AS user_id,
            u.name AS user_name,
            SUM(sa.duration_seconds) AS total_duration_seconds,
            SUM(sa.pages_count) AS total_pages,
            COUNT(DISTINCT sa.activity_date) AS active_days
        FROM strava_activities sa
        JOIN users u ON u.id = sa.user_id
        WHERE sa.activity_date BETWEEN :start_date AND :end_date
        GROUP BY u.id, u.name
        ORDER BY total_duration_seconds DESC
    ");
    $stmtAll->execute([
        ':start_date' => $startDate,
        ':end_date'   => $endDate
    ]);
    $allRows = $stmtAll->fetchAll(PDO::FETCH_ASSOC);
    foreach ($allRows as $index => $row) {
        if ((int)$row['user_id'] === $myUserId) {
            $durationSec  = (int)$row['total_duration_seconds'];
            $durationMin  = (int)ceil($durationSec / 60);
            $totalPages   = (int)$row['total_pages'];
            $activeDays   = (int)$row['active_days'];
            
            $nameParts = explode(' ', trim($row['user_name']));
            $initials  = '';
            foreach (array_slice($nameParts, 0, 2) as $part) {
                $initials .= strtoupper(mb_substr($part, 0, 1));
            }
            
            $myRankData = [
                'rank'             => $index + 1,
                'user_id'          => $myUserId,
                'user_name'        => $row['user_name'],
                'initials'         => $initials,
                'total_minutes'    => $durationMin,
                'total_pages'      => $totalPages,
                'active_days'      => $activeDays,
                'display_score'    => number_format($durationMin * 10 + $totalPages * 5)
            ];
            break;
        }
    }
    // Jika tidak ditemukan aktivitas sama sekali untuk user ini
    if (!$myRankData) {
        $userStmt = $db->prepare("SELECT name FROM users WHERE id = ?");
        $userStmt->execute([$myUserId]);
        $userRow = $userStmt->fetch();
        if ($userRow) {
            $nameParts = explode(' ', trim($userRow['name']));
            $initials  = '';
            foreach (array_slice($nameParts, 0, 2) as $part) {
                $initials .= strtoupper(mb_substr($part, 0, 1));
            }
            $myRankData = [
                'rank'             => count($allRows) + 1,
                'user_id'          => $myUserId,
                'user_name'        => $userRow['name'],
                'initials'         => $initials,
                'total_minutes'    => 0,
                'total_pages'      => 0,
                'active_days'      => 0,
                'display_score'    => "0"
            ];
        }
    }
}

echo json_encode([
    'success' => true,
    'period'  => $period,
    'from'    => $startDate,
    'to'      => $endDate,
    'data'    => $leaderboard,
    'my_rank' => $myRankData
]);


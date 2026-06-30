<?php
require_once '../config/db.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
    exit;
}

$userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$date = isset($_POST['activity_date']) ? $_POST['activity_date'] : '';
$duration = isset($_POST['duration_seconds']) ? intval($_POST['duration_seconds']) : 0;
$pages = isset($_POST['pages_count']) ? intval($_POST['pages_count']) : 0;

if ($userId <= 0 || empty($date) || $duration < 0 || $pages < 0) {
    echo json_encode(['status' => 'error', 'message' => 'Invalid parameters']);
    exit;
}

try {
    // 1. Create table if not exists
    $createTableQuery = "CREATE TABLE IF NOT EXISTS `strava_activities` (
        `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
        `user_id` INT(11) UNSIGNED NOT NULL,
        `activity_date` DATE NOT NULL,
        `duration_seconds` INT(11) NOT NULL DEFAULT 0,
        `pages_count` INT(11) NOT NULL DEFAULT 0,
        PRIMARY KEY (`id`),
        UNIQUE KEY `unique_user_date` (`user_id`, `activity_date`),
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    
    $conn->exec($createTableQuery);

    // 2. Insert or update progress
    $stmt = $conn->prepare("INSERT INTO `strava_activities` (user_id, activity_date, duration_seconds, pages_count) 
        VALUES (:user_id, :activity_date, :duration, :pages)
        ON DUPLICATE KEY UPDATE 
        duration_seconds = duration_seconds + :duration_update,
        pages_count = pages_count + :pages_update");
        
    $stmt->execute([
        ':user_id' => $userId,
        ':activity_date' => $date,
        ':duration' => $duration,
        ':pages' => $pages,
        ':duration_update' => $duration,
        ':pages_update' => $pages
    ]);

    echo json_encode(['status' => 'success', 'message' => 'Activity updated successfully']);

} catch (PDOException $e) {
    echo json_encode(['status' => 'error', 'message' => 'Database error: ' . $e->getMessage()]);
}
?>

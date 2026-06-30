<?php
// ============================================================
// events_api.php
// API untuk Event & Kalender Kegiatan (list, monthly, save, delete)
// ============================================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');

require_once __DIR__ . '/../config/database.php';

$db = getDB();

$action = $_REQUEST['action'] ?? 'list';

if ($action === 'list') {
    $featuredOnly = isset($_GET['featured_only']) ? intval($_GET['featured_only']) : 0;
    
    if ($featuredOnly === 1) {
        $stmt = $db->query("SELECT * FROM events WHERE is_featured = 1 ORDER BY event_date ASC");
    } else {
        $stmt = $db->query("SELECT * FROM events ORDER BY event_date ASC");
    }
    
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $events = array_map(function($row) {
        return [
            'id'          => (int)$row['id'],
            'title'       => $row['title'],
            'category'    => $row['category'],
            'description' => $row['description'],
            'event_date'  => $row['event_date'],
            'time_range'  => $row['time_range'],
            'speaker'     => $row['speaker'],
            'location'    => $row['location'],
            'is_featured' => (bool)$row['is_featured']
        ];
    }, $rows);
    
    echo json_encode(['success' => true, 'data' => $events]);
    exit;
}

if ($action === 'monthly') {
    $year  = isset($_GET['year']) ? intval($_GET['year']) : date('Y');
    $month = isset($_GET['month']) ? intval($_GET['month']) : date('m');
    
    // Format YYYY-MM
    $monthStr = sprintf('%04d-%02d', $year, $month);
    
    $stmt = $db->prepare("SELECT * FROM events WHERE event_date LIKE ? ORDER BY event_date ASC");
    $stmt->execute([$monthStr . '-%']);
    
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $events = array_map(function($row) {
        return [
            'id'          => (int)$row['id'],
            'title'       => $row['title'],
            'category'    => $row['category'],
            'description' => $row['description'],
            'event_date'  => $row['event_date'],
            'time_range'  => $row['time_range'],
            'speaker'     => $row['speaker'],
            'location'    => $row['location'],
            'is_featured' => (bool)$row['is_featured']
        ];
    }, $rows);
    
    echo json_encode(['success' => true, 'data' => $events]);
    exit;
}

if ($action === 'save') {
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    
    // Check if user is admin
    $userStmt = $db->prepare("SELECT role FROM users WHERE id = ?");
    $userStmt->execute([$userId]);
    $user = $userStmt->fetch();
    
    if (!$user || $user['role'] !== 'admin') {
        echo json_encode(['success' => false, 'message' => 'Akses ditolak. Hanya admin yang dapat mengelola event.']);
        exit;
    }
    
    $id          = isset($_POST['id']) ? intval($_POST['id']) : 0;
    $title       = trim($_POST['title'] ?? '');
    $category    = trim($_POST['category'] ?? 'Kajian');
    $description = trim($_POST['description'] ?? '');
    $eventDate   = trim($_POST['event_date'] ?? '');
    $timeRange   = trim($_POST['time_range'] ?? '09:00 - 11:30 WIB');
    $speaker     = trim($_POST['speaker'] ?? '');
    $location    = trim($_POST['location'] ?? 'Online Zoom');
    $isFeatured  = isset($_POST['is_featured']) ? intval($_POST['is_featured']) : 0;
    
    if (empty($title) || empty($description) || empty($eventDate) || empty($speaker)) {
        echo json_encode(['success' => false, 'message' => 'Semua field wajib diisi']);
        exit;
    }
    
    if ($id > 0) {
        // Edit existing
        $stmt = $db->prepare("
            UPDATE events 
            SET title = ?, category = ?, description = ?, event_date = ?, time_range = ?, speaker = ?, location = ?, is_featured = ?
            WHERE id = ?
        ");
        $stmt->execute([$title, $category, $description, $eventDate, $timeRange, $speaker, $location, $isFeatured, $id]);
        $eventId = $id;
    } else {
        // Create new
        $stmt = $db->prepare("
            INSERT INTO events (title, category, description, event_date, time_range, speaker, location, is_featured)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([$title, $category, $description, $eventDate, $timeRange, $speaker, $location, $isFeatured]);
        $eventId = (int)$db->lastInsertId();
    }
    
    echo json_encode([
        'success' => true,
        'message' => 'Event berhasil disimpan',
        'data' => [
            'id'          => $eventId,
            'title'       => $title,
            'category'    => $category,
            'description' => $description,
            'event_date'  => $eventDate,
            'time_range'  => $timeRange,
            'speaker'     => $speaker,
            'location'    => $location,
            'is_featured' => (bool)$isFeatured
        ]
    ]);
    exit;
}

if ($action === 'delete') {
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    
    // Check if user is admin
    $userStmt = $db->prepare("SELECT role FROM users WHERE id = ?");
    $userStmt->execute([$userId]);
    $user = $userStmt->fetch();
    
    if (!$user || $user['role'] !== 'admin') {
        echo json_encode(['success' => false, 'message' => 'Akses ditolak.']);
        exit;
    }
    
    $id = isset($_POST['id']) ? intval($_POST['id']) : 0;
    
    if ($id <= 0) {
        echo json_encode(['success' => false, 'message' => 'ID tidak valid']);
        exit;
    }
    
    $stmt = $db->prepare("DELETE FROM events WHERE id = ?");
    $stmt->execute([$id]);
    
    echo json_encode(['success' => true, 'message' => 'Event berhasil dihapus']);
    exit;
}

echo json_encode(['success' => false, 'message' => 'Action tidak dikenal']);
?>

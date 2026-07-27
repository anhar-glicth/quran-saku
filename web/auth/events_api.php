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

// Self-healing migration: check if image_url column exists, if not create it
try {
    $db->query("SELECT image_url FROM events LIMIT 1");
} catch (PDOException $e) {
    try {
        $db->query("ALTER TABLE events ADD COLUMN image_url VARCHAR(255) DEFAULT NULL");
    } catch (PDOException $ex) {
        // Ignore if already exists or other error
    }
}

// Self-healing migration: check if link_url column exists, if not create it
try {
    $db->query("SELECT link_url FROM events LIMIT 1");
} catch (PDOException $e) {
    try {
        $db->query("ALTER TABLE events ADD COLUMN link_url VARCHAR(255) DEFAULT ''");
    } catch (PDOException $ex) {
        // Ignore if already exists
    }
}

// Self-healing migration: check if event_registrations table exists, if not create it
try {
    $db->query("SELECT id FROM event_registrations LIMIT 1");
} catch (PDOException $e) {
    try {
        $db->query("
            CREATE TABLE IF NOT EXISTS event_registrations (
                id            INT(11) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                event_id      INT(11) UNSIGNED NOT NULL,
                user_id       INT(11) UNSIGNED NOT NULL,
                name          VARCHAR(100) NOT NULL,
                email         VARCHAR(100) NOT NULL,
                phone         VARCHAR(20) NOT NULL,
                notes         TEXT DEFAULT NULL,
                registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_registration (event_id, user_id),
                FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        ");
    } catch (PDOException $ex) {
        // Ignore
    }
}

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
            'is_featured' => (bool)$row['is_featured'],
            'image_url'   => $row['image_url'] ?? '',
            'link_url'    => $row['link_url'] ?? ''
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
            'is_featured' => (bool)$row['is_featured'],
            'image_url'   => $row['image_url'] ?? '',
            'link_url'    => $row['link_url'] ?? ''
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
    $imageUrl    = trim($_POST['image_url'] ?? '');
    $linkUrl     = trim($_POST['link_url'] ?? '');
    
    if (empty($title) || empty($description) || empty($eventDate) || empty($speaker)) {
        echo json_encode(['success' => false, 'message' => 'Semua field wajib diisi']);
        exit;
    }
    
    if ($id > 0) {
        // Edit existing
        $stmt = $db->prepare("
            UPDATE events 
            SET title = ?, category = ?, description = ?, event_date = ?, time_range = ?, speaker = ?, location = ?, is_featured = ?, image_url = ?, link_url = ?
            WHERE id = ?
        ");
        $stmt->execute([$title, $category, $description, $eventDate, $timeRange, $speaker, $location, $isFeatured, $imageUrl, $linkUrl, $id]);
        $eventId = $id;
    } else {
        // Create new
        $stmt = $db->prepare("
            INSERT INTO events (title, category, description, event_date, time_range, speaker, location, is_featured, image_url, link_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([$title, $category, $description, $eventDate, $timeRange, $speaker, $location, $isFeatured, $imageUrl, $linkUrl]);
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
            'is_featured' => (bool)$isFeatured,
            'image_url'   => $imageUrl,
            'link_url'    => $linkUrl
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

if ($action === 'register') {
    $eventId = isset($_POST['event_id']) ? intval($_POST['event_id']) : 0;
    $userId  = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $name    = trim($_POST['name'] ?? '');
    $email   = trim($_POST['email'] ?? '');
    $phone   = trim($_POST['phone'] ?? '');
    $notes   = trim($_POST['notes'] ?? '');
    
    if ($eventId <= 0 || $userId <= 0 || empty($name) || empty($email) || empty($phone)) {
        echo json_encode(['success' => false, 'message' => 'Lengkapi nama, email, dan no WhatsApp']);
        exit;
    }
    
    try {
        $stmt = $db->prepare("
            INSERT INTO event_registrations (event_id, user_id, name, email, phone, notes)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE name = VALUES(name), email = VALUES(email), phone = VALUES(phone), notes = VALUES(notes)
        ");
        $stmt->execute([$eventId, $userId, $name, $email, $phone, empty($notes) ? null : $notes]);
        echo json_encode(['success' => true, 'message' => 'Berhasil mendaftar kegiatan']);
    } catch (Exception $e) {
        echo json_encode(['success' => false, 'message' => 'Gagal mendaftar: ' . $e->getMessage()]);
    }
    exit;
}

if ($action === 'check_registration') {
    $eventId = isset($_GET['event_id']) ? intval($_GET['event_id']) : 0;
    $userId  = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    
    if ($eventId <= 0 || $userId <= 0) {
        echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
        exit;
    }
    
    $stmt = $db->prepare("SELECT id FROM event_registrations WHERE event_id = ? AND user_id = ? LIMIT 1");
    $stmt->execute([$eventId, $userId]);
    $isRegistered = (bool)$stmt->fetch();
    
    echo json_encode([
        'success' => true,
        'is_registered' => $isRegistered
    ]);
    exit;
}

if ($action === 'get_registrations') {
    $adminId = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    $eventId = isset($_GET['event_id']) ? intval($_GET['event_id']) : 0;
    
    if ($adminId <= 0 || $eventId <= 0) {
        echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
        exit;
    }
    
    // Check if user is admin
    $userStmt = $db->prepare("SELECT role FROM users WHERE id = ?");
    $userStmt->execute([$adminId]);
    $user = $userStmt->fetch();
    
    if (!$user || $user['role'] !== 'admin') {
        echo json_encode(['success' => false, 'message' => 'Akses ditolak. Hanya admin yang dapat melihat pendaftar.']);
        exit;
    }
    
    $stmt = $db->prepare("SELECT * FROM event_registrations WHERE event_id = ? ORDER BY registered_at DESC");
    $stmt->execute([$eventId]);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $registrations = array_map(function($row) {
        return [
            'id' => (int)$row['id'],
            'event_id' => (int)$row['event_id'],
            'user_id' => (int)$row['user_id'],
            'name' => $row['name'],
            'email' => $row['email'],
            'phone' => $row['phone'],
            'notes' => $row['notes'] ?? '',
            'registered_at' => $row['registered_at']
        ];
    }, $rows);
    
    echo json_encode([
        'success' => true,
        'total' => count($registrations),
        'data' => $registrations
    ]);
    exit;
}

echo json_encode(['success' => false, 'message' => 'Action tidak dikenal']);
?>

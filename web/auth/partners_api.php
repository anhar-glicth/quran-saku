<?php
// ============================================================
// partners_api.php
// API untuk Pejuang Kebaikan / Mitra (list, add)
// ============================================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');

require_once __DIR__ . '/../config/database.php';

$db = getDB();

$action = $_REQUEST['action'] ?? 'list';

if ($action === 'list') {
    $categoryId = $_GET['category_id'] ?? '';
    
    if (empty($categoryId)) {
        $stmt = $db->query("SELECT * FROM partners ORDER BY id ASC");
    } else {
        $stmt = $db->prepare("SELECT * FROM partners WHERE category_id = ? ORDER BY id ASC");
        $stmt->execute([$categoryId]);
    }
    
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $partners = array_map(function($row) {
        return [
            'id'          => (int)$row['id'],
            'category_id' => $row['category_id'],
            'logo_text'   => $row['logo_text'],
            'name'        => $row['name'],
            'description' => $row['description'],
            'bg_color'    => $row['bg_color'],
            'text_color'  => $row['text_color']
        ];
    }, $rows);
    
    echo json_encode(['success' => true, 'data' => $partners]);
    exit;
}

if ($action === 'add') {
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    
    // Check if user is admin
    $userStmt = $db->prepare("SELECT role FROM users WHERE id = ?");
    $userStmt->execute([$userId]);
    $user = $userStmt->fetch();
    
    if (!$user || $user['role'] !== 'admin') {
        echo json_encode(['success' => false, 'message' => 'Akses ditolak. Hanya admin yang dapat menambah mitra.']);
        exit;
    }
    
    $categoryId  = $_POST['category_id'] ?? '';
    $logoText    = trim($_POST['logo_text'] ?? '');
    $name        = trim($_POST['name'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $bgColor     = $_POST['bg_color'] ?? '#E0F2F1';
    $textColor   = $_POST['text_color'] ?? '#004D40';
    
    if (empty($categoryId) || empty($logoText) || empty($name) || empty($description)) {
        echo json_encode(['success' => false, 'message' => 'Semua field wajib diisi']);
        exit;
    }
    
    $stmt = $db->prepare("
        INSERT INTO partners (category_id, logo_text, name, description, bg_color, text_color)
        VALUES (?, ?, ?, ?, ?, ?)
    ");
    $stmt->execute([$categoryId, $logoText, $name, $description, $bgColor, $textColor]);
    
    echo json_encode([
        'success' => true,
        'message' => 'Mitra berhasil ditambahkan',
        'data' => [
            'id'          => (int)$db->lastInsertId(),
            'category_id' => $categoryId,
            'logo_text'   => $logoText,
            'name'        => $name,
            'description' => $description,
            'bg_color'    => $bgColor,
            'text_color'  => $textColor
        ]
    ]);
    exit;
}

echo json_encode(['success' => false, 'message' => 'Action tidak dikenal']);
?>

<?php
// ============================================================
// campaign_api.php
// API untuk Campaign Donasi
// ============================================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');

require_once __DIR__ . '/../config/database.php';

$db = getDB();

// Auto-migration: buat tabel campaigns jika belum ada
try {
    $db->query("
        CREATE TABLE IF NOT EXISTS campaigns (
            id          INT AUTO_INCREMENT PRIMARY KEY,
            title       VARCHAR(150) NOT NULL,
            description TEXT,
            image_url   VARCHAR(500) DEFAULT '',
            donate_url  VARCHAR(500) DEFAULT '',
            is_active   TINYINT(1) DEFAULT 1,
            created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) CHARACTER SET utf8mb4
    ");
} catch (PDOException $e) {
    // Tabel mungkin sudah ada
}

$action = $_REQUEST['action'] ?? 'list';

// ─── LIST ────────────────────────────────────────────────────
if ($action === 'list') {
    $showAll = isset($_GET['all']) ? intval($_GET['all']) : 0;

    if ($showAll === 1) {
        $stmt = $db->query("SELECT * FROM campaigns ORDER BY id DESC");
    } else {
        $stmt = $db->query("SELECT * FROM campaigns WHERE is_active = 1 ORDER BY id DESC");
    }

    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $campaigns = array_map(function ($row) {
        return [
            'id'          => (int)$row['id'],
            'title'       => $row['title'],
            'description' => $row['description'] ?? '',
            'image_url'   => $row['image_url'] ?? '',
            'donate_url'  => $row['donate_url'] ?? '',
            'is_active'   => (bool)$row['is_active'],
        ];
    }, $rows);

    echo json_encode(['success' => true, 'data' => $campaigns]);
    exit;
}

// ─── SAVE (Create / Update) ───────────────────────────────────
if ($action === 'save') {
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;

    // Cek role admin
    $userStmt = $db->prepare("SELECT role FROM users WHERE id = ?");
    $userStmt->execute([$userId]);
    $user = $userStmt->fetch();

    if (!$user || $user['role'] !== 'admin') {
        echo json_encode(['success' => false, 'message' => 'Akses ditolak. Hanya admin yang dapat mengelola campaign.']);
        exit;
    }

    $id          = isset($_POST['id']) ? intval($_POST['id']) : 0;
    $title       = trim($_POST['title'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $imageUrl    = trim($_POST['image_url'] ?? '');
    $donateUrl   = trim($_POST['donate_url'] ?? '');
    $isActive    = isset($_POST['is_active']) ? intval($_POST['is_active']) : 1;

    if (empty($title)) {
        echo json_encode(['success' => false, 'message' => 'Judul campaign tidak boleh kosong']);
        exit;
    }

    if ($id > 0) {
        // Edit existing
        $stmt = $db->prepare("
            UPDATE campaigns
            SET title = ?, description = ?, image_url = ?, donate_url = ?, is_active = ?
            WHERE id = ?
        ");
        $stmt->execute([$title, $description, $imageUrl, $donateUrl, $isActive, $id]);
        $campaignId = $id;
    } else {
        // Create new
        $stmt = $db->prepare("
            INSERT INTO campaigns (title, description, image_url, donate_url, is_active)
            VALUES (?, ?, ?, ?, ?)
        ");
        $stmt->execute([$title, $description, $imageUrl, $donateUrl, $isActive]);
        $campaignId = (int)$db->lastInsertId();
    }

    echo json_encode([
        'success' => true,
        'message' => 'Campaign berhasil disimpan',
        'data' => [
            'id'          => $campaignId,
            'title'       => $title,
            'description' => $description,
            'image_url'   => $imageUrl,
            'donate_url'  => $donateUrl,
            'is_active'   => (bool)$isActive,
        ]
    ]);
    exit;
}

// ─── DELETE ───────────────────────────────────────────────────
if ($action === 'delete') {
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;

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

    $stmt = $db->prepare("DELETE FROM campaigns WHERE id = ?");
    $stmt->execute([$id]);

    echo json_encode(['success' => true, 'message' => 'Campaign berhasil dihapus']);
    exit;
}

echo json_encode(['success' => false, 'message' => 'Action tidak dikenal']);
?>

<?php
// ============================================================
// group_api.php
// API untuk Fitur Grup Ngaji (create, join, my_group, pending_requests, update)
// ============================================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');

require_once __DIR__ . '/../config/database.php';

$db = getDB();

$action = $_REQUEST['action'] ?? 'my_group';

// Helper function to generate unique group code
function generateGroupCode($db) {
    $chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    do {
        $code = '';
        for ($i = 0; $i < 8; $i++) {
            $code .= $chars[rand(0, strlen($chars) - 1)];
        }
        $stmt = $db->prepare("SELECT id FROM ngaji_groups WHERE group_code = ?");
        $stmt->execute([$code]);
    } while ($stmt->fetch());
    return $code;
}

// Helper to get baseUrl dynamically
function getBaseUrl() {
    $host = $_SERVER['HTTP_HOST'] ?? '127.0.0.1';
    return "http://" . $host . "/quran_android/web/";
}

// ─── AMBIL GRUP SAYA / DETAIL GRUP ───────────────────────────
if ($action === 'my_group') {
    $userId = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    $groupId = isset($_GET['group_id']) ? intval($_GET['group_id']) : 0;
    
    if ($groupId > 0) {
        // Query group directly by ID
        $stmt = $db->prepare("
            SELECT g.*, 'active' AS member_status, u.name AS admin_name, u2.name AS last_reader_name
            FROM ngaji_groups g
            JOIN users u ON u.id = g.admin_user_id
            LEFT JOIN users u2 ON u2.id = g.last_reader_id
            WHERE g.id = ?
            LIMIT 1
        ");
        $stmt->execute([$groupId]);
        $group = $stmt->fetch(PDO::FETCH_ASSOC);
    } else if ($userId > 0) {
        // Query group based on user's active/pending membership
        $stmt = $db->prepare("
            SELECT g.*, m.status AS member_status, u.name AS admin_name, u2.name AS last_reader_name
            FROM group_members m
            JOIN ngaji_groups g ON g.id = m.group_id
            JOIN users u ON u.id = g.admin_user_id
            LEFT JOIN users u2 ON u2.id = g.last_reader_id
            WHERE m.user_id = ?
            LIMIT 1
        ");
        $stmt->execute([$userId]);
        $group = $stmt->fetch(PDO::FETCH_ASSOC);
    } else {
        echo json_encode(['success' => false, 'message' => 'Parameter user_id atau group_id tidak valid']);
        exit;
    }
    
    if (!$group) {
        echo json_encode(['success' => true, 'has_group' => false]);
        exit;
    }
    
    $groupId = (int)$group['id'];
    $members = [];
    $pendingRequests = [];
    $relay = [];
    $progressPct = 0;
    
    // Fetch members, relays, and progress if status is active
    if ($group['member_status'] === 'active') {
        // Fetch active members
        $mStmt = $db->prepare("
            SELECT m.user_id, u.name AS user_name, m.last_page_read, m.status, m.joined_at
            FROM group_members m
            JOIN users u ON u.id = m.user_id
            WHERE m.group_id = ? AND m.status = 'active'
            ORDER BY m.joined_at ASC
        ");
        $mStmt->execute([$groupId]);
        $members = $mStmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Fetch pending requests (if user is admin)
        // Also support fetching pending list if requested by admin userId
        if ((int)$group['admin_user_id'] === $userId || $userId === 0) {
            $pStmt = $db->prepare("
                SELECT m.user_id, u.name AS user_name, m.joined_at
                FROM group_members m
                JOIN users u ON u.id = m.user_id
                WHERE m.group_id = ? AND m.status = 'pending'
                ORDER BY m.joined_at ASC
            ");
            $pStmt->execute([$groupId]);
            $pendingRequests = $pStmt->fetchAll(PDO::FETCH_ASSOC);
        }
        
        // Fetch reading relays
        $rStmt = $db->prepare("
            SELECT r.*, u.name AS user_name
            FROM ngaji_reading_relay r
            JOIN users u ON u.id = r.user_id
            WHERE r.group_id = ?
            ORDER BY r.read_at DESC
        ");
        $rStmt->execute([$groupId]);
        $relay = $rStmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Calculate progress percentage based on unique pages read in relay
        $pagesMap = [];
        foreach ($relay as $rItem) {
            if ($rItem['page_number'] > 0) {
                $pagesMap[$rItem['page_number']] = true;
            }
        }
        $totalPagesRead = count($pagesMap);
        $totalTargetPages = (int)$group['khatam_target'] * 604;
        $progressPct = min(100, $totalTargetPages > 0 ? round(($totalPagesRead / $totalTargetPages) * 100) : 0);
    }
    
    echo json_encode([
        'success' => true,
        'has_group' => true,
        'group' => [
            'id'                => $groupId,
            'group_code'        => $group['group_code'],
            'name'              => $group['name'],
            'description'       => $group['description'] ?? '',
            'photo_url'         => $group['photo_url'] ? getBaseUrl() . $group['photo_url'] : null,
            'admin_user_id'     => (int)$group['admin_user_id'],
            'admin_name'        => $group['admin_name'],
            'khatam_target'     => (int)$group['khatam_target'],
            'duration_days'     => (int)$group['duration_days'],
            'current_page'      => (int)$group['current_page'],
            'last_reader_id'    => $group['last_reader_id'] ? (int)$group['last_reader_id'] : null,
            'last_reader_name'  => $group['last_reader_name'] ?: 'Belum ada',
            'member_status'     => $group['member_status']
        ],
        'members' => array_map(function($m) {
            return [
                'user_id' => (int)$m['user_id'],
                'user_name' => $m['user_name'],
                'last_page_read' => (int)$m['last_page_read'],
                'joined_at' => $m['joined_at']
            ];
        }, $members),
        'relay' => array_map(function($r) {
            return [
                'id' => (int)$r['id'],
                'user_id' => (int)$r['user_id'],
                'user_name' => $r['user_name'],
                'surah_number' => (int)$r['surah_number'],
                'surah_name' => $r['surah_name'],
                'ayah_number' => (int)$r['ayah_number'],
                'page_number' => (int)$r['page_number'],
                'read_at' => $r['read_at']
            ];
        }, $relay),
        'progressPercent' => (int)$progressPct
    ]);
    exit;
}

// ─── BUAT GRUP BARU ──────────────────────────────────────────
if ($action === 'create') {
    $userId       = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $name         = trim($_POST['name'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $khatamTarget = isset($_POST['khatam_target']) ? intval($_POST['khatam_target']) : 1;
    $durationDays = isset($_POST['duration_days']) ? intval($_POST['duration_days']) : 30;
    $photoBase64  = $_POST['photo_base64'] ?? '';
    
    if ($userId <= 0 || empty($name) || empty($description)) {
        echo json_encode(['success' => false, 'message' => 'Lengkapi nama dan deskripsi grup']);
        exit;
    }
    
    // Cek apakah user sudah punya grup
    $check = $db->prepare("SELECT id FROM group_members WHERE user_id = ? AND status != 'rejected' LIMIT 1");
    $check->execute([$userId]);
    if ($check->fetch()) {
        echo json_encode(['success' => false, 'message' => 'Anda sudah bergabung di grup lain. Keluar dulu untuk membuat grup baru.']);
        exit;
    }
    
    // Generate unique group code: QS-XXXX
    $code = generateGroupCode($db);
    
    // Handle photo upload
    $photoPath = null;
    if (!empty($photoBase64)) {
        $imgData = base64_decode($photoBase64);
        if ($imgData !== false) {
            $dir = '../uploads/grup/';
            if (!file_exists($dir)) {
                mkdir($dir, 0777, true);
            }
            $fileName = uniqid() . '.jpg';
            file_put_contents($dir . $fileName, $imgData);
            $photoPath = 'uploads/grup/' . $fileName;
        }
    }
    
    $db->beginTransaction();
    try {
        $stmt = $db->prepare("
            INSERT INTO ngaji_groups (group_code, name, description, photo_url, admin_user_id, khatam_target, duration_days)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([$code, $name, $description, $photoPath, $userId, $khatamTarget, $durationDays]);
        $groupId = $db->lastInsertId();
        
        // Tambahkan pembuat sebagai anggota aktif
        $mStmt = $db->prepare("
            INSERT INTO group_members (group_id, user_id, status)
            VALUES (?, ?, 'active')
        ");
        $mStmt->execute([$groupId, $userId]);
        
        $db->commit();
        echo json_encode(['success' => true, 'message' => 'Grup berhasil dibuat!']);
    } catch (Exception $e) {
        $db->rollBack();
        echo json_encode(['success' => false, 'message' => 'Gagal membuat grup: ' . $e->getMessage()]);
    }
    exit;
}

// ─── REQUEST JOIN GRUP ───────────────────────────────────────
if ($action === 'join') {
    $userId    = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $groupCode = strtoupper(trim($_POST['group_code'] ?? ''));
    
    if ($userId <= 0 || empty($groupCode)) {
        echo json_encode(['success' => false, 'message' => 'Kode grup tidak boleh kosong']);
        exit;
    }
    
    // Cek apakah user sudah punya grup
    $check = $db->prepare("SELECT id FROM group_members WHERE user_id = ? AND status != 'rejected' LIMIT 1");
    $check->execute([$userId]);
    if ($check->fetch()) {
        echo json_encode(['success' => false, 'message' => 'Anda sudah bergabung di grup lain. Keluar dulu sebelum join grup baru.']);
        exit;
    }
    
    // Cari grup berdasarkan kode
    $gStmt = $db->prepare("SELECT id FROM ngaji_groups WHERE group_code = ? LIMIT 1");
    $gStmt->execute([$groupCode]);
    $group = $gStmt->fetch();
    
    if (!$group) {
        echo json_encode(['success' => false, 'message' => 'Grup dengan kode tersebut tidak ditemukan.']);
        exit;
    }
    
    // Masukkan sebagai pending
    $stmt = $db->prepare("
        INSERT INTO group_members (group_id, user_id, status)
        VALUES (?, ?, 'pending')
        ON DUPLICATE KEY UPDATE status = 'pending'
    ");
    $stmt->execute([$group['id'], $userId]);
    
    echo json_encode(['success' => true, 'message' => 'Permintaan join berhasil dikirim. Menunggu ACC admin grup.']);
    exit;
}

// ─── UPDATE HALAMAN BACAAN GRUP ──────────────────────────────
if ($action === 'update_page') {
    $userId  = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $page    = isset($_POST['page_number']) ? intval($_POST['page_number']) : 1;
    
    if ($userId <= 0 || $page <= 0) {
        echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
        exit;
    }
    
    // Cari grup aktif user
    $gStmt = $db->prepare("
        SELECT group_id FROM group_members 
        WHERE user_id = ? AND status = 'active' 
        LIMIT 1
    ");
    $gStmt->execute([$userId]);
    $member = $gStmt->fetch();
    
    if (!$member) {
        echo json_encode(['success' => false, 'message' => 'Anda tidak bergabung di grup aktif manapun.']);
        exit;
    }
    
    $groupId = $member['group_id'];
    
    $db->beginTransaction();
    try {
        // Update halaman di level grup
        $uStmt = $db->prepare("
            UPDATE ngaji_groups 
            SET current_page = ?, last_reader_id = ? 
            WHERE id = ?
        ");
        $uStmt->execute([$page, $userId, $groupId]);
        
        // Update halaman terakhir dibaca di level anggota
        $mStmt = $db->prepare("
            UPDATE group_members 
            SET last_page_read = ? 
            WHERE group_id = ? AND user_id = ?
        ");
        $mStmt->execute([$page, $groupId, $userId]);
        
        // Tambahkan ke ngaji_reading_relay
        $rStmt = $db->prepare("
            INSERT INTO ngaji_reading_relay (group_id, user_id, surah_number, surah_name, ayah_number, page_number)
            VALUES (?, ?, 0, NULL, 0, ?)
        ");
        $rStmt->execute([$groupId, $userId, $page]);
        
        $db->commit();
        echo json_encode(['success' => true, 'message' => 'Progress bacaan grup di-update ke halaman ' . $page]);
    } catch (Exception $e) {
        $db->rollBack();
        echo json_encode(['success' => false, 'message' => 'Gagal meng-update progress: ' . $e->getMessage()]);
    }
    exit;
}

// ─── AMBIL PERMINTAAN GABUNG PENDING (ADMIN ONLY) ─────────────
if ($action === 'pending_requests') {
    $userId = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    $groupId = isset($_GET['group_id']) ? intval($_GET['group_id']) : 0;
    
    if ($userId <= 0 || $groupId <= 0) {
        echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
        exit;
    }
    
    // Cek admin grup
    $aStmt = $db->prepare("SELECT admin_user_id FROM ngaji_groups WHERE id = ? LIMIT 1");
    $aStmt->execute([$groupId]);
    $group = $aStmt->fetch();
    
    if (!$group || (int)$group['admin_user_id'] !== $userId) {
        echo json_encode(['success' => false, 'message' => 'Akses ditolak. Anda bukan admin grup ini']);
        exit;
    }
    
    $pStmt = $db->prepare("
        SELECT m.user_id, u.name AS user_name, m.joined_at
        FROM group_members m
        JOIN users u ON u.id = m.user_id
        WHERE m.group_id = ? AND m.status = 'pending'
        ORDER BY m.joined_at ASC
    ");
    $pStmt->execute([$groupId]);
    $pending = $pStmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'data' => array_map(function($m) {
            return [
                'user_id' => (int)$m['user_id'],
                'user_name' => $m['user_name'],
                'role' => 'member',
                'joined_at' => $m['joined_at']
            ];
        }, $pending)
    ]);
    exit;
}

// ─── ACC/REJECT ANGGOTA (ADMIN ONLY) ─────────────────────────
if ($action === 'approve_member' || $action === 'reject_member') {
    $adminId = isset($_POST['admin_id']) ? intval($_POST['admin_id']) : 0;
    $targetUserId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $groupId = isset($_POST['group_id']) ? intval($_POST['group_id']) : 0;
    
    if ($adminId <= 0 || $targetUserId <= 0 || $groupId <= 0) {
        echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
        exit;
    }
    
    // Validasi apakah adminId benar-benar admin dari groupId
    $aStmt = $db->prepare("SELECT admin_user_id FROM ngaji_groups WHERE id = ? LIMIT 1");
    $aStmt->execute([$groupId]);
    $group = $aStmt->fetch();
    
    if (!$group || (int)$group['admin_user_id'] !== $adminId) {
        echo json_encode(['success' => false, 'message' => 'Anda bukan admin dari grup ini']);
        exit;
    }
    
    if ($action === 'approve_member') {
        $stmt = $db->prepare("
            UPDATE group_members 
            SET status = 'active', joined_at = NOW() 
            WHERE group_id = ? AND user_id = ?
        ");
    } else {
        $stmt = $db->prepare("
            DELETE FROM group_members 
            WHERE group_id = ? AND user_id = ?
        ");
    }
    $stmt->execute([$groupId, $targetUserId]);
    
    echo json_encode(['success' => true, 'message' => 'Status keanggotaan berhasil diperbarui']);
    exit;
}

// ─── RESPOND JOIN REQUEST (ADMIN ONLY) ───────────────────────
if ($action === 'respond_join_request') {
    $adminId = isset($_POST['admin_id']) ? intval($_POST['admin_id']) : 0;
    $targetUserId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $groupId = isset($_POST['group_id']) ? intval($_POST['group_id']) : 0;
    $responseAction = $_POST['action_type'] ?? ''; // 'approve' or 'reject'
    
    if ($adminId <= 0 || $targetUserId <= 0 || $groupId <= 0 || !in_array($responseAction, ['approve', 'reject'])) {
        echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
        exit;
    }
    
    // Validasi admin
    $aStmt = $db->prepare("SELECT admin_user_id FROM ngaji_groups WHERE id = ? LIMIT 1");
    $aStmt->execute([$groupId]);
    $group = $aStmt->fetch();
    
    if (!$group || (int)$group['admin_user_id'] !== $adminId) {
        echo json_encode(['success' => false, 'message' => 'Anda bukan admin dari grup ini']);
        exit;
    }
    
    if ($responseAction === 'approve') {
        $stmt = $db->prepare("
            UPDATE group_members 
            SET status = 'active', joined_at = NOW() 
            WHERE group_id = ? AND user_id = ?
        ");
    } else {
        $stmt = $db->prepare("
            DELETE FROM group_members 
            WHERE group_id = ? AND user_id = ?
        ");
    }
    $stmt->execute([$groupId, $targetUserId]);
    
    echo json_encode(['success' => true, 'message' => 'Status keanggotaan berhasil diperbarui']);
    exit;
}

// ─── UPDATE INFO GRUP (ADMIN ONLY) ───────────────────────────
if ($action === 'update_group') {
    $adminId     = isset($_POST['admin_id']) ? intval($_POST['admin_id']) : 0;
    $groupId     = isset($_POST['group_id']) ? intval($_POST['group_id']) : 0;
    $name        = trim($_POST['name'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $photoBase64  = $_POST['photo_base64'] ?? '';
    
    if ($adminId <= 0 || $groupId <= 0 || empty($name)) {
        echo json_encode(['success' => false, 'message' => 'Nama grup tidak boleh kosong']);
        exit;
    }
    
    // Cek admin grup
    $aStmt = $db->prepare("SELECT admin_user_id, photo_url FROM ngaji_groups WHERE id = ? LIMIT 1");
    $aStmt->execute([$groupId]);
    $group = $aStmt->fetch();
    
    if (!$group || (int)$group['admin_user_id'] !== $adminId) {
        echo json_encode(['success' => false, 'message' => 'Anda bukan admin grup ini']);
        exit;
    }
    
    // Handle photo upload jika dikirim
    $photoPath = $group['photo_url'];
    if (!empty($photoBase64)) {
        $imgData = base64_decode($photoBase64);
        if ($imgData !== false) {
            $dir = '../uploads/grup/';
            if (!file_exists($dir)) {
                mkdir($dir, 0777, true);
            }
            $fileName = uniqid() . '.jpg';
            file_put_contents($dir . $fileName, $imgData);
            $photoPath = 'uploads/grup/' . $fileName;
        }
    }
    
    $stmt = $db->prepare("
        UPDATE ngaji_groups 
        SET name = ?, description = ?, photo_url = ? 
        WHERE id = ?
    ");
    $stmt->execute([$name, $description, $photoPath, $groupId]);
    
    echo json_encode(['success' => true, 'message' => 'Grup berhasil diperbarui']);
    exit;
}

echo json_encode(['success' => false, 'message' => 'Action tidak dikenal']);
?>

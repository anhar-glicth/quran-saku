<?php
// ============================================================
// community_prayers_api.php
// API untuk Titip Doa Komunitas (list, post, react)
// ============================================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');

require_once __DIR__ . '/../config/database.php';

$db = getDB();

$action = $_REQUEST['action'] ?? 'list';
$userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;

// ─── LIST DOA ──────────────────────────────────────────────
if ($action === 'list') {
    $requestUserId = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    $limit  = isset($_GET['limit']) ? intval($_GET['limit']) : 20;

    $stmt = $db->prepare("
        SELECT 
            cp.id,
            cp.user_id,
            u.name AS user_name,
            cp.arabic_text,
            cp.latin_text,
            cp.like_count,
            cp.aamiin_count,
            cp.created_at,
            -- Cek apakah user ini sudah like
            (SELECT COUNT(*) FROM prayer_reactions pr 
             WHERE pr.prayer_id = cp.id AND pr.user_id = :req_user_id1 AND pr.reaction_type = 'like') AS is_liked,
            -- Cek apakah user ini sudah aamiin
            (SELECT COUNT(*) FROM prayer_reactions pr2 
             WHERE pr2.prayer_id = cp.id AND pr2.user_id = :req_user_id2 AND pr2.reaction_type = 'aamiin') AS is_aaminned
        FROM community_prayers cp
        JOIN users u ON u.id = cp.user_id
        ORDER BY cp.created_at DESC
        LIMIT :limit
    ");
    $stmt->execute([
        ':req_user_id1' => $requestUserId,
        ':req_user_id2' => $requestUserId,
        ':limit'        => $limit
    ]);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $prayers = array_map(function($row) {
        return [
            'id'          => (int)$row['id'],
            'user_id'     => (int)$row['user_id'],
            'user_name'   => $row['user_name'],
            'arabic_text' => $row['arabic_text'] ?? '',
            'latin_text'  => $row['latin_text'],
            'like_count'  => (int)$row['like_count'],
            'aamiin_count'=> (int)$row['aamiin_count'],
            'is_liked'    => (bool)$row['is_liked'],
            'is_aaminned' => (bool)$row['is_aaminned'],
            'created_at'  => $row['created_at']
        ];
    }, $rows);

    echo json_encode(['success' => true, 'data' => $prayers]);
    exit;
}

// ─── POST DOA BARU ─────────────────────────────────────────
if ($action === 'post_prayer') {
    if ($userId <= 0) {
        echo json_encode(['success' => false, 'message' => 'User tidak valid']);
        exit;
    }

    $latinText  = trim($_POST['latin_text'] ?? '');
    $arabicText = trim($_POST['arabic_text'] ?? '');

    if (empty($latinText)) {
        echo json_encode(['success' => false, 'message' => 'Isi doa tidak boleh kosong']);
        exit;
    }

    $stmt = $db->prepare("
        INSERT INTO community_prayers (user_id, arabic_text, latin_text)
        VALUES (:user_id, :arabic_text, :latin_text)
    ");
    $stmt->execute([
        ':user_id'     => $userId,
        ':arabic_text' => empty($arabicText) ? null : $arabicText,
        ':latin_text'  => $latinText
    ]);

    $newId = $db->lastInsertId();

    // Ambil nama user
    $userStmt = $db->prepare("SELECT name FROM users WHERE id = ?");
    $userStmt->execute([$userId]);
    $user = $userStmt->fetch();

    echo json_encode([
        'success' => true,
        'message' => 'Doa berhasil dikirim',
        'data' => [
            'id'          => (int)$newId,
            'user_id'     => $userId,
            'user_name'   => $user['name'] ?? '',
            'arabic_text' => $arabicText,
            'latin_text'  => $latinText,
            'like_count'  => 0,
            'aamiin_count'=> 0,
            'is_liked'    => false,
            'is_aaminned' => false,
            'created_at'  => date('Y-m-d H:i:s')
        ]
    ]);
    exit;
}

// ─── REACT (Like / Aamiin toggle) ──────────────────────────
if ($action === 'react') {
    if ($userId <= 0) {
        echo json_encode(['success' => false, 'message' => 'User tidak valid']);
        exit;
    }

    $prayerId     = intval($_POST['prayer_id'] ?? 0);
    $reactionType = $_POST['reaction_type'] ?? ''; // 'like' atau 'aamiin'

    if ($prayerId <= 0 || !in_array($reactionType, ['like', 'aamiin'])) {
        echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
        exit;
    }

    // Cek apakah sudah bereaksi
    $checkStmt = $db->prepare("
        SELECT id FROM prayer_reactions 
        WHERE user_id = :user_id AND prayer_id = :prayer_id AND reaction_type = :reaction_type
    ");
    $checkStmt->execute([
        ':user_id'       => $userId,
        ':prayer_id'     => $prayerId,
        ':reaction_type' => $reactionType
    ]);
    $existing = $checkStmt->fetch();

    $column = $reactionType === 'like' ? 'like_count' : 'aamiin_count';

    if ($existing) {
        // Cabut reaksi
        $db->prepare("DELETE FROM prayer_reactions WHERE user_id = ? AND prayer_id = ? AND reaction_type = ?")
           ->execute([$userId, $prayerId, $reactionType]);

        $db->prepare("UPDATE community_prayers SET $column = GREATEST(0, $column - 1) WHERE id = ?")
           ->execute([$prayerId]);

        $isActive = false;
    } else {
        // Tambah reaksi
        $db->prepare("INSERT INTO prayer_reactions (user_id, prayer_id, reaction_type) VALUES (?,?,?)")
           ->execute([$userId, $prayerId, $reactionType]);

        $db->prepare("UPDATE community_prayers SET $column = $column + 1 WHERE id = ?")
           ->execute([$prayerId]);

        $isActive = true;
    }

    // Ambil count terbaru
    $cntStmt = $db->prepare("SELECT like_count, aamiin_count FROM community_prayers WHERE id = ?");
    $cntStmt->execute([$prayerId]);
    $counts = $cntStmt->fetch();

    echo json_encode([
        'success'      => true,
        'is_active'    => $isActive,
        'like_count'   => (int)($counts['like_count'] ?? 0),
        'aamiin_count' => (int)($counts['aamiin_count'] ?? 0)
    ]);
    exit;
}

echo json_encode(['success' => false, 'message' => 'Action tidak dikenal']);

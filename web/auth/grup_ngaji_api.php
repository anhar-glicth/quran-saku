<?php
// ============================================================
// grup_ngaji_api.php
// API untuk Fitup Grup Ngaji Komunitas
// ============================================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');

require_once __DIR__ . '/../config/database.php';

$db = getDB();
$action = $_REQUEST['action'] ?? '';
$userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : (isset($_GET['user_id']) ? intval($_GET['user_id']) : 0);

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

if (empty($action)) {
    echo json_encode(['success' => false, 'message' => 'Action tidak boleh kosong']);
    exit;
}

switch ($action) {
    case 'create_group':
        if ($userId <= 0) {
            echo json_encode(['success' => false, 'message' => 'User ID tidak valid']);
            exit;
        }
        $name = trim($_POST['name'] ?? '');
        $targetKhatam = intval($_POST['target_khatam'] ?? 1);
        $durationDays = intval($_POST['duration_days'] ?? 30);
        $photoUrl = trim($_POST['photo_url'] ?? '');

        if (empty($name)) {
            echo json_encode(['success' => false, 'message' => 'Nama grup tidak boleh kosong']);
            exit;
        }

        $groupCode = generateGroupCode($db);
        $startDate = date('Y-m-d');
        $endDate = date('Y-m-d', strtotime("+$durationDays days"));

        try {
            $db->beginTransaction();

            $stmt = $db->prepare("
                INSERT INTO ngaji_groups (group_code, name, photo_url, creator_id, target_khatam, duration_days, start_date, end_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ");
            $stmt->execute([$groupCode, $name, empty($photoUrl) ? null : $photoUrl, $userId, $targetKhatam, $durationDays, $startDate, $endDate]);
            $groupId = $db->lastInsertId();

            // Creator otomatis jadi admin & active member di grup tersebut
            $stmtMember = $db->prepare("
                INSERT INTO ngaji_group_members (group_id, user_id, role, status)
                VALUES (?, ?, 'admin', 'active')
            ");
            $stmtMember->execute([$groupId, $userId]);

            $db->commit();
            echo json_encode([
                'success' => true,
                'message' => 'Grup berhasil dibuat',
                'data' => [
                    'id' => (int)$groupId,
                    'group_code' => $groupCode,
                    'name' => $name,
                    'photo_url' => $photoUrl,
                    'target_khatam' => $targetKhatam,
                    'duration_days' => $durationDays
                ]
            ]);
        } catch (Exception $e) {
            $db->rollBack();
            echo json_encode(['success' => false, 'message' => 'Gagal membuat grup: ' . $e->getMessage()]);
        }
        break;

    case 'get_my_groups':
        if ($userId <= 0) {
            echo json_encode(['success' => false, 'message' => 'User ID tidak valid']);
            exit;
        }
        // Ambil grup-grup di mana status user adalah 'active' atau 'pending'
        $stmt = $db->prepare("
            SELECT g.*, m.role, m.status AS member_status,
                   (SELECT COUNT(*) FROM ngaji_group_members WHERE group_id = g.id AND status = 'active') AS member_count,
                   (SELECT r.surah_name FROM ngaji_reading_relay r WHERE r.group_id = g.id ORDER BY r.read_at DESC LIMIT 1) AS last_read_surah,
                   (SELECT r.ayah_number FROM ngaji_reading_relay r WHERE r.group_id = g.id ORDER BY r.read_at DESC LIMIT 1) AS last_read_ayah,
                   (SELECT u.name FROM ngaji_reading_relay r JOIN users u ON u.id = r.user_id WHERE r.group_id = g.id ORDER BY r.read_at DESC LIMIT 1) AS last_read_user
            FROM ngaji_groups g
            JOIN ngaji_group_members m ON m.group_id = g.id
            WHERE m.user_id = ?
            ORDER BY g.created_at DESC
        ");
        $stmt->execute([$userId]);
        $groups = $stmt->fetchAll(PDO::FETCH_ASSOC);

        echo json_encode(['success' => true, 'data' => $groups]);
        break;

    case 'join_group':
        if ($userId <= 0) {
            echo json_encode(['success' => false, 'message' => 'User ID tidak valid']);
            exit;
        }
        $groupCode = strtoupper(trim($_POST['group_code'] ?? ''));
        if (empty($groupCode)) {
            echo json_encode(['success' => false, 'message' => 'Kode grup tidak boleh kosong']);
            exit;
        }

        // Cari grup berdasarkan kode
        $stmt = $db->prepare("SELECT id FROM ngaji_groups WHERE group_code = ?");
        $stmt->execute([$groupCode]);
        $group = $stmt->fetch();

        if (!$group) {
            echo json_encode(['success' => false, 'message' => 'Grup tidak ditemukan dengan kode tersebut']);
            exit;
        }

        $groupId = $group['id'];

        // Cek jika sudah tergabung atau minta join
        $stmtCheck = $db->prepare("SELECT status FROM ngaji_group_members WHERE group_id = ? AND user_id = ?");
        $stmtCheck->execute([$groupId, $userId]);
        $existing = $stmtCheck->fetch();

        if ($existing) {
            if ($existing['status'] === 'active') {
                echo json_encode(['success' => false, 'message' => 'Anda sudah bergabung di grup ini']);
            } else if ($existing['status'] === 'pending') {
                echo json_encode(['success' => false, 'message' => 'Permintaan join Anda sedang menunggu persetujuan admin']);
            } else {
                // Jika rejected, bisa minta gabung lagi (update status ke pending)
                $stmtUpdate = $db->prepare("UPDATE ngaji_group_members SET status = 'pending' WHERE group_id = ? AND user_id = ?");
                $stmtUpdate->execute([$groupId, $userId]);
                echo json_encode(['success' => true, 'message' => 'Mengirim ulang permintaan gabung ke admin']);
            }
            exit;
        }

        // Simpan request join (pending status)
        $stmtInsert = $db->prepare("
            INSERT INTO ngaji_group_members (group_id, user_id, role, status)
            VALUES (?, ?, 'member', 'pending')
        ");
        if ($stmtInsert->execute([$groupId, $userId])) {
            echo json_encode(['success' => true, 'message' => 'Permintaan bergabung telah dikirim ke admin']);
        } else {
            echo json_encode(['success' => false, 'message' => 'Gagal mengirim permintaan']);
        }
        break;

    case 'get_group_detail':
        $groupId = isset($_GET['group_id']) ? intval($_GET['group_id']) : 0;
        if ($groupId <= 0) {
            echo json_encode(['success' => false, 'message' => 'Group ID tidak valid']);
            exit;
        }

        // Ambil info grup
        $stmtGroup = $db->prepare("
            SELECT g.*, u.name AS creator_name,
                   (SELECT COUNT(*) FROM ngaji_group_members WHERE group_id = g.id AND status = 'active') AS member_count
            FROM ngaji_groups g
            JOIN users u ON u.id = g.creator_id
            WHERE g.id = ?
        ");
        $stmtGroup->execute([$groupId]);
        $groupInfo = $stmtGroup->fetch(PDO::FETCH_ASSOC);

        if (!$groupInfo) {
            echo json_encode(['success' => false, 'message' => 'Grup tidak ditemukan']);
            exit;
        }

        // Ambil data anggota aktif
        $stmtMembers = $db->prepare("
            SELECT u.id, u.name, u.email, m.role, m.status, m.joined_at
            FROM ngaji_group_members m
            JOIN users u ON u.id = m.user_id
            WHERE m.group_id = ? AND m.status = 'active'
            ORDER BY m.role DESC, u.name ASC
        ");
        $stmtMembers->execute([$groupId]);
        $members = $stmtMembers->fetchAll(PDO::FETCH_ASSOC);

        // Ambil list bacaan terakhir / relay
        $stmtRelay = $db->prepare("
            SELECT r.*, u.name AS user_name
            FROM ngaji_reading_relay r
            JOIN users u ON u.id = r.user_id
            WHERE r.group_id = ?
            ORDER BY r.read_at DESC
        ");
        $stmtRelay->execute([$groupId]);
        $relayHistory = $stmtRelay->fetchAll(PDO::FETCH_ASSOC);

        // Cari progress / target khatam progress berdasarkan relay page read
        // 1 khatam = 604 halaman
        $totalPagesRead = 0;
        $pagesMap = [];
        foreach ($relayHistory as $r) {
            if ($r['page_number'] > 0) {
                $pagesMap[$r['page_number']] = true;
            }
        }
        $totalPagesRead = count($pagesMap);
        $totalTargetPages = $groupInfo['target_khatam'] * 604;
        $progressPct = min(100, $totalTargetPages > 0 ? round(($totalPagesRead / $totalTargetPages) * 100) : 0);

        echo json_encode([
            'success' => true,
            'group' => $groupInfo,
            'progress_percent' => $progressPct,
            'members' => $members,
            'relay' => $relayHistory
        ]);
        break;

    case 'update_group_name':
        if ($userId <= 0) {
            echo json_encode(['success' => false, 'message' => 'User ID tidak valid']);
            exit;
        }
        $groupId = isset($_POST['group_id']) ? intval($_POST['group_id']) : 0;
        $newName = trim($_POST['name'] ?? '');
        $photoUrl = trim($_POST['photo_url'] ?? '');

        if ($groupId <= 0 || empty($newName)) {
            echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
            exit;
        }

        // Pastikan user adalah admin grup
        $stmtCheck = $db->prepare("SELECT role FROM ngaji_group_members WHERE group_id = ? AND user_id = ? AND role = 'admin'");
        $stmtCheck->execute([$groupId, $userId]);
        if (!$stmtCheck->fetch()) {
            echo json_encode(['success' => false, 'message' => 'Hanya admin grup yang dapat mengubah info grup']);
            exit;
        }

        $stmtUpdate = $db->prepare("UPDATE ngaji_groups SET name = ?, photo_url = ? WHERE id = ?");
        if ($stmtUpdate->execute([$newName, empty($photoUrl) ? null : $photoUrl, $groupId])) {
            echo json_encode(['success' => true, 'message' => 'Info grup berhasil diupdate']);
        } else {
            echo json_encode(['success' => false, 'message' => 'Gagal mengupdate info grup']);
        }
        break;

    case 'get_pending_members':
        if ($userId <= 0) {
            echo json_encode(['success' => false, 'message' => 'User ID tidak valid']);
            exit;
        }
        $groupId = isset($_GET['group_id']) ? intval($_GET['group_id']) : 0;

        // Pastikan user adalah admin grup
        $stmtCheck = $db->prepare("SELECT role FROM ngaji_group_members WHERE group_id = ? AND user_id = ? AND role = 'admin'");
        $stmtCheck->execute([$groupId, $userId]);
        if (!$stmtCheck->fetch()) {
            echo json_encode(['success' => false, 'message' => 'Akses ditolak']);
            exit;
        }

        $stmt = $db->prepare("
            SELECT m.id AS member_row_id, u.id AS user_id, u.name, u.email, m.joined_at
            FROM ngaji_group_members m
            JOIN users u ON u.id = m.user_id
            WHERE m.group_id = ? AND m.status = 'pending'
            ORDER BY m.joined_at ASC
        ");
        $stmt->execute([$groupId]);
        $pending = $stmt->fetchAll(PDO::FETCH_ASSOC);

        echo json_encode(['success' => true, 'data' => $pending]);
        break;

    case 'respond_join_request':
        if ($userId <= 0) {
            echo json_encode(['success' => false, 'message' => 'User ID tidak valid']);
            exit;
        }
        $groupId = isset($_POST['group_id']) ? intval($_POST['group_id']) : 0;
        $targetMemberId = isset($_POST['target_user_id']) ? intval($_POST['target_user_id']) : 0;
        $responseAction = $_POST['response_action'] ?? ''; // 'approve' atau 'reject'

        if ($groupId <= 0 || $targetMemberId <= 0 || !in_array($responseAction, ['approve', 'reject'])) {
            echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
            exit;
        }

        // Cek admin status
        $stmtCheck = $db->prepare("SELECT role FROM ngaji_group_members WHERE group_id = ? AND user_id = ? AND role = 'admin'");
        $stmtCheck->execute([$groupId, $userId]);
        if (!$stmtCheck->fetch()) {
            echo json_encode(['success' => false, 'message' => 'Hanya admin yang dapat menyetujui permintaan']);
            exit;
        }

        $status = ($responseAction === 'approve') ? 'active' : 'rejected';
        $stmtUpdate = $db->prepare("UPDATE ngaji_group_members SET status = ? WHERE group_id = ? AND user_id = ?");
        if ($stmtUpdate->execute([$status, $groupId, $targetMemberId])) {
            echo json_encode(['success' => true, 'message' => 'Berhasil memproses permintaan gabung']);
        } else {
            echo json_encode(['success' => false, 'message' => 'Gagal memproses permintaan']);
        }
        break;

    case 'add_reading_relay':
        if ($userId <= 0) {
            echo json_encode(['success' => false, 'message' => 'User ID tidak valid']);
            exit;
        }
        $groupId = isset($_POST['group_id']) ? intval($_POST['group_id']) : 0;
        $surahNumber = intval($_POST['surah_number'] ?? 0);
        $surahName = trim($_POST['surah_name'] ?? '');
        $ayahNumber = intval($_POST['ayah_number'] ?? 0);
        $pageNumber = isset($_POST['page_number']) ? intval($_POST['page_number']) : null;

        if ($groupId <= 0 || $surahNumber <= 0 || $ayahNumber <= 0) {
            echo json_encode(['success' => false, 'message' => 'Parameter tidak valid']);
            exit;
        }

        // Pastikan user adalah active member grup
        $stmtCheck = $db->prepare("SELECT status FROM ngaji_group_members WHERE group_id = ? AND user_id = ? AND status = 'active'");
        $stmtCheck->execute([$groupId, $userId]);
        if (!$stmtCheck->fetch()) {
            echo json_encode(['success' => false, 'message' => 'Anda bukan anggota aktif grup ini']);
            exit;
        }

        $stmtInsert = $db->prepare("
            INSERT INTO ngaji_reading_relay (group_id, user_id, surah_number, surah_name, ayah_number, page_number)
            VALUES (?, ?, ?, ?, ?, ?)
        ");
        if ($stmtInsert->execute([$groupId, $userId, $surahNumber, $surahName, $ayahNumber, $pageNumber])) {
            echo json_encode(['success' => true, 'message' => 'Berhasil membagikan tilawah terakhir ke grup']);
        } else {
            echo json_encode(['success' => false, 'message' => 'Gagal menyimpan bacaan relay']);
        }
        break;

    default:
        echo json_encode(['success' => false, 'message' => 'Action tidak dikenali']);
        break;
}

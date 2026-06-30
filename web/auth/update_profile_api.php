<?php
// ============================================
// Quran Saku - Update Profile API
// ============================================

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/functions.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$userId = intval($_POST['user_id'] ?? 0);
$name   = sanitize($_POST['name']   ?? '');
$email  = sanitize($_POST['email']  ?? '');

if ($userId <= 0 || empty($name) || empty($email)) {
    echo json_encode(['success' => false, 'message' => 'User ID, nama, dan email wajib diisi.']);
    exit;
}

if (!isValidEmail($email)) {
    echo json_encode(['success' => false, 'message' => 'Format email tidak valid.']);
    exit;
}

try {
    $db = getDB();
    
    // Check if email is already taken by another user
    $checkStmt = $db->prepare("SELECT id FROM users WHERE email = ? AND id != ? LIMIT 1");
    $checkStmt->execute([$email, $userId]);
    if ($checkStmt->fetch()) {
        echo json_encode(['success' => false, 'message' => 'Email sudah digunakan oleh pengguna lain.']);
        exit;
    }

    // Update profile details
    $stmt = $db->prepare("UPDATE users SET name = ?, email = ? WHERE id = ?");
    $result = $stmt->execute([$name, $email, $userId]);

    if ($result) {
        echo json_encode([
            'success' => true,
            'message' => 'Profil berhasil diperbarui.',
            'user' => [
                'id' => $userId,
                'name' => $name,
                'email' => $email
            ]
        ]);
    } else {
        echo json_encode(['success' => false, 'message' => 'Gagal memperbarui profil.']);
    }

} catch (Exception $e) {
    error_log($e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Server error: ' . $e->getMessage()]);
}

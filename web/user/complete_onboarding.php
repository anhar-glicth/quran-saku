<?php
// ============================================
// Quran Saku - Selesaikan Onboarding
// Set session flag + redirect ke dashboard
// ============================================

require_once __DIR__ . '/../includes/session.php';

requireLogin();

// Tandai onboarding selesai di session
$_SESSION['onboarding_done'] = true;

// Bisa juga simpan ke database
try {
    $db = getDB();
    // Anda bisa tambah kolom onboarding_done di tabel users
    // $db->prepare("UPDATE users SET onboarding_done = 1 WHERE id = ?")->execute([$_SESSION['user_id']]);
} catch (Exception $e) {}

redirect(APP_URL . '/user/dashboard.php');

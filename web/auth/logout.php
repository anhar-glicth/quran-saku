<?php
// ============================================
// Quran Saku - Logout
// ============================================

require_once __DIR__ . '/../includes/session.php';
require_once __DIR__ . '/../includes/functions.php';

if (isLoggedIn()) {
    logActivity($_SESSION['user_id'], 'logout', 'User logout');
}

destroySession();
redirect(APP_URL . '/index.php?msg=logged_out');

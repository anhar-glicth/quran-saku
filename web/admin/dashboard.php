<?php
// ============================================
// Quran Saku - Dashboard Admin
// ============================================

require_once __DIR__ . '/../includes/session.php';
require_once __DIR__ . '/../includes/functions.php';

requireAdmin();

$user = getCurrentUser();
$initials = getAvatarInitials($user['name']);

// Ambil data statistik
try {
    $db = getDB();

    $totalUsers     = $db->query("SELECT COUNT(*) FROM users WHERE role = 'user'")->fetchColumn();
    $totalAdmins    = $db->query("SELECT COUNT(*) FROM users WHERE role = 'admin'")->fetchColumn();
    $totalBookmarks = $db->query("SELECT COUNT(*) FROM bookmarks")->fetchColumn();

    // Ambil daftar user terbaru
    $stmtUsers = $db->query("SELECT id, name, email, role, is_active, last_login, created_at FROM users ORDER BY created_at DESC LIMIT 10");
    $users = $stmtUsers->fetchAll();

    // Ambil log aktivitas terbaru
    $stmtLogs = $db->query("
        SELECT al.action, al.description, al.ip_address, al.created_at, u.name
        FROM activity_log al
        LEFT JOIN users u ON al.user_id = u.id
        ORDER BY al.created_at DESC LIMIT 8
    ");
    $logs = $stmtLogs->fetchAll();

} catch (Exception $e) {
    $totalUsers = $totalAdmins = $totalBookmarks = 0;
    $users = $logs = [];
}

// Aksi: Toggle status aktif user
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action'])) {
    $targetId = (int)($_POST['user_id'] ?? 0);
    if ($_POST['action'] === 'toggle_status' && $targetId) {
        try {
            $db->prepare("UPDATE users SET is_active = 1 - is_active WHERE id = ? AND role != 'admin'")
               ->execute([$targetId]);
            logActivity($user['id'], 'admin_toggle_user', "Admin mengubah status user ID $targetId");
        } catch (Exception $e) {}
    }
    redirect(APP_URL . '/admin/dashboard.php');
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Strava Quran - Admin Panel</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <style>
        :root {
            --primary: #FF9800;
            --primary-hover: #F57C00;
            --primary-light: #FFF3E0;
            --bg-body: #F5F7FA;
            --sidebar-bg: #1A2035;
            --sidebar-text: #A0AEC0;
            --sidebar-hover: rgba(255, 152, 0, 0.08);
            --sidebar-active: rgba(255, 152, 0, 0.15);
            --card-bg: #FFFFFF;
            --text-main: #2D3748;
            --text-muted: #718096;
            --border-color: #E2E8F0;
            --success-color: #38A169;
            --error-color: #E53E3E;
            --shadow-md: 0 4px 20px -2px rgba(0, 0, 0, 0.06);
            --radius-lg: 16px;
            --radius-md: 12px;
            --transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
            background-color: var(--bg-body);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
        }

        /* Sidebar */
        .sidebar {
            width: 240px;
            min-height: 100vh;
            background-color: var(--sidebar-bg);
            display: flex;
            flex-direction: column;
            position: fixed;
            top: 0;
            left: 0;
            z-index: 200;
            padding-top: 20px;
        }

        .sidebar-logo {
            padding: 0 24px 25px;
            border-bottom: 1px solid rgba(255,255,255,0.06);
            margin-bottom: 20px;
        }

        .sidebar-logo h2 {
            font-family: 'Outfit', sans-serif;
            font-size: 22px;
            font-weight: 800;
            color: var(--primary);
        }

        .sidebar-logo h2 span { color: #fff; }

        .sidebar-logo small {
            display: block;
            font-size: 11px;
            color: var(--sidebar-text);
            margin-top: 3px;
            text-transform: uppercase;
            letter-spacing: 1px;
        }

        .nav-link {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 13px 24px;
            color: var(--sidebar-text);
            text-decoration: none;
            font-size: 14px;
            font-weight: 500;
            transition: var(--transition);
            border-radius: 0;
        }

        .nav-link:hover {
            background-color: var(--sidebar-hover);
            color: #fff;
        }

        .nav-link.active {
            background-color: var(--sidebar-active);
            color: var(--primary);
            border-right: 3px solid var(--primary);
        }

        .nav-link i {
            width: 20px;
            text-align: center;
        }

        .sidebar-footer {
            margin-top: auto;
            padding: 16px 24px;
            border-top: 1px solid rgba(255,255,255,0.06);
        }

        .sidebar-user {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 14px;
        }

        .avatar-sm {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background-color: var(--primary-light);
            color: var(--primary);
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 13px;
            border: 2px solid var(--primary);
        }

        .sidebar-user-name {
            font-size: 13px;
            font-weight: 600;
            color: #fff;
        }

        .sidebar-user-role {
            font-size: 11px;
            color: var(--primary);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .btn-logout {
            width: 100%;
            padding: 10px;
            border-radius: var(--radius-md);
            border: 1px solid rgba(255,255,255,0.1);
            background: transparent;
            color: var(--sidebar-text);
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            text-decoration: none;
            transition: var(--transition);
        }

        .btn-logout:hover {
            background-color: rgba(229, 62, 62, 0.1);
            color: #FC8181;
            border-color: rgba(229, 62, 62, 0.3);
        }

        /* Main Content Area */
        .main-content {
            margin-left: 240px;
            flex: 1;
            padding: 40px;
        }

        .page-header {
            margin-bottom: 32px;
        }

        .page-header h1 {
            font-family: 'Outfit', sans-serif;
            font-size: 30px;
            font-weight: 700;
            margin-bottom: 5px;
        }

        .page-header p {
            color: var(--text-muted);
            font-size: 15px;
        }

        /* Stats Grid */
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 20px;
            margin-bottom: 36px;
        }

        @media (max-width: 900px) {
            .stats-grid { grid-template-columns: 1fr 1fr; }
            .main-content { padding: 20px; }
        }

        .stat-card {
            background-color: var(--card-bg);
            border-radius: var(--radius-lg);
            padding: 24px;
            box-shadow: var(--shadow-md);
            display: flex;
            align-items: center;
            gap: 18px;
        }

        .stat-icon {
            width: 54px;
            height: 54px;
            border-radius: var(--radius-md);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
        }

        .stat-icon.orange { background-color: var(--primary-light); color: var(--primary); }
        .stat-icon.green  { background-color: #F0FFF4; color: var(--success-color); }
        .stat-icon.blue   { background-color: #EBF8FF; color: #3182CE; }

        .stat-info label {
            font-size: 12px;
            font-weight: 600;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .stat-info h3 {
            font-size: 26px;
            font-weight: 700;
            color: var(--text-main);
        }

        /* Panel Card */
        .panel-card {
            background-color: var(--card-bg);
            border-radius: var(--radius-lg);
            box-shadow: var(--shadow-md);
            margin-bottom: 30px;
            overflow: hidden;
        }

        .panel-header {
            padding: 22px 28px;
            border-bottom: 1px solid var(--border-color);
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .panel-header h2 {
            font-family: 'Outfit', sans-serif;
            font-size: 18px;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        /* Table Styles */
        table {
            width: 100%;
            border-collapse: collapse;
        }

        th, td {
            padding: 14px 20px;
            text-align: left;
            font-size: 13px;
        }

        th {
            background-color: #F8FAFC;
            font-weight: 700;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            border-bottom: 1px solid var(--border-color);
        }

        td {
            border-bottom: 1px solid #F7FAFC;
            color: var(--text-main);
        }

        tr:last-child td {
            border-bottom: none;
        }

        tr:hover td {
            background-color: #FAFBFC;
        }

        /* Status Badge */
        .badge {
            padding: 4px 12px;
            border-radius: 999px;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .badge-green  { background-color: #F0FFF4; color: var(--success-color); }
        .badge-red    { background-color: #FFF5F5; color: var(--error-color); }
        .badge-orange { background-color: var(--primary-light); color: var(--primary); }

        /* Action Buttons */
        .btn-action {
            padding: 6px 14px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
            border: none;
            cursor: pointer;
            transition: var(--transition);
        }

        .btn-activate   { background-color: #F0FFF4; color: var(--success-color); }
        .btn-deactivate { background-color: #FFF5F5; color: var(--error-color); }
        .btn-activate:hover   { background-color: var(--success-color); color: #fff; }
        .btn-deactivate:hover { background-color: var(--error-color); color: #fff; }

        /* Log List */
        .log-list {
            padding: 20px 28px;
        }

        .log-item {
            display: flex;
            gap: 14px;
            align-items: flex-start;
            padding: 12px 0;
            border-bottom: 1px solid #F7FAFC;
        }

        .log-item:last-child {
            border-bottom: none;
        }

        .log-dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background-color: var(--primary);
            margin-top: 5px;
            flex-shrink: 0;
        }

        .log-action {
            font-size: 13px;
            font-weight: 600;
            color: var(--text-main);
        }

        .log-desc {
            font-size: 12px;
            color: var(--text-muted);
        }

        .log-time {
            font-size: 11px;
            color: #A0AEC0;
            margin-left: auto;
            flex-shrink: 0;
        }
    </style>
</head>
<body>

    <!-- Sidebar -->
    <aside class="sidebar">
        <div class="sidebar-logo">
            <h2>Quran <span>saku</span></h2>
            <small>Admin Panel</small>
        </div>

        <a href="dashboard.php" class="nav-link active">
            <i class="fa-solid fa-gauge-high"></i> Dashboard
        </a>
        <a href="#" class="nav-link" onclick="alert('Fitur manajemen user segera hadir!')">
            <i class="fa-solid fa-users"></i> Manajemen User
        </a>
        <a href="#" class="nav-link" onclick="alert('Fitur konten segera hadir!')">
            <i class="fa-solid fa-book-quran"></i> Konten Quran
        </a>
        <a href="#" class="nav-link" onclick="alert('Fitur laporan segera hadir!')">
            <i class="fa-solid fa-chart-bar"></i> Laporan
        </a>
        <a href="#" class="nav-link" onclick="alert('Pengaturan segera hadir!')">
            <i class="fa-solid fa-gear"></i> Pengaturan
        </a>

        <div class="sidebar-footer">
            <div class="sidebar-user">
                <div class="avatar-sm"><?php echo $initials; ?></div>
                <div>
                    <div class="sidebar-user-name"><?php echo htmlspecialchars($user['name']); ?></div>
                    <div class="sidebar-user-role">Administrator</div>
                </div>
            </div>
            <a href="../auth/logout.php" class="btn-logout">
                <i class="fa-solid fa-right-from-bracket"></i> Keluar
            </a>
        </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
        
        <div class="page-header">
            <h1>Dashboard Admin</h1>
            <p>Ringkasan dan statistik aplikasi Strava Quran</p>
        </div>

        <!-- Stats -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-icon orange">
                    <i class="fa-solid fa-users"></i>
                </div>
                <div class="stat-info">
                    <label>Total Pengguna</label>
                    <h3><?php echo $totalUsers; ?></h3>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon green">
                    <i class="fa-solid fa-user-shield"></i>
                </div>
                <div class="stat-info">
                    <label>Administrator</label>
                    <h3><?php echo $totalAdmins; ?></h3>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon blue">
                    <i class="fa-solid fa-bookmark"></i>
                </div>
                <div class="stat-info">
                    <label>Total Bookmark</label>
                    <h3><?php echo $totalBookmarks; ?></h3>
                </div>
            </div>
        </div>

        <!-- User Management Table -->
        <div class="panel-card">
            <div class="panel-header">
                <h2><i class="fa-solid fa-users" style="color: var(--primary);"></i> Daftar Pengguna</h2>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Nama</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th>Bergabung</th>
                        <th>Aksi</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (empty($users)): ?>
                        <tr><td colspan="7" style="text-align: center; color: var(--text-muted);">Tidak ada data pengguna.</td></tr>
                    <?php else: ?>
                        <?php foreach ($users as $i => $u): ?>
                            <tr>
                                <td><?php echo $i + 1; ?></td>
                                <td style="font-weight: 600;"><?php echo htmlspecialchars($u['name']); ?></td>
                                <td style="color: var(--text-muted);"><?php echo htmlspecialchars($u['email']); ?></td>
                                <td>
                                    <span class="badge <?php echo $u['role'] === 'admin' ? 'badge-orange' : 'badge-green'; ?>">
                                        <?php echo $u['role']; ?>
                                    </span>
                                </td>
                                <td>
                                    <span class="badge <?php echo $u['is_active'] ? 'badge-green' : 'badge-red'; ?>">
                                        <?php echo $u['is_active'] ? 'Aktif' : 'Nonaktif'; ?>
                                    </span>
                                </td>
                                <td style="color: var(--text-muted);"><?php echo formatDateID($u['created_at']); ?></td>
                                <td>
                                    <?php if ($u['role'] !== 'admin'): ?>
                                        <form method="POST" style="display: inline;">
                                            <input type="hidden" name="action" value="toggle_status">
                                            <input type="hidden" name="user_id" value="<?php echo $u['id']; ?>">
                                            <button type="submit" class="btn-action <?php echo $u['is_active'] ? 'btn-deactivate' : 'btn-activate'; ?>"
                                                onclick="return confirm('Ubah status pengguna ini?')">
                                                <?php echo $u['is_active'] ? 'Nonaktifkan' : 'Aktifkan'; ?>
                                            </button>
                                        </form>
                                    <?php else: ?>
                                        <span style="font-size: 12px; color: var(--text-muted);">—</span>
                                    <?php endif; ?>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>

        <!-- Activity Log -->
        <div class="panel-card">
            <div class="panel-header">
                <h2><i class="fa-solid fa-clock-rotate-left" style="color: var(--primary);"></i> Log Aktivitas Terbaru</h2>
            </div>
            <div class="log-list">
                <?php if (empty($logs)): ?>
                    <p style="color: var(--text-muted); text-align: center;">Belum ada aktivitas tercatat.</p>
                <?php else: ?>
                    <?php foreach ($logs as $log): ?>
                        <div class="log-item">
                            <div class="log-dot"></div>
                            <div>
                                <div class="log-action"><?php echo htmlspecialchars($log['action']); ?>
                                    <?php if ($log['name']): ?>
                                        <span style="color: var(--primary);">— <?php echo htmlspecialchars($log['name']); ?></span>
                                    <?php endif; ?>
                                </div>
                                <div class="log-desc"><?php echo htmlspecialchars($log['description'] ?? ''); ?></div>
                            </div>
                            <span class="log-time"><?php echo date('d/m H:i', strtotime($log['created_at'])); ?></span>
                        </div>
                    <?php endforeach; ?>
                <?php endif; ?>
            </div>
        </div>

    </main>

</body>
</html>

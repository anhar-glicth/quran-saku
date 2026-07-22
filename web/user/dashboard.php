<?php
// ============================================
// Quran Saku - Dashboard User
// ============================================

require_once __DIR__ . '/../includes/session.php';
require_once __DIR__ . '/../includes/functions.php';

// Wajib login
requireLogin();

// Jangan izinkan admin mengakses dashboard user biasa (atau sesuaikan kebutuhan)
if (isAdmin()) {
    redirect(APP_URL . '/admin/dashboard.php');
}

$user = getCurrentUser();
if (!$user) {
    destroySession();
    redirect(APP_URL . '/index.php?msg=login_required');
}

$initials = getAvatarInitials($user['name']);

// Mock Data Surah
$surahs = [
    1 => ['name' => 'Al-Fatihah', 'ayahs' => 7, 'type' => 'Makkiyah'],
    2 => ['name' => 'Al-Baqarah', 'ayahs' => 286, 'type' => 'Madaniyah'],
    3 => ['name' => 'Ali \'Imran', 'ayahs' => 200, 'type' => 'Madaniyah'],
    4 => ['name' => 'An-Nisa\'', 'ayahs' => 176, 'type' => 'Madaniyah'],
    5 => ['name' => 'Al-Ma\'idah', 'ayahs' => 120, 'type' => 'Madaniyah'],
    6 => ['name' => 'Al-An\'am', 'ayahs' => 165, 'type' => 'Makkiyah'],
];

// Ambil riwayat membaca terakhir
$progress = getLastReadingProgress($user['id']);
$bookmarks_count = getUserBookmarkCount($user['id']);

// Fitur Tambah Bookmark (Simpel)
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'add_bookmark') {
    $surah_num = (int)($_POST['surah_number'] ?? 1);
    $ayah_num = (int)($_POST['ayah_number'] ?? 1);
    $surah_name = $surahs[$surah_num]['name'] ?? 'Surah';
    
    try {
        $db = getDB();
        $stmt = $db->prepare("INSERT INTO bookmarks (user_id, surah_number, surah_name, ayah_number) VALUES (?, ?, ?, ?)");
        $stmt->execute([$user['id'], $surah_num, $surah_name, $ayah_num]);
        logActivity($user['id'], 'add_bookmark', "Menambahkan bookmark $surah_name Ayat $ayah_num");
        setFlash('success', "Berhasil menambahkan Bookmark: Surah $surah_name ayat $ayah_num!");
        redirect(APP_URL . '/user/dashboard.php');
    } catch (Exception $e) {
        setFlash('error', 'Gagal menambahkan bookmark.');
    }
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Strava Quran - Dashboard</title>
    
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    
    <!-- FontAwesome Icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <style>
        :root {
            --primary: #FF9800;
            --primary-hover: #F57C00;
            --primary-light: #FFF3E0;
            --bg-body: #FAF9F6;
            --card-bg: #FFFFFF;
            --text-main: #2D3748;
            --text-muted: #718096;
            --border-color: #E2E8F0;
            --success-color: #38A169;
            --error-color: #E53E3E;
            --shadow-md: 0 4px 20px -2px rgba(255, 152, 0, 0.08), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
            --radius-lg: 16px;
            --radius-md: 12px;
            --transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
            background-color: var(--bg-body);
            color: var(--text-main);
            min-height: 100vh;
        }

        /* Navbar */
        nav {
            background-color: var(--card-bg);
            border-bottom: 1px solid var(--border-color);
            padding: 15px 30px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            position: sticky;
            top: 0;
            z-index: 100;
        }

        .nav-logo {
            font-size: 24px;
            font-weight: 800;
            color: var(--primary);
            font-family: 'Outfit', sans-serif;
            text-decoration: none;
        }

        .nav-logo span {
            color: var(--text-main);
        }

        .nav-profile {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background-color: var(--primary-light);
            color: var(--primary);
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
            border: 2px solid var(--primary);
        }

        .logout-btn {
            padding: 8px 16px;
            border-radius: var(--radius-md);
            border: 1px solid var(--border-color);
            background: none;
            color: var(--text-muted);
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            text-decoration: none;
            transition: var(--transition);
        }

        .logout-btn:hover {
            background-color: #FFF5F5;
            color: var(--error-color);
            border-color: #FED7D7;
        }

        /* Main Layout */
        .container {
            max-width: 1000px;
            margin: 40px auto;
            padding: 0 20px;
        }

        /* Welcome Header */
        .welcome-section {
            margin-bottom: 30px;
        }

        .welcome-section h2 {
            font-family: 'Outfit', sans-serif;
            font-size: 28px;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 5px;
        }

        .welcome-section p {
            color: var(--text-muted);
            font-size: 15px;
        }

        /* Stats Grid */
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }

        .stat-card {
            background-color: var(--card-bg);
            border-radius: var(--radius-lg);
            padding: 24px;
            box-shadow: var(--shadow-md);
            border: 1px solid rgba(255, 152, 0, 0.05);
            display: flex;
            align-items: center;
            gap: 20px;
            position: relative;
            overflow: hidden;
        }

        .stat-icon {
            width: 50px;
            height: 50px;
            border-radius: var(--radius-md);
            background-color: var(--primary-light);
            color: var(--primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
        }

        .stat-info h3 {
            font-size: 13px;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 4px;
        }

        .stat-info p {
            font-size: 20px;
            font-weight: 700;
            color: var(--text-main);
        }

        /* Content Layout */
        .content-grid {
            display: grid;
            grid-template-columns: 2fr 1fr;
            gap: 30px;
        }

        @media (max-width: 768px) {
            .content-grid {
                grid-template-columns: 1fr;
            }
        }

        /* Panel Card */
        .panel-card {
            background-color: var(--card-bg);
            border-radius: var(--radius-lg);
            padding: 30px;
            box-shadow: var(--shadow-md);
            border: 1px solid rgba(0, 0, 0, 0.02);
            margin-bottom: 30px;
        }

        .panel-title {
            font-family: 'Outfit', sans-serif;
            font-size: 20px;
            font-weight: 700;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        /* Surah List */
        .surah-list {
            display: flex;
            flex-direction: column;
            gap: 15px;
        }

        .surah-item {
            padding: 16px;
            border-radius: var(--radius-md);
            border: 1px solid var(--border-color);
            display: flex;
            justify-content: space-between;
            align-items: center;
            transition: var(--transition);
        }

        .surah-item:hover {
            border-color: var(--primary);
            background-color: var(--primary-light);
        }

        .surah-details h4 {
            font-size: 15px;
            font-weight: 600;
            color: var(--text-main);
            margin-bottom: 4px;
        }

        .surah-details span {
            font-size: 12px;
            color: var(--text-muted);
        }

        .surah-action {
            display: flex;
            gap: 10px;
        }

        .btn-read {
            padding: 8px 16px;
            border-radius: 8px;
            background-color: var(--primary);
            color: white;
            font-size: 12px;
            font-weight: 600;
            border: none;
            cursor: pointer;
            transition: var(--transition);
            text-decoration: none;
        }

        .btn-read:hover {
            background-color: var(--primary-hover);
        }

        /* Form Quick Bookmark */
        .bookmark-form {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .form-select, .form-input {
            width: 100%;
            padding: 12px;
            border-radius: var(--radius-md);
            border: 1.5px solid var(--border-color);
            font-family: inherit;
            outline: none;
        }

        .form-select:focus, .form-input:focus {
            border-color: var(--primary);
        }

        .btn-submit {
            padding: 12px;
            border-radius: var(--radius-md);
            background-color: var(--primary);
            color: white;
            font-weight: 600;
            border: none;
            cursor: pointer;
            transition: var(--transition);
        }

        .btn-submit:hover {
            background-color: var(--primary-hover);
        }

        /* Flash Message */
        .flash-message {
            padding: 12px 18px;
            border-radius: var(--radius-md);
            margin-bottom: 25px;
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 14px;
            font-weight: 500;
        }
        .flash-success {
            background-color: #F0FFF4;
            color: var(--success-color);
            border: 1px solid #C6F6D5;
        }
        .flash-error {
            background-color: #FFF5F5;
            color: var(--error-color);
            border: 1px solid #FED7D7;
        }
    </style>
</head>
<body>

    <!-- Navbar -->
    <nav>
        <a href="#" class="nav-logo">Quran <span>saku</span></a>
        <div class="nav-profile">
            <div class="avatar" title="<?php echo htmlspecialchars($user['name']); ?>">
                <?php echo $initials; ?>
            </div>
            <a href="../auth/logout.php" class="logout-btn">
                <i class="fa-solid fa-right-from-bracket"></i> Keluar
            </a>
        </div>
    </nav>

    <!-- Main Container -->
    <div class="container">
        
        <!-- Welcome Header -->
        <div class="welcome-section">
            <h2>Selamat Datang, <?php echo htmlspecialchars($user['name']); ?>!</h2>
            <p>Semoga harimu dipenuhi dengan berkah Al-Quran.</p>
        </div>

        <!-- Flash messages -->
        <?php $flash = getFlash(); if ($flash): ?>
            <div class="flash-message flash-<?php echo $flash['type']; ?>">
                <i class="fa-solid <?php echo $flash['type'] === 'success' ? 'fa-circle-check' : 'fa-circle-exclamation'; ?>"></i>
                <span><?php echo htmlspecialchars($flash['message']); ?></span>
            </div>
        <?php endif; ?>

        <!-- Stats Grid -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-icon"><i class="fa-solid fa-book-open"></i></div>
                <div class="stat-info">
                    <h3>Terakhir Dibaca</h3>
                    <p>
                        <?php 
                        if ($progress) {
                            echo "Surah " . htmlspecialchars($progress['surah_name']) . ": Ayat " . $progress['ayah_number'];
                        } else {
                            echo "Belum ada riwayat";
                        }
                        ?>
                    </p>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon"><i class="fa-solid fa-bookmark"></i></div>
                <div class="stat-info">
                    <h3>Total Bookmark</h3>
                    <p><?php echo $bookmarks_count; ?> Ayat</p>
                </div>
            </div>
        </div>

        <!-- Content Layout -->
        <div class="content-grid">
            
            <!-- Daftar Surah -->
            <div class="panel-card">
                <h3 class="panel-title"><i class="fa-solid fa-list-ol" style="color: var(--primary);"></i> Daftar Surah Pilihan</h3>
                <div class="surah-list">
                    <?php foreach ($surahs as $num => $details): ?>
                        <div class="surah-item">
                            <div class="surah-details">
                                <h4><?php echo $num . '. ' . htmlspecialchars($details['name']); ?></h4>
                                <span><?php echo $details['ayahs']; ?> Ayat • <?php echo htmlspecialchars($details['type']); ?></span>
                            </div>
                            <div class="surah-action">
                                <a href="#" class="btn-read" onclick="alert('Fitur membaca surah segera hadir!')">Mulai Baca</a>
                            </div>
                        </div>
                    <?php endforeach; ?>
                </div>
            </div>

            <!-- Tambah Bookmark Cepat -->
            <div class="panel-card" style="height: fit-content;">
                <h3 class="panel-title"><i class="fa-solid fa-folder-plus" style="color: var(--primary);"></i> Tandai Bacaan</h3>
                <form class="bookmark-form" action="" method="POST">
                    <input type="hidden" name="action" value="add_bookmark">
                    <div>
                        <label class="form-label">Pilih Surah</label>
                        <select name="surah_number" class="form-select">
                            <?php foreach ($surahs as $num => $details): ?>
                                <option value="<?php echo $num; ?>"><?php echo htmlspecialchars($details['name']); ?></option>
                            <?php endforeach; ?>
                        </select>
                    </div>
                    <div>
                        <label class="form-label">Ayat Ke</label>
                        <input type="number" name="ayah_number" class="form-input" min="1" max="300" value="1" required>
                    </div>
                    <button type="submit" class="btn-submit">Simpan Penanda</button>
                </form>
            </div>

        </div>

    </div>

</body>
</html>

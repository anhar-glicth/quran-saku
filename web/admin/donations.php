<?php
// ============================================================
// donations.php - Admin Panel Kelola Donasi
// ============================================================

require_once __DIR__ . '/../includes/session.php';
require_once __DIR__ . '/../config/database.php';

// Pastikan user adalah admin
requireAdmin();

$db = getDB();
$message = '';
$error   = '';

// Tangani pembuatan/penambahan donasi baru oleh admin
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action'])) {
    if ($_POST['action'] === 'create') {
        $userName = trim($_POST['user_name'] ?? '');
        $amount   = floatval($_POST['amount'] ?? 0);
        $campaign = trim($_POST['campaign_title'] ?? 'Donasi Umum');

        if (!empty($userName) && $amount > 0) {
            $stmt = $db->prepare("INSERT INTO donations (user_name, amount, campaign_title, created_at) VALUES (?, ?, ?, NOW())");
            if ($stmt->execute([$userName, $amount, $campaign])) {
                $message = "Donasi berhasil ditambahkan!";
            } else {
                $error = "Gagal menyimpan donasi.";
            }
        } else {
            $error = "Nama donatur dan nominal harus diisi dengan benar.";
        }
    } elseif ($_POST['action'] === 'delete') {
        $id = intval($_POST['id'] ?? 0);
        if ($id > 0) {
            $stmt = $db->prepare("DELETE FROM donations WHERE id = ?");
            $stmt->execute([$id]);
            $message = "Donasi telah dihapus.";
        }
    }
}

// Ambil semua daftar donasi
$stmt = $db->query("SELECT * FROM donations ORDER BY id DESC");
$donations = $stmt->fetchAll(PDO::FETCH_ASSOC);

?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kelola Donasi - Admin STRAVA QURAN</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background-color: #f4f6f9; font-family: 'Segoe UI', sans-serif; }
        .card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
        .navbar-brand { font-weight: bold; color: #ff9800 !important; }
        .btn-orange { background-color: #ff9800; color: #fff; font-weight: 600; }
        .btn-orange:hover { background-color: #e68a00; color: #fff; }
    </style>
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
    <div class="container">
        <a class="navbar-brand" href="dashboard.php">STRAVA QURAN ADMIN</a>
        <div class="navbar-nav ms-auto">
            <a class="nav-link" href="dashboard.php">Dashboard</a>
            <a class="nav-link active" href="donations.php">Donasi Ticker</a>
            <a class="nav-link text-danger" href="../auth/logout.php">Logout</a>
        </div>
    </div>
</nav>

<div class="container mb-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2>🎁 Kelola Donasi Ticker App</h2>
        <button class="btn btn-orange" data-bs-toggle="modal" data-bs-target="#addDonationModal">+ Tambah Donasi Baru</button>
    </div>

    <?php if ($message): ?>
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <?= htmlspecialchars($message) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    <?php endif; ?>

    <?php if ($error): ?>
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <?= htmlspecialchars($error) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    <?php endif; ?>

    <div class="card p-3">
        <table class="table table-hover align-middle">
            <thead class="table-light">
                <tr>
                    <th>ID</th>
                    <th>Nama Donatur</th>
                    <th>Nominal</th>
                    <th>Program / Campaign</th>
                    <th>Waktu Dibuat</th>
                    <th>Aksi</th>
                </tr>
            </thead>
            <tbody>
                <?php if (empty($donations)): ?>
                    <tr><td colspan="6" class="text-center text-muted py-4">Belum ada catatan donasi.</td></tr>
                <?php else: ?>
                    <?php foreach ($donations as $d): ?>
                        <tr>
                            <td>#<?= $d['id'] ?></td>
                            <td><strong><?= htmlspecialchars($d['user_name']) ?></strong></td>
                            <td class="text-success font-monospace fw-bold">Rp <?= number_format($d['amount'], 0, ',', '.') ?></td>
                            <td><span class="badge bg-warning text-dark"><?= htmlspecialchars($d['campaign_title']) ?></span></td>
                            <td><?= date('d M Y, H:i', strtotime($d['created_at'])) ?></td>
                            <td>
                                <form method="POST" onsubmit="return confirm('Yakin ingin menghapus donasi ini?');" class="d-inline">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="<?= $d['id'] ?>">
                                    <button type="submit" class="btn btn-sm btn-outline-danger">Hapus</button>
                                </form>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                <?php endif; ?>
            </tbody>
        </table>
    </div>
</div>

<!-- Modal Tambah Donasi -->
<div class="modal fade" id="addDonationModal" tabindex="-1">
    <div class="modal-dialog">
        <form method="POST" class="modal-content">
            <input type="hidden" name="action" value="create">
            <div class="modal-header">
                <h5 class="modal-title">Tambah Data Donasi</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-content p-3">
                <div class="mb-3">
                    <label class="form-label">Nama Donatur / User</label>
                    <input type="text" name="user_name" class="form-control" placeholder="Contoh: Ahmad Fauzi / Hamba Allah" required>
                </div>
                <div class="mb-3">
                    <label class="form-label">Nominal Donasi (Rp)</label>
                    <input type="number" name="amount" class="form-control" placeholder="Contoh: 100000" required>
                </div>
                <div class="mb-3">
                    <label class="form-label">Nama Campaign / Program</label>
                    <input type="text" name="campaign_title" class="form-control" value="Sedekah Mushaf Al-Qur'an">
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Batal</button>
                <button type="submit" class="btn btn-orange">Simpan Donasi</button>
            </div>
        </form>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

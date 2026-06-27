<?php
// ============================================
// Quran Saku - Halaman Pembayaran Zakat
// ============================================

require_once __DIR__ . '/includes/session.php';
require_once __DIR__ . '/includes/functions.php';

// Ambil info user jika sudah login (opsional)
$user = isLoggedIn() ? getCurrentUser() : null;
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pembayaran Zakat - Quran Saku</title>
    
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    
    <!-- FontAwesome Icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <style>
        :root {
            --primary: #FF6D00;
            --primary-hover: #E65100;
            --primary-light: #FFF3E0;
            --accent-green: #2E7D32;
            --accent-green-hover: #1B5E20;
            --bg-body: #FFF9F6;
            --card-bg: #FFFFFF;
            --text-main: #3D2513;
            --text-muted: #8D7A6E;
            --border-color: #FFD0BC;
            --success-color: #2E7D32;
            --shadow-md: 0 8px 30px rgba(255, 109, 0, 0.08);
            --radius-lg: 20px;
            --radius-md: 12px;
            --transition: all 0.3s ease;
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
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }

        .payment-container {
            width: 100%;
            max-width: 480px;
            background: var(--card-bg);
            border-radius: var(--radius-lg);
            box-shadow: var(--shadow-md);
            padding: 30px;
            border: 1px solid var(--border-color);
            position: relative;
            overflow: hidden;
        }

        .payment-container::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 6px;
            background: linear-gradient(90deg, var(--primary), var(--accent-green));
        }

        .header {
            text-align: center;
            margin-bottom: 24px;
        }

        .logo-icon {
            font-size: 2.5rem;
            color: var(--primary);
            margin-bottom: 12px;
        }

        .title {
            font-family: 'Outfit', sans-serif;
            font-size: 1.6rem;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 6px;
        }

        .subtitle {
            font-size: 0.9rem;
            color: var(--text-muted);
            line-height: 1.4;
        }

        .form-group {
            margin-bottom: 18px;
        }

        label {
            display: block;
            font-size: 0.85rem;
            font-weight: 600;
            margin-bottom: 8px;
            color: var(--text-main);
        }

        .input-group {
            position: relative;
        }

        .input-group i {
            position: absolute;
            left: 14px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
            font-size: 1rem;
        }

        .input-control {
            width: 100%;
            padding: 12px 14px 12px 42px;
            border: 1.5px solid var(--border-color);
            border-radius: var(--radius-md);
            font-family: inherit;
            font-size: 0.95rem;
            color: var(--text-main);
            outline: none;
            transition: var(--transition);
        }

        .input-control:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(255, 109, 0, 0.15);
        }

        .payment-methods {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 10px;
            margin-top: 8px;
        }

        .method-card {
            border: 1.5px solid var(--border-color);
            border-radius: var(--radius-md);
            padding: 12px;
            text-align: center;
            cursor: pointer;
            transition: var(--transition);
            background: #FAFAFA;
        }

        .method-card:hover {
            border-color: var(--primary);
            background: var(--primary-light);
        }

        .method-card.active {
            border-color: var(--primary);
            background: var(--primary-light);
            font-weight: bold;
        }

        .method-card i {
            font-size: 1.4rem;
            color: var(--text-muted);
            margin-bottom: 6px;
            display: block;
        }

        .method-card.active i {
            color: var(--primary);
        }

        .method-label {
            font-size: 0.75rem;
            color: var(--text-main);
        }

        .btn-submit {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            width: 100%;
            padding: 14px;
            background-color: var(--accent-green);
            color: white;
            border: none;
            border-radius: var(--radius-md);
            font-size: 1rem;
            font-weight: 700;
            cursor: pointer;
            transition: var(--transition);
            margin-top: 24px;
            box-shadow: 0 4px 12px rgba(46, 125, 50, 0.2);
        }

        .btn-submit:hover {
            background-color: var(--accent-green-hover);
            transform: translateY(-1px);
        }

        .footer-note {
            text-align: center;
            margin-top: 20px;
            font-size: 0.75rem;
            color: var(--text-muted);
        }

        /* Success State View */
        .success-view {
            display: none;
            text-align: center;
            padding: 20px 0;
        }

        .success-icon {
            font-size: 4rem;
            color: var(--success-color);
            margin-bottom: 16px;
        }
    </style>
</head>
<body>

    <div class="payment-container">
        <!-- Form View -->
        <div id="payment-form">
            <div class="header">
                <div class="logo-icon">
                    <i class="fa-solid fa-hand-holding-heart"></i>
                </div>
                <h1 class="title">Pembayaran Zakat</h1>
                <p class="subtitle">Salurkan zakat Anda secara aman melalui portal resmi mitra pembayaran Quran Saku.</p>
            </div>

            <form id="zakatForm" onsubmit="handlePayment(event)">
                <!-- Nama Pembayar Zakat -->
                <div class="form-group">
                    <label for="payer_name">Nama Lengkap (Muzakki)</label>
                    <div class="input-group">
                        <i class="fa-regular fa-user"></i>
                        <input type="text" id="payer_name" class="input-control" placeholder="Masukkan nama lengkap Anda" required 
                               value="<?php echo htmlspecialchars($user['name'] ?? ''); ?>">
                    </div>
                </div>

                <!-- Jumlah Zakat -->
                <div class="form-group">
                    <label for="zakat_amount">Jumlah Pembayaran Zakat (Rp)</label>
                    <div class="input-group">
                        <i class="fa-solid fa-rupiah-sign"></i>
                        <input type="number" id="zakat_amount" class="input-control" placeholder="Contoh: 150000" required>
                    </div>
                </div>

                <!-- Metode Pembayaran -->
                <div class="form-group">
                    <label>Metode Pembayaran</label>
                    <div class="payment-methods">
                        <div class="method-card active" onclick="selectMethod(this, 'Transfer Bank')">
                            <i class="fa-solid fa-building-columns"></i>
                            <span class="method-label">Transfer Bank</span>
                        </div>
                        <div class="method-card" onclick="selectMethod(this, 'Qris')">
                            <i class="fa-solid fa-qrcode"></i>
                            <span class="method-label">QRIS</span>
                        </div>
                        <div class="method-card" onclick="selectMethod(this, 'E-Wallet')">
                            <i class="fa-solid fa-wallet"></i>
                            <span class="method-label">GoPay / OVO</span>
                        </div>
                    </div>
                    <input type="hidden" id="payment_method" value="Transfer Bank">
                </div>

                <button type="submit" class="btn-submit">
                    <i class="fa-solid fa-circle-check"></i>
                    Konfirmasi Pembayaran
                </button>
            </form>
        </div>

        <!-- Success View -->
        <div id="success-view" class="success-view">
            <div class="success-icon">
                <i class="fa-solid fa-circle-check"></i>
            </div>
            <h1 class="title">Pembayaran Berhasil!</h1>
            <p class="subtitle" style="margin-bottom: 24px;">Alhamdulillah, zakat Anda sebesar <strong id="success-amount">Rp 0</strong> telah kami terima. Semoga mendatangkan berkah bagi Anda dan keluarga.</p>
            <button onclick="resetForm()" class="btn-submit" style="background-color: var(--primary); box-shadow: 0 4px 12px rgba(255, 109, 0, 0.2);">
                <i class="fa-solid fa-arrow-rotate-left"></i>
                Kembali ke Kalkulator
            </button>
        </div>

        <p class="footer-note">&copy; <?php echo date('Y'); ?> Quran Saku. Semua Transaksi Terenkripsi Aman.</p>
    </div>

    <script>
        function selectMethod(element, method) {
            // Remove active class from all methods
            const cards = document.querySelectorAll('.method-card');
            cards.forEach(card => card.classList.remove('active'));
            
            // Add active class to clicked
            element.classList.add('active');
            
            // Set hidden input value
            document.getElementById('payment_method').value = method;
        }

        function handlePayment(event) {
            event.preventDefault();
            
            const amount = document.getElementById('zakat_amount').value;
            const payerName = document.getElementById('payer_name').value;
            const method = document.getElementById('payment_method').value;

            // Format Currency
            const formatted = new Intl.NumberFormat('id-ID', {
                style: 'currency',
                currency: 'IDR',
                maximumFractionDigits: 0
            }).format(amount);

            // Display success
            document.getElementById('success-amount').innerText = formatted;
            document.getElementById('payment-form').style.display = 'none';
            document.getElementById('success-view').style.display = 'block';
        }

        function resetForm() {
            // Reset form input values
            document.getElementById('zakatForm').reset();
            document.getElementById('payment-form').style.display = 'block';
            document.getElementById('success-view').style.display = 'none';
        }
    </script>
</body>
</html>

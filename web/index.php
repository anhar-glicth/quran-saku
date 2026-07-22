<?php
// ============================================
// Quran Saku - Halaman Utama (Login & Register)
// ============================================

require_once __DIR__ . '/includes/session.php';
require_once __DIR__ . '/includes/functions.php';

// Jika sudah login, langsung ke dashboard masing-masing role
if (isLoggedIn()) {
    if (isAdmin()) {
        redirect(APP_URL . '/admin/dashboard.php');
    } else {
        redirect(APP_URL . '/user/dashboard.php');
    }
}

$csrf_token = generateCsrfToken();
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Strava Quran - Masuk & Daftar</title>
    
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
            --error-color: #E53E3E;
            --success-color: #38A169;
            --shadow-sm: 0 1px 3px rgba(0,0,0,0.05), 0 1px 2px rgba(0,0,0,0.05);
            --shadow-md: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.05);
            --shadow-lg: 0 10px 25px -5px rgba(255, 152, 0, 0.1), 0 8px 10px -6px rgba(255, 152, 0, 0.05);
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
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            overflow-x: hidden;
            position: relative;
        }

        /* Decorative background shapes */
        .bg-shape {
            position: absolute;
            border-radius: 50%;
            filter: blur(80px);
            z-index: 1;
            opacity: 0.5;
        }
        .shape-1 {
            width: 300px;
            height: 300px;
            background: var(--primary-light);
            top: -50px;
            left: -50px;
        }
        .shape-2 {
            width: 250px;
            height: 250px;
            background: #FFE0B2;
            bottom: -50px;
            right: -50px;
        }

        .auth-container {
            width: 100%;
            max-width: 420px;
            z-index: 10;
            position: relative;
        }

        /* Brand Header */
        .brand-header {
            text-align: center;
            margin-bottom: 30px;
        }

        .brand-logo {
            font-size: 36px;
            font-weight: 800;
            color: var(--primary);
            font-family: 'Outfit', sans-serif;
            letter-spacing: -0.5px;
            line-height: 1.2;
            margin-bottom: 4px;
            animation: slideDown 0.6s ease;
        }

        .brand-logo span {
            color: var(--text-main);
        }

        .brand-tagline {
            font-size: 14px;
            color: var(--text-muted);
            font-weight: 500;
        }

        /* Auth Card */
        .auth-card {
            background-color: var(--card-bg);
            border-radius: var(--radius-lg);
            box-shadow: var(--shadow-lg);
            border: 1px solid rgba(255, 152, 0, 0.08);
            padding: 35px 30px;
            width: 100%;
            transition: var(--transition);
        }

        /* Tabs Navigation */
        .tabs-header {
            display: flex;
            background-color: #F7FAFC;
            padding: 6px;
            border-radius: var(--radius-md);
            margin-bottom: 30px;
            border: 1px solid var(--border-color);
        }

        .tab-btn {
            flex: 1;
            padding: 10px 15px;
            font-size: 14px;
            font-weight: 600;
            color: var(--text-muted);
            background: none;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            transition: var(--transition);
            outline: none;
        }

        .tab-btn.active {
            background-color: var(--card-bg);
            color: var(--text-main);
            box-shadow: var(--shadow-sm);
        }

        /* Forms Wrapper */
        .forms-wrapper {
            position: relative;
            overflow: hidden;
            min-height: 320px;
        }

        .form-pane {
            display: none;
            width: 100%;
            animation: fadeIn 0.4s ease;
        }

        .form-pane.active {
            display: block;
        }

        /* Form Controls */
        .form-group {
            margin-bottom: 20px;
            position: relative;
        }

        .form-label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: var(--text-main);
            margin-bottom: 8px;
        }

        .input-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }

        .form-input {
            width: 100%;
            padding: 14px 16px;
            font-size: 14px;
            border-radius: var(--radius-md);
            border: 1.5px solid var(--border-color);
            background-color: #FAFAFA;
            color: var(--text-main);
            font-family: inherit;
            outline: none;
            transition: var(--transition);
        }

        .form-input:focus {
            border-color: var(--primary);
            background-color: var(--card-bg);
            box-shadow: 0 0 0 4px rgba(255, 152, 0, 0.12);
        }

        .form-input::placeholder {
            color: #A0AEC0;
        }

        .toggle-password {
            position: absolute;
            right: 16px;
            color: var(--text-muted);
            cursor: pointer;
            transition: var(--transition);
            background: none;
            border: none;
            outline: none;
            padding: 4px;
        }

        .toggle-password:hover {
            color: var(--primary);
        }

        /* Extra Links */
        .extra-links {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 24px;
            font-size: 13px;
        }

        .remember-me {
            display: flex;
            align-items: center;
            gap: 8px;
            color: var(--text-muted);
            cursor: pointer;
        }

        .remember-me input {
            accent-color: var(--primary);
            width: 16px;
            height: 16px;
            cursor: pointer;
        }

        .forgot-link {
            color: var(--text-muted);
            text-decoration: none;
            font-weight: 500;
            transition: var(--transition);
        }

        .forgot-link:hover {
            color: var(--primary);
        }

        /* Action Button */
        .btn-submit {
            width: 100%;
            padding: 14px;
            border-radius: var(--radius-md);
            border: none;
            background-color: var(--primary);
            color: #FFFFFF;
            font-size: 15px;
            font-weight: 700;
            cursor: pointer;
            box-shadow: 0 4px 6px rgba(255, 152, 0, 0.2);
            transition: var(--transition);
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
        }

        .btn-submit:hover {
            background-color: var(--primary-hover);
            transform: translateY(-1px);
            box-shadow: 0 6px 12px rgba(255, 152, 0, 0.3);
        }

        .btn-submit:active {
            transform: translateY(1px);
        }

        .btn-submit:disabled {
            background-color: #CBD5E0;
            box-shadow: none;
            cursor: not-allowed;
        }

        .btn-submit .spinner {
            display: none;
            width: 18px;
            height: 18px;
            border: 2px solid rgba(255,255,255,0.3);
            border-top-color: #fff;
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
        }

        /* Divider */
        .divider {
            display: flex;
            align-items: center;
            margin: 25px 0;
            color: var(--text-muted);
            font-size: 12px;
            font-weight: 500;
        }

        .divider::before, .divider::after {
            content: '';
            flex: 1;
            height: 1px;
            background-color: var(--border-color);
        }

        .divider span {
            padding: 0 15px;
        }

        /* Social Sign In */
        .social-buttons {
            display: flex;
            justify-content: center;
            gap: 15px;
        }

        .social-btn {
            width: 50px;
            height: 50px;
            border-radius: 50%;
            border: 1px solid var(--border-color);
            background-color: var(--card-bg);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            color: var(--text-main);
            cursor: pointer;
            transition: var(--transition);
            text-decoration: none;
        }

        .social-btn:hover {
            border-color: var(--primary);
            background-color: var(--primary-light);
            color: var(--primary);
            transform: translateY(-2px);
        }

        /* Alerts */
        .alert {
            padding: 12px 16px;
            border-radius: var(--radius-md);
            font-size: 13px;
            font-weight: 500;
            margin-bottom: 20px;
            display: none;
            align-items: center;
            gap: 10px;
            line-height: 1.4;
            animation: slideDown 0.4s ease;
        }

        .alert-error {
            background-color: #FFF5F5;
            color: var(--error-color);
            border: 1px solid #FED7D7;
        }

        .alert-success {
            background-color: #F0FFF4;
            color: var(--success-color);
            border: 1px solid #C6F6D5;
        }

        .alert-info {
            background-color: #EBF8FF;
            color: #2B6CB0;
            border: 1px solid #BEE3F8;
        }

        /* Animations */
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(8px); }
            to { opacity: 1; transform: translateY(0); }
        }

        @keyframes slideDown {
            from { opacity: 0; transform: translateY(-10px); }
            to { opacity: 1; transform: translateY(0); }
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }

        @media (max-width: 480px) {
            .auth-card {
                padding: 25px 20px;
            }
        }
    </style>
</head>
<body>

    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>

    <div class="auth-container">
        
        <!-- Brand Header -->
        <div class="brand-header">
            <h1 class="brand-logo">Quran <span>saku</span></h1>
            <p class="brand-tagline">Teman setia tadarus dan ibadah harian Anda</p>
        </div>

        <!-- Auth Card -->
        <div class="auth-card">
            
            <!-- Tabs -->
            <div class="tabs-header">
                <button type="button" class="tab-btn active" onclick="switchTab('login')">Sign in</button>
                <button type="button" class="tab-btn" onclick="switchTab('register')">Register</button>
            </div>

            <!-- Global Alert -->
            <div id="alertBox" class="alert">
                <i class="fa-solid fa-circle-exclamation"></i>
                <span id="alertMessage"></span>
            </div>

            <!-- Forms Wrapper -->
            <div class="forms-wrapper">
                
                <!-- LOGIN FORM -->
                <div id="loginPane" class="form-pane active">
                    <form id="loginForm" novalidate>
                        <input type="hidden" name="csrf_token" value="<?php echo $csrf_token; ?>">
                        
                        <div class="form-group">
                            <label class="form-label" for="loginEmail">Email address</label>
                            <div class="input-wrapper">
                                <input type="email" name="email" id="loginEmail" class="form-input" placeholder="Your email" required>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label" for="loginPassword">Password</label>
                            <div class="input-wrapper">
                                <input type="password" name="password" id="loginPassword" class="form-input" placeholder="Password" required>
                                <button type="button" class="toggle-password" onclick="togglePasswordVisibility('loginPassword', this)">
                                    <i class="fa-regular fa-eye-slash"></i>
                                </button>
                            </div>
                        </div>

                        <div class="extra-links">
                            <label class="remember-me">
                                <input type="checkbox" name="remember" id="rememberMe">
                                Ingat saya
                            </label>
                            <a href="#" class="forgot-link" onclick="showAlert('info', 'Fitur reset password belum diimplementasikan.')">Forgot password?</a>
                        </div>

                        <button type="submit" class="btn-submit" id="loginBtn">
                            <span>Sign in</span>
                            <span class="spinner"></span>
                        </button>
                    </form>
                </div>

                <!-- REGISTER FORM -->
                <div id="registerPane" class="form-pane">
                    <form id="registerForm" novalidate>
                        <input type="hidden" name="csrf_token" value="<?php echo $csrf_token; ?>">
                        
                        <div class="form-group">
                            <label class="form-label" for="registerName">Full Name</label>
                            <div class="input-wrapper">
                                <input type="text" name="name" id="registerName" class="form-input" placeholder="Your full name" required>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label" for="registerEmail">Email address</label>
                            <div class="input-wrapper">
                                <input type="email" name="email" id="registerEmail" class="form-input" placeholder="Your email address" required>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label" for="registerPassword">Password</label>
                            <div class="input-wrapper">
                                <input type="password" name="password" id="registerPassword" class="form-input" placeholder="Create strong password" required>
                                <button type="button" class="toggle-password" onclick="togglePasswordVisibility('registerPassword', this)">
                                    <i class="fa-regular fa-eye-slash"></i>
                                </button>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label" for="confirmPassword">Confirm Password</label>
                            <div class="input-wrapper">
                                <input type="password" name="password_confirm" id="confirmPassword" class="form-input" placeholder="Repeat password" required>
                                <button type="button" class="toggle-password" onclick="togglePasswordVisibility('confirmPassword', this)">
                                    <i class="fa-regular fa-eye-slash"></i>
                                </button>
                            </div>
                        </div>

                        <button type="submit" class="btn-submit" id="registerBtn">
                            <span>Register</span>
                            <span class="spinner"></span>
                        </button>
                    </form>
                </div>

            </div>

            <!-- Divider -->
            <div class="divider">
                <span>Other sign in options</span>
            </div>

            <!-- Social Buttons -->
            <div class="social-buttons">
                <a href="#" class="social-btn" onclick="showAlert('info', 'Login dengan Facebook segera hadir.')">
                    <i class="fa-brands fa-facebook-f" style="color: #1877F2;"></i>
                </a>
                <a href="#" class="social-btn" onclick="showAlert('info', 'Login dengan Google segera hadir.')">
                    <i class="fa-brands fa-google" style="color: #EA4335;"></i>
                </a>
                <a href="#" class="social-btn" onclick="showAlert('info', 'Login dengan Apple segera hadir.')">
                    <i class="fa-brands fa-apple" style="color: #000000;"></i>
                </a>
            </div>

        </div>
    </div>

    <!-- AJAX Script -->
    <script>
        // Check URL parameters for flash messages
        window.addEventListener('DOMContentLoaded', () => {
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('msg') === 'login_required') {
                showAlert('error', 'Sila masuk terlebih dahulu untuk mengakses aplikasi.');
            } else if (urlParams.get('msg') === 'logged_out') {
                showAlert('success', 'Anda telah sukses keluar dari akun.');
            }
        });

        // Tab Switcher
        function switchTab(tab) {
            const tabButtons = document.querySelectorAll('.tab-btn');
            const forms = document.querySelectorAll('.form-pane');
            const alertBox = document.getElementById('alertBox');
            
            // Hide alert when switching tabs
            alertBox.style.display = 'none';

            tabButtons.forEach(btn => btn.classList.remove('active'));
            forms.forEach(form => form.classList.remove('active'));

            if (tab === 'login') {
                tabButtons[0].classList.add('active');
                document.getElementById('loginPane').classList.add('active');
            } else {
                tabButtons[1].classList.add('active');
                document.getElementById('registerPane').classList.add('active');
            }
        }

        // Toggle Password visibility
        function togglePasswordVisibility(inputId, btn) {
            const input = document.getElementById(inputId);
            const icon = btn.querySelector('i');
            
            if (input.type === 'password') {
                input.type = 'text';
                icon.className = 'fa-regular fa-eye';
            } else {
                input.type = 'password';
                icon.className = 'fa-regular fa-eye-slash';
            }
        }

        // Alert Utility
        function showAlert(type, message) {
            const alertBox = document.getElementById('alertBox');
            const alertMessage = document.getElementById('alertMessage');
            
            alertBox.className = 'alert'; // reset classes
            
            if (type === 'error') {
                alertBox.classList.add('alert-error');
                alertBox.querySelector('i').className = 'fa-solid fa-circle-exclamation';
            } else if (type === 'success') {
                alertBox.classList.add('alert-success');
                alertBox.querySelector('i').className = 'fa-solid fa-circle-check';
            } else {
                alertBox.classList.add('alert-info');
                alertBox.querySelector('i').className = 'fa-solid fa-circle-info';
            }

            alertMessage.textContent = message;
            alertBox.style.display = 'flex';
        }

        // Handle Login Submission
        document.getElementById('loginForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const form = this;
            const btn = document.getElementById('loginBtn');
            const spinner = btn.querySelector('.spinner');
            const btnText = btn.querySelector('span');

            // Client-side validations
            const email = document.getElementById('loginEmail').value.trim();
            const pass = document.getElementById('loginPassword').value;

            if (!email || !pass) {
                showAlert('error', 'Semua kolom wajib diisi.');
                return;
            }

            // Set Loading state
            btn.disabled = true;
            spinner.style.display = 'inline-block';
            btnText.style.opacity = '0.7';

            const formData = new FormData(form);

            fetch('auth/login.php', {
                method: 'POST',
                body: formData
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    showAlert('success', data.message);
                    setTimeout(() => {
                        window.location.href = data.redirect;
                    }, 1000);
                } else {
                    showAlert('error', data.message);
                    btn.disabled = false;
                    spinner.style.display = 'none';
                    btnText.style.opacity = '1';
                }
            })
            .catch(err => {
                console.error(err);
                showAlert('error', 'Terjadi kesalahan jaringan atau server.');
                btn.disabled = false;
                spinner.style.display = 'none';
                btnText.style.opacity = '1';
            });
        });

        // Handle Register Submission
        document.getElementById('registerForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const form = this;
            const btn = document.getElementById('registerBtn');
            const spinner = btn.querySelector('.spinner');
            const btnText = btn.querySelector('span');

            // Client-side validations
            const name = document.getElementById('registerName').value.trim();
            const email = document.getElementById('registerEmail').value.trim();
            const pass = document.getElementById('registerPassword').value;
            const confirm = document.getElementById('confirmPassword').value;

            if (!name || !email || !pass || !confirm) {
                showAlert('error', 'Semua kolom wajib diisi.');
                return;
            }

            if (pass.length < 8) {
                showAlert('error', 'Kata sandi minimal 8 karakter.');
                return;
            }

            if (pass !== confirm) {
                showAlert('error', 'Konfirmasi kata sandi tidak sesuai.');
                return;
            }

            // Set Loading state
            btn.disabled = true;
            spinner.style.display = 'inline-block';
            btnText.style.opacity = '0.7';

            const formData = new FormData(form);

            fetch('auth/register.php', {
                method: 'POST',
                body: formData
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    showAlert('success', data.message);
                    setTimeout(() => {
                        window.location.href = data.redirect;
                    }, 1000);
                } else {
                    showAlert('error', data.message);
                    btn.disabled = false;
                    spinner.style.display = 'none';
                    btnText.style.opacity = '1';
                }
            })
            .catch(err => {
                console.error(err);
                showAlert('error', 'Terjadi kesalahan jaringan atau server.');
                btn.disabled = false;
                spinner.style.display = 'none';
                btnText.style.opacity = '1';
            });
        });
    </script>
</body>
</html>

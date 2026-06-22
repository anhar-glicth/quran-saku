<?php
// ============================================
// Quran Saku - Onboarding Page
// ============================================

require_once __DIR__ . '/../includes/session.php';
require_once __DIR__ . '/../includes/functions.php';

requireLogin();

// Jika admin, langsung ke admin dashboard
if (isAdmin()) {
    redirect(APP_URL . '/admin/dashboard.php');
}

// Jika user sudah lewati onboarding, langsung ke dashboard
if (!empty($_SESSION['onboarding_done'])) {
    redirect(APP_URL . '/user/dashboard.php');
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quran Saku - Selamat Datang</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700;800&family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">

    <style>
        :root {
            --gold-1: #FFC107;
            --gold-2: #FF9800;
            --gold-3: #FFD54F;
            --cream: #FFFDE7;
            --text-dark: #3E2723;
            --text-mid: #5D4037;
            --white: #FFFFFF;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }

        html, body {
            width: 100%;
            height: 100%;
            overflow: hidden;
            font-family: 'Plus Jakarta Sans', sans-serif;
            background: #F5EDD5;
        }

        /* ─── Wrapper phone ─────────────────────── */
        .phone-shell {
            max-width: 390px;
            height: 100vh;
            max-height: 820px;
            margin: 0 auto;
            position: relative;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            background: #FFF8E1;
        }

        /* ─── Slides Container ──────────────────── */
        .slides-track {
            display: flex;
            width: 300%;
            height: 100%;
            transition: transform 0.55s cubic-bezier(0.77, 0, 0.18, 1);
        }

        .slide {
            width: calc(100% / 3);
            height: 100%;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: flex-end;
            position: relative;
            overflow: hidden;
        }

        /* ─── Illustration Area (top 60%) ───────── */
        .illus-area {
            position: absolute;
            top: 0; left: 0; right: 0;
            height: 62%;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        /* Golden gradient bg per slide */
        .slide-1 .illus-area { background: linear-gradient(160deg, #FFD54F 0%, #FFA000 60%, #FF6F00 100%); }
        .slide-2 .illus-area { background: linear-gradient(160deg, #FFCA28 0%, #FB8C00 60%, #E65100 100%); }
        .slide-3 .illus-area { background: linear-gradient(160deg, #FFE082 0%, #FFB300 60%, #FF6F00 100%); }

        /* ─── Wave Divider ──────────────────────── */
        .wave-divider {
            position: absolute;
            bottom: 37%;
            left: 0; right: 0;
            height: 80px;
            overflow: hidden;
            z-index: 2;
        }

        .wave-divider svg {
            width: 100%;
            height: 100%;
        }

        /* ─── Text Content (bottom 40%) ─────────── */
        .text-area {
            position: absolute;
            bottom: 0; left: 0; right: 0;
            height: 40%;
            padding: 0 32px 20px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            background: #FFF8E1;
            z-index: 1;
        }

        .slide-title {
            font-family: 'Outfit', sans-serif;
            font-size: 28px;
            font-weight: 800;
            color: var(--gold-2);
            text-align: center;
            margin-bottom: 12px;
            line-height: 1.2;
        }

        .slide-desc {
            font-size: 14px;
            color: var(--text-mid);
            text-align: center;
            line-height: 1.7;
            max-width: 280px;
        }

        /* ─── Navigation Bar ────────────────────── */
        .nav-bar {
            position: absolute;
            bottom: 0; left: 0; right: 0;
            height: 80px;
            padding: 0 32px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            z-index: 10;
            background: transparent;
        }

        /* Dots */
        .dots {
            display: flex;
            gap: 8px;
            align-items: center;
        }

        .dot {
            width: 8px;
            height: 8px;
            border-radius: 99px;
            background: #D7B880;
            transition: all 0.35s ease;
        }

        .dot.active {
            width: 22px;
            background: var(--gold-2);
        }

        /* Arrow Button */
        .btn-arrow {
            width: 52px;
            height: 52px;
            border-radius: 50%;
            background: var(--gold-2);
            border: none;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 4px 14px rgba(255, 152, 0, 0.4);
            transition: all 0.25s ease;
            color: #fff;
        }

        .btn-arrow:hover {
            transform: scale(1.08);
            box-shadow: 0 6px 20px rgba(255, 152, 0, 0.5);
        }

        .btn-arrow svg {
            width: 22px;
            height: 22px;
        }

        /* Skip / Get Started btn */
        .btn-skip {
            font-size: 13px;
            font-weight: 600;
            color: #BCAAA4;
            background: none;
            border: none;
            cursor: pointer;
            padding: 8px 4px;
            letter-spacing: 0.3px;
            transition: color 0.2s;
        }

        .btn-skip:hover { color: var(--gold-2); }

        .btn-start {
            flex: 1;
            padding: 15px;
            border-radius: 14px;
            background: linear-gradient(135deg, var(--gold-1), var(--gold-2));
            border: none;
            color: #fff;
            font-size: 16px;
            font-weight: 700;
            font-family: 'Outfit', sans-serif;
            cursor: pointer;
            box-shadow: 0 6px 18px rgba(255, 152, 0, 0.35);
            transition: all 0.25s ease;
            display: none;
        }

        .btn-start:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 22px rgba(255, 152, 0, 0.45);
        }

        /* ─── SVG Illustrations ─────────────────── */
        .illus-svg {
            width: 85%;
            max-width: 300px;
            filter: drop-shadow(0 12px 30px rgba(0,0,0,0.12));
            animation: floatAnim 4s ease-in-out infinite;
        }

        @keyframes floatAnim {
            0%, 100% { transform: translateY(0px); }
            50% { transform: translateY(-12px); }
        }

        /* Slide-specific animation delays */
        .slide-2 .illus-svg { animation-delay: 0.3s; }
        .slide-3 .illus-svg { animation-delay: 0.6s; }

        /* Orbs decorations */
        .orb {
            position: absolute;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.18);
        }

        .orb-1 { width: 120px; height: 120px; top: 10%; right: 10%; }
        .orb-2 { width: 70px;  height: 70px;  top: 5%;  right: 25%; }
        .orb-3 { width: 50px;  height: 50px;  top: 18%; left: 10%; }
    </style>
</head>
<body>

<div class="phone-shell">
    <div class="slides-track" id="slidesTrack">

        <!-- ═══════════════════════════════════════ -->
        <!-- SLIDE 1 – Intro -->
        <!-- ═══════════════════════════════════════ -->
        <div class="slide slide-1">
            <div class="illus-area">
                <!-- Decorative orbs -->
                <div class="orb orb-1"></div>
                <div class="orb orb-2"></div>
                <div class="orb orb-3"></div>

                <!-- Illustration: person reading Quran -->
                <svg class="illus-svg" viewBox="0 0 300 260" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <!-- Prayer rug -->
                    <ellipse cx="150" cy="230" rx="120" ry="22" fill="#8D6E63" opacity="0.3"/>
                    <rect x="50" y="190" width="200" height="35" rx="8" fill="#C1440E" opacity="0.85"/>
                    <rect x="58" y="196" width="184" height="23" rx="6" fill="#E8A87C" opacity="0.4"/>
                    <!-- Quran book -->
                    <rect x="80" y="170" width="140" height="30" rx="5" fill="#4CAF50"/>
                    <rect x="80" y="170" width="68" height="30" rx="5" fill="#2E7D32"/>
                    <rect x="148" y="172" width="2" height="26" fill="#1B5E20"/>
                    <!-- Spine light -->
                    <rect x="82" y="173" width="4" height="24" rx="2" fill="rgba(255,255,255,0.3)"/>
                    <!-- Pages texture -->
                    <line x1="90" y1="177" x2="144" y2="177" stroke="rgba(255,255,255,0.3)" stroke-width="1"/>
                    <line x1="90" y1="181" x2="144" y2="181" stroke="rgba(255,255,255,0.3)" stroke-width="1"/>
                    <line x1="90" y1="185" x2="130" y2="185" stroke="rgba(255,255,255,0.3)" stroke-width="1"/>
                    <line x1="152" y1="177" x2="215" y2="177" stroke="rgba(255,255,255,0.2)" stroke-width="1"/>
                    <line x1="152" y1="181" x2="215" y2="181" stroke="rgba(255,255,255,0.2)" stroke-width="1"/>
                    <!-- Body sitting -->
                    <ellipse cx="150" cy="155" rx="38" ry="22" fill="#FFF8E1"/>
                    <ellipse cx="150" cy="155" rx="34" ry="18" fill="#FFECB3"/>
                    <!-- Robe/Jubah -->
                    <path d="M118 155 Q112 175 115 195 L185 195 Q188 175 182 155 Q165 165 150 163 Q135 165 118 155Z" fill="#F57F17"/>
                    <!-- Neck -->
                    <rect x="143" y="118" width="14" height="18" rx="7" fill="#FFCC80"/>
                    <!-- Head -->
                    <ellipse cx="150" cy="108" rx="26" ry="28" fill="#FFCC80"/>
                    <!-- Kopiah (hat) -->
                    <ellipse cx="150" cy="84" rx="22" ry="6" fill="#4E342E"/>
                    <rect x="128" y="78" width="44" height="12" rx="6" fill="#5D4037"/>
                    <!-- Eyes -->
                    <ellipse cx="141" cy="108" rx="3" ry="3.5" fill="#3E2723"/>
                    <ellipse cx="159" cy="108" rx="3" ry="3.5" fill="#3E2723"/>
                    <!-- Eye shine -->
                    <circle cx="142.5" cy="106.5" r="1" fill="white"/>
                    <circle cx="160.5" cy="106.5" r="1" fill="white"/>
                    <!-- Smile -->
                    <path d="M143 115 Q150 120 157 115" stroke="#8D6E63" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                    <!-- Beard -->
                    <path d="M138 116 Q140 128 150 130 Q160 128 162 116" fill="#5D4037" opacity="0.7"/>
                    <!-- Hands holding book -->
                    <ellipse cx="115" cy="178" rx="10" ry="8" fill="#FFCC80"/>
                    <ellipse cx="185" cy="178" rx="10" ry="8" fill="#FFCC80"/>
                    <!-- Mosque silhouette in bg -->
                    <path d="M10 240 L10 200 Q10 190 20 190 Q30 190 30 200 L30 240Z" fill="rgba(255,255,255,0.15)"/>
                    <circle cx="20" cy="186" r="10" fill="rgba(255,255,255,0.15)"/>
                    <rect x="14" y="176" width="12" height="12" fill="rgba(255,255,255,0.15)"/>
                    <path d="M270 240 L270 200 Q270 190 280 190 Q290 190 290 200 L290 240Z" fill="rgba(255,255,255,0.15)"/>
                    <circle cx="280" cy="186" r="10" fill="rgba(255,255,255,0.15)"/>
                    <rect x="274" y="176" width="12" height="12" fill="rgba(255,255,255,0.15)"/>
                    <!-- Stars -->
                    <circle cx="60"  cy="40"  r="3" fill="rgba(255,255,255,0.6)"/>
                    <circle cx="240" cy="30"  r="2" fill="rgba(255,255,255,0.5)"/>
                    <circle cx="200" cy="55"  r="2.5" fill="rgba(255,255,255,0.4)"/>
                    <circle cx="90"  cy="25"  r="2" fill="rgba(255,255,255,0.5)"/>
                </svg>
            </div>

            <!-- Wave -->
            <div class="wave-divider">
                <svg viewBox="0 0 390 80" preserveAspectRatio="none">
                    <path d="M0 40 Q97.5 0 195 40 Q292.5 80 390 40 L390 80 L0 80 Z" fill="#FFF8E1"/>
                </svg>
            </div>

            <div class="text-area" style="justify-content: flex-start; padding-top: 18px;">
                <h2 class="slide-title">QURAN<br>SAKU</h2>
                <p class="slide-desc">Quran saku adalah super app untuk membantu kamu dalam beribadah, menghapal quran serta pengingat ibadah lainnya.</p>
            </div>
        </div>

        <!-- ═══════════════════════════════════════ -->
        <!-- SLIDE 2 – Features -->
        <!-- ═══════════════════════════════════════ -->
        <div class="slide slide-2">
            <div class="illus-area">
                <div class="orb orb-1"></div>
                <div class="orb orb-2"></div>

                <!-- Illustration: Mosque + quran on sajadah -->
                <svg class="illus-svg" viewBox="0 0 300 260" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <!-- Sky orbs -->
                    <circle cx="150" cy="60"  r="45" fill="rgba(255,255,255,0.25)"/>
                    <circle cx="150" cy="55"  r="30" fill="rgba(255,255,255,0.2)"/>
                    <!-- Mosque main dome -->
                    <path d="M90 180 L90 130 Q90 90 150 90 Q210 90 210 130 L210 180Z" fill="rgba(255,255,255,0.9)"/>
                    <!-- Main dome cap -->
                    <ellipse cx="150" cy="90" rx="60" ry="20" fill="rgba(255,255,255,0.9)"/>
                    <path d="M110 90 Q150 60 190 90" fill="#F9A825"/>
                    <!-- Minaret left -->
                    <rect x="60" y="120" width="25" height="60" rx="4" fill="rgba(255,255,255,0.85)"/>
                    <ellipse cx="72.5" cy="120" rx="12.5" ry="8" fill="#F9A825"/>
                    <rect x="68" y="112" width="9" height="10" fill="#F9A825"/>
                    <!-- Minaret right -->
                    <rect x="215" y="120" width="25" height="60" rx="4" fill="rgba(255,255,255,0.85)"/>
                    <ellipse cx="227.5" cy="120" rx="12.5" ry="8" fill="#F9A825"/>
                    <rect x="223" y="112" width="9" height="10" fill="#F9A825"/>
                    <!-- Door -->
                    <path d="M132 180 L132 148 Q132 138 150 138 Q168 138 168 148 L168 180Z" fill="#F57F17" opacity="0.9"/>
                    <!-- Windows -->
                    <ellipse cx="110" cy="145" rx="9" ry="11" fill="#F9A825" opacity="0.7"/>
                    <ellipse cx="190" cy="145" rx="9" ry="11" fill="#F9A825" opacity="0.7"/>
                    <!-- Ground / Sajadah -->
                    <ellipse cx="150" cy="215" rx="110" ry="18" fill="#A1887F" opacity="0.35"/>
                    <path d="M55 200 Q150 195 245 200 L245 225 Q150 230 55 225Z" fill="#C1440E" opacity="0.75"/>
                    <!-- Quran on sajadah -->
                    <rect x="108" y="193" width="84" height="22" rx="5" fill="#2E7D32"/>
                    <rect x="108" y="193" width="40" height="22" rx="5" fill="#1B5E20"/>
                    <rect x="148" y="195" width="2" height="18" fill="#0A3D00"/>
                    <!-- Crescent + star -->
                    <path d="M145 35 Q160 28 168 42 Q155 36 145 35Z" fill="rgba(255,255,255,0.85)"/>
                    <circle cx="158" cy="28" r="2" fill="rgba(255,255,255,0.8)"/>
                    <!-- Ground line -->
                    <rect x="0" y="225" width="300" height="35" rx="0" fill="rgba(255,255,255,0.15)"/>
                </svg>
            </div>

            <div class="wave-divider">
                <svg viewBox="0 0 390 80" preserveAspectRatio="none">
                    <path d="M0 50 Q97.5 10 195 50 Q292.5 90 390 50 L390 80 L0 80 Z" fill="#FFF8E1"/>
                </svg>
            </div>

            <div class="text-area" style="justify-content: flex-start; padding-top: 18px;">
                <h2 class="slide-title">Ibadah<br>Lebih Mudah</h2>
                <p class="slide-desc">Jadwal sholat, arah kiblat, dzikir harian, dan doa pilihan tersedia dalam satu aplikasi yang elegan.</p>
            </div>
        </div>

        <!-- ═══════════════════════════════════════ -->
        <!-- SLIDE 3 – Be Better -->
        <!-- ═══════════════════════════════════════ -->
        <div class="slide slide-3">
            <div class="illus-area">
                <div class="orb orb-1"></div>
                <div class="orb orb-2"></div>
                <div class="orb orb-3"></div>

                <!-- Illustration: silhouette trees + orbs (based on screenshot) -->
                <svg class="illus-svg" viewBox="0 0 300 260" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <!-- Big orb -->
                    <circle cx="150" cy="80" r="65" fill="rgba(255,255,255,0.22)"/>
                    <!-- Smaller orb overlapping -->
                    <circle cx="200" cy="55" r="38" fill="rgba(255,255,255,0.18)"/>
                    <!-- Inner bright circles -->
                    <circle cx="145" cy="75" r="45" fill="rgba(255,255,255,0.15)"/>

                    <!-- Tree / Minaret Silhouettes (bottom) -->
                    <!-- Left large -->
                    <path d="M40 260 L40 180 Q40 155 60 148 Q80 155 80 180 L80 260Z" fill="rgba(255,255,255,0.18)"/>
                    <ellipse cx="60" cy="148" rx="20" ry="28" fill="rgba(255,255,255,0.18)"/>
                    <!-- Left mid -->
                    <path d="M15 260 L15 200 Q15 185 28 180 Q41 185 41 200 L41 260Z" fill="rgba(255,255,255,0.12)"/>
                    <ellipse cx="28" cy="180" rx="14" ry="20" fill="rgba(255,255,255,0.12)"/>
                    <!-- Right large -->
                    <path d="M220 260 L220 180 Q220 155 240 148 Q260 155 260 180 L260 260Z" fill="rgba(255,255,255,0.18)"/>
                    <ellipse cx="240" cy="148" rx="20" ry="28" fill="rgba(255,255,255,0.18)"/>
                    <!-- Right small -->
                    <path d="M258 260 L258 205 Q258 190 270 185 Q282 190 282 205 L282 260Z" fill="rgba(255,255,255,0.12)"/>
                    <ellipse cx="270" cy="185" rx="13" ry="19" fill="rgba(255,255,255,0.12)"/>

                    <!-- Horizon glow -->
                    <ellipse cx="150" cy="200" rx="130" ry="30" fill="rgba(255,255,255,0.08)"/>

                    <!-- Stars -->
                    <circle cx="50"  cy="30"  r="2.5" fill="rgba(255,255,255,0.7)"/>
                    <circle cx="250" cy="40"  r="2"   fill="rgba(255,255,255,0.6)"/>
                    <circle cx="100" cy="20"  r="1.5" fill="rgba(255,255,255,0.5)"/>
                    <circle cx="210" cy="25"  r="1.5" fill="rgba(255,255,255,0.5)"/>
                    <circle cx="170" cy="15"  r="2"   fill="rgba(255,255,255,0.6)"/>

                    <!-- Crescent moon -->
                    <path d="M135 48 Q155 35 165 55 Q148 45 135 48Z" fill="rgba(255,255,255,0.85)"/>
                </svg>
            </div>

            <div class="wave-divider">
                <svg viewBox="0 0 390 80" preserveAspectRatio="none">
                    <path d="M0 35 Q97.5 75 195 35 Q292.5 -5 390 35 L390 80 L0 80 Z" fill="#FFF8E1"/>
                </svg>
            </div>

            <div class="text-area" style="justify-content: flex-start; padding-top: 18px;">
                <h2 class="slide-title">Be Better</h2>
                <p class="slide-desc">Menyediakan semua kebutuhan ibadahmu di dalam satu aplikasi. Mulai perjalanan spiritualmu hari ini.</p>
            </div>
        </div>

    </div><!-- /.slides-track -->

    <!-- ─── Navigation Bar ──────────────────────── -->
    <div class="nav-bar" id="navBar">
        <!-- Skip button -->
        <button class="btn-skip" id="btnSkip" onclick="skipOnboarding()">Lewati</button>

        <!-- Dots -->
        <div class="dots" id="dotsContainer">
            <div class="dot active" id="dot-0"></div>
            <div class="dot"       id="dot-1"></div>
            <div class="dot"       id="dot-2"></div>
        </div>

        <!-- Next / Start button -->
        <button class="btn-arrow" id="btnNext" onclick="nextSlide()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
        </button>
    </div>

    <!-- Get Started button (shows on last slide, replaces nav) -->
    <div id="startBar" style="display:none; position:absolute; bottom:0; left:0; right:0; padding:16px 28px 24px; background:#FFF8E1; z-index:10;">
        <button class="btn-start" id="btnStart" style="display:block; width:100%;" onclick="finishOnboarding()">
            Mulai Sekarang ✨
        </button>
    </div>

</div>

<script>
    let current = 0;
    const total = 3;
    const track = document.getElementById('slidesTrack');
    const dots  = document.querySelectorAll('.dot');
    const btnNext  = document.getElementById('btnNext');
    const btnSkip  = document.getElementById('btnSkip');
    const navBar   = document.getElementById('navBar');
    const startBar = document.getElementById('startBar');

    function goToSlide(index) {
        current = index;
        // Each slide is 1/3 of the full track width (which is 300%)
        // So 1 slide = 33.333% of track = 100% of viewport
        track.style.transform = `translateX(-${(100 / 3) * index}%)`;

        // Update dots
        dots.forEach((d, i) => {
            d.classList.toggle('active', i === index);
        });

        // Show/hide Get Started
        if (index === total - 1) {
            navBar.style.display = 'none';
            startBar.style.display = 'block';
        } else {
            navBar.style.display = 'flex';
            startBar.style.display = 'none';
        }
    }

    function nextSlide() {
        if (current < total - 1) {
            goToSlide(current + 1);
        }
    }

    function skipOnboarding() {
        finishOnboarding();
    }

    function finishOnboarding() {
        window.location.href = 'complete_onboarding.php';
    }

    // Touch/swipe support
    let startX = 0;
    const shell = document.querySelector('.phone-shell');
    
    shell.addEventListener('touchstart', e => { startX = e.touches[0].clientX; }, { passive: true });
    shell.addEventListener('touchend', e => {
        const diff = startX - e.changedTouches[0].clientX;
        if (Math.abs(diff) > 50) {
            if (diff > 0 && current < total - 1) goToSlide(current + 1);
            else if (diff < 0 && current > 0) goToSlide(current - 1);
        }
    }, { passive: true });

    // Mouse drag support (desktop)
    let isDragging = false;
    let dragStartX = 0;
    shell.addEventListener('mousedown', e => { isDragging = true; dragStartX = e.clientX; });
    shell.addEventListener('mouseup', e => {
        if (!isDragging) return;
        isDragging = false;
        const diff = dragStartX - e.clientX;
        if (Math.abs(diff) > 60) {
            if (diff > 0 && current < total - 1) goToSlide(current + 1);
            else if (diff < 0 && current > 0) goToSlide(current - 1);
        }
    });
</script>
</body>
</html>

package com.quran.labs.androidquran.ui.fragment

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.quran.labs.androidquran.CatatanActivity
import com.quran.labs.androidquran.DzikirActivity
import com.quran.labs.androidquran.KhatamActivity
import com.quran.labs.androidquran.KiblatActivity
import com.quran.labs.androidquran.PejuangQuranActivity
import com.quran.labs.androidquran.KalenderActivity
import com.quran.labs.androidquran.ui.QuranActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.SessionManager
import com.quran.labs.androidquran.adhan.AdhanScheduler
import com.quran.labs.androidquran.adhan.IndonesiaCities
import com.quran.labs.androidquran.adhan.PrayerTimeCalculator
import com.quran.labs.androidquran.model.CampaignItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PendampingIbadahFragment : Fragment() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pendamping_ibadah, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Navigasi ---
        view.findViewById<View>(R.id.card_quran)?.setOnClickListener {
            val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.navigation_tilawah
        }
        view.findViewById<View>(R.id.card_dzikir)?.setOnClickListener {
            startActivity(Intent(activity, DzikirActivity::class.java))
        }
        view.findViewById<View>(R.id.card_kiblat)?.setOnClickListener {
            startActivity(Intent(activity, KiblatActivity::class.java))
        }
        view.findViewById<View>(R.id.card_khatam)?.setOnClickListener {
            startActivity(Intent(activity, KhatamActivity::class.java))
        }

        view.findViewById<View>(R.id.card_catatan)?.setOnClickListener {
            startActivity(Intent(activity, CatatanActivity::class.java))
        }
        view.findViewById<View>(R.id.card_kalender)?.setOnClickListener {
            startActivity(Intent(activity, KalenderActivity::class.java))
        }
        view.findViewById<View>(R.id.card_pejuang_kebaikan)?.setOnClickListener {
            startActivity(Intent(activity, PejuangQuranActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_alhamdulillah)?.setOnClickListener {
            Toast.makeText(context, "Alhamdulillah! Semoga Allah meridhoi amalan kita.", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.fab_last_page)?.setOnClickListener {
            (activity as? QuranActivity)?.jumpToLastPage()
        }

        // --- Tampilkan waktu sholat ---
        updatePrayerTimes(view)

        // --- Pilih Kota ---
        view.findViewById<View>(R.id.btn_select_city)?.setOnClickListener {
            showCityPickerDialog()
        }

        // --- Load Campaign Dinamis ---
        loadCampaigns(view)

        // --- Setup Rotator Donasi Ticker (5 Detik) ---
        setupDonationTicker(view)

        // --- Toggle Notifikasi Adzan ---
        val switchAdhan = view.findViewById<SwitchMaterial>(R.id.switch_adhan)
        val tvStatus = view.findViewById<TextView>(R.id.tv_adhan_status)
        val ctx = requireContext()

        // Set state dari SharedPreferences
        switchAdhan?.isChecked = AdhanScheduler.isEnabled(ctx)
        updateAdhanStatusText(tvStatus, AdhanScheduler.isEnabled(ctx))

        switchAdhan?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Cek dan minta permission jika diperlukan
                if (!checkAndRequestPermissions()) {
                    switchAdhan.isChecked = false
                    return@setOnCheckedChangeListener
                }
                AdhanScheduler.setEnabled(ctx, true)
                updateAdhanStatusText(tvStatus, true)
                Toast.makeText(ctx, "✅ Notifikasi adzan diaktifkan", Toast.LENGTH_SHORT).show()
            } else {
                AdhanScheduler.setEnabled(ctx, false)
                updateAdhanStatusText(tvStatus, false)
                Toast.makeText(ctx, "🔕 Notifikasi adzan dinonaktifkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePrayerTimes(view: View) {
        try {
            val ctx = requireContext()
            val prefs = ctx.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
            val cityName = prefs.getString("city_name", "Jakarta") ?: "Jakarta"
            val lat = prefs.getFloat("lat", -6.2088f).toDouble()  // Default: Jakarta
            val lng = prefs.getFloat("lng", 106.8456f).toDouble()

            val tz = if (cityName == "GPS") {
                (java.util.TimeZone.getDefault().rawOffset / 3600000.0)
            } else {
                prefs.getFloat("timezone", 7.0f).toDouble()
            }

            val btnSelectCity = view.findViewById<TextView>(R.id.btn_select_city)
            if (cityName == "GPS") {
                btnSelectCity?.text = "📍 GPS Otomatis"
            } else {
                btnSelectCity?.text = "📍 $cityName"
            }

            val calculator = PrayerTimeCalculator(lat, lng, tz)
            val times = calculator.calculate(Calendar.getInstance())

            view.findViewById<TextView>(R.id.tv_subuh)?.text = timeFormat.format(times.fajr.time)
            view.findViewById<TextView>(R.id.tv_dzuhur)?.text = timeFormat.format(times.dhuhr.time)
            view.findViewById<TextView>(R.id.tv_ashar)?.text = timeFormat.format(times.asr.time)
            view.findViewById<TextView>(R.id.tv_maghrib)?.text = timeFormat.format(times.maghrib.time)
            view.findViewById<TextView>(R.id.tv_isya)?.text = timeFormat.format(times.isha.time)

            highlightActivePrayer(view, times)
        } catch (e: Exception) {
            // Jika gagal, tampilkan -- sebagai fallback
        }
    }

    private fun highlightActivePrayer(view: View, times: PrayerTimeCalculator.PrayerTimes) {
        val now = Calendar.getInstance()
        fun getMinutesSinceMidnight(cal: Calendar): Int {
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }

        val nowMin = getMinutesSinceMidnight(now)
        val subuhMin = getMinutesSinceMidnight(times.fajr)
        val dzuhurMin = getMinutesSinceMidnight(times.dhuhr)
        val asharMin = getMinutesSinceMidnight(times.asr)
        val maghribMin = getMinutesSinceMidnight(times.maghrib)
        val isyaMin = getMinutesSinceMidnight(times.isha)

        val activeColId = when {
            nowMin in subuhMin until dzuhurMin -> R.id.col_subuh
            nowMin in dzuhurMin until asharMin -> R.id.col_dzuhur
            nowMin in asharMin until maghribMin -> R.id.col_ashar
            nowMin in maghribMin until isyaMin -> R.id.col_maghrib
            else -> R.id.col_isya
        }

        val columns = listOf(
            Triple(R.id.col_subuh, R.id.label_subuh, R.id.tv_subuh),
            Triple(R.id.col_dzuhur, R.id.label_dzuhur, R.id.tv_dzuhur),
            Triple(R.id.col_ashar, R.id.label_ashar, R.id.tv_ashar),
            Triple(R.id.col_maghrib, R.id.label_maghrib, R.id.tv_maghrib),
            Triple(R.id.col_isya, R.id.label_isya, R.id.tv_isya)
        )

        for ((colId, labelId, tvId) in columns) {
            val col = view.findViewById<View>(colId)
            val label = view.findViewById<TextView>(labelId)
            val tv = view.findViewById<TextView>(tvId)

            if (colId == activeColId) {
                col?.setBackgroundResource(R.drawable.bg_active_prayer_time)
                label?.setTextColor(0xFFFFFFFF.toInt())
                tv?.setTextColor(0xFFFFFFFF.toInt())
            } else {
                col?.setBackgroundResource(0)
                label?.setTextColor(0xFF757575.toInt())
                tv?.setTextColor(0xFF212121.toInt())
            }
        }
    }

    private fun updateAdhanStatusText(tv: TextView?, enabled: Boolean) {
        tv?.text = if (enabled) {
            "✅ Aktif — Notifikasi & adzan otomatis"
        } else {
            "Nonaktif — Tap untuk mengaktifkan"
        }
        tv?.setTextColor(
            if (enabled) 0xFF2E7D32.toInt() else 0xFF757575.toInt()
        )
    }

    private fun checkAndRequestPermissions(): Boolean {
        val ctx = requireContext()

        // Cek SCHEDULE_EXACT_ALARM di Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    ctx,
                    "Izinkan 'Alarm yang Tepat Waktu' di pengaturan",
                    Toast.LENGTH_LONG
                ).show()
                // Buka pengaturan alarm
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                }
                startActivity(intent)
                return false
            }
        }

        // Cek POST_NOTIFICATIONS di Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                return false
            }
        }

        return true
    }

    private fun showCityPickerDialog() {
        val ctx = requireContext()
        val items = ArrayList<String>()
        items.add("🛰️ Lokasi GPS (Otomatis)")

        val sortedCities = IndonesiaCities.cities.sortedBy { it.name }
        items.addAll(sortedCities.map { "${it.name} (${it.province}) - ${it.timezone}" })

        AlertDialog.Builder(ctx)
            .setTitle("Pilih Lokasi Jadwal Sholat")
            .setItems(items.toTypedArray()) { _, which ->
                if (which == 0) {
                    if (!checkLocationPermission()) {
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            102
                        )
                    } else {
                        setGpsMode()
                    }
                } else {
                    val selectedCity = sortedCities[which - 1]
                    AdhanScheduler.saveCityData(
                        ctx,
                        selectedCity.name,
                        selectedCity.latitude,
                        selectedCity.longitude,
                        selectedCity.timezoneOffset
                    )
                    view?.let { updatePrayerTimes(it) }
                    Toast.makeText(ctx, "Lokasi diubah ke: ${selectedCity.name}", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setGpsMode() {
        val ctx = requireContext()
        ctx.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("city_name", "GPS")
            apply()
        }
        AdhanScheduler.updateLocationAndSchedule(ctx)
        view?.let { updatePrayerTimes(it) }
        Toast.makeText(ctx, "Menggunakan Lokasi GPS Otomatis", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 102) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setGpsMode()
            } else {
                Toast.makeText(requireContext(), "Izin lokasi diperlukan untuk GPS otomatis", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Campaign Donasi ─────────────────────────────────────────

    private fun loadCampaigns(view: View) {
        val sessionManager = SessionManager(requireContext())
        val isAdmin = sessionManager.isLoggedIn() && sessionManager.getUserRole() == "admin"

        val rvCampaigns = view.findViewById<RecyclerView>(R.id.rv_campaigns)
        val tvNoCampaign = view.findViewById<TextView>(R.id.tv_no_campaign)
        val btnAddCampaign = view.findViewById<TextView>(R.id.btn_add_campaign)

        // Tampilkan tombol tambah untuk admin
        if (isAdmin) {
            btnAddCampaign?.visibility = View.VISIBLE
            btnAddCampaign?.setOnClickListener { showCampaignDialog(null, view) }
        }

        rvCampaigns?.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val allParam = if (isAdmin) 1 else 0
                val response = AuthClient.apiService.getCampaigns(all = allParam)
                if (response.isSuccessful) {
                    val campaigns = response.body()?.data ?: emptyList()
                    if (campaigns.isEmpty()) {
                        tvNoCampaign?.visibility = View.VISIBLE
                        rvCampaigns?.visibility = View.GONE
                    } else {
                        tvNoCampaign?.visibility = View.GONE
                        rvCampaigns?.visibility = View.VISIBLE
                        rvCampaigns?.adapter = CampaignAdapter(campaigns, isAdmin,
                            onClick = { campaign ->
                                if (campaign.donateUrl.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(campaign.donateUrl))
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Link informasi belum tersedia", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLongClick = { campaign ->
                                if (isAdmin) showAdminCampaignOptions(campaign, view)
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                // Gagal load — biarkan kosong
            }
        }
    }

    private fun showAdminCampaignOptions(campaign: CampaignItem, view: View) {
        val options = arrayOf("✏️ Edit Program", "❌ Hapus Program")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilihan Admin")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCampaignDialog(campaign, view)
                    1 -> confirmDeleteCampaign(campaign, view)
                }
            }
            .show()
    }

    private fun confirmDeleteCampaign(campaign: CampaignItem, view: View) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Program")
            .setMessage("Yakin ingin menghapus program \"${campaign.title}\"?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                val sessionManager = SessionManager(requireContext())
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = AuthClient.apiService.deleteCampaign(
                            userId = sessionManager.getUserId(),
                            id = campaign.id
                        )
                        activity?.runOnUiThread {
                            if (response.isSuccessful && response.body()?.success == true) {
                                Toast.makeText(context, "Program berhasil dihapus", Toast.LENGTH_SHORT).show()
                                loadCampaigns(view)
                            } else {
                                Toast.makeText(context, "Gagal menghapus program", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Koneksi server gagal", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showCampaignDialog(campaignToEdit: CampaignItem?, view: View) {
        val ctx = context ?: return
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_campaign, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.et_campaign_title)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_campaign_desc)
        val etImageUrl = dialogView.findViewById<EditText>(R.id.et_campaign_image_url)
        val etDonateUrl = dialogView.findViewById<EditText>(R.id.et_campaign_donate_url)
        val cbActive = dialogView.findViewById<CheckBox>(R.id.cb_campaign_active)
        val imgPreview = dialogView.findViewById<ImageView>(R.id.img_campaign_photo_preview)
        val tvPlaceholder = dialogView.findViewById<TextView>(R.id.tv_campaign_photo_placeholder)

        // Fill existing data if editing
        if (campaignToEdit != null) {
            etTitle.setText(campaignToEdit.title)
            etDesc.setText(campaignToEdit.description)
            etImageUrl.setText(campaignToEdit.imageUrl)
            etDonateUrl.setText(campaignToEdit.donateUrl)
            cbActive.isChecked = campaignToEdit.isActive

            // Load existing photo preview
            val existingUrl = campaignToEdit.imageUrl
            if (existingUrl.isNotEmpty()) {
                tvPlaceholder.visibility = View.GONE
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            val conn = java.net.URL(existingUrl).openConnection() as java.net.HttpURLConnection
                            conn.doInput = true; conn.connect()
                            BitmapFactory.decodeStream(conn.inputStream)
                        }
                        imgPreview.setImageBitmap(bitmap)
                    } catch (_: Exception) {}
                }
            }
        }

        // Live preview foto dari URL
        etImageUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s.toString().trim()
                if (url.startsWith("http")) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                conn.doInput = true; conn.connect()
                                BitmapFactory.decodeStream(conn.inputStream)
                            }
                            imgPreview.setImageBitmap(bitmap)
                            tvPlaceholder.visibility = View.GONE
                        } catch (_: Exception) {}
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        AlertDialog.Builder(ctx)
            .setTitle(if (campaignToEdit == null) "➕ Tambah Program" else "✏️ Edit Program")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val title = etTitle.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                val imageUrl = etImageUrl.text.toString().trim()
                val donateUrl = etDonateUrl.text.toString().trim()
                val isActive = if (cbActive.isChecked) 1 else 0

                if (title.isEmpty()) {
                    Toast.makeText(ctx, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sessionManager = SessionManager(ctx)
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = AuthClient.apiService.saveCampaign(
                            userId = sessionManager.getUserId(),
                            id = campaignToEdit?.id ?: 0,
                            title = title,
                            description = desc,
                            imageUrl = imageUrl,
                            donateUrl = donateUrl,
                            isActive = isActive
                        )
                        activity?.runOnUiThread {
                            if (response.isSuccessful && response.body()?.success == true) {
                                Toast.makeText(ctx, "Program berhasil disimpan", Toast.LENGTH_SHORT).show()
                                loadCampaigns(view)
                            } else {
                                Toast.makeText(ctx, "Gagal menyimpan: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            Toast.makeText(ctx, "Koneksi server gagal", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─── Campaign RecyclerView Adapter ───────────────────────────

    inner class CampaignAdapter(
        private val list: List<CampaignItem>,
        private val isAdmin: Boolean,
        private val onClick: (CampaignItem) -> Unit,
        private val onLongClick: (CampaignItem) -> Unit
    ) : RecyclerView.Adapter<CampaignAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgBanner: ImageView = view.findViewById(R.id.img_campaign_banner)
            val tvTitle: TextView = view.findViewById(R.id.tv_campaign_title)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_campaign_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val campaign = list[position]
            holder.tvTitle.text = campaign.title

            // Load banner image from URL
            if (campaign.imageUrl.isNotEmpty()) {
                val url = campaign.imageUrl
                holder.imgBanner.tag = url
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                            conn.doInput = true; conn.connect()
                            BitmapFactory.decodeStream(conn.inputStream)
                        }
                        if (holder.imgBanner.tag == url) {
                            holder.imgBanner.setImageBitmap(bitmap)
                        }
                    } catch (_: Exception) {}
                }
            }

            holder.itemView.setOnClickListener { onClick(campaign) }
            if (isAdmin) {
                holder.itemView.setOnLongClickListener {
                    onLongClick(campaign)
                    true
                }
            }
        }

        override fun getItemCount() = list.size
    }

    // ─── ROTATOR DONASI TICKER (5 DETIK) ──────────────────────
    private var tickerHandler: android.os.Handler? = null
    private var tickerRunnable: Runnable? = null
    private var donationItems = listOf<com.quran.labs.androidquran.model.DonationItem>()
    private var currentDonationIndex = 0

    private fun setupDonationTicker(view: View) {
        val switcher = view.findViewById<TextSwitcher>(R.id.switcher_donation_ticker) ?: return

        switcher.setFactory {
            TextView(requireContext()).apply {
                setTextColor(android.graphics.Color.WHITE)
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        // Animasi fade in & fade out halus
        val fadeIn = android.view.animation.AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        val fadeOut = android.view.animation.AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
        switcher.inAnimation = fadeIn
        switcher.outAnimation = fadeOut

        // Data awal default sampel
        donationItems = listOf(
            com.quran.labs.androidquran.model.DonationItem(1, "Ahmad Fauzi", 100000.0, "Rp 100.000", "Sedekah Mushaf", "5 mnt lalu"),
            com.quran.labs.androidquran.model.DonationItem(2, "Hamba Allah", 50000.0, "Rp 50.000", "Infaq Dakwah", "15 mnt lalu"),
            com.quran.labs.androidquran.model.DonationItem(3, "Siti Nurhaliza", 250000.0, "Rp 250.000", "Beasiswa Tahfidz", "45 mnt lalu")
        )

        displayCurrentDonation(switcher)

        // Fetch data asli dari server API
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getDonationsTicker()
                if (response.isSuccessful) {
                    val list = response.body()?.data
                    if (!list.isNullOrEmpty()) {
                        donationItems = list
                        currentDonationIndex = 0
                        displayCurrentDonation(switcher)
                    }
                }
            } catch (_: Exception) {}
        }

        // Jalankan rotasi otomatis 5 detik
        startDonationTickerLoop(switcher)
    }

    private fun displayCurrentDonation(switcher: TextSwitcher) {
        if (donationItems.isEmpty()) return
        val item = donationItems[currentDonationIndex % donationItems.size]
        val text = "${item.userName} berkontribusi kebaikan ${item.formattedAmount} • ${item.timeAgo}"
        switcher.setText(text)
    }

    private fun startDonationTickerLoop(switcher: TextSwitcher) {
        stopDonationTickerLoop()
        tickerHandler = android.os.Handler(android.os.Looper.getMainLooper())
        tickerRunnable = object : Runnable {
            override fun run() {
                if (donationItems.isNotEmpty()) {
                    currentDonationIndex = (currentDonationIndex + 1) % donationItems.size
                    displayCurrentDonation(switcher)
                }
                tickerHandler?.postDelayed(this, 5000) // 5 Detik
            }
        }
        tickerHandler?.postDelayed(tickerRunnable!!, 5000)
    }

    private fun stopDonationTickerLoop() {
        tickerRunnable?.let { tickerHandler?.removeCallbacks(it) }
        tickerHandler = null
    }

    override fun onDestroyView() {
        stopDonationTickerLoop()
        super.onDestroyView()
    }
}

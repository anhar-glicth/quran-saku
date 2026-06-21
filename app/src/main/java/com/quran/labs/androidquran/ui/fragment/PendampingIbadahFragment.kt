package com.quran.labs.androidquran.ui.fragment

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.quran.labs.androidquran.CatatanActivity
import com.quran.labs.androidquran.DzikirActivity
import com.quran.labs.androidquran.KhatamActivity
import com.quran.labs.androidquran.KiblatActivity
import com.quran.labs.androidquran.PejuangQuranActivity
import com.quran.labs.androidquran.KalenderActivity
import com.quran.labs.androidquran.ui.QuranActivity
import com.quran.labs.androidquran.R
import androidx.appcompat.app.AlertDialog
import com.quran.labs.androidquran.adhan.AdhanScheduler
import com.quran.labs.androidquran.adhan.IndonesiaCities
import com.quran.labs.androidquran.adhan.PrayerTimeCalculator
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
        view.findViewById<View>(R.id.card_zakat)?.setOnClickListener {
            Toast.makeText(context, "Fitur Zakat sedang dikembangkan", Toast.LENGTH_SHORT).show()
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
}

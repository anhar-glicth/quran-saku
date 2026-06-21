package com.quran.labs.androidquran.adhan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Mengelola penjadwalan alarm untuk notifikasi adzan.
 * Menjadwalkan alarm 5 menit sebelum (reminder) dan tepat waktu adzan.
 */
object AdhanScheduler {

    private const val TAG = "AdhanScheduler"
    const val EXTRA_PRAYER_NAME = "prayer_name"
    const val EXTRA_ALARM_TYPE = "alarm_type"
    const val TYPE_REMINDER = "reminder"   // 5 menit sebelum
    const val TYPE_ADHAN = "adhan"         // tepat waktu

    // Request codes untuk setiap sholat (harus unik)
    private val PRAYER_CODES = mapOf(
        "Subuh" to 100,
        "Dzuhur" to 200,
        "Ashar" to 300,
        "Maghrib" to 400,
        "Isya" to 500
    )

    fun scheduleForToday(context: Context) {
        val prefs = context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("adhan_enabled", false)) {
            Log.d(TAG, "Adhan notification is disabled, skipping schedule")
            return
        }

        val cityName = prefs.getString("city_name", "Jakarta") ?: "Jakarta"
        val lat = prefs.getFloat("lat", -6.2088f).toDouble()
        val lng = prefs.getFloat("lng", 106.8456f).toDouble()
        val tz = if (cityName == "GPS") {
            (java.util.TimeZone.getDefault().rawOffset / 3600000.0)
        } else {
            prefs.getFloat("timezone", 7.0f).toDouble()
        }

        scheduleWithCoordinates(context, lat, lng, tz)
    }

    /**
     * Menjadwalkan adzan untuk hari berikutnya.
     * Dipanggil setelah alarm Isya selesai, agar adzan terus berjalan setiap hari
     * tanpa perlu membuka aplikasi (kecuali HP di-restart, BootReceiver yang handle).
     */
    fun scheduleForNextDay(context: Context) {
        val prefs = context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("adhan_enabled", false)) {
            Log.d(TAG, "Adhan disabled, skipping next day schedule")
            return
        }

        val cityName = prefs.getString("city_name", "Jakarta") ?: "Jakarta"
        val lat = prefs.getFloat("lat", -6.2088f).toDouble()
        val lng = prefs.getFloat("lng", 106.8456f).toDouble()
        val tz = if (cityName == "GPS") {
            (java.util.TimeZone.getDefault().rawOffset / 3600000.0)
        } else {
            prefs.getFloat("timezone", 7.0f).toDouble()
        }

        Log.d(TAG, "Scheduling adhan for tomorrow")

        // Hitung untuk tanggal besok
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val calculator = PrayerTimeCalculator(lat, lng, tz)
        val prayerTimes = calculator.calculate(tomorrow)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms for next day")
                return
            }
        }

        prayerTimes.getSholatTimes().forEach { (name, calendar) ->
            val baseCode = PRAYER_CODES[name] ?: return@forEach
            val prayerTimeMs = calendar.timeInMillis

            // Alarm 5 menit sebelum (reminder)
            val reminderTimeMs = prayerTimeMs - (5 * 60 * 1000)
            if (reminderTimeMs > now) {
                scheduleAlarm(context, alarmManager, name, reminderTimeMs, TYPE_REMINDER, baseCode + 1)
                Log.d(TAG, "Scheduled tomorrow REMINDER for $name at ${calendar.time}")
            }

            // Alarm tepat waktu adzan
            if (prayerTimeMs > now) {
                scheduleAlarm(context, alarmManager, name, prayerTimeMs, TYPE_ADHAN, baseCode + 2)
                Log.d(TAG, "Scheduled tomorrow ADHAN for $name at ${calendar.time}")
            }
        }
    }

    fun scheduleWithCoordinates(context: Context, lat: Double, lng: Double, timezoneOffset: Double = 7.0) {
        val calculator = PrayerTimeCalculator(lat, lng, timezoneOffset)
        val prayerTimes = calculator.calculate()

        Log.d(TAG, "Scheduling adhan for: lat=$lat, lng=$lng")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        // Cek permission untuk exact alarm di Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms - permission not granted")
                return
            }
        }

        prayerTimes.getSholatTimes().forEach { (name, calendar) ->
            val baseCode = PRAYER_CODES[name] ?: return@forEach
            val prayerTimeMs = calendar.timeInMillis

            // Alarm 5 menit sebelum (reminder)
            val reminderTimeMs = prayerTimeMs - (5 * 60 * 1000)
            if (reminderTimeMs > now) {
                scheduleAlarm(context, alarmManager, name, reminderTimeMs, TYPE_REMINDER, baseCode + 1)
                Log.d(TAG, "Scheduled REMINDER for $name at ${calendar.time} (5 min early)")
            }

            // Alarm tepat waktu adzan
            if (prayerTimeMs > now) {
                scheduleAlarm(context, alarmManager, name, prayerTimeMs, TYPE_ADHAN, baseCode + 2)
                Log.d(TAG, "Scheduled ADHAN for $name at ${calendar.time}")
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        prayerName: String,
        triggerAtMs: Long,
        type: String,
        requestCode: Int
    ) {
        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_NAME, prayerName)
            putExtra(EXTRA_ALARM_TYPE, type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        PRAYER_CODES.forEach { (name, baseCode) ->
            listOf(baseCode + 1, baseCode + 2).forEach { requestCode ->
                val intent = Intent(context, AdhanAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent?.let { alarmManager.cancel(it) }
            }
        }
        Log.d(TAG, "All adhan alarms cancelled")
    }

    fun saveLocation(context: Context, lat: Double, lng: Double) {
        context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE).edit().apply {
            putFloat("lat", lat.toFloat())
            putFloat("lng", lng.toFloat())
            apply()
        }
    }

    fun saveCityData(context: Context, cityName: String, lat: Double, lng: Double, timezoneOffset: Double) {
        context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("city_name", cityName)
            putFloat("lat", lat.toFloat())
            putFloat("lng", lng.toFloat())
            putFloat("timezone", timezoneOffset.toFloat())
            apply()
        }
        // Jadwalkan ulang dengan koordinat baru
        if (isEnabled(context)) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                cancelAll(context)
                scheduleWithCoordinates(context, lat, lng, timezoneOffset)
            }
        }
    }

    fun getSavedCityName(context: Context): String {
        return context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
            .getString("city_name", "Jakarta") ?: "Jakarta"
    }

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
            .getBoolean("adhan_enabled", false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE).edit().apply {
            putBoolean("adhan_enabled", enabled)
            apply()
        }
        if (enabled) {
            val prefs = context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
            val cityName = prefs.getString("city_name", "Jakarta") ?: "Jakarta"
            if (cityName == "GPS") {
                updateLocationAndSchedule(context)
            } else {
                scheduleForToday(context)
            }
            // Jadwalkan WorkManager sebagai lapisan keamanan backup
            AdhanWorker.schedule(context)
        } else {
            cancelAll(context)
            // Batalkan juga WorkManager
            AdhanWorker.cancel(context)
        }
    }

    fun updateLocationAndSchedule(context: Context) {
        val prefs = context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
        val cityName = prefs.getString("city_name", "Jakarta") ?: "Jakarta"

        if (cityName == "GPS") {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var bestLocation: Location? = null
                val providers = locationManager.getProviders(true)
                for (provider in providers) {
                    @Suppress("MissingPermission")
                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
                if (bestLocation != null) {
                    prefs.edit().apply {
                        putFloat("lat", bestLocation.latitude.toFloat())
                        putFloat("lng", bestLocation.longitude.toFloat())
                        apply()
                    }
                    Log.d(TAG, "Updated location: ${bestLocation.latitude}, ${bestLocation.longitude}")
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Location permission not granted, using saved location")
            }
        }
        scheduleForToday(context)
    }
}

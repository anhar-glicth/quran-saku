package com.quran.labs.androidquran.adhan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.quran.labs.androidquran.QuranDataActivity
import com.quran.labs.androidquran.R

/**
 * BroadcastReceiver yang menerima alarm dari AlarmManager.
 * - TYPE_REMINDER: tampilkan notifikasi peringatan 5 menit sebelum adzan
 * - TYPE_ADHAN: start AdhanService untuk memutar suara adzan
 */
class AdhanAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID_REMINDER = "adhan_reminder_channel"
        const val CHANNEL_ID_ADHAN = "adhan_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER_NAME) ?: return
        val type = intent.getStringExtra(AdhanScheduler.EXTRA_ALARM_TYPE) ?: return

        Log.d("AdhanAlarmReceiver", "Received alarm: $prayerName - $type")

        createNotificationChannels(context)

        when (type) {
            AdhanScheduler.TYPE_REMINDER -> showReminderNotification(context, prayerName)
            AdhanScheduler.TYPE_ADHAN -> startAdhanService(context, prayerName)
        }

        // Jika ini adalah alarm Isya (sholat terakhir), jadwalkan untuk hari berikutnya
        if (type == AdhanScheduler.TYPE_ADHAN && prayerName == "Isya") {
            AdhanScheduler.scheduleForToday(context)
        }
    }

    private fun showReminderNotification(context: Context, prayerName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, QuranDataActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.ic_pendamping_ibadah)
            .setContentTitle("⏰ Waktu Sholat $prayerName dalam 5 Menit")
            .setContentText("Bersiaplah untuk melaksanakan sholat $prayerName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Waktu sholat $prayerName akan segera tiba. Bersiaplah untuk berwudhu dan melaksanakan sholat tepat waktu.")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFFFF6D00.toInt()) // Oranye
            .build()

        nm.notify(prayerName.hashCode(), notification)
    }

    private fun startAdhanService(context: Context, prayerName: String) {
        val serviceIntent = Intent(context, AdhanService::class.java).apply {
            putExtra(AdhanScheduler.EXTRA_PRAYER_NAME, prayerName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel untuk reminder 5 menit sebelum
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Pengingat Waktu Sholat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi 5 menit sebelum waktu sholat"
            }

            // Channel untuk adzan
            val adhanChannel = NotificationChannel(
                CHANNEL_ID_ADHAN,
                "Adzan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat waktu adzan tiba"
                setSound(null, null) // Suara dihandle oleh AdhanService
            }

            nm.createNotificationChannel(reminderChannel)
            nm.createNotificationChannel(adhanChannel)
        }
    }
}

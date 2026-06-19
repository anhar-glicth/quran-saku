package com.quran.labs.androidquran.adhan

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.quran.labs.androidquran.QuranDataActivity
import com.quran.labs.androidquran.R

/**
 * ForegroundService yang memutar suara adzan saat waktu sholat tiba.
 * Berjalan di foreground agar tidak dibunuh oleh sistem Android.
 */
class AdhanService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        private const val TAG = "AdhanService"
        private const val NOTIFICATION_ID = 9999
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra(AdhanScheduler.EXTRA_PRAYER_NAME) ?: "Sholat"

        Log.d(TAG, "Starting AdhanService for: $prayerName")

        // Tampilkan foreground notification
        showForegroundNotification(prayerName)

        // Minta audio focus agar adzan bisa didengar
        requestAudioFocus()

        // Putar audio adzan
        playAdhan(prayerName)

        return START_NOT_STICKY
    }

    private fun showForegroundNotification(prayerName: String) {
        val openAppIntent = Intent(this, QuranDataActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AdhanService::class.java).apply {
            action = "STOP_ADHAN"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AdhanAlarmReceiver.CHANNEL_ID_ADHAN)
            .setSmallIcon(R.drawable.ic_pendamping_ibadah)
            .setContentTitle("🕌 Waktu Sholat $prayerName")
            .setContentText("Allahu Akbar! Hayya 'alash-shalah...")
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_time, "Hentikan Adzan", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(0xFFFF6D00.toInt())
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun requestAudioFocus() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build()
            audioFocusRequest = focusRequest
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun playAdhan(prayerName: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                // Cek apakah ada file adhan di raw resources
                val rawId = try {
                    // Coba load dari raw resource (adhan.mp3)
                    resources.getIdentifier("adhan", "raw", packageName)
                } catch (e: Exception) { 0 }

                if (rawId != 0) {
                    val afd = resources.openRawResourceFd(rawId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                } else {
                    // Fallback: gunakan URL streaming adzan publik
                    setDataSource("https://islamicfinder.s3.amazonaws.com/prayer_times/adhan/adhan.mp3")
                }

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                setOnCompletionListener {
                    Log.d(TAG, "Adhan completed for $prayerName")
                    stopSelf()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopSelf()
                    true
                }

                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing adhan: ${e.message}")
            stopSelf()
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        releaseAudioFocus()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)

        Log.d(TAG, "AdhanService destroyed")
    }
}

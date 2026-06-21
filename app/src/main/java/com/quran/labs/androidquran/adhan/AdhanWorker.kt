package com.quran.labs.androidquran.adhan

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Worker yang berjalan setiap hari untuk menjadwalkan ulang alarm adzan.
 * Ini adalah lapisan keamanan kedua setelah AlarmManager + BootReceiver.
 *
 * Skenario yang ditangani:
 * - Aplikasi tidak dibuka berhari-hari
 * - AlarmManager kehilangan state karena battery optimization
 * - Cadangan jika BootReceiver tidak terpanggil
 */
class AdhanWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "AdhanWorker"
        const val WORK_NAME = "adhan_daily_scheduler"

        /**
         * Menjadwalkan AdhanWorker untuk berjalan setiap hari.
         * Harus dipanggil saat adzan diaktifkan dan saat aplikasi pertama kali dijalankan.
         */
        fun schedule(context: Context) {
            Log.d(TAG, "Scheduling daily adhan worker")

            // Hitung delay sampai pukul 00:05 besok (5 menit setelah tengah malam)
            // Ini memastikan worker berjalan tepat setelah pergantian hari
            val now = Calendar.getInstance()
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val initialDelay = nextMidnight.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<AdhanWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

            Log.d(TAG, "AdhanWorker scheduled, initial delay: ${initialDelay / 60000} minutes")
        }

        /**
         * Membatalkan AdhanWorker. Dipanggil saat adzan dinonaktifkan.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "AdhanWorker cancelled")
        }
    }

    override fun doWork(): Result {
        Log.d(TAG, "AdhanWorker running — rescheduling adhan for today")

        return try {
            // Jadwalkan ulang adzan untuk hari ini
            AdhanScheduler.scheduleForToday(context)
            Log.d(TAG, "AdhanWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "AdhanWorker failed: ${e.message}")
            Result.retry()
        }
    }
}

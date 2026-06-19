package com.quran.labs.androidquran.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Menerima broadcast BOOT_COMPLETED untuk menjadwalkan ulang alarm adzan
 * setelah HP restart (AlarmManager alarm hilang setelah reboot).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Device rebooted — rescheduling adhan alarms")
            AdhanScheduler.updateLocationAndSchedule(context)
        }
    }
}

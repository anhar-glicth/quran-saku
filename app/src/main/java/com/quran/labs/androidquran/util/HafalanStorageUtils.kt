package com.quran.labs.androidquran.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class untuk menyimpan dan membaca riwayat setoran hafalan
 * menggunakan SharedPreferences dengan format JSON.
 */
object HafalanStorageUtils {

  private const val PREF_NAME = "hafalan_history"
  private const val KEY_RECORDS = "records"
  private const val MAX_RECORDS = 100

  data class HafalanRecord(
    val surahName: String,
    val surahNum: Int,
    val startAyah: Int,
    val endAyah: Int,
    val accuracy: Int,
    val timestamp: Long,
    val status: String // "LULUS" or "TIDAK_LULUS"
  ) {
    fun formattedDate(): String {
      val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
      return sdf.format(Date(timestamp))
    }
    fun ayahRange(): String = "Ayat $startAyah - $endAyah"
    fun isLulus(): Boolean = status == "LULUS"
  }

  fun saveRecord(context: Context, record: HafalanRecord) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val existing = prefs.getString(KEY_RECORDS, "[]")
    val array = JSONArray(existing)

    val obj = JSONObject().apply {
      put("surahName", record.surahName)
      put("surahNum", record.surahNum)
      put("startAyah", record.startAyah)
      put("endAyah", record.endAyah)
      put("accuracy", record.accuracy)
      put("timestamp", record.timestamp)
      put("status", record.status)
    }

    // Sisipkan di awal (terbaru di atas)
    val newArray = JSONArray()
    newArray.put(obj)
    for (i in 0 until minOf(array.length(), MAX_RECORDS - 1)) {
      newArray.put(array.getJSONObject(i))
    }

    prefs.edit().putString(KEY_RECORDS, newArray.toString()).apply()
  }

  fun getAllRecords(context: Context): List<HafalanRecord> {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
    val array = JSONArray(json)
    val list = mutableListOf<HafalanRecord>()
    for (i in 0 until array.length()) {
      val obj = array.getJSONObject(i)
      list.add(
        HafalanRecord(
          surahName = obj.getString("surahName"),
          surahNum = obj.getInt("surahNum"),
          startAyah = obj.getInt("startAyah"),
          endAyah = obj.getInt("endAyah"),
          accuracy = obj.getInt("accuracy"),
          timestamp = obj.getLong("timestamp"),
          status = obj.getString("status")
        )
      )
    }
    return list
  }

  fun getLulusCount(context: Context): Int =
    getAllRecords(context).count { it.isLulus() }

  fun getTotalCount(context: Context): Int =
    getAllRecords(context).size

  fun getAverageAccuracy(context: Context): Int {
    val records = getAllRecords(context)
    if (records.isEmpty()) return 0
    return records.sumOf { it.accuracy } / records.size
  }

  fun clearAll(context: Context) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
      .edit().remove(KEY_RECORDS).apply()
  }
}

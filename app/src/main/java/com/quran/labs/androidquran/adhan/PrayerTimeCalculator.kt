package com.quran.labs.androidquran.adhan

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * Kalkulasi waktu sholat menggunakan algoritma astronomi standar.
 * Metode: MWL (Muslim World League) — digunakan oleh Kemenag Indonesia
 * Fajr angle: 18°, Isha angle: 17°
 */
class PrayerTimeCalculator(
    private val latitude: Double,
    private val longitude: Double,
    private val timeZone: Double = (TimeZone.getDefault().rawOffset / 3600000.0)
) {

    // Metode MWL (Muslim World League) / Kemenag Indonesia
    private val fajrAngle = 18.0
    private val ishaAngle = 17.0

    data class PrayerTimes(
        val fajr: Calendar,
        val sunrise: Calendar,
        val dhuhr: Calendar,
        val asr: Calendar,
        val maghrib: Calendar,
        val isha: Calendar
    ) {
        fun getNamedTimes(): List<Pair<String, Calendar>> = listOf(
            "Subuh" to fajr,
            "Syuruq" to sunrise,
            "Dzuhur" to dhuhr,
            "Ashar" to asr,
            "Maghrib" to maghrib,
            "Isya" to isha
        )

        fun getSholatTimes(): List<Pair<String, Calendar>> = listOf(
            "Subuh" to fajr,
            "Dzuhur" to dhuhr,
            "Ashar" to asr,
            "Maghrib" to maghrib,
            "Isya" to isha
        )
    }

    fun calculate(date: Calendar = Calendar.getInstance()): PrayerTimes {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)

        val julianDate = toJulian(year, month, day) - longitude / (15.0 * 24.0)

        val d = julianDate - 2451545.0
        val decl = declination(d)
        val eqt = equationOfTime(d)

        val noon = 12.0 - longitude / 15.0 - eqt

        val fajrTime = noon - hourAngle(fajrAngle, decl) / 15.0
        val sunriseTime = noon - hourAngle(0.833, decl) / 15.0
        val dhuhrTime = noon + 0.5 / 60.0 // Add 30 seconds margin
        val asrTime = noon + asrHourAngle(decl) / 15.0
        val maghribTime = noon + hourAngle(0.833, decl) / 15.0
        val ishaTime = noon + hourAngle(ishaAngle, decl) / 15.0

        return PrayerTimes(
            fajr = timeToCalendar(fajrTime, date),
            sunrise = timeToCalendar(sunriseTime, date),
            dhuhr = timeToCalendar(dhuhrTime, date),
            asr = timeToCalendar(asrTime, date),
            maghrib = timeToCalendar(maghribTime, date),
            isha = timeToCalendar(ishaTime, date)
        )
    }

    private fun declination(d: Double): Double {
        val g = 357.529 + 0.98560028 * d
        val q = 280.459 + 0.98564736 * d
        val l = q + 1.915 * sin(d2r(g)) + 0.020 * sin(d2r(2 * g))
        val e = 23.439 - 0.00000036 * d
        return r2d(asin(sin(d2r(e)) * sin(d2r(l))))
    }

    private fun equationOfTime(d: Double): Double {
        val g = 357.529 + 0.98560028 * d
        val q = 280.459 + 0.98564736 * d
        val l = q + 1.915 * sin(d2r(g)) + 0.020 * sin(d2r(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = r2d(atan2(cos(d2r(e)) * sin(d2r(l)), cos(d2r(l)))) / 15.0
        val eqt = fixHour(q / 15.0) - fixHour(ra)
        return eqt - 24.0 * round(eqt / 24.0)
    }

    private fun hourAngle(angle: Double, decl: Double): Double {
        val cosHA = (sin(d2r(-angle)) - sin(d2r(latitude)) * sin(d2r(decl))) /
                (cos(d2r(latitude)) * cos(d2r(decl)))
        return if (cosHA.isNaN() || cosHA > 1.0 || cosHA < -1.0) 0.0
        else r2d(acos(cosHA))
    }

    // Asar: Shafi'i method (shadow = 1x object height)
    private fun asrHourAngle(decl: Double): Double {
        val shadowFactor = 1.0 // Shafi'i
        val angle = -r2d(atan(1.0 / (shadowFactor + tan(d2r(abs(latitude - decl))))))
        return hourAngle(angle, decl)
    }

    private fun toJulian(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) { y--; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun timeToCalendar(hours: Double, baseDate: Calendar): Calendar {
        val adjustedHours = hours + timeZone
        val h = floor(adjustedHours).toInt()
        val m = floor((adjustedHours - h) * 60).toInt()
        val s = floor(((adjustedHours - h) * 60 - m) * 60).toInt()

        return (baseDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, ((h % 24) + 24) % 24)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, s)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun fixHour(a: Double): Double {
        var x = a % 24
        if (x < 0) x += 24
        return x
    }

    private fun d2r(d: Double) = d * PI / 180.0
    private fun r2d(r: Double) = r * 180.0 / PI
}

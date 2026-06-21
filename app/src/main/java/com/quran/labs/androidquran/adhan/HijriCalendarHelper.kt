package com.quran.labs.androidquran.adhan

import java.util.Calendar

object HijriCalendarHelper {

    data class FastingInfo(
        val name: String,
        val description: String,
        val isFasting: Boolean,
        val badge: String = "Puasa Sunnah"
    )

    // Tabular Islamic Calendar Algorithm
    fun gregorianToHijri(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        var y = year
        var m = month
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = Math.floor(y / 100.0).toInt()
        val b = 2 - a + Math.floor(a / 4.0).toInt()
        val jd = Math.floor(365.25 * (y + 4716)).toInt() + Math.floor(30.6001 * (m + 1)).toInt() + day + b - 1524

        val epoch = 1948439 // Hijri Epoch JDN (15 July 622 AD)
        val diff = jd - epoch
        val cycle = Math.floor(diff / 10631.0).toInt()
        val remains = diff % 10631

        var hijriYear = cycle * 30 + 1
        var daysLeft = remains

        val leapPattern = intArrayOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)

        for (i in 0 until 30) {
            val isLeap = leapPattern.contains(i)
            val yearDays = if (isLeap) 355 else 354
            if (daysLeft >= yearDays) {
                daysLeft -= yearDays
                hijriYear++
            } else {
                break
            }
        }

        var hijriMonth = 1
        val monthDays = intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)
        val isHijriLeap = leapPattern.contains((hijriYear - 1) % 30)

        for (i in 0 until 12) {
            var days = monthDays[i]
            if (i == 11 && isHijriLeap) {
                days = 30
            }
            if (daysLeft >= days) {
                daysLeft -= days
                hijriMonth++
            } else {
                break
            }
        }

        val hijriDay = daysLeft + 1
        return Triple(hijriYear, hijriMonth, hijriDay)
    }

    fun getMonthName(hijriMonth: Int): String {
        return when (hijriMonth) {
            1 -> "Muharram"
            2 -> "Safar"
            3 -> "Rabi'ul Awal"
            4 -> "Rabi'ul Akhir"
            5 -> "Jumadil Awal"
            6 -> "Jumadil Akhir"
            7 -> "Rajab"
            8 -> "Sya'ban"
            9 -> "Ramadhan"
            10 -> "Syawal"
            11 -> "Dzulqa'dah"
            12 -> "Dzulhijjah"
            else -> ""
        }
    }

    fun getFastingInfo(year: Int, month: Int, day: Int, dayOfWeek: Int): FastingInfo {
        val (_, hMonth, hDay) = gregorianToHijri(year, month, day)

        // 1. Ramadan (Wajib Fasting)
        if (hMonth == 9) {
            return FastingInfo(
                name = "Puasa Wajib Ramadhan",
                description = "Fardhu (wajib) bagi setiap umat muslim yang baligh dan berakal untuk berpuasa di bulan Ramadhan.",
                isFasting = true,
                badge = "Puasa Wajib"
            )
        }

        // Haram/Forbidden fasting days (Eids and Tashreeq)
        if (hMonth == 10 && hDay == 1) {
            return FastingInfo(
                name = "Hari Raya Idul Fitri (Haram Puasa)",
                description = "Diharamkan melakukan puasa pada Hari Raya Idul Fitri 1 Syawal.",
                isFasting = false,
                badge = "Diharamkan"
            )
        }
        if (hMonth == 12 && hDay == 10) {
            return FastingInfo(
                name = "Hari Raya Idul Adha (Haram Puasa)",
                description = "Diharamkan melakukan puasa pada Hari Raya Idul Adha 10 Dzulhijjah.",
                isFasting = false,
                badge = "Diharamkan"
            )
        }
        if (hMonth == 12 && (hDay == 11 || hDay == 12 || hDay == 13)) {
            return FastingInfo(
                name = "Hari Tasyrik (Haram Puasa)",
                description = "Hari-hari tasyrik (11, 12, dan 13 Dzulhijjah) adalah waktu makan, minum, dan dzikir kepada Allah, diharamkan berpuasa.",
                isFasting = false,
                badge = "Diharamkan"
            )
        }

        // 2. Arafah Fasting
        if (hMonth == 12 && hDay == 9) {
            return FastingInfo(
                name = "Puasa Sunnah Arafah",
                description = "Puasa sunnah yang dikerjakan pada hari Arafah (9 Dzulhijjah) bagi umat Islam yang tidak melaksanakan ibadah Haji. Menghapus dosa setahun yang lalu dan setahun yang akan datang.",
                isFasting = true
            )
        }

        // 3. Tasu'a & Asyura Fasting
        if (hMonth == 1 && hDay == 9) {
            return FastingInfo(
                name = "Puasa Sunnah Tasu'a",
                description = "Puasa sunnah pada 9 Muharram sebagai penyerta puasa Asyura agar menyelisihi ibadah kaum Yahudi.",
                isFasting = true
            )
        }
        if (hMonth == 1 && hDay == 10) {
            return FastingInfo(
                name = "Puasa Sunnah Asyura",
                description = "Puasa sunnah pada 10 Muharram yang memiliki keutamaan menghapus dosa setahun yang telah lalu.",
                isFasting = true
            )
        }

        // 4. Syawal 6-day Fasting
        if (hMonth == 10 && hDay in 2..7) {
            return FastingInfo(
                name = "Puasa Sunnah Syawal",
                description = "Puasa sunnah enam hari di bulan Syawal setelah hari raya Idul Fitri. Keutamaannya bernilai seperti berpuasa sepanjang tahun.",
                isFasting = true
            )
        }

        // 5. Ayyamul Bidh Fasting
        if (hDay == 13 || hDay == 14 || hDay == 15) {
            return FastingInfo(
                name = "Puasa Sunnah Ayyamul Bidh",
                description = "Puasa sunnah pada pertengahan bulan Hijriah (tanggal 13, 14, dan 15). Keutamaannya seperti berpuasa sepanjang tahun.",
                isFasting = true
            )
        }

        // 6. Monday & Thursday Fasting
        if (dayOfWeek == Calendar.MONDAY) {
            return FastingInfo(
                name = "Puasa Sunnah Senin",
                description = "Puasa sunnah rutin pada hari Senin, hari di mana amal-amal manusia dihadapkan kepada Allah SWT dan hari lahirnya Rasulullah SAW.",
                isFasting = true
            )
        }
        if (dayOfWeek == Calendar.THURSDAY) {
            return FastingInfo(
                name = "Puasa Sunnah Kamis",
                description = "Puasa sunnah rutin pada hari Kamis, hari di mana catatan amalan pekanan manusia diangkat dan dilaporkan kepada Allah SWT.",
                isFasting = true
            )
        }

        return FastingInfo(
            name = "Hari Biasa",
            description = "Tidak ada anjuran puasa sunnah khusus hari ini. Namun, Anda tetap bisa melakukan puasa Daud atau puasa sunnah mutlak lainnya.",
            isFasting = false,
            badge = "Ibadah Harian"
        )
    }
}

package com.quran.labs.androidquran.adhan

/**
 * Daftar kota-kota di Indonesia dengan koordinat dan timezone masing-masing.
 * Timezone Indonesia: WIB = UTC+7, WITA = UTC+8, WIT = UTC+9
 */
data class CityData(
    val name: String,
    val province: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneOffset: Double // dalam jam, misalnya 7.0 untuk WIB
) {
    val timezone: String get() = when (timezoneOffset) {
        7.0 -> "WIB"
        8.0 -> "WITA"
        9.0 -> "WIT"
        else -> "UTC+${timezoneOffset.toInt()}"
    }
}

object IndonesiaCities {
    val cities: List<CityData> = listOf(
        // === WIB (UTC+7) ===
        CityData("Banda Aceh",     "Aceh",            5.5477,   95.3239, 7.0),
        CityData("Medan",          "Sumatera Utara",   3.5952,   98.6722, 7.0),
        CityData("Padang",         "Sumatera Barat",  -0.9471,  100.4172, 7.0),
        CityData("Pekanbaru",      "Riau",             0.5071,  101.4478, 7.0),
        CityData("Batam",          "Kepri",            1.0456,  104.0305, 7.0),
        CityData("Jambi",          "Jambi",           -1.6101,  103.6131, 7.0),
        CityData("Palembang",      "Sumatera Selatan",-2.9761,  104.7754, 7.0),
        CityData("Bengkulu",       "Bengkulu",        -3.7928,  102.2608, 7.0),
        CityData("Bandar Lampung", "Lampung",         -5.4295,  105.2610, 7.0),
        CityData("Jakarta",        "DKI Jakarta",     -6.2088,  106.8456, 7.0),
        CityData("Bogor",          "Jawa Barat",      -6.5971,  106.8060, 7.0),
        CityData("Bandung",        "Jawa Barat",      -6.9175,  107.6191, 7.0),
        CityData("Serang",         "Banten",          -6.1201,  106.1503, 7.0),
        CityData("Semarang",       "Jawa Tengah",     -6.9932,  110.4203, 7.0),
        CityData("Yogyakarta",     "DIY",             -7.7956,  110.3695, 7.0),
        CityData("Surakarta",      "Jawa Tengah",     -7.5755,  110.8243, 7.0),
        CityData("Surabaya",       "Jawa Timur",      -7.2575,  112.7521, 7.0),
        CityData("Malang",         "Jawa Timur",      -7.9667,  112.6326, 7.0),
        CityData("Pontianak",      "Kalimantan Barat", -0.0263,  109.3425, 7.0),
        CityData("Pangkalpinang",  "Bangka Belitung", -2.1316,  106.1164, 7.0),

        // === WITA (UTC+8) ===
        CityData("Denpasar",       "Bali",            -8.6705,  115.2126, 8.0),
        CityData("Mataram",        "NTB",             -8.5833,  116.1167, 8.0),
        CityData("Kupang",         "NTT",             -10.1772, 123.6070, 8.0),
        CityData("Makassar",       "Sulawesi Selatan",-5.1477,  119.4327, 8.0),
        CityData("Kendari",        "Sulawesi Tenggara",-3.9985, 122.5129, 8.0),
        CityData("Palu",           "Sulawesi Tengah", -0.8917,  119.8707, 8.0),
        CityData("Gorontalo",      "Gorontalo",        0.5387,  123.0595, 8.0),
        CityData("Manado",         "Sulawesi Utara",   1.4748,  124.8421, 8.0),
        CityData("Balikpapan",     "Kalimantan Timur",-1.2675,  116.8283, 8.0),
        CityData("Samarinda",      "Kalimantan Timur",-0.4948,  117.1436, 8.0),
        CityData("Banjarmasin",    "Kalimantan Selatan",-3.3194, 114.5908, 8.0),
        CityData("Palangkaraya",   "Kalimantan Tengah",-2.2161, 113.9135, 8.0),

        // === WIT (UTC+9) ===
        CityData("Ternate",        "Maluku Utara",     0.7897,  127.3842, 9.0),
        CityData("Ambon",          "Maluku",          -3.6508,  128.1908, 9.0),
        CityData("Sorong",         "Papua Barat",     -0.8792,  131.2546, 9.0),
        CityData("Manokwari",      "Papua Barat",     -0.8615,  134.0622, 9.0),
        CityData("Jayapura",       "Papua",           -2.5916,  140.6690, 9.0),
    )

    fun findByName(name: String): CityData? = cities.find { it.name == name }
}

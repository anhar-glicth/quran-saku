package com.quran.labs.androidquran.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DoaHarianItem(
    @Json(name = "id")          val id: Int,
    @Json(name = "category")    val category: String,  // pagi, sore, harian, shalat, quran
    @Json(name = "title")       val title: String,
    @Json(name = "arabic")      val arabic: String,
    @Json(name = "latin")       val latin: String,
    @Json(name = "translation") val translation: String,
    @Json(name = "source")      val source: String
)

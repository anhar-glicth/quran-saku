package com.quran.labs.androidquran.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DonationItem(
    @Json(name = "id")               val id: Int = 0,
    @Json(name = "user_name")        val userName: String = "",
    @Json(name = "amount")           val amount: Double = 0.0,
    @Json(name = "formatted_amount") val formattedAmount: String = "",
    @Json(name = "campaign")         val campaign: String = "",
    @Json(name = "time_ago")         val timeAgo: String = ""
)

@JsonClass(generateAdapter = true)
data class DonationTickerResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data")    val data: List<DonationItem>? = null
)

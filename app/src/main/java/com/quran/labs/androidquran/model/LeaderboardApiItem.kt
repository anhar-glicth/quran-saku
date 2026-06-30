package com.quran.labs.androidquran.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LeaderboardApiItem(
    @Json(name = "rank")          val rank: Int,
    @Json(name = "user_id")       val userId: Int,
    @Json(name = "user_name")     val userName: String,
    @Json(name = "initials")      val initials: String,
    @Json(name = "total_minutes") val totalMinutes: Int,
    @Json(name = "total_pages")   val totalPages: Int,
    @Json(name = "active_days")   val activeDays: Int,
    @Json(name = "display_score") val displayScore: String
)

@JsonClass(generateAdapter = true)
data class LeaderboardResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "period")  val period: String,
    @Json(name = "from")    val from: String,
    @Json(name = "to")      val to: String,
    @Json(name = "data")    val data: List<LeaderboardApiItem>?,
    @Json(name = "my_rank") val myRank: LeaderboardApiItem?
)


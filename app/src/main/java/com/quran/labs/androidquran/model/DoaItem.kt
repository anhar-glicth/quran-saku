package com.quran.labs.androidquran.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DoaItem(
    @Json(name = "id")           val id: Int,
    @Json(name = "user_id")      val userId: Int,
    @Json(name = "user_name")    val userName: String,
    @Json(name = "arabic_text")  val arabicText: String?,
    @Json(name = "latin_text")   val latinText: String,
    @Json(name = "like_count")   val likeCount: Int,
    @Json(name = "aamiin_count") val aamiinCount: Int,
    @Json(name = "is_liked")     val isLiked: Boolean,
    @Json(name = "is_aaminned")  val isAaminned: Boolean,
    @Json(name = "created_at")   val createdAt: String
)

@JsonClass(generateAdapter = true)
data class DoaListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data")    val data: List<DoaItem>?
)

@JsonClass(generateAdapter = true)
data class DoaPostResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data")    val data: DoaItem?
)

@JsonClass(generateAdapter = true)
data class ReactResponse(
    @Json(name = "success")       val success: Boolean,
    @Json(name = "is_active")     val isActive: Boolean,
    @Json(name = "like_count")    val likeCount: Int,
    @Json(name = "aamiin_count")  val aamiinCount: Int
)

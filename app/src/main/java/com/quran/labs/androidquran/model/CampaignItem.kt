package com.quran.labs.androidquran.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CampaignItem(
    @Json(name = "id")          val id: Int,
    @Json(name = "title")       val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "image_url")   val imageUrl: String,
    @Json(name = "donate_url")  val donateUrl: String,
    @Json(name = "is_active")   val isActive: Boolean
)

@JsonClass(generateAdapter = true)
data class CampaignListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data")    val data: List<CampaignItem>?
)

@JsonClass(generateAdapter = true)
data class CampaignSaveResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data")    val data: CampaignItem?
)

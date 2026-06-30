package com.quran.labs.androidquran.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PartnerApiItem(
    @Json(name = "id")           val id: Int,
    @Json(name = "category_id")  val categoryId: String,
    @Json(name = "logo_text")    val logoText: String,
    @Json(name = "name")         val name: String,
    @Json(name = "description")  val description: String,
    @Json(name = "bg_color")     val bgColor: String,
    @Json(name = "text_color")   val textColor: String
)

@JsonClass(generateAdapter = true)
data class PartnerListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data")    val data: List<PartnerApiItem>?
)

@JsonClass(generateAdapter = true)
data class PartnerAddResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data")    val data: PartnerApiItem?
)

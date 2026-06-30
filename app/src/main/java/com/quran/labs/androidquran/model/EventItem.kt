package com.quran.labs.androidquran.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EventItem(
    @Json(name = "id")          val id: Int,
    @Json(name = "title")       val title: String,
    @Json(name = "category")    val category: String,
    @Json(name = "description") val description: String,
    @Json(name = "event_date")  val eventDate: String, // YYYY-MM-DD
    @Json(name = "time_range")  val timeRange: String,
    @Json(name = "speaker")     val speaker: String,
    @Json(name = "location")    val location: String,
    @Json(name = "is_featured") val isFeatured: Boolean
)

@JsonClass(generateAdapter = true)
data class EventListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data")    val data: List<EventItem>?
)

@JsonClass(generateAdapter = true)
data class EventSaveResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data")    val data: EventItem?
)

@JsonClass(generateAdapter = true)
data class SimpleResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?
)

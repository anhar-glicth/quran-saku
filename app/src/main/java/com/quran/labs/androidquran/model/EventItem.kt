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
    @Json(name = "is_featured") val isFeatured: Boolean,
    @Json(name = "image_url")   val imageUrl: String? = null
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

@JsonClass(generateAdapter = true)
data class EventRegistration(
    @Json(name = "id")            val id: Int,
    @Json(name = "event_id")      val eventId: Int,
    @Json(name = "user_id")       val userId: Int,
    @Json(name = "name")          val name: String,
    @Json(name = "email")         val email: String,
    @Json(name = "phone")         val phone: String,
    @Json(name = "notes")         val notes: String?,
    @Json(name = "registered_at") val registeredAt: String
)

@JsonClass(generateAdapter = true)
data class RegistrationStatusResponse(
    @Json(name = "success")       val success: Boolean,
    @Json(name = "is_registered") val isRegistered: Boolean,
    @Json(name = "message")       val message: String? = null
)

@JsonClass(generateAdapter = true)
data class EventRegistrationsResponse(
    @Json(name = "success")       val success: Boolean,
    @Json(name = "total")         val total: Int,
    @Json(name = "data")          val data: List<EventRegistration>?
)

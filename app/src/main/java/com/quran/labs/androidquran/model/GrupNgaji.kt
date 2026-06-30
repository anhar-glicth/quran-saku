package com.quran.labs.androidquran.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GrupNgaji(
    @Json(name = "id")               val id: Int,
    @Json(name = "group_code")       val groupCode: String,
    @Json(name = "name")             val name: String,
    @Json(name = "description")      val description: String,
    @Json(name = "photo_url")        val photoUrl: String?,
    @Json(name = "admin_user_id")    val creatorId: Int, // Maps admin_user_id to creatorId
    @Json(name = "khatam_target")    val targetKhatam: Int, // Maps khatam_target to targetKhatam
    @Json(name = "duration_days")    val durationDays: Int,
    @Json(name = "current_page")     val currentPage: Int,
    @Json(name = "last_reader_id")   val lastReaderId: Int?,
    @Json(name = "last_reader_name") val lastReaderName: String?,
    @Json(name = "member_status")    val memberStatus: String = "active"
)

@JsonClass(generateAdapter = true)
data class GrupMember(
    @Json(name = "user_id")        val id: Int, // Maps user_id to id
    @Json(name = "user_name")      val name: String, // Maps user_name to name
    @Json(name = "role")           val role: String = "member",
    @Json(name = "last_page_read") val lastPageRead: Int = 1,
    @Json(name = "joined_at")      val joinedAt: String
)

@JsonClass(generateAdapter = true)
data class ReadingRelay(
    @Json(name = "id")           val id: Int = 0,
    @Json(name = "user_id")      val userId: Int,
    @Json(name = "user_name")    val userName: String,
    @Json(name = "surah_number") val surahNumber: Int = 1,
    @Json(name = "surah_name")   val surahName: String? = null,
    @Json(name = "ayah_number")  val ayahNumber: Int = 1,
    @Json(name = "page_number")  val pageNumber: Int,
    @Json(name = "read_at")      val readAt: String
)

@JsonClass(generateAdapter = true)
data class MyGroupResponse(
    @Json(name = "success")   val success: Boolean,
    @Json(name = "has_group") val hasGroup: Boolean,
    @Json(name = "group")     val group: GrupNgaji?,
    @Json(name = "members")   val members: List<GrupMember>?,
    @Json(name = "relay")     val relay: List<ReadingRelay>?,
    @Json(name = "progressPercent") val progressPercent: Int?
)

@JsonClass(generateAdapter = true)
data class PendingMembersResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data")    val data: List<GrupMember>?
)

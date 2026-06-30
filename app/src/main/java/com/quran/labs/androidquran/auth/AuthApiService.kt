package com.quran.labs.androidquran.auth

import com.quran.labs.androidquran.model.DoaListResponse
import com.quran.labs.androidquran.model.DoaPostResponse
import com.quran.labs.androidquran.model.LeaderboardResponse
import com.quran.labs.androidquran.model.ReactResponse
import com.quran.labs.androidquran.model.PartnerListResponse
import com.quran.labs.androidquran.model.PartnerAddResponse
import com.quran.labs.androidquran.model.EventListResponse
import com.quran.labs.androidquran.model.EventSaveResponse
import com.quran.labs.androidquran.model.SimpleResponse
import com.quran.labs.androidquran.model.MyGroupResponse
import com.quran.labs.androidquran.model.PendingMembersResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {

    @FormUrlEncoded
    @POST("auth/login_api.php")
    suspend fun login(
        @Field("email")    email: String,
        @Field("password") password: String
    ): Response<AuthResponse>

    @FormUrlEncoded
    @POST("auth/register_api.php")
    suspend fun register(
        @Field("name")             name: String,
        @Field("email")            email: String,
        @Field("password")         password: String,
        @Field("password_confirm") passwordConfirm: String
    ): Response<AuthResponse>

    // ─── Komunitas: Titip Doa ────────────────────────────────
    @GET("auth/community_prayers_api.php")
    suspend fun getDoas(
        @Query("action")  action: String = "list",
        @Query("user_id") userId: Int = 0,
        @Query("limit")   limit: Int = 30
    ): Response<DoaListResponse>

    @FormUrlEncoded
    @POST("auth/community_prayers_api.php")
    suspend fun postDoa(
        @Field("action")      action: String = "post_prayer",
        @Field("user_id")     userId: Int,
        @Field("latin_text")  latinText: String,
        @Field("arabic_text") arabicText: String = ""
    ): Response<DoaPostResponse>

    @FormUrlEncoded
    @POST("auth/community_prayers_api.php")
    suspend fun reactToDoa(
        @Field("action")        action: String = "react",
        @Field("user_id")       userId: Int,
        @Field("prayer_id")     prayerId: Int,
        @Field("reaction_type") reactionType: String
    ): Response<ReactResponse>

    // ─── Komunitas: Leaderboard ──────────────────────────────
    @GET("auth/leaderboard_api.php")
    suspend fun getLeaderboard(
        @Query("period") period: String = "weekly",
        @Query("limit")  limit: Int = 10
    ): Response<LeaderboardResponse>

    // ─── Pejuang Kebaikan: Partners ──────────────────────────
    @GET("auth/partners_api.php")
    suspend fun getPartners(
        @Query("action")      action: String = "list",
        @Query("category_id") categoryId: String = ""
    ): Response<PartnerListResponse>

    @FormUrlEncoded
    @POST("auth/partners_api.php")
    suspend fun addPartner(
        @Field("action")      action: String = "add",
        @Field("user_id")     userId: Int,
        @Field("category_id") categoryId: String,
        @Field("logo_text")   logoText: String,
        @Field("name")        name: String,
        @Field("description") description: String,
        @Field("bg_color")    bgColor: String = "#E0F2F1",
        @Field("text_color")  textColor: String = "#004D40"
    ): Response<PartnerAddResponse>

    // ─── Kalender & Kegiatan: Events ─────────────────────────
    @GET("auth/events_api.php")
    suspend fun getEvents(
        @Query("action")        action: String = "list",
        @Query("featured_only") featuredOnly: Int = 0
    ): Response<EventListResponse>

    @GET("auth/events_api.php")
    suspend fun getMonthlyEvents(
        @Query("action") action: String = "monthly",
        @Query("year")   year: Int,
        @Query("month")  month: Int
    ): Response<EventListResponse>

    @FormUrlEncoded
    @POST("auth/events_api.php")
    suspend fun saveEvent(
        @Field("action")      action: String = "save",
        @Field("user_id")     userId: Int,
        @Field("id")          id: Int = 0,
        @Field("title")       title: String,
        @Field("category")    category: String,
        @Field("description") description: String,
        @Field("event_date")  eventDate: String, // YYYY-MM-DD
        @Field("time_range")  timeRange: String = "09:00 - 11:30 WIB",
        @Field("speaker")     speaker: String,
        @Field("location")    location: String = "Online Zoom",
        @Field("is_featured") isFeatured: Int = 0
    ): Response<EventSaveResponse>

    @FormUrlEncoded
    @POST("auth/events_api.php")
    suspend fun deleteEvent(
        @Field("action")  action: String = "delete",
        @Field("user_id") userId: Int,
        @Field("id")      id: Int
    ): Response<SimpleResponse>

    // ─── Grup Ngaji ──────────────────────────────────────────
    @GET("auth/group_api.php")
    suspend fun getMyGroup(
        @Query("action")  action: String = "my_group",
        @Query("user_id") userId: Int
    ): Response<MyGroupResponse>

    @GET("auth/group_api.php")
    suspend fun getGroupDetail(
        @Query("action")   action: String = "my_group",
        @Query("user_id")  userId: Int = 0, // Fallback parameter
        @Query("group_id") groupId: Int
    ): Response<MyGroupResponse>

    @FormUrlEncoded
    @POST("auth/group_api.php")
    suspend fun createGroup(
        @Field("action")         action: String = "create",
        @Field("user_id")        userId: Int,
        @Field("name")           name: String,
        @Field("description")    description: String,
        @Field("khatam_target")  khatamTarget: Int = 1,
        @Field("duration_days")  durationDays: Int = 30,
        @Field("photo_base64")   photoBase64: String = ""
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("auth/group_api.php")
    suspend fun joinGroup(
        @Field("action")     action: String = "join",
        @Field("user_id")    userId: Int,
        @Field("group_code") groupCode: String
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("auth/group_api.php")
    suspend fun updateGroupPage(
        @Field("action")      action: String = "update_page",
        @Field("user_id")     userId: Int,
        @Field("page_number") pageNumber: Int
    ): Response<SimpleResponse>

    @GET("auth/group_api.php")
    suspend fun getPendingMembers(
        @Query("action")   action: String = "pending_requests",
        @Query("user_id")  userId: Int, // Admin's user ID check
        @Query("group_id") groupId: Int
    ): Response<PendingMembersResponse>

    @FormUrlEncoded
    @POST("auth/group_api.php")
    suspend fun respondJoinRequest(
        @Field("action")  action: String = "approve_member", // Will use approve_member or reject_member action from parameter
        @Field("admin_id") adminId: Int,
        @Field("user_id")  targetUserId: Int,
        @Field("group_id") groupId: Int,
        @Field("action_type") responseAction: String // approve or reject
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("auth/group_api.php")
    suspend fun updateGroupName(
        @Field("action")       action: String = "update_group",
        @Field("admin_id")     userId: Int,
        @Field("group_id")     groupId: Int,
        @Field("name")         name: String,
        @Field("photo_base64")  photoUrl: String = ""
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("auth/group_api.php")
    suspend fun approveMember(
        @Field("action")  action: String = "approve_member",
        @Field("admin_id") adminId: Int,
        @Field("user_id")  userId: Int,
        @Field("group_id") groupId: Int
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("auth/group_api.php")
    suspend fun rejectMember(
        @Field("action")  action: String = "reject_member",
        @Field("admin_id") adminId: Int,
        @Field("user_id")  userId: Int,
        @Field("group_id") groupId: Int
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("auth/group_api.php")
    suspend fun updateGroup(
        @Field("action")       action: String = "update_group",
        @Field("admin_id")     adminId: Int,
        @Field("group_id")     groupId: Int,
        @Field("name")         name: String,
        @Field("description")  description: String,
        @Field("photo_base64")  photoBase64: String = ""
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("auth/update_profile_api.php")
    suspend fun updateProfile(
        @Field("user_id") userId: Int,
        @Field("name")    name: String,
        @Field("email")   email: String
    ): Response<SimpleResponse>
}

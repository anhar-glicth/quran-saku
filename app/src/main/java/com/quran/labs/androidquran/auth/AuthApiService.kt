package com.quran.labs.androidquran.auth

import com.quran.labs.androidquran.model.DoaListResponse
import com.quran.labs.androidquran.model.DoaPostResponse
import com.quran.labs.androidquran.model.LeaderboardResponse
import com.quran.labs.androidquran.model.ReactResponse
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
}

package com.quran.labs.androidquran.auth

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

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
}

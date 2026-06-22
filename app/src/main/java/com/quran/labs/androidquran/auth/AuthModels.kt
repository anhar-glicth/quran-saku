package com.quran.labs.androidquran.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─── Request Models ──────────────────────────
@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @Json(name = "password_confirm") val passwordConfirm: String
)

// ─── Response Models ─────────────────────────
@JsonClass(generateAdapter = true)
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: UserData? = null
)

@JsonClass(generateAdapter = true)
data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val avatar: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

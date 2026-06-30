package com.quran.labs.androidquran.auth

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit

object AuthClient {

    // ── GANTI dengan IP komputer kamu di jaringan WiFi ──
    // Jika tes di emulator gunakan: http://10.0.2.2/quran_android/web/
    // Jika tes di HP nyata gunakan: http://192.168.x.x/quran_android/web/
    const val BASE_URL = "http://172.21.93.124/quran_android/web/"

    private val moshi: Moshi = Moshi.Builder()
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApiService::class.java)
    }
}

// ─── Session Manager (SharedPreferences) ─────────────────
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quran_saku_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN     = "is_logged_in"
        private const val KEY_USER_ID           = "user_id"
        private const val KEY_USER_NAME         = "user_name"
        private const val KEY_USER_EMAIL        = "user_email"
        private const val KEY_USER_ROLE         = "user_role"
        private const val KEY_ONBOARDING_DONE   = "onboarding_done"
    }

    fun saveUserSession(user: UserData) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putInt(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_ROLE, user.role)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "user") ?: "user"

    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}

package com.quran.labs.androidquran.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.quran.labs.androidquran.QuranDataActivity
import com.quran.labs.androidquran.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        // Cek jika sudah login
        if (session.isLoggedIn()) {
            navigateAfterLogin()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        // Real-time validation
        binding.etEmail.doOnTextChanged { _, _, _, _ -> clearError() }
        binding.etPassword.doOnTextChanged { _, _, _, _ -> clearError() }

        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Validasi client-side
        if (email.isEmpty()) { binding.tilEmail.error = "Email wajib diisi"; return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Format email tidak valid"; return
        }
        if (password.isEmpty()) { binding.tilPassword.error = "Password wajib diisi"; return }
        if (password.length < 6) { binding.tilPassword.error = "Password minimal 6 karakter"; return }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.login(email, password)
                val body     = response.body()

                if (response.isSuccessful && body != null) {
                    if (body.success && body.user != null) {
                        session.saveUserSession(body.user)
                        navigateAfterLogin()
                    } else {
                        showError(body.message)
                    }
                } else {
                    showError("Gagal terhubung ke server (${response.code()})")
                }
            } catch (e: IOException) {
                showError("Tidak ada koneksi internet. Periksa jaringan Anda.")
            } catch (e: HttpException) {
                showError("Terjadi kesalahan server: ${e.message()}")
            } catch (e: Exception) {
                showError("Terjadi kesalahan: ${e.localizedMessage}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateAfterLogin() {
        if (!session.isOnboardingDone()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        } else {
            startActivity(Intent(this, QuranDataActivity::class.java))
        }
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled      = !loading
        binding.progressBar.visibility  = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.text           = if (loading) "" else "Sign in"
    }

    private fun clearError() {
        binding.tilEmail.error    = null
        binding.tilPassword.error = null
        binding.tvError.visibility = View.GONE
    }

    private fun showError(msg: String) {
        binding.tvError.text       = msg
        binding.tvError.visibility = View.VISIBLE
    }
}

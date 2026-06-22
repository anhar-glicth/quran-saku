package com.quran.labs.androidquran.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.quran.labs.androidquran.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        setupListeners()
    }

    private fun setupListeners() {
        binding.etName.doOnTextChanged     { _, _, _, _ -> binding.tilName.error = null }
        binding.etEmail.doOnTextChanged    { _, _, _, _ -> binding.tilEmail.error = null }
        binding.etPassword.doOnTextChanged { _, _, _, _ -> binding.tilPassword.error = null }
        binding.etConfirm.doOnTextChanged  { _, _, _, _ -> binding.tilConfirm.error = null }

        binding.btnRegister.setOnClickListener { attemptRegister() }

        binding.tvGoToLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name     = binding.etName.text.toString().trim()
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm  = binding.etConfirm.text.toString()

        // Validasi
        if (name.isEmpty())  { binding.tilName.error = "Nama wajib diisi"; return }
        if (name.length < 3) { binding.tilName.error = "Nama minimal 3 karakter"; return }
        if (email.isEmpty())  { binding.tilEmail.error = "Email wajib diisi"; return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Format email tidak valid"; return
        }
        if (password.isEmpty())  { binding.tilPassword.error = "Password wajib diisi"; return }
        if (password.length < 8) { binding.tilPassword.error = "Password minimal 8 karakter"; return }
        if (!password.any { it.isUpperCase() }) {
            binding.tilPassword.error = "Password harus ada huruf kapital"; return
        }
        if (!password.any { it.isDigit() }) {
            binding.tilPassword.error = "Password harus ada angka"; return
        }
        if (password != confirm) {
            binding.tilConfirm.error = "Konfirmasi password tidak cocok"; return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.register(name, email, password, confirm)
                val body     = response.body()

                if (response.isSuccessful && body != null) {
                    if (body.success && body.user != null) {
                        session.saveUserSession(body.user)
                        // Setelah register, langsung ke Onboarding
                        startActivity(Intent(this@RegisterActivity, OnboardingActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    } else {
                        binding.tvError.text = body.message
                        binding.tvError.visibility = View.VISIBLE
                    }
                } else {
                    binding.tvError.text = "Gagal terhubung ke server"
                    binding.tvError.visibility = View.VISIBLE
                }
            } catch (e: IOException) {
                binding.tvError.text = "Tidak ada koneksi internet."
                binding.tvError.visibility = View.VISIBLE
            } catch (e: HttpException) {
                binding.tvError.text = "Server error: ${e.message()}"
                binding.tvError.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.tvError.text = "Kesalahan: ${e.localizedMessage}"
                binding.tvError.visibility = View.VISIBLE
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled    = !loading
        binding.progressBar.visibility   = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.text         = if (loading) "" else "Register"
    }
}

package com.quran.labs.androidquran

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.math.ceil

class KhatamActivity : AppCompatActivity() {

    private lateinit var txtKhatamPercentage: TextView
    private lateinit var progressKhatam: LinearProgressIndicator
    private lateinit var txtKhatamPages: TextView
    private lateinit var txtKhatamTarget: TextView
    private lateinit var txtKhatamSpeed: TextView
    private lateinit var edtCurrentPage: EditText
    private lateinit var edtTargetDays: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_khatam)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        txtKhatamPercentage = findViewById(R.id.txt_khatam_percentage)
        progressKhatam = findViewById(R.id.progress_khatam)
        txtKhatamPages = findViewById(R.id.txt_khatam_pages)
        txtKhatamTarget = findViewById(R.id.txt_khatam_target)
        txtKhatamSpeed = findViewById(R.id.txt_khatam_speed)
        edtCurrentPage = findViewById(R.id.edt_current_page)
        edtTargetDays = findViewById(R.id.edt_target_days)

        loadProgress()

        findViewById<View>(R.id.btn_save_khatam)?.setOnClickListener {
            saveProgress()
        }
    }

    private fun loadProgress() {
        val sharedPref = getSharedPreferences("khatam_prefs", Context.MODE_PRIVATE)
        val currentPage = sharedPref.getInt("current_page", 0)
        val targetDays = sharedPref.getInt("target_days", 30)

        updateUI(currentPage, targetDays)

        edtCurrentPage.setText(currentPage.toString())
        edtTargetDays.setText(targetDays.toString())
    }

    private fun saveProgress() {
        val pageStr = edtCurrentPage.text.toString().trim()
        val daysStr = edtTargetDays.text.toString().trim()

        if (pageStr.isEmpty() || daysStr.isEmpty()) {
            Toast.makeText(this, "Mohon isi semua data", Toast.LENGTH_SHORT).show()
            return
        }

        val page = pageStr.toIntOrNull() ?: 0
        val days = daysStr.toIntOrNull() ?: 30

        if (page < 0 || page > 604) {
            Toast.makeText(this, "Halaman harus berada di antara 0 - 604", Toast.LENGTH_SHORT).show()
            return
        }

        if (days <= 0) {
            Toast.makeText(this, "Target hari harus lebih dari 0", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("khatam_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("current_page", page)
            putInt("target_days", days)
            apply()
        }

        updateUI(page, days)
        Toast.makeText(this, "Kemajuan khatam berhasil disimpan!", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(currentPage: Int, targetDays: Int) {
        val totalPages = 604
        val percentage = (currentPage * 100) / totalPages

        txtKhatamPercentage.text = "$percentage%"
        progressKhatam.progress = percentage
        txtKhatamPages.text = "Halaman: $currentPage / $totalPages"
        txtKhatamTarget.text = "Target: $targetDays Hari"

        val remainingPages = totalPages - currentPage
        if (remainingPages <= 0) {
            txtKhatamSpeed.text = "Selamat! Anda telah mengkhatamkan Al-Quran! 🎉"
        } else {
            val speed = ceil(remainingPages.toDouble() / targetDays).toInt()
            txtKhatamSpeed.text = "Untuk mencapai target, baca sekitar $speed halaman per hari."
        }
    }
}

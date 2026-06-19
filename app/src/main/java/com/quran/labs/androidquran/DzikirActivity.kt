package com.quran.labs.androidquran

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class DzikirActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dzikir)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        setupCategoryButton(R.id.btn_dzikir_pagi, "pagi", "Dzikir Pagi")
        setupCategoryButton(R.id.btn_dzikir_sore, "sore", "Dzikir Sore")
        setupCategoryButton(R.id.btn_dzikir_shalat, "shalat", "Dzikir Setelah Shalat")
        setupCategoryButton(R.id.btn_doa_quran, "quran", "Doa di Al-Qur'an")
        setupCategoryButton(R.id.btn_hadits_arbain, "hadits", "Hadits Arba'in Nawawi")
    }

    private fun setupCategoryButton(resId: Int, categoryKey: String, title: String) {
        findViewById<View>(resId)?.setOnClickListener {
            val intent = Intent(this, DzikirReadActivity::class.java).apply {
                putExtra("category", categoryKey)
                putExtra("title", title)
            }
            startActivity(intent)
        }
    }
}

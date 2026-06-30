package com.quran.labs.androidquran

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import android.content.Intent
import android.view.View

class PejuangQuranActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_pejuang_quran)

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    toolbar.title = "Pejuang Quran"
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener { finish() }

    setupCategoryButton(R.id.btn_mitra_utama, "mitra_utama", "Mitra Utama")
    setupCategoryButton(R.id.btn_pendukung_resmi, "pendukung_resmi", "Pendukung Resmi")
    setupCategoryButton(R.id.btn_mitra_distribusi, "mitra_distribusi", "Mitra Distribusi")
    setupCategoryButton(R.id.btn_mitra_edukasi, "mitra_edukasi", "Mitra Edukasi")
  }

  private fun setupCategoryButton(resId: Int, categoryId: String, categoryName: String) {
    findViewById<View>(resId)?.setOnClickListener {
      val intent = Intent(this, PejuangPartnerDetailActivity::class.java).apply {
        putExtra("category_id", categoryId)
        putExtra("category_name", categoryName)
      }
      startActivity(intent)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}

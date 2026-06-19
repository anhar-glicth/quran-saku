package com.quran.labs.androidquran

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class GrupNgajiActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_grup_ngaji)

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    toolbar.title = "Grup Ngaji"
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    findViewById<Button>(R.id.btn_buat_grup)?.setOnClickListener {
      Toast.makeText(this, "Fitur membuat grup baru segera hadir!", Toast.LENGTH_SHORT).show()
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

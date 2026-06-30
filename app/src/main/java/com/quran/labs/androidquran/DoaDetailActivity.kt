package com.quran.labs.androidquran

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray
import java.io.InputStream

class DoaDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doa_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val doaId = intent.getStringExtra("doa_id") ?: ""
        val doaTitle = intent.getStringExtra("doa_title") ?: "Detail Doa"
        
        supportActionBar?.title = "Detail Doa"
        findViewById<TextView>(R.id.tv_detail_title).text = doaTitle

        loadDoaDetails(doaId)
    }

    private fun loadDoaDetails(doaId: String) {
        try {
            // Read prayerData.json
            val inputStream: InputStream = assets.open("json/prayer/prayerData.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            var arabicText = ""
            var translationText = ""
            var sourceText = "Sumber tidak diketahui"
            var notesText = ""

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val prayerId = obj.optString("prayer_id")
                
                // Compare with requested ID
                if (prayerId == doaId) {
                    // Parse text list (Arabic, Bahasa, English)
                    val texts = obj.optJSONArray("text")
                    if (texts != null) {
                        for (j in 0 until texts.length()) {
                            val tObj = texts.getJSONObject(j)
                            val lang = tObj.optString("language")
                            val txtVal = tObj.optString("text")
                            if (lang == "arabic") {
                                arabicText = txtVal
                            } else if (lang == "bahasa") {
                                translationText = txtVal
                            }
                        }
                    }

                    // Parse source
                    val sources = obj.optJSONArray("source")
                    if (sources != null && sources.length() > 0) {
                        val sObj = sources.getJSONObject(0)
                        sourceText = sObj.optString("text")
                    }

                    // Parse notes (instructions)
                    val notes = obj.optJSONArray("notes")
                    if (notes != null && notes.length() > 0) {
                        val nObj = notes.getJSONObject(0)
                        notesText = nObj.optString("text")
                    }
                    break
                }
            }

            findViewById<TextView>(R.id.tv_detail_arabic).text = arabicText
            findViewById<TextView>(R.id.tv_detail_translation).text = translationText
            findViewById<TextView>(R.id.tv_detail_source).text = sourceText

            val layoutNotes = findViewById<View>(R.id.layout_notes)
            val tvNotes = findViewById<TextView>(R.id.tv_detail_notes)
            if (notesText.isNotEmpty()) {
                layoutNotes.visibility = View.VISIBLE
                tvNotes.text = notesText
            } else {
                layoutNotes.visibility = View.GONE
            }

        } catch (e: Exception) {
            findViewById<TextView>(R.id.tv_detail_arabic).text = "Error loading details"
            findViewById<TextView>(R.id.tv_detail_translation).text = e.message
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

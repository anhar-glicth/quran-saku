package com.quran.labs.androidquran

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.LoginActivity
import com.quran.labs.androidquran.auth.SessionManager
import com.quran.labs.androidquran.model.EventItem
import com.quran.labs.androidquran.ui.fragment.EventFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID    = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_EVENT_DESC  = "event_desc"
        const val EXTRA_EVENT_DATE  = "event_date"
        const val EXTRA_EVENT_TIME  = "event_time"
        const val EXTRA_EVENT_SPEAKER   = "event_speaker"
        const val EXTRA_EVENT_LOCATION  = "event_location"
        const val EXTRA_EVENT_CATEGORY  = "event_category"
        const val EXTRA_EVENT_IMAGE_URL = "event_image_url"
        const val EXTRA_EVENT_LINK_URL  = "event_link_url"
        const val EXTRA_EVENT_IS_FEATURED = "event_is_featured"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_event_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val sessionManager = SessionManager(this)
        val isAdmin = sessionManager.isLoggedIn() && sessionManager.getUserRole() == "admin"

        // Retrieve event data from intent
        val eventId       = intent.getIntExtra(EXTRA_EVENT_ID, 0)
        val title         = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: ""
        val description   = intent.getStringExtra(EXTRA_EVENT_DESC) ?: ""
        val date          = intent.getStringExtra(EXTRA_EVENT_DATE) ?: ""
        val time          = intent.getStringExtra(EXTRA_EVENT_TIME) ?: ""
        val speaker       = intent.getStringExtra(EXTRA_EVENT_SPEAKER) ?: ""
        val location      = intent.getStringExtra(EXTRA_EVENT_LOCATION) ?: ""
        val category      = intent.getStringExtra(EXTRA_EVENT_CATEGORY) ?: ""
        val imageUrl      = intent.getStringExtra(EXTRA_EVENT_IMAGE_URL) ?: ""
        val linkUrl       = intent.getStringExtra(EXTRA_EVENT_LINK_URL) ?: ""
        val isFeatured    = intent.getBooleanExtra(EXTRA_EVENT_IS_FEATURED, false)

        // Bind views
        findViewById<TextView>(R.id.tv_detail_event_title).text = title
        findViewById<TextView>(R.id.tv_detail_description).text = description
        findViewById<TextView>(R.id.tv_detail_speaker).text = speaker
        findViewById<TextView>(R.id.tv_detail_location).text = location
        findViewById<TextView>(R.id.tv_detail_category_badge).text = category.uppercase()
        findViewById<TextView>(R.id.tv_detail_datetime).text =
            "${formatDateIndo(date)} • $time"

        val btnOpenLink = findViewById<MaterialButton>(R.id.btn_open_event_link)
        if (linkUrl.isNotEmpty()) {
            btnOpenLink.visibility = View.VISIBLE
            val lowerLink = linkUrl.lowercase()
            when {
                lowerLink.contains("zoom") || lowerLink.contains("meet") || lowerLink.contains("teams") -> {
                    btnOpenLink.text = "📹 Buka Link Zoom / Virtual Meeting"
                    btnOpenLink.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
                }
                lowerLink.contains("maps") || lowerLink.contains("goo.gl") -> {
                    btnOpenLink.text = "📍 Buka Lokasi di Google Maps"
                    btnOpenLink.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                }
                else -> {
                    btnOpenLink.text = "🔗 Buka Link Acara"
                    btnOpenLink.setBackgroundColor(android.graphics.Color.parseColor("#FF6D00"))
                }
            }

            btnOpenLink.setOnClickListener {
                try {
                    val uri = if (!linkUrl.startsWith("http://") && !linkUrl.startsWith("https://")) {
                        Uri.parse("https://$linkUrl")
                    } else {
                        Uri.parse(linkUrl)
                    }
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    Toast.makeText(this, "Tidak dapat membuka link", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            btnOpenLink.visibility = View.GONE
        }

        val btnRegister = findViewById<MaterialButton>(R.id.btn_register_event)
        val btnViewRegistrants = findViewById<MaterialButton>(R.id.btn_view_registrants)

        // Check registration status if user is logged in
        if (sessionManager.isLoggedIn()) {
            val userId = sessionManager.getUserId()
            lifecycleScope.launch {
                try {
                    val response = AuthClient.apiService.checkRegistration(eventId = eventId, userId = userId)
                    if (response.isSuccessful && response.body()?.isRegistered == true) {
                        runOnUiThread {
                            btnRegister.text = "✓ Anda Sudah Terdaftar"
                            btnRegister.isEnabled = false
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // Daftar button click
        btnRegister.setOnClickListener {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Login terlebih dahulu untuk mendaftar", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                return@setOnClickListener
            }
            showRegisterDialog(eventId, title, sessionManager, btnRegister)
        }

        // Load banner image
        if (imageUrl.isNotEmpty()) {
            val imgBanner = findViewById<ImageView>(R.id.img_event_detail_banner)
            lifecycleScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        val conn = java.net.URL(imageUrl).openConnection() as java.net.HttpURLConnection
                        conn.doInput = true; conn.connect()
                        BitmapFactory.decodeStream(conn.inputStream)
                    }
                    imgBanner.setImageBitmap(bitmap)
                } catch (_: Exception) {}
            }
        }

        // Admin-only options: Edit, Delete, and View Registrants
        if (isAdmin) {
            val btnEdit = findViewById<MaterialButton>(R.id.btn_edit_event_detail)
            val btnDelete = findViewById<MaterialButton>(R.id.btn_delete_event_detail)
            btnEdit.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
            btnViewRegistrants.visibility = View.VISIBLE

            btnEdit.setOnClickListener {
                // Return to EventFragment with edit flag
                val resultIntent = Intent().apply {
                    putExtra("edit_event_id", eventId)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            btnDelete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("⚠️ Hapus Event")
                    .setMessage("Yakin ingin menghapus event \"$title\"?")
                    .setPositiveButton("Hapus") { _, _ ->
                        deleteEvent(eventId)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }

            btnViewRegistrants.setOnClickListener {
                viewRegistrants(eventId, sessionManager.getUserId())
            }
        }
    }

    private fun showRegisterDialog(eventId: Int, title: String, sessionManager: SessionManager, btnRegister: MaterialButton) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_register_event, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_reg_name)
        val etEmail = dialogView.findViewById<EditText>(R.id.et_reg_email)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_reg_phone)
        val etNotes = dialogView.findViewById<EditText>(R.id.et_reg_notes)

        // Pre-fill user details from session
        etName.setText(sessionManager.getUserName())
        etEmail.setText(sessionManager.getUserEmail())

        AlertDialog.Builder(this)
            .setTitle("📝 Pendaftaran Event")
            .setMessage("Daftar untuk kegiatan: $title")
            .setView(dialogView)
            .setPositiveButton("Kirim Pendaftaran") { _, _ ->
                val name = etName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val notes = etNotes.text.toString().trim()

                if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Nama, Email, dan No HP wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        val response = AuthClient.apiService.registerEvent(
                            eventId = eventId,
                            userId = sessionManager.getUserId(),
                            name = name,
                            email = email,
                            phone = phone,
                            notes = notes
                        )
                        runOnUiThread {
                            if (response.isSuccessful && response.body()?.success == true) {
                                Toast.makeText(this@EventDetailActivity, "Pendaftaran berhasil! 🌟", Toast.LENGTH_SHORT).show()
                                btnRegister.text = "✓ Anda Sudah Terdaftar"
                                btnRegister.isEnabled = false
                            } else {
                                Toast.makeText(this@EventDetailActivity, "Gagal mendaftar: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@EventDetailActivity, "Koneksi gagal", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun viewRegistrants(eventId: Int, adminId: Int) {
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getEventRegistrations(userId = adminId, eventId = eventId)
                runOnUiThread {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        val list = body.data ?: emptyList()
                        val total = body.total
                        
                        if (list.isEmpty()) {
                            AlertDialog.Builder(this@EventDetailActivity)
                                .setTitle("👥 Daftar Pendaftar")
                                .setMessage("Belum ada pendaftar untuk kegiatan ini.")
                                .setPositiveButton("Tutup", null)
                                .show()
                            return@runOnUiThread
                        }

                        val sb = java.lang.StringBuilder()
                        sb.append("Total Pendaftar: $total orang\n\n")
                        list.forEachIndexed { index, reg ->
                            sb.append("${index + 1}. ${reg.name}\n")
                            sb.append("   📧 ${reg.email}\n")
                            sb.append("   📞 ${reg.phone}\n")
                            if (!reg.notes.isNullOrEmpty()) {
                                sb.append("   📝 Catatan: ${reg.notes}\n")
                            }
                            sb.append("\n")
                        }

                        AlertDialog.Builder(this@EventDetailActivity)
                            .setTitle("👥 Daftar Pendaftar")
                            .setMessage(sb.toString())
                            .setPositiveButton("Tutup", null)
                            .show()
                    } else {
                        Toast.makeText(this@EventDetailActivity, "Gagal mengambil data pendaftar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@EventDetailActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteEvent(eventId: Int) {
        val sessionManager = SessionManager(this)
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.deleteEvent(
                    userId = sessionManager.getUserId(),
                    id = eventId
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    runOnUiThread {
                        Toast.makeText(this@EventDetailActivity, "Event berhasil dihapus", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@EventDetailActivity, "Gagal menghapus event", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@EventDetailActivity, "Koneksi gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatDateIndo(dateStr: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = format.parse(dateStr) ?: return dateStr
            SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id")).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

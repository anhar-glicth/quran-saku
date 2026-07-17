package com.quran.labs.androidquran

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.SessionManager
import kotlinx.coroutines.launch

class GrupNgajiActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var progressGrup: ProgressBar
    private lateinit var layEmptyGrup: View
    private lateinit var btnJoin: Button
    private lateinit var btnCreate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_grup_ngaji)

        sessionManager = SessionManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "Grup Ngaji"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        progressGrup = findViewById(R.id.progress_grup)
        layEmptyGrup = findViewById(R.id.lay_empty_grup)
        btnJoin = findViewById(R.id.btn_join_by_id)
        btnCreate = findViewById(R.id.btn_buat_grup)

        btnJoin.setOnClickListener {
            showJoinGroupDialog()
        }

        btnCreate.setOnClickListener {
            showCreateGroupDialog()
        }

        checkUserGroup()
    }

    private fun checkUserGroup() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Login terlebih dahulu untuk mengakses Grup Ngaji", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressGrup.visibility = View.VISIBLE
        layEmptyGrup.visibility = View.GONE
        btnJoin.isEnabled = false
        btnCreate.isEnabled = false

        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getMyGroup(userId = userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    progressGrup.visibility = View.GONE
                    btnJoin.isEnabled = true
                    btnCreate.isEnabled = true

                    if (body != null && body.hasGroup && body.group != null) {
                        val status = body.group.memberStatus
                        if (status == "active") {
                            val intent = Intent(this@GrupNgajiActivity, GrupDetailActivity::class.java).apply {
                                putExtra("group_id", body.group.id)
                            }
                            startActivity(intent)
                            finish()
                        } else if (status == "pending") {
                            layEmptyGrup.visibility = View.VISIBLE
                            val tvTitle = layEmptyGrup.findViewById<TextView>(R.id.tv_empty_title) ?: findViewById(R.id.tv_empty_title)
                            val tvDesc = layEmptyGrup.findViewById<TextView>(R.id.tv_empty_desc) ?: findViewById(R.id.tv_empty_desc)
                            if (tvTitle != null) tvTitle.text = "Permintaan Join Pending ⌛"
                            if (tvDesc != null) tvDesc.text = "Permintaan gabung Anda ke grup \"${body.group.name}\" sedang menunggu persetujuan (ACC) dari admin grup."
                            btnJoin.visibility = View.GONE
                            btnCreate.visibility = View.GONE
                        }
                    } else {
                        layEmptyGrup.visibility = View.VISIBLE
                        btnJoin.visibility = View.VISIBLE
                        btnCreate.visibility = View.VISIBLE
                    }
                } else {
                    showError("Gagal memuat status grup (${response.code()})")
                }
            } catch (e: Exception) {
                showError("Tidak dapat terhubung ke server. Pastikan XAMPP aktif.")
            }
        }
    }

    private fun showError(msg: String) {
        progressGrup.visibility = View.GONE
        layEmptyGrup.visibility = View.VISIBLE
        val tvTitle = layEmptyGrup.findViewById<TextView>(R.id.tv_empty_title) ?: findViewById(R.id.tv_empty_title)
        val tvDesc = layEmptyGrup.findViewById<TextView>(R.id.tv_empty_desc) ?: findViewById(R.id.tv_empty_desc)
        if (tvTitle != null) tvTitle.text = "Koneksi Gagal"
        if (tvDesc != null) tvDesc.text = msg
    }

    private fun showJoinGroupDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val etCode = EditText(this).apply {
            hint = "Kode Grup (Contoh: QS-A1B2)"
            textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setHintTextColor(Color.parseColor("#BBBBBB"))
        }
        layout.addView(etCode)

        AlertDialog.Builder(this)
            .setTitle("🔑 Gabung Grup Ngaji")
            .setMessage("Masukkan Kode Grup (ID) unik dari grup yang ingin Anda ikuti:")
            .setView(layout)
            .setPositiveButton("Kirim Request") { _, _ ->
                val code = etCode.text.toString().trim().uppercase()
                if (code.isNotEmpty()) {
                    joinGroup(code)
                } else {
                    Toast.makeText(this, "Kode grup wajib diisi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun joinGroup(groupCode: String) {
        val userId = sessionManager.getUserId()
        progressGrup.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.joinGroup(userId = userId, groupCode = groupCode)
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@GrupNgajiActivity, "Request join berhasil dikirim!", Toast.LENGTH_SHORT).show()
                    checkUserGroup()
                } else {
                    progressGrup.visibility = View.GONE
                    Toast.makeText(this@GrupNgajiActivity, response.body()?.message ?: "Gagal bergabung ke grup", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressGrup.visibility = View.GONE
                Toast.makeText(this@GrupNgajiActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateGroupDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_group_name)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_group_desc)
        val etKhatam = dialogView.findViewById<EditText>(R.id.et_group_khatam)
        val etDuration = dialogView.findViewById<EditText>(R.id.et_group_duration)

        AlertDialog.Builder(this)
            .setTitle("👥 Buat Grup Ngaji Baru")
            .setView(dialogView)
            .setPositiveButton("Buat Grup") { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                val khatamStr = etKhatam.text.toString().trim()
                val durationStr = etDuration.text.toString().trim()

                val khatam = if (khatamStr.isNotEmpty()) khatamStr.toInt() else 1
                val duration = if (durationStr.isNotEmpty()) durationStr.toInt() else 30

                if (name.isNotEmpty() && desc.isNotEmpty()) {
                    createGroup(name, desc, khatam, duration)
                } else {
                    Toast.makeText(this, "Nama dan deskripsi grup wajib diisi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun createGroup(name: String, desc: String, khatam: Int, duration: Int) {
        val userId = sessionManager.getUserId()
        progressGrup.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.createGroup(
                    userId = userId, name = name, description = desc,
                    khatamTarget = khatam, durationDays = duration
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@GrupNgajiActivity, "Grup berhasil dibuat! 🎉", Toast.LENGTH_SHORT).show()
                    checkUserGroup()
                } else {
                    progressGrup.visibility = View.GONE
                    Toast.makeText(this@GrupNgajiActivity, response.body()?.message ?: "Gagal membuat grup", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressGrup.visibility = View.GONE
                Toast.makeText(this@GrupNgajiActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
            }
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

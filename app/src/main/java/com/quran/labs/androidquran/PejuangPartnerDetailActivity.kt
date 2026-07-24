package com.quran.labs.androidquran

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.SessionManager
import com.quran.labs.androidquran.model.PartnerApiItem
import kotlinx.coroutines.launch

class PejuangPartnerDetailActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton
    
    private val partners = mutableListOf<PartnerApiItem>()
    private lateinit var adapter: PartnerAdapter
    private var categoryId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pejuang_partner_detail)

        sessionManager = SessionManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        categoryId = intent.getStringExtra("category_id") ?: ""
        val categoryName = intent.getStringExtra("category_name") ?: "Mitra Pendukung"

        val tvTitle = findViewById<TextView>(R.id.tv_category_title)
        val tvDesc = findViewById<TextView>(R.id.tv_category_desc)

        tvTitle.text = categoryName

        val descText = when (categoryId) {
            "mitra_utama" -> "Lembaga pendukung utama yang mendanai dan ikut andil penuh menyebarkan program Strava Quran."
            "pendukung_resmi" -> "Instansi dan donatur resmi penyokong sarana prasarana serta biaya operasional program."
            "mitra_distribusi" -> "Masjid, yayasan, dan organisasi penyalur resmi mushaf dan edukasi aplikasi ke umat."
            "mitra_edukasi" -> "Lembaga pendidikan, pondok pesantren, dan pembina tahfidz mitra program Strava Quran."
            else -> "Lembaga-lembaga yang ikut berpartisipasi membagikan atau mendukung program Quran."
        }
        tvDesc.text = descText

        rv = findViewById(R.id.rv_partners)
        progress = findViewById(R.id.progress_partners)
        tvEmpty = findViewById(R.id.tv_partners_empty)
        fabAdd = findViewById(R.id.fab_add_partner)

        rv.layoutManager = LinearLayoutManager(this)
        adapter = PartnerAdapter(partners)
        rv.adapter = adapter

        // Setup Admin controls
        if (sessionManager.isLoggedIn() && sessionManager.getUserRole() == "admin") {
            fabAdd.visibility = View.VISIBLE
            fabAdd.setOnClickListener {
                showAddPartnerDialog()
            }
        } else {
            fabAdd.visibility = View.GONE
        }

        loadPartners()
    }

    private fun loadPartners() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        rv.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getPartners(categoryId = categoryId)
                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    progress.visibility = View.GONE
                    if (list.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rv.visibility = View.VISIBLE
                        partners.clear()
                        partners.addAll(list)
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    showError("Gagal memuat mitra (${response.code()})")
                }
            } catch (e: Exception) {
                showError("Tidak dapat terhubung ke server.")
            }
        }
    }

    private fun showError(msg: String) {
        progress.visibility = View.GONE
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = msg
        rv.visibility = View.GONE
    }

    private fun showAddPartnerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_partner, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_partner_name)
        val etLogo = dialogView.findViewById<EditText>(R.id.et_partner_logo)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_partner_desc)
        val rgColors = dialogView.findViewById<RadioGroup>(R.id.rg_colors)

        // Color presets
        val presets = listOf(
            Triple("#E0F2F1", "#004D40", "Teal"),
            Triple("#FFF3E0", "#E65100", "Orange"),
            Triple("#E8F5E9", "#1B5E20", "Green"),
            Triple("#F3E5F5", "#4A148C", "Purple"),
            Triple("#FFEBEE", "#C62828", "Red")
        )

        presets.forEachIndexed { index, preset ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = preset.third
                setTextColor(Color.parseColor(preset.second))
                buttonTintList = ColorStateList.valueOf(Color.parseColor(preset.second))
                setPadding(0, 0, 20, 0)
            }
            rgColors.addView(rb)
            if (index == 0) rgColors.check(rb.id)
        }

        AlertDialog.Builder(this)
            .setTitle("➕ Tambah Mitra Baru")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val name = etName.text.toString().trim()
                val logo = etLogo.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                
                val checkedId = rgColors.checkedRadioButtonId
                val selectedIndex = (0 until rgColors.childCount).indexOfFirst { rgColors.getChildAt(it).id == checkedId }
                val colorPreset = presets.getOrElse(selectedIndex) { presets[0] }

                if (name.isNotEmpty() && logo.isNotEmpty() && desc.isNotEmpty()) {
                    savePartner(logo, name, desc, colorPreset.first, colorPreset.second)
                } else {
                    Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun savePartner(logo: String, name: String, desc: String, bg: String, text: String) {
        val adminUserId = sessionManager.getUserId()
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.addPartner(
                    userId = adminUserId,
                    categoryId = categoryId,
                    logoText = logo,
                    name = name,
                    description = desc,
                    bgColor = bg,
                    textColor = text
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@PejuangPartnerDetailActivity, "Mitra berhasil disimpan", Toast.LENGTH_SHORT).show()
                    loadPartners()
                } else {
                    Toast.makeText(this@PejuangPartnerDetailActivity, "Gagal menyimpan mitra: " + response.body()?.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PejuangPartnerDetailActivity, "Koneksi server gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAdminOptionsDialog(item: PartnerApiItem) {
        val options = arrayOf("✏️ Edit Mitra", "❌ Hapus Mitra")
        AlertDialog.Builder(this)
            .setTitle("Pilihan Admin")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditPartnerDialog(item)
                    1 -> confirmDeletePartner(item)
                }
            }
            .show()
    }

    private fun showEditPartnerDialog(item: PartnerApiItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_partner, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_partner_name)
        val etLogo = dialogView.findViewById<EditText>(R.id.et_partner_logo)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_partner_desc)
        val rgColors = dialogView.findViewById<RadioGroup>(R.id.rg_colors)

        etName.setText(item.name)
        etLogo.setText(item.logoText)
        etDesc.setText(item.description)

        // Color presets
        val presets = listOf(
            Triple("#E0F2F1", "#004D40", "Teal"),
            Triple("#FFF3E0", "#E65100", "Orange"),
            Triple("#E8F5E9", "#1B5E20", "Green"),
            Triple("#F3E5F5", "#4A148C", "Purple"),
            Triple("#FFEBEE", "#C62828", "Red")
        )

        presets.forEachIndexed { index, preset ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = preset.third
                setTextColor(Color.parseColor(preset.second))
                buttonTintList = ColorStateList.valueOf(Color.parseColor(preset.second))
                setPadding(0, 0, 20, 0)
            }
            rgColors.addView(rb)
            
            if (item.bgColor.equals(preset.first, ignoreCase = true) && 
                item.textColor.equals(preset.second, ignoreCase = true)) {
                rgColors.check(rb.id)
            } else if (index == 0 && rgColors.checkedRadioButtonId == View.NO_ID) {
                rgColors.check(rb.id)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("✏️ Edit Mitra")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val name = etName.text.toString().trim()
                val logo = etLogo.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                
                val checkedId = rgColors.checkedRadioButtonId
                val selectedIndex = (0 until rgColors.childCount).indexOfFirst { rgColors.getChildAt(it).id == checkedId }
                val colorPreset = presets.getOrElse(selectedIndex) { presets[0] }

                if (name.isNotEmpty() && logo.isNotEmpty() && desc.isNotEmpty()) {
                    updatePartner(item.id, logo, name, desc, colorPreset.first, colorPreset.second)
                } else {
                    Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updatePartner(id: Int, logo: String, name: String, desc: String, bg: String, text: String) {
        val adminUserId = sessionManager.getUserId()
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.editPartner(
                    userId = adminUserId,
                    id = id,
                    categoryId = categoryId,
                    logoText = logo,
                    name = name,
                    description = desc,
                    bgColor = bg,
                    textColor = text
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@PejuangPartnerDetailActivity, "Mitra berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    loadPartners()
                } else {
                    Toast.makeText(this@PejuangPartnerDetailActivity, "Gagal memperbarui mitra: " + response.body()?.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PejuangPartnerDetailActivity, "Koneksi server gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeletePartner(item: PartnerApiItem) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Mitra")
            .setMessage("Apakah Anda yakin ingin menghapus mitra \"${item.name}\"?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                deletePartner(item.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePartner(id: Int) {
        val adminUserId = sessionManager.getUserId()
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.deletePartner(
                    userId = adminUserId,
                    id = id
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@PejuangPartnerDetailActivity, "Mitra berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadPartners()
                } else {
                    Toast.makeText(this@PejuangPartnerDetailActivity, "Gagal menghapus mitra: " + response.body()?.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PejuangPartnerDetailActivity, "Koneksi server gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class PartnerAdapter(private val list: List<PartnerApiItem>) :
        RecyclerView.Adapter<PartnerAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val logoCard: MaterialCardView = view.findViewById(R.id.card_logo_container)
            val tvLogo: TextView = view.findViewById(R.id.tv_partner_logo)
            val tvName: TextView = view.findViewById(R.id.tv_partner_name)
            val tvDesc: TextView = view.findViewById(R.id.tv_partner_desc)
            val tvRole: TextView = view.findViewById(R.id.tv_partner_role)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pejuang_partner, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvName.text = item.name
            holder.tvDesc.text = item.description
            holder.tvLogo.text = item.logoText

            try {
                val parsedBg = Color.parseColor(item.bgColor)
                val parsedText = Color.parseColor(item.textColor)
                holder.logoCard.setCardBackgroundColor(parsedBg)
                holder.tvLogo.setTextColor(parsedText)
            } catch (e: IllegalArgumentException) {
                // Fallback ke warna default jika format warna dari server tidak valid
                holder.logoCard.setCardBackgroundColor(Color.parseColor("#E0F2F1"))
                holder.tvLogo.setTextColor(Color.parseColor("#004D40"))
            }

            val roleName = when (item.categoryId) {
                "mitra_utama" -> "Mitra Utama"
                "pendukung_resmi" -> "Pendukung"
                "mitra_distribusi" -> "Distribusi"
                "mitra_edukasi" -> "Edukasi"
                else -> "Mitra"
            }
            holder.tvRole.text = roleName

            // Setup edit/delete on long press for admin
            if (sessionManager.isLoggedIn() && sessionManager.getUserRole() == "admin") {
                holder.itemView.setOnLongClickListener {
                    showAdminOptionsDialog(item)
                    true
                }
            } else {
                holder.itemView.setOnLongClickListener(null)
            }
        }

        override fun getItemCount(): Int = list.size
    }
}

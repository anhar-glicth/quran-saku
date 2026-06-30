package com.quran.labs.androidquran

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class PejuangPartnerDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pejuang_partner_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val categoryId = intent.getStringExtra("category_id") ?: ""
        val categoryName = intent.getStringExtra("category_name") ?: "Mitra Pendukung"

        val tvTitle = findViewById<TextView>(R.id.tv_category_title)
        val tvDesc = findViewById<TextView>(R.id.tv_category_desc)

        tvTitle.text = categoryName

        val items = getPartners(categoryId)
        
        val descText = when (categoryId) {
            "mitra_utama" -> "Lembaga pendukung utama yang mendanai dan ikut andil penuh menyebarkan program Quran Saku."
            "pendukung_resmi" -> "Instansi dan donatur resmi penyokong sarana prasarana serta biaya operasional program."
            "mitra_distribusi" -> "Masjid, yayasan, dan organisasi penyalur resmi mushaf dan edukasi aplikasi ke umat."
            "mitra_edukasi" -> "Lembaga pendidikan, pondok pesantren, dan pembina tahfidz mitra program Quran Saku."
            else -> "Lembaga-lembaga yang ikut berpartisipasi membagikan atau mendukung program Quran."
        }
        tvDesc.text = descText

        val rv = findViewById<RecyclerView>(R.id.rv_partners)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = PartnerAdapter(items)
    }

    private fun getPartners(categoryId: String): List<PartnerItem> {
        val list = mutableListOf<PartnerItem>()
        when (categoryId) {
            "mitra_utama" -> {
                list.add(PartnerItem("YPQ", "Yayasan Pejuang Quran", "Penyedia utama program distribusi mushaf digital.", "Mitra Utama", 0xFFE0F2F1.toInt(), 0xFF004D40.toInt()))
                list.add(PartnerItem("RZ", "Rumah Zakat", "Mendukung penyebaran program dakwah Al-Quran di pedalaman.", "Mitra Utama", 0xFFFFF3E0.toInt(), 0xFFE65100.toInt()))
                list.add(PartnerItem("DD", "Dompet Dhuafa", "Fasilitator pendistribusian program Al-Quran untuk kaum dhuafa.", "Mitra Utama", 0xFFE8F5E9.toInt(), 0xFF1B5E20.toInt()))
            }
            "pendukung_resmi" -> {
                list.add(PartnerItem("BMM", "Baitul Maal Muamalat", "Donatur utama penyediaan sarana dan operasional server.", "Pendukung", 0xFFFFE0B2.toInt(), 0xFFE65100.toInt()))
                list.add(PartnerItem("LMS", "Lazismu", "Penyokong finansial program dakwah Al-Quran Saku.", "Pendukung", 0xFFF3E5F5.toInt(), 0xFF4A148C.toInt()))
                list.add(PartnerItem("LSN", "LAZISNU", "Mitra resmi pendanaan dan penyebaran program syiar Quran.", "Pendukung", 0xFFE0F7FA.toInt(), 0xFF006064.toInt()))
            }
            "mitra_distribusi" -> {
                list.add(PartnerItem("MAI", "Masjid Istiqlal", "Penyalur resmi program aplikasi langsung ke jamaah Masjid Istiqlal.", "Distribusi", 0xFFE8EAF6.toInt(), 0xFF1A237E.toInt()))
                list.add(PartnerItem("MAA", "Masjid Agung Al-Azhar", "Penyalur program kajian dan aplikasi ke jamaah sekolah & masjid.", "Distribusi", 0xFFFCE4EC.toInt(), 0xFF880E4F.toInt()))
                list.add(PartnerItem("DMI", "Dewan Masjid Indonesia", "Jaringan distribusi aplikasi untuk takmir masjid nasional.", "Distribusi", 0xFFFFFDE7.toInt(), 0xFFF57F17.toInt()))
            }
            "mitra_edukasi" -> {
                list.add(PartnerItem("PPDQ", "Daarul Qur'an", "Mitra pembinaan hafalan, tilawah, dan pemahaman ayat santri.", "Edukasi", 0xFFFCE4EC.toInt(), 0xFF880E4F.toInt()))
                list.add(PartnerItem("CQF", "Cinta Quran Foundation", "Penyedia materi edukasi dakwah dan pembinaan baca tulis Quran.", "Edukasi", 0xFFE0F2F1.toInt(), 0xFF004D40.toInt()))
                list.add(PartnerItem("RTI", "Rumah Tahfidz Indonesia", "Penyelenggara bimbingan tahfidz Quran berbasis kurikulum digital.", "Edukasi", 0xFFFFF3E0.toInt(), 0xFFE65100.toInt()))
            }
        }
        return list
    }

    data class PartnerItem(
        val logoText: String,
        val name: String,
        val description: String,
        val role: String,
        val bgColor: Int,
        val textColor: Int
    )

    inner class PartnerAdapter(private val list: List<PartnerItem>) :
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
            holder.tvRole.text = item.role
            holder.tvLogo.text = item.logoText
            
            holder.tvLogo.setTextColor(item.textColor)
            holder.logoCard.setCardBackgroundColor(ColorStateList.valueOf(item.bgColor))
            
            holder.tvRole.setTextColor(item.textColor)
            holder.tvRole.backgroundTintList = ColorStateList.valueOf(item.bgColor)
        }

        override fun getItemCount(): Int = list.size
    }
}

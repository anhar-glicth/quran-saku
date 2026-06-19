package com.quran.labs.androidquran.ui.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.DoaActivity
import com.quran.labs.androidquran.GrupNgajiActivity
import com.quran.labs.androidquran.PejuangQuranActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.RankActivity

class KomunitasFragment : Fragment() {

  private var isAamiinned = false
  private var aamiinCount = 0

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_komunitas, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // 1. Setup Daily Prayer Card (Aamiinkan Button)
    val btnAamiinkan = view.findViewById<View>(R.id.btn_aamiinkan)
    val txtAamiinCount = view.findViewById<TextView>(R.id.txt_aamiin_count)

    btnAamiinkan?.setOnClickListener {
      if (!isAamiinned) {
        isAamiinned = true
        aamiinCount++
        txtAamiinCount?.text = "Aamiin ($aamiinCount)"
        txtAamiinCount?.setTextColor(Color.parseColor("#FFE0B2"))
        Toast.makeText(context, "Aamiin! Doa telah diaminkan.", Toast.LENGTH_SHORT).show()
      } else {
        isAamiinned = false
        txtAamiinCount?.text = "Aamiinkan"
        txtAamiinCount?.setTextColor(Color.WHITE)
      }
    }

    // 2. Setup Quick Action Menus
    view.findViewById<View>(R.id.btn_menu_doa)?.setOnClickListener {
      val intent = Intent(context, DoaActivity::class.java)
      startActivity(intent)
    }
    view.findViewById<View>(R.id.btn_menu_rank)?.setOnClickListener {
      val intent = Intent(context, RankActivity::class.java)
      startActivity(intent)
    }
    view.findViewById<View>(R.id.btn_menu_grup)?.setOnClickListener {
      val intent = Intent(context, GrupNgajiActivity::class.java)
      startActivity(intent)
    }
    view.findViewById<View>(R.id.btn_menu_pejuang)?.setOnClickListener {
      val intent = Intent(context, PejuangQuranActivity::class.java)
      startActivity(intent)
    }

    // 3. Setup "Lihat Semua" Titip Doa
    view.findViewById<View>(R.id.txt_lihat_semua)?.setOnClickListener {
      Toast.makeText(context, "Membuka semua titip doa...", Toast.LENGTH_SHORT).show()
    }

    // 4. Setup Leaderboard Segmented Toggle
    val txtToggleMingguan = view.findViewById<TextView>(R.id.txt_toggle_mingguan)
    val txtToggleBulanan = view.findViewById<TextView>(R.id.txt_toggle_bulanan)

    val txtInitials1 = view.findViewById<TextView>(R.id.txt_initials_1)
    val layInitials1 = view.findViewById<View>(R.id.lay_initials_1)
    val txtName1 = view.findViewById<TextView>(R.id.txt_name_1)
    val txtDesc1 = view.findViewById<TextView>(R.id.txt_desc_1)
    val txtProgress1 = view.findViewById<TextView>(R.id.txt_progress_1)

    val txtInitials2 = view.findViewById<TextView>(R.id.txt_initials_2)
    val layInitials2 = view.findViewById<View>(R.id.lay_initials_2)
    val txtName2 = view.findViewById<TextView>(R.id.txt_name_2)
    val txtDesc2 = view.findViewById<TextView>(R.id.txt_desc_2)
    val txtProgress2 = view.findViewById<TextView>(R.id.txt_progress_2)

    val txtInitials3 = view.findViewById<TextView>(R.id.txt_initials_3)
    val layInitials3 = view.findViewById<View>(R.id.lay_initials_3)
    val txtName3 = view.findViewById<TextView>(R.id.txt_name_3)
    val txtDesc3 = view.findViewById<TextView>(R.id.txt_desc_3)
    val txtProgress3 = view.findViewById<TextView>(R.id.txt_progress_3)

    fun updateLeaderboardUI(isWeekly: Boolean) {
      val ctx = context ?: return
      if (isWeekly) {
        // Toggle tabs styling
        txtToggleMingguan?.setBackgroundResource(R.drawable.bg_segmented_selected)
        txtToggleMingguan?.setTextColor(Color.WHITE)
        txtToggleBulanan?.setBackgroundResource(0)
        txtToggleBulanan?.setTextColor(Color.parseColor("#666666"))

        // Update data
        txtInitials1?.text = "KT"
        layInitials1?.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.ripple_dark)?.withAlpha(0) // reset tint or update custom colors
        layInitials1?.setBackgroundResource(R.drawable.bg_circle_solid)
        layInitials1?.backgroundTintList = ContextCompat.getColorStateList(ctx, android.R.color.white)
        layInitials1?.background?.setTint(Color.parseColor("#FDF2E9"))
        txtInitials1?.setTextColor(Color.parseColor("#E28743"))
        txtName1?.text = "Keluarga Taqwa"
        txtDesc1?.text = "12 Anggota Aktif"
        txtProgress1?.text = "2.450"

        txtInitials2?.text = "MQ"
        layInitials2?.setBackgroundResource(R.drawable.bg_circle_solid)
        layInitials2?.background?.setTint(Color.parseColor("#EBF5FB"))
        txtInitials2?.setTextColor(Color.parseColor("#2980B9"))
        txtName2?.text = "Majelis Quran Jakarta"
        txtDesc2?.text = "45 Anggota Aktif"
        txtProgress2?.text = "2.120"

        txtInitials3?.text = "SF"
        layInitials3?.setBackgroundResource(R.drawable.bg_circle_solid)
        layInitials3?.background?.setTint(Color.parseColor("#F2F3F4"))
        txtInitials3?.setTextColor(Color.parseColor("#7F8C8D"))
        txtName3?.text = "Sahabat Fillah"
        txtDesc3?.text = "8 Anggota Aktif"
        txtProgress3?.text = "1.980"
      } else {
        // Toggle tabs styling
        txtToggleBulanan?.setBackgroundResource(R.drawable.bg_segmented_selected)
        txtToggleBulanan?.setTextColor(Color.WHITE)
        txtToggleMingguan?.setBackgroundResource(0)
        txtToggleMingguan?.setTextColor(Color.parseColor("#666666"))

        // Update data (Bulanan)
        txtInitials1?.text = "MQ"
        layInitials1?.setBackgroundResource(R.drawable.bg_circle_solid)
        layInitials1?.background?.setTint(Color.parseColor("#EBF5FB"))
        txtInitials1?.setTextColor(Color.parseColor("#2980B9"))
        txtName1?.text = "Majelis Quran Jakarta"
        txtDesc1?.text = "45 Anggota Aktif"
        txtProgress1?.text = "9.840"

        txtInitials2?.text = "KT"
        layInitials2?.setBackgroundResource(R.drawable.bg_circle_solid)
        layInitials2?.background?.setTint(Color.parseColor("#FDF2E9"))
        txtInitials2?.setTextColor(Color.parseColor("#E28743"))
        txtName2?.text = "Keluarga Taqwa"
        txtDesc2?.text = "12 Anggota Aktif"
        txtProgress2?.text = "8.950"

        txtInitials3?.text = "SF"
        layInitials3?.setBackgroundResource(R.drawable.bg_circle_solid)
        layInitials3?.background?.setTint(Color.parseColor("#F2F3F4"))
        txtInitials3?.setTextColor(Color.parseColor("#7F8C8D"))
        txtName3?.text = "Sahabat Fillah"
        txtDesc3?.text = "8 Anggota Aktif"
        txtProgress3?.text = "7.200"
      }
    }

    txtToggleMingguan?.setOnClickListener {
      updateLeaderboardUI(true)
      Toast.makeText(context, "Menampilkan data Peringkat Mingguan", Toast.LENGTH_SHORT).show()
    }

    txtToggleBulanan?.setOnClickListener {
      updateLeaderboardUI(false)
      Toast.makeText(context, "Menampilkan data Peringkat Bulanan", Toast.LENGTH_SHORT).show()
    }

    // Initialize with Weekly data
    updateLeaderboardUI(true)

    // 5. Setup Donasi / Wakaf Buttons
    view.findViewById<View>(R.id.btn_donasi_maluku)?.setOnClickListener {
      Toast.makeText(context, "Membuka halaman Donasi Pedalaman Maluku...", Toast.LENGTH_SHORT).show()
    }
    view.findViewById<View>(R.id.btn_donasi_braille)?.setOnClickListener {
      Toast.makeText(context, "Membuka halaman Wakaf Quran Braille...", Toast.LENGTH_SHORT).show()
    }
  }
}

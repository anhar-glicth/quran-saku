package com.quran.labs.androidquran.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.util.HafalanStorageUtils

class TahfidzFragment : Fragment() {

  private lateinit var tvTotalSetor: TextView
  private lateinit var tvTotalLulus: TextView
  private lateinit var tvRataAkurasi: TextView
  private lateinit var historyContainer: LinearLayout
  private lateinit var layoutEmpty: LinearLayout

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_tahfidz, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    tvTotalSetor     = view.findViewById(R.id.tv_total_setor)
    tvTotalLulus     = view.findViewById(R.id.tv_total_lulus)
    tvRataAkurasi    = view.findViewById(R.id.tv_rata_akurasi)
    historyContainer = view.findViewById(R.id.history_container)
    layoutEmpty      = view.findViewById(R.id.layout_empty)

    view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
      parentFragmentManager.popBackStack()
    }

    view.findViewById<View>(R.id.btn_add_target).setOnClickListener {
      parentFragmentManager.beginTransaction()
          .replace(R.id.fragment_container, TahfidzRecitationFragment.newInstance())
          .addToBackStack(null)
          .commit()
    }

    loadHistory()
  }

  override fun onResume() {
    super.onResume()
    // Refresh setiap kali kembali ke halaman ini
    loadHistory()
  }

  private fun loadHistory() {
    val ctx = requireContext()
    val records = HafalanStorageUtils.getAllRecords(ctx)

    // Update statistik
    tvTotalSetor.text  = records.size.toString()
    tvTotalLulus.text  = HafalanStorageUtils.getLulusCount(ctx).toString()
    tvRataAkurasi.text = "${HafalanStorageUtils.getAverageAccuracy(ctx)}%"

    // Tampilkan / sembunyikan empty state
    if (records.isEmpty()) {
      layoutEmpty.visibility      = View.VISIBLE
      historyContainer.visibility = View.GONE
      return
    }
    layoutEmpty.visibility      = View.GONE
    historyContainer.visibility = View.VISIBLE

    // Bangun kartu riwayat secara programatik
    historyContainer.removeAllViews()
    val inflater = LayoutInflater.from(ctx)

    for (record in records) {
      val card = inflater.inflate(R.layout.item_hafalan_history, historyContainer, false)

      card.findViewById<TextView>(R.id.tv_item_surah).text =
          "${record.surahName}  •  ${record.ayahRange()}"
      card.findViewById<TextView>(R.id.tv_item_date).text = record.formattedDate()

      val tvStatus   = card.findViewById<TextView>(R.id.tv_item_status)
      val tvAccuracy = card.findViewById<TextView>(R.id.tv_item_accuracy)
      val progress   = card.findViewById<ProgressBar>(R.id.pb_item_accuracy)

      tvAccuracy.text = "${record.accuracy}%"
      progress.progress = record.accuracy

      if (record.isLulus()) {
        tvStatus.text = "✅ Lulus"
        tvStatus.setTextColor(Color.parseColor("#2E7D32"))
        tvAccuracy.setTextColor(Color.parseColor("#2E7D32"))
        progress.progressTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
      } else {
        tvStatus.text = "❌ Belum Lulus"
        tvStatus.setTextColor(Color.parseColor("#C62828"))
        tvAccuracy.setTextColor(Color.parseColor("#C62828"))
        progress.progressTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))
      }

      historyContainer.addView(card)
    }
  }

  companion object {
    fun newInstance(): TahfidzFragment = TahfidzFragment()
  }
}

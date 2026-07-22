package com.quran.labs.androidquran.ui.fragment

import android.content.Intent
import android.net.Uri
import com.quran.labs.androidquran.auth.AuthClient
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.R
import java.text.NumberFormat
import java.util.Locale

class ZakatFragment : Fragment() {

  private var activeTab = "fitrah" // "fitrah", "profesi", "maal"

  // Rates Configuration
  private lateinit var etGoldPrice: EditText
  private lateinit var etRicePrice: EditText

  // Tabs
  private lateinit var tabFitrah: TextView
  private lateinit var tabProfesi: TextView
  private lateinit var tabMaal: TextView

  // Input Layouts
  private lateinit var layoutFitrah: LinearLayout
  private lateinit var layoutProfesi: LinearLayout
  private lateinit var layoutMaal: LinearLayout

  // Zakat Fitrah Inputs
  private lateinit var etFitrahJiwa: EditText

  // Zakat Profesi Inputs
  private lateinit var etProfesiGaji: EditText
  private lateinit var etProfesiBonus: EditText
  private lateinit var etProfesiPengeluaran: EditText

  // Zakat Maal Inputs
  private lateinit var etMaalTabungan: EditText
  private lateinit var etMaalEmas: EditText
  private lateinit var etMaalSaham: EditText
  private lateinit var etMaalHutang: EditText

  // Result UI
  private lateinit var tvStatus: TextView
  private lateinit var tvNisabInfo: TextView
  private lateinit var tvZakatAmount: TextView
  private lateinit var tvCalculationNotes: TextView
  private lateinit var btnAction: Button
  private lateinit var btnPay: Button

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_zakat, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Back click
    view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
      parentFragmentManager.popBackStack()
    }

    // Initialize inputs & controls
    etGoldPrice = view.findViewById(R.id.et_gold_price)
    etRicePrice = view.findViewById(R.id.et_rice_price)

    tabFitrah = view.findViewById(R.id.tab_fitrah)
    tabProfesi = view.findViewById(R.id.tab_profesi)
    tabMaal = view.findViewById(R.id.tab_maal)

    layoutFitrah = view.findViewById(R.id.layout_fitrah_inputs)
    layoutProfesi = view.findViewById(R.id.layout_profesi_inputs)
    layoutMaal = view.findViewById(R.id.layout_maal_inputs)

    etFitrahJiwa = view.findViewById(R.id.et_fitrah_jiwa)

    etProfesiGaji = view.findViewById(R.id.et_profesi_gaji)
    etProfesiBonus = view.findViewById(R.id.et_profesi_bonus)
    etProfesiPengeluaran = view.findViewById(R.id.et_profesi_pengeluaran)

    etMaalTabungan = view.findViewById(R.id.et_maal_tabungan)
    etMaalEmas = view.findViewById(R.id.et_maal_emas)
    etMaalSaham = view.findViewById(R.id.et_maal_saham)
    etMaalHutang = view.findViewById(R.id.et_maal_hutang)

    tvStatus = view.findViewById(R.id.tv_zakat_status)
    tvNisabInfo = view.findViewById(R.id.tv_nisab_info)
    tvZakatAmount = view.findViewById(R.id.tv_zakat_amount)
    tvCalculationNotes = view.findViewById(R.id.tv_calculation_notes)
    btnAction = view.findViewById(R.id.btn_action)
    btnPay = view.findViewById(R.id.btn_pay)

    // Tab Listeners
    tabFitrah.setOnClickListener { switchTab("fitrah") }
    tabProfesi.setOnClickListener { switchTab("profesi") }
    tabMaal.setOnClickListener { switchTab("maal") }

    // Input changes triggers real-time calculation
    val watcher = object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
      override fun afterTextChanged(s: Editable?) {
        calculateZakat()
      }
    }

    etGoldPrice.addTextChangedListener(watcher)
    etRicePrice.addTextChangedListener(watcher)
    etFitrahJiwa.addTextChangedListener(watcher)
    etProfesiGaji.addTextChangedListener(watcher)
    etProfesiBonus.addTextChangedListener(watcher)
    etProfesiPengeluaran.addTextChangedListener(watcher)
    etMaalTabungan.addTextChangedListener(watcher)
    etMaalEmas.addTextChangedListener(watcher)
    etMaalSaham.addTextChangedListener(watcher)
    etMaalHutang.addTextChangedListener(watcher)

    // Share Button action
    btnAction.setOnClickListener {
      shareCalculation()
    }

    btnPay.setOnClickListener {
      payZakat()
    }

    // Run initial calculation
    calculateZakat()
  }

  private fun switchTab(tab: String) {
    activeTab = tab
    val ctx = requireContext()

    // Reset Tabs layout styles
    tabFitrah.setBackgroundResource(0)
    tabFitrah.setTextColor(ContextCompat.getColor(ctx, R.color.toolbar_text))
    tabProfesi.setBackgroundResource(0)
    tabProfesi.setTextColor(ContextCompat.getColor(ctx, R.color.toolbar_text))
    tabMaal.setBackgroundResource(0)
    tabMaal.setTextColor(ContextCompat.getColor(ctx, R.color.toolbar_text))

    // Set selected Tab layout style
    val selectedTab = when (tab) {
      "fitrah" -> tabFitrah
      "profesi" -> tabProfesi
      else -> tabMaal
    }
    selectedTab.setBackgroundResource(R.drawable.circle_orange_bg)
    selectedTab.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.accent_color)
    selectedTab.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))

    // Switch input layouts visibility
    layoutFitrah.visibility = if (tab == "fitrah") View.VISIBLE else View.GONE
    layoutProfesi.visibility = if (tab == "profesi") View.VISIBLE else View.GONE
    layoutMaal.visibility = if (tab == "maal") View.VISIBLE else View.GONE

    calculateZakat()
  }

  private fun calculateZakat() {
    val goldPrice = etGoldPrice.text.toString().toDoubleOrNull() ?: 1400000.0
    val ricePrice = etRicePrice.text.toString().toDoubleOrNull() ?: 15000.0

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
      maximumFractionDigits = 0
    }

    when (activeTab) {
      "fitrah" -> {
        val jiwa = etFitrahJiwa.text.toString().toIntOrNull() ?: 0
        val berasPerJiwa = 2.5 // kg
        val totalZakatUang = jiwa * berasPerJiwa * ricePrice

        tvStatus.text = "Wajib Zakat"
        tvStatus.setBackgroundResource(R.drawable.circle_peach_bg)
        tvStatus.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.ayah_bookmark_color)
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        tvNisabInfo.text = "Zakat Fitrah wajib dibayarkan oleh seluruh jiwa Muslim di akhir bulan Ramadan."
        tvZakatAmount.text = currencyFormatter.format(totalZakatUang)
        tvCalculationNotes.text = "Setara dengan ${jiwa * berasPerJiwa} kg beras untuk $jiwa jiwa (estimasi @ Rp ${currencyFormatter.format(ricePrice).replace("Rp", "")}/kg)."
      }

      "profesi" -> {
        val gaji = etProfesiGaji.text.toString().toDoubleOrNull() ?: 0.0
        val bonus = etProfesiBonus.text.toString().toDoubleOrNull() ?: 0.0
        val pengeluaran = etProfesiPengeluaran.text.toString().toDoubleOrNull() ?: 0.0

        val pendapatanBersih = (gaji + bonus) - pengeluaran
        val nisabBulanan = 522.0 * ricePrice // 522 kg beras
        val isWajib = pendapatanBersih >= nisabBulanan

        updateStatusBadge(isWajib)

        tvNisabInfo.text = "Batas Nisab Bulanan: ${currencyFormatter.format(nisabBulanan)} (522 kg beras)"

        if (isWajib) {
          val zakat = pendapatanBersih * 0.025
          tvZakatAmount.text = currencyFormatter.format(zakat)
          tvCalculationNotes.text = "Pendapatan bersih Anda ${currencyFormatter.format(pendapatanBersih)} telah mencapai nisab. Wajib membayar zakat profesi sebesar 2.5%."
        } else {
          tvZakatAmount.text = "Rp 0"
          val sisa = nisabBulanan - pendapatanBersih
          tvCalculationNotes.text = "Pendapatan bersih Anda ${currencyFormatter.format(pendapatanBersih)} belum mencapai nisab. Kurang ${currencyFormatter.format(sisa)} untuk wajib zakat."
        }
      }

      "maal" -> {
        val tabungan = etMaalTabungan.text.toString().toDoubleOrNull() ?: 0.0
        val emas = etMaalEmas.text.toString().toDoubleOrNull() ?: 0.0
        val saham = etMaalSaham.text.toString().toDoubleOrNull() ?: 0.0
        val hutang = etMaalHutang.text.toString().toDoubleOrNull() ?: 0.0

        val hartaBersih = (tabungan + emas + saham) - hutang
        val nisabTahunan = 85.0 * goldPrice // 85 gram emas
        val isWajib = hartaBersih >= nisabTahunan

        updateStatusBadge(isWajib)

        tvNisabInfo.text = "Batas Nisab Tahunan: ${currencyFormatter.format(nisabTahunan)} (85 gram emas)"

        if (isWajib) {
          val zakat = hartaBersih * 0.025
          tvZakatAmount.text = currencyFormatter.format(zakat)
          tvCalculationNotes.text = "Total harta bersih simpanan Anda ${currencyFormatter.format(hartaBersih)} telah mencapai nisab. Wajib membayar zakat maal sebesar 2.5%."
        } else {
          tvZakatAmount.text = "Rp 0"
          val sisa = nisabTahunan - hartaBersih
          tvCalculationNotes.text = "Total harta bersih simpanan Anda ${currencyFormatter.format(hartaBersih)} belum mencapai nisab haul. Kurang ${currencyFormatter.format(sisa)} untuk wajib zakat."
        }
      }
    }
  }

  private fun updateStatusBadge(isWajib: Boolean) {
    val ctx = requireContext()
    if (isWajib) {
      tvStatus.text = "Wajib Zakat"
      tvStatus.setBackgroundResource(R.drawable.circle_peach_bg)
      tvStatus.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.ayah_bookmark_color)
      tvStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
    } else {
      tvStatus.text = "Belum Wajib"
      tvStatus.setBackgroundResource(R.drawable.circle_peach_bg)
      tvStatus.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.disabled_color)
      tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
    }
  }

  private fun shareCalculation() {
    val textToShare = """
      *Kalkulasi Zakat - Strava Quran*
      Kategori: Zakat ${activeTab.replaceFirstChar { it.uppercase() }}
      Status: ${tvStatus.text}
      Jumlah Zakat: ${tvZakatAmount.text}
      
      _${tvCalculationNotes.text}_
      
      Dihitung menggunakan Aplikasi Strava Quran.
    """.trimIndent()

    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, textToShare)
      type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Bagikan Hasil Kalkulasi")
    startActivity(shareIntent)
  }

  private fun payZakat() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AuthClient.BASE_URL + "zakat.php"))
    startActivity(intent)
  }

  override fun onResume() {
    super.onResume()
    // Hide parent activity toolbar area to ensure no double toolbars are visible
    (activity as? AppCompatActivity)?.supportActionBar?.hide()
    activity?.findViewById<View>(R.id.toolbar_area)?.visibility = View.GONE
  }

  override fun onPause() {
    super.onPause()
    // Restore parent activity toolbar area when leaving
    (activity as? AppCompatActivity)?.supportActionBar?.show()
    activity?.findViewById<View>(R.id.toolbar_area)?.visibility = View.VISIBLE
  }

  companion object {
    fun newInstance(): ZakatFragment = ZakatFragment()
  }
}

package com.quran.labs.androidquran.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.ui.QuranActivity
import com.quran.labs.androidquran.ui.helpers.JumpDestination
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

class TilawahDashboardFragment : Fragment() {

  @Inject
  lateinit var quranInfo: QuranInfo

  @Inject
  lateinit var quranDisplayData: QuranDisplayData

  private var lastPage = 293 // Default to Al-Kahf

  override fun onAttach(context: Context) {
    super.onAttach(context)
    val app = context.applicationContext as QuranApplication
    app.applicationComponent.inject(this)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_tilawah_detail, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
      parentFragmentManager.popBackStack()
    }

    // Set up popular Surah click events
    view.findViewById<View>(R.id.card_sura_yasin).setOnClickListener {
      (activity as? JumpDestination)?.jumpTo(440) // Surah Yasin (Page 440)
    }

    view.findViewById<View>(R.id.card_sura_kahf).setOnClickListener {
      (activity as? JumpDestination)?.jumpTo(293) // Surah Al-Kahf (Page 293)
    }

    view.findViewById<View>(R.id.card_sura_mulk).setOnClickListener {
      (activity as? JumpDestination)?.jumpTo(562) // Surah Al-Mulk (Page 562)
    }

    view.findViewById<View>(R.id.card_sura_rahman).setOnClickListener {
      (activity as? JumpDestination)?.jumpTo(531) // Surah Ar-Rahman (Page 531)
    }

    val continueBtn = view.findViewById<Button>(R.id.btn_continue_read)
    continueBtn.setOnClickListener {
      (activity as? JumpDestination)?.jumpTo(lastPage)
    }

    // Load actual last read page if QuranActivity is active
    val activity = activity
    if (activity is QuranActivity) {
      viewLifecycleOwner.lifecycleScope.launch {
        val recentPage = activity.latestPage()
        if (recentPage > 0) {
          lastPage = recentPage
          val suraNumber = quranInfo.getSuraNumberFromPage(recentPage)
          val suraName = quranDisplayData.getSuraName(requireContext(), suraNumber, wantPrefix = true)
          val firstAyah = quranInfo.getFirstAyahOnPage(recentPage)
          
          view.findViewById<TextView>(R.id.txt_last_read_sura).text = "$suraName: Ayat $firstAyah"
          view.findViewById<TextView>(R.id.txt_last_read_page).text = "Halaman $recentPage"
        }
      }
    }
  }

  companion object {
    fun newInstance(): TilawahDashboardFragment = TilawahDashboardFragment()
  }
}

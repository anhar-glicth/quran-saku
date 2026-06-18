package com.quran.labs.androidquran.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.view.SlidingTabLayout
import com.quran.labs.androidquran.util.QuranUtils
import kotlin.math.abs

class TilawahFragment : Fragment() {
  private var isRtl = false

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_tilawah, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    isRtl = QuranUtils.isRtl()

    val pager = view.findViewById<ViewPager>(R.id.index_pager)
    pager.offscreenPageLimit = 3
    val pagerAdapter = PagerAdapter(childFragmentManager)
    pager.adapter = pagerAdapter
    val indicator = view.findViewById<SlidingTabLayout>(R.id.indicator)
    indicator.setViewPager(pager)

    val pageToJumpTo = if (isRtl) {
      TITLES.size - 1
    } else {
      0
    }
    pager.currentItem = pageToJumpTo
  }

  private inner class PagerAdapter(fm: FragmentManager) :
      FragmentPagerAdapter(fm) {

    override fun getCount() = 3

    override fun getItem(position: Int): Fragment {
      var pos = position
      if (isRtl) {
        pos = abs(position - 2)
      }
      return when (pos) {
        SURA_LIST -> SuraListFragment.newInstance()
        JUZ2_LIST -> JuzListFragment.newInstance()
        BOOKMARKS_LIST -> BookmarksFragment.newInstance()
        else -> BookmarksFragment.newInstance()
      }
    }

    override fun getItemId(position: Int): Long {
      val pos = if (isRtl) abs(position - 2) else position
      return when (pos) {
        SURA_LIST -> SURA_LIST.toLong()
        JUZ2_LIST -> JUZ2_LIST.toLong()
        BOOKMARKS_LIST -> BOOKMARKS_LIST.toLong()
        else -> BOOKMARKS_LIST.toLong()
      }
    }

    override fun getPageTitle(position: Int): CharSequence {
      val resId = if (isRtl) ARABIC_TITLES[position] else TITLES[position]
      return getString(resId)
    }
  }

  companion object {
    private val TITLES = intArrayOf(
        R.string.quran_sura,
        R.string.quran_juz2,
        R.string.menu_bookmarks
    )
    private val ARABIC_TITLES = intArrayOf(
        R.string.menu_bookmarks,
        R.string.quran_juz2,
        R.string.quran_sura
    )
    private const val SURA_LIST = 0
    private const val JUZ2_LIST = 1
    private const val BOOKMARKS_LIST = 2
  }
}

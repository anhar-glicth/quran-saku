package com.quran.labs.androidquran.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.R

class TajwidFragment : Fragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_tajwid, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
      parentFragmentManager.popBackStack()
    }

    // Set up category cards click listeners
    view.findViewById<View>(R.id.card_nun_sukun).setOnClickListener {
      openDetailFragment("nun_sukun")
    }

    view.findViewById<View>(R.id.card_mim_sukun).setOnClickListener {
      openDetailFragment("mim_sukun")
    }

    view.findViewById<View>(R.id.card_alif_lam).setOnClickListener {
      openDetailFragment("alif_lam")
    }

    view.findViewById<View>(R.id.card_hukum_mad).setOnClickListener {
      openDetailFragment("hukum_mad")
    }

    view.findViewById<View>(R.id.card_idgham_qalqalah).setOnClickListener {
      openDetailFragment("idgham_qalqalah")
    }
  }

  private fun openDetailFragment(category: String) {
    parentFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, TajwidDetailFragment.newInstance(category))
        .addToBackStack(null)
        .commit()
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
    fun newInstance(): TajwidFragment = TajwidFragment()
  }
}

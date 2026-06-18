package com.quran.labs.androidquran.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.R

class TahfidzFragment : Fragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_tahfidz, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
      parentFragmentManager.popBackStack()
    }

    view.findViewById<View>(R.id.btn_add_target).setOnClickListener {
      parentFragmentManager.beginTransaction()
          .replace(R.id.fragment_container, TahfidzRecitationFragment.newInstance())
          .addToBackStack(null)
          .commit()
    }
  }

  companion object {
    fun newInstance(): TahfidzFragment = TahfidzFragment()
  }
}

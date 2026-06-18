package com.quran.labs.androidquran.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.AboutUsActivity
import com.quran.labs.androidquran.HelpActivity
import com.quran.labs.androidquran.QuranPreferenceActivity
import com.quran.labs.androidquran.R

class ProfilFragment : Fragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_profil, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.findViewById<View>(R.id.btn_settings)?.setOnClickListener {
      startActivity(Intent(requireContext(), QuranPreferenceActivity::class.java))
    }

    view.findViewById<View>(R.id.btn_help)?.setOnClickListener {
      startActivity(Intent(requireContext(), HelpActivity::class.java))
    }

    view.findViewById<View>(R.id.btn_about)?.setOnClickListener {
      startActivity(Intent(requireContext(), AboutUsActivity::class.java))
    }
  }
}

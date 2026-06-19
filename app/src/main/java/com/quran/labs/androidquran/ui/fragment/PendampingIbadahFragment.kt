package com.quran.labs.androidquran.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.DzikirActivity
import com.quran.labs.androidquran.KiblatActivity
import com.quran.labs.androidquran.KhatamActivity
import com.quran.labs.androidquran.CatatanActivity

class PendampingIbadahFragment : Fragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_pendamping_ibadah, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.findViewById<View>(R.id.card_dzikir)?.setOnClickListener {
      startActivity(Intent(activity, DzikirActivity::class.java))
    }

    view.findViewById<View>(R.id.card_kiblat)?.setOnClickListener {
      startActivity(Intent(activity, KiblatActivity::class.java))
    }

    view.findViewById<View>(R.id.card_khatam)?.setOnClickListener {
      startActivity(Intent(activity, KhatamActivity::class.java))
    }

    view.findViewById<View>(R.id.card_catatan)?.setOnClickListener {
      startActivity(Intent(activity, CatatanActivity::class.java))
    }
  }
}

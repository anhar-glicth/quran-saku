package com.quran.labs.androidquran.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.R

class EventFragment : Fragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_event, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Register Now button for featured event
    view.findViewById<View>(R.id.btn_register_featured)?.setOnClickListener {
      Toast.makeText(context, "Mendaftar ke Meaningful Life in Islam...", Toast.LENGTH_SHORT).show()
    }

    // Join buttons for upcoming events
    view.findViewById<View>(R.id.btn_join_youth)?.setOnClickListener {
      Toast.makeText(context, "Bergabung ke Youth Spiritual Circle...", Toast.LENGTH_SHORT).show()
    }

    view.findViewById<View>(R.id.btn_join_social)?.setOnClickListener {
      Toast.makeText(context, "Bergabung ke Social Care Weekend...", Toast.LENGTH_SHORT).show()
    }

    view.findViewById<View>(R.id.btn_join_fiqh)?.setOnClickListener {
      Toast.makeText(context, "Bergabung ke Fiqh of Daily Life...", Toast.LENGTH_SHORT).show()
    }
  }
}

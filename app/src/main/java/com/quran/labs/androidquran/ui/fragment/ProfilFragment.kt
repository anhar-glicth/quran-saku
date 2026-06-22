package com.quran.labs.androidquran.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.AboutUsActivity
import com.quran.labs.androidquran.HelpActivity
import com.quran.labs.androidquran.QuranPreferenceActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.auth.LoginActivity
import com.quran.labs.androidquran.auth.SessionManager

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

    val session = SessionManager(requireContext())

    // Bind User Info
    val tvUserName = view.findViewById<TextView>(R.id.tv_user_name)
    val tvUserEmail = view.findViewById<TextView>(R.id.tv_user_email)
    val tvUserRole = view.findViewById<TextView>(R.id.tv_user_role)
    val tvAvatarText = view.findViewById<TextView>(R.id.tv_avatar_text)

    if (session.isLoggedIn()) {
      val name = session.getUserName()
      val email = session.getUserEmail()
      val role = session.getUserRole()

      tvUserName?.text = name
      tvUserEmail?.text = " • $email"
      tvUserRole?.text = role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

      // Generate initials for avatar
      val initials = name.split(" ")
          .filter { it.isNotBlank() }
          .take(2)
          .map { it.first().uppercase() }
          .joinToString("")
      tvAvatarText?.text = if (initials.isNotEmpty()) initials else "👤"
    } else {
      tvUserName?.text = "Tamu"
      tvUserEmail?.text = ""
      tvUserRole?.text = "Guest"
      tvAvatarText?.text = "👤"
    }

    // Set up Click Listeners
    view.findViewById<View>(R.id.btn_settings)?.setOnClickListener {
      startActivity(Intent(requireContext(), QuranPreferenceActivity::class.java))
    }

    view.findViewById<View>(R.id.btn_help)?.setOnClickListener {
      startActivity(Intent(requireContext(), HelpActivity::class.java))
    }

    view.findViewById<View>(R.id.btn_about)?.setOnClickListener {
      startActivity(Intent(requireContext(), AboutUsActivity::class.java))
    }

    val performLogout = View.OnClickListener {
      session.logout()
      Toast.makeText(requireContext(), "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
      startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      })
    }

    view.findViewById<View>(R.id.btn_logout_top)?.setOnClickListener(performLogout)
    view.findViewById<View>(R.id.btn_logout)?.setOnClickListener(performLogout)
  }
}

package com.quran.labs.androidquran.ui.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.quran.labs.androidquran.AboutUsActivity
import com.quran.labs.androidquran.HelpActivity
import com.quran.labs.androidquran.QuranPreferenceActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.adhan.AdhanScheduler
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.LoginActivity
import com.quran.labs.androidquran.auth.SessionManager
import com.quran.labs.androidquran.auth.UserData
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.util.ThemeUtil
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private lateinit var session: SessionManager
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvAvatarText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())

        // Bind User Info Views
        tvUserName = view.findViewById(R.id.tv_user_name)
        tvUserEmail = view.findViewById(R.id.tv_user_email)
        tvUserRole = view.findViewById(R.id.tv_user_role)
        tvAvatarText = view.findViewById(R.id.tv_avatar_text)

        updateProfileUI()

        // ─── Mode Gelap Setup ────────────────────────────────
        val switchDarkMode = view.findViewById<Switch>(R.id.switch_dark_mode)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentTheme = prefs.getString(Constants.PREF_APP_THEME, Constants.THEME_LIGHT)
        
        switchDarkMode?.isChecked = currentTheme == Constants.THEME_DARK
        switchDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            val selectedTheme = if (isChecked) Constants.THEME_DARK else Constants.THEME_LIGHT
            prefs.edit().putString(Constants.PREF_APP_THEME, selectedTheme).apply()
            ThemeUtil.setTheme(selectedTheme)
        }

        // ─── Notifikasi Adzan Setup ─────────────────────────
        val switchAdzan = view.findViewById<Switch>(R.id.switch_adzan)
        switchAdzan?.isChecked = AdhanScheduler.isEnabled(requireContext())
        switchAdzan?.setOnCheckedChangeListener { _, isChecked ->
            AdhanScheduler.setEnabled(requireContext(), isChecked)
            val msg = if (isChecked) "Notifikasi adzan diaktifkan" else "Notifikasi adzan dimatikan"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // ─── Edit Profil Click ───────────────────────────────
        view.findViewById<View>(R.id.btn_edit_profil)?.setOnClickListener {
            if (session.isLoggedIn()) {
                showEditProfileDialog()
            } else {
                Toast.makeText(requireContext(), "Harap login terlebih dahulu untuk mengedit profil", Toast.LENGTH_SHORT).show()
            }
        }

        // ─── WhatsApp Customer Service Click ─────────────────
        view.findViewById<View>(R.id.btn_live_chat)?.setOnClickListener {
            try {
                val phoneNumber = "6287822352371"
                val message = "Assalamu'alaikum CS Strava Quran, saya ingin bertanya..."
                val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${android.net.Uri.encode(message)}"
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up other Click Listeners
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

    private fun updateProfileUI() {
        if (session.isLoggedIn()) {
            val name = session.getUserName()
            val email = session.getUserEmail()
            val role = session.getUserRole()

            tvUserName.text = name
            tvUserEmail.text = " • $email"
            tvUserRole.text = role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            // Generate initials for avatar
            val initials = name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
            tvAvatarText.text = if (initials.isNotEmpty()) initials else "👤"
        } else {
            tvUserName.text = "Tamu"
            tvUserEmail.text = ""
            tvUserRole.text = "Guest"
            tvAvatarText.text = "👤"
        }
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_profile_name)
        val etEmail = dialogView.findViewById<EditText>(R.id.et_profile_email)

        etName.setText(session.getUserName())
        etEmail.setText(session.getUserEmail())

        AlertDialog.Builder(requireContext())
            .setTitle("✏️ Edit Profil")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = etName.text.toString().trim()
                val newEmail = etEmail.text.toString().trim()

                if (newName.isNotEmpty() && newEmail.isNotEmpty()) {
                    performProfileUpdate(newName, newEmail)
                } else {
                    Toast.makeText(requireContext(), "Nama dan email tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performProfileUpdate(newName: String, newEmail: String) {
        val userId = session.getUserId()
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.updateProfile(userId = userId, name = newName, email = newEmail)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Update local session manager
                    val updatedUser = UserData(
                        id = userId,
                        name = newName,
                        email = newEmail,
                        role = session.getUserRole()
                    )
                    session.saveUserSession(updatedUser)
                    
                    Toast.makeText(requireContext(), "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    updateProfileUI()
                } else {
                    Toast.makeText(requireContext(), response.body()?.message ?: "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

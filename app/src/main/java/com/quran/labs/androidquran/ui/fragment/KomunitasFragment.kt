package com.quran.labs.androidquran.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quran.labs.androidquran.DoaActivity
import com.quran.labs.androidquran.GrupNgajiActivity
import com.quran.labs.androidquran.PejuangQuranActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.RankActivity
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.SessionManager
import com.quran.labs.androidquran.model.DoaItem
import com.quran.labs.androidquran.model.LeaderboardApiItem
import com.quran.labs.androidquran.ui.adapter.DoaAdapter
import kotlinx.coroutines.launch

class KomunitasFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var doaAdapter: DoaAdapter

    private var rvDoa: RecyclerView? = null
    private var progressDoa: ProgressBar? = null
    private var txtDoaEmpty: TextView? = null
    private var progressLeaderboard: ProgressBar? = null
    private var txtLeaderboardEmpty: TextView? = null

    // Leaderboard views
    private val leaderboardViewIds = listOf(
        Triple(R.id.txt_rank_1, R.id.txt_name_1, Pair(R.id.txt_desc_1, R.id.txt_progress_1)),
        Triple(R.id.txt_rank_2, R.id.txt_name_2, Pair(R.id.txt_desc_2, R.id.txt_progress_2)),
        Triple(R.id.txt_rank_3, R.id.txt_name_3, Pair(R.id.txt_desc_3, R.id.txt_progress_3))
    )
    private val initialsViewIds = listOf(
        Pair(R.id.lay_initials_1, R.id.txt_initials_1),
        Pair(R.id.lay_initials_2, R.id.txt_initials_2),
        Pair(R.id.lay_initials_3, R.id.txt_initials_3)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_komunitas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        // Setup RecyclerView untuk Titip Doa
        rvDoa = view.findViewById(R.id.rv_doa_list)
        progressDoa = view.findViewById(R.id.progress_doa)
        txtDoaEmpty = view.findViewById(R.id.txt_doa_empty)
        progressLeaderboard = view.findViewById(R.id.progress_leaderboard)
        txtLeaderboardEmpty = view.findViewById(R.id.txt_leaderboard_empty)

        doaAdapter = DoaAdapter(
            mutableListOf(),
            onLike = { item, position -> reactToDoa(item, position, "like") },
            onAamiin = { item, position -> reactToDoa(item, position, "aamiin") }
        )

        rvDoa?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = doaAdapter
        }

        // Tombol + Titip Doa
        view.findViewById<View>(R.id.btn_tambah_doa)?.setOnClickListener {
            showTambahDoaDialog()
        }

        // Quick Actions
        view.findViewById<View>(R.id.btn_menu_doa)?.setOnClickListener {
            startActivity(Intent(context, DoaActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_menu_rank)?.setOnClickListener {
            startActivity(Intent(context, RankActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_menu_grup)?.setOnClickListener {
            startActivity(Intent(context, GrupNgajiActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_menu_pejuang)?.setOnClickListener {
            startActivity(Intent(context, PejuangQuranActivity::class.java))
        }

        // Leaderboard toggle
        val txtToggleMingguan = view.findViewById<TextView>(R.id.txt_toggle_mingguan)
        val txtToggleBulanan = view.findViewById<TextView>(R.id.txt_toggle_bulanan)

        txtToggleMingguan?.setOnClickListener {
            setToggleSelected(txtToggleMingguan, txtToggleBulanan)
            fetchLeaderboard(view, "weekly")
        }
        txtToggleBulanan?.setOnClickListener {
            setToggleSelected(txtToggleBulanan, txtToggleMingguan)
            fetchLeaderboard(view, "monthly")
        }

        // Initial load
        fetchDoas()
        fetchLeaderboard(view, "weekly")
    }

    // ─────────────────────────────────────────────────────────────────
    // TITIP DOA — Fetch dari API
    // ─────────────────────────────────────────────────────────────────

    private fun fetchDoas() {
        val userId = sessionManager.getUserId()
        progressDoa?.visibility = View.VISIBLE
        txtDoaEmpty?.visibility = View.GONE
        rvDoa?.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getDoas(userId = userId)
                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    activity?.runOnUiThread {
                        progressDoa?.visibility = View.GONE
                        if (list.isEmpty()) {
                            txtDoaEmpty?.visibility = View.VISIBLE
                            rvDoa?.visibility = View.GONE
                        } else {
                            txtDoaEmpty?.visibility = View.GONE
                            rvDoa?.visibility = View.VISIBLE
                            doaAdapter.replaceAll(list)
                        }
                    }
                } else {
                    showDoaError("Gagal memuat doa (${response.code()})")
                }
            } catch (e: Exception) {
                showDoaError("Tidak dapat terhubung ke server. Pastikan XAMPP aktif.")
            }
        }
    }

    private fun showDoaError(msg: String) {
        activity?.runOnUiThread {
            progressDoa?.visibility = View.GONE
            txtDoaEmpty?.visibility = View.VISIBLE
            txtDoaEmpty?.text = msg
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // REACT (Like / Aamiin) — Kirim ke API
    // ─────────────────────────────────────────────────────────────────

    private fun reactToDoa(item: DoaItem, position: Int, type: String) {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(context, "Login terlebih dahulu untuk bereaksi", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = sessionManager.getUserId()

        val updatedItem = if (type == "like") {
            item.copy(
                isLiked = !item.isLiked,
                likeCount = if (item.isLiked) item.likeCount - 1 else item.likeCount + 1
            )
        } else {
            item.copy(
                isAaminned = !item.isAaminned,
                aamiinCount = if (item.isAaminned) item.aamiinCount - 1 else item.aamiinCount + 1
            )
        }
        doaAdapter.updateItem(position, updatedItem)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.reactToDoa(
                    userId = userId,
                    prayerId = item.id,
                    reactionType = type
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        // Update dengan nilai server yang akurat
                        val serverUpdated = updatedItem.copy(
                            likeCount = body.likeCount,
                            aamiinCount = body.aamiinCount,
                            isLiked = if (type == "like") body.isActive else updatedItem.isLiked,
                            isAaminned = if (type == "aamiin") body.isActive else updatedItem.isAaminned
                        )
                        activity?.runOnUiThread {
                            doaAdapter.updateItem(position, serverUpdated)
                        }
                    }
                }
            } catch (e: Exception) {
                // Rollback optimistic update jika gagal
                activity?.runOnUiThread {
                    doaAdapter.updateItem(position, item) // balik ke semula
                    Toast.makeText(context, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TAMBAH DOA — Dialog Input
    // ─────────────────────────────────────────────────────────────────

    private fun showTambahDoaDialog() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(context, "Login terlebih dahulu untuk titip doa", Toast.LENGTH_SHORT).show()
            return
        }

        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val etDoa = EditText(ctx).apply {
            hint = "Tulis doamu di sini... 🤲"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 6
            background = null
            setHintTextColor(Color.parseColor("#BBBBBB"))
        }
        layout.addView(etDoa)

        AlertDialog.Builder(ctx)
            .setTitle("✍️ Titip Doa")
            .setMessage("Doamu akan dibagikan kepada sesama pengguna Strava Quran")
            .setView(layout)
            .setPositiveButton("Kirim Doa 🤲") { _, _ ->
                val doaText = etDoa.text.toString().trim()
                if (doaText.isNotEmpty()) {
                    kirimDoa(doaText)
                } else {
                    Toast.makeText(ctx, "Doa tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun kirimDoa(latinText: String) {
        val userId = sessionManager.getUserId()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.postDoa(
                    userId = userId,
                    latinText = latinText
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val newDoa = response.body()?.data
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Doa berhasil dikirim! 🤲", Toast.LENGTH_SHORT).show()
                        if (newDoa != null) {
                            txtDoaEmpty?.visibility = View.GONE
                            rvDoa?.visibility = View.VISIBLE
                            doaAdapter.prependItem(newDoa)
                            rvDoa?.scrollToPosition(0)
                        } else {
                            fetchDoas() // refresh ulang jika data tidak ada di response
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Gagal mengirim doa", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Tidak dapat terhubung ke server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // LEADERBOARD — Fetch dari API
    // ─────────────────────────────────────────────────────────────────

    private fun fetchLeaderboard(view: View, period: String) {
        progressLeaderboard?.visibility = View.VISIBLE
        txtLeaderboardEmpty?.visibility = View.GONE
        hideLeaderboardRows(view)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getLeaderboard(period = period)
                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    activity?.runOnUiThread {
                        progressLeaderboard?.visibility = View.GONE
                        if (list.isEmpty()) {
                            txtLeaderboardEmpty?.visibility = View.VISIBLE
                        } else {
                            txtLeaderboardEmpty?.visibility = View.GONE
                            updateLeaderboardUI(view, list)
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        progressLeaderboard?.visibility = View.GONE
                        txtLeaderboardEmpty?.visibility = View.VISIBLE
                        txtLeaderboardEmpty?.text = "Gagal memuat leaderboard"
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressLeaderboard?.visibility = View.GONE
                    txtLeaderboardEmpty?.visibility = View.VISIBLE
                    txtLeaderboardEmpty?.text = "Tidak dapat terhubung ke server"
                }
            }
        }
    }

    private fun hideLeaderboardRows(view: View) {
        for (i in 1..3) {
            val rankId = resources.getIdentifier("txt_rank_$i", "id", context?.packageName)
            val nameId = resources.getIdentifier("txt_name_$i", "id", context?.packageName)
            val descId = resources.getIdentifier("txt_desc_$i", "id", context?.packageName)
            val progId = resources.getIdentifier("txt_progress_$i", "id", context?.packageName)
            val initLayId = resources.getIdentifier("lay_initials_$i", "id", context?.packageName)
            view.findViewById<View>(rankId)?.visibility = View.INVISIBLE
            view.findViewById<View>(nameId)?.visibility = View.INVISIBLE
            view.findViewById<View>(descId)?.visibility = View.INVISIBLE
            view.findViewById<View>(progId)?.visibility = View.INVISIBLE
            view.findViewById<View>(initLayId)?.visibility = View.INVISIBLE
        }
    }

    private fun updateLeaderboardUI(view: View, items: List<LeaderboardApiItem>) {
        val medals = listOf("🥇", "🥈", "🥉")
        val avatarColors = listOf(
            "#FDF2E9" to "#E28743",
            "#EBF5FB" to "#2980B9",
            "#E9F7EF" to "#27AE60"
        )
        val myUserId = sessionManager.getUserId()

        for (i in 0 until minOf(3, items.size)) {
            val item = items[i]
            val ctx = context ?: break

            val rankId   = resources.getIdentifier("txt_rank_${ i+1 }", "id", ctx.packageName)
            val nameId   = resources.getIdentifier("txt_name_${ i+1 }", "id", ctx.packageName)
            val descId   = resources.getIdentifier("txt_desc_${ i+1 }", "id", ctx.packageName)
            val progId   = resources.getIdentifier("txt_progress_${ i+1 }", "id", ctx.packageName)
            val layInitId= resources.getIdentifier("lay_initials_${ i+1 }", "id", ctx.packageName)
            val txtInitId= resources.getIdentifier("txt_initials_${ i+1 }", "id", ctx.packageName)

            val tvRank    = view.findViewById<TextView>(rankId)
            val tvName    = view.findViewById<TextView>(nameId)
            val tvDesc    = view.findViewById<TextView>(descId)
            val tvProg    = view.findViewById<TextView>(progId)
            val layInit   = view.findViewById<View>(layInitId)
            val tvInit    = view.findViewById<TextView>(txtInitId)

            tvRank?.visibility = View.VISIBLE
            tvName?.visibility = View.VISIBLE
            tvDesc?.visibility = View.VISIBLE
            tvProg?.visibility = View.VISIBLE
            layInit?.visibility = View.VISIBLE

            tvRank?.text = medals.getOrElse(i) { "${i + 1}" }
            tvName?.text = item.userName
            tvDesc?.text = "${item.activeDays} hari aktif • ${item.totalPages} hlm"
            tvProg?.text = "${item.totalMinutes}"

            // Highlight baris user sendiri
            if (item.userId == myUserId) {
                tvName?.setTextColor(Color.parseColor("#FF6D00"))
                tvProg?.setTextColor(Color.parseColor("#FF6D00"))
            } else {
                tvName?.setTextColor(Color.parseColor("#212121"))
                tvProg?.setTextColor(Color.parseColor("#783A06"))
            }

            // Avatar warna
            val colorPair = avatarColors[i % avatarColors.size]
            layInit?.background?.setTint(Color.parseColor(colorPair.first))
            tvInit?.text = item.initials
            tvInit?.setTextColor(Color.parseColor(colorPair.second))
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────

    private fun setToggleSelected(selected: TextView?, unselected: TextView?) {
        selected?.setBackgroundResource(R.drawable.bg_segmented_selected)
        selected?.setTextColor(Color.WHITE)
        unselected?.setBackgroundResource(0)
        unselected?.setTextColor(Color.parseColor("#666666"))
    }
}

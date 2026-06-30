package com.quran.labs.androidquran

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.SessionManager
import com.quran.labs.androidquran.model.LeaderboardApiItem
import kotlinx.coroutines.launch

class RankActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var rvRank: RecyclerView
    private lateinit var progressRank: ProgressBar
    private lateinit var txtEmpty: TextView
    private lateinit var cardLeaderboard: View
    private lateinit var cardUserRank: View

    private lateinit var tvMyRank: TextView
    private lateinit var tvMyInitials: TextView
    private lateinit var tvMyName: TextView
    private lateinit var tvMyStat: TextView
    private lateinit var tvMyScore: TextView

    private val rankItems = mutableListOf<LeaderboardApiItem>()
    private lateinit var adapter: RankListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rank)

        sessionManager = SessionManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "Peringkat"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Bind views
        rvRank = findViewById(R.id.rv_rank)
        progressRank = findViewById(R.id.progress_rank)
        txtEmpty = findViewById(R.id.txt_rank_empty)
        cardLeaderboard = findViewById(R.id.card_leaderboard)
        cardUserRank = findViewById(R.id.card_user_rank)

        tvMyRank = findViewById(R.id.tv_my_rank)
        tvMyInitials = findViewById(R.id.tv_my_initials)
        tvMyName = findViewById(R.id.tv_my_name)
        tvMyStat = findViewById(R.id.tv_my_stat)
        tvMyScore = findViewById(R.id.tv_my_score)

        rvRank.layoutManager = LinearLayoutManager(this)
        adapter = RankListAdapter(rankItems, sessionManager.getUserId())
        rvRank.adapter = adapter

        // Setup Toggles
        val txtToggleMingguan = findViewById<TextView>(R.id.txt_toggle_mingguan)
        val txtToggleBulanan = findViewById<TextView>(R.id.txt_toggle_bulanan)

        fun selectToggle(selected: TextView, unselected: TextView, period: String) {
            selected.setBackgroundResource(R.drawable.bg_segmented_selected)
            selected.setTextColor(Color.WHITE)
            unselected.setBackgroundResource(0)
            unselected.setTextColor(Color.parseColor("#666666"))
            loadRankData(period)
        }

        txtToggleMingguan.setOnClickListener {
            selectToggle(txtToggleMingguan, txtToggleBulanan, "weekly")
        }

        txtToggleBulanan.setOnClickListener {
            selectToggle(txtToggleBulanan, txtToggleMingguan, "monthly")
        }

        // Initial load
        loadRankData("weekly")
    }

    private fun loadRankData(period: String) {
        progressRank.visibility = View.VISIBLE
        txtEmpty.visibility = View.GONE
        cardLeaderboard.visibility = View.GONE
        cardUserRank.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getLeaderboard(period = period, limit = 50)
                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    runOnUiThread {
                        progressRank.visibility = View.GONE
                        if (list.isEmpty()) {
                            txtEmpty.visibility = View.VISIBLE
                        } else {
                            cardLeaderboard.visibility = View.VISIBLE
                            rankItems.clear()
                            rankItems.addAll(list)
                            adapter.notifyDataSetChanged()
                            setupStickyUserCard(list)
                        }
                    }
                } else {
                    showError("Gagal memuat peringkat (${response.code()})")
                }
            } catch (e: Exception) {
                showError("Tidak dapat terhubung ke server. Pastikan XAMPP aktif.")
            }
        }
    }

    private fun setupStickyUserCard(list: List<LeaderboardApiItem>) {
        val myUserId = sessionManager.getUserId()
        if (myUserId <= 0) return

        val myItem = list.find { it.userId == myUserId }
        if (myItem != null) {
            cardUserRank.visibility = View.VISIBLE
            tvMyRank.text = "#${myItem.rank}"
            tvMyInitials.text = myItem.initials
            tvMyName.text = myItem.userName
            tvMyStat.text = "${myItem.activeDays} hari aktif • ${myItem.totalPages} halaman"
            tvMyScore.text = myItem.totalMinutes.toString()
        } else {
            // User tidak ada di leaderboard periode ini
            if (sessionManager.isLoggedIn()) {
                cardUserRank.visibility = View.VISIBLE
                tvMyRank.text = "#-"
                tvMyInitials.text = sessionManager.getUserName().take(2).uppercase()
                tvMyName.text = sessionManager.getUserName()
                tvMyStat.text = "Mulai tilawah untuk masuk peringkat!"
                tvMyScore.text = "0"
            }
        }
    }

    private fun showError(msg: String) {
        runOnUiThread {
            progressRank.visibility = View.GONE
            txtEmpty.visibility = View.VISIBLE
            txtEmpty.text = msg
            cardUserRank.visibility = View.GONE
            cardLeaderboard.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    inner class RankListAdapter(
        private val list: List<LeaderboardApiItem>,
        private val currentUserId: Int
    ) : RecyclerView.Adapter<RankListAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val layRow: View = view.findViewById(R.id.lay_rank_row)
            val txtRankNum: TextView = view.findViewById(R.id.txt_rank_num)
            val layAvatar: View = view.findViewById(R.id.lay_rank_avatar)
            val txtInitials: TextView = view.findViewById(R.id.txt_rank_initials)
            val txtName: TextView = view.findViewById(R.id.txt_rank_name)
            val txtDesc: TextView = view.findViewById(R.id.txt_rank_desc)
            val txtScore: TextView = view.findViewById(R.id.txt_rank_score)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_rank_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]

            val medals = listOf("🥇", "🥈", "🥉")
            if (position < 3) {
                holder.txtRankNum.text = medals[position]
                holder.txtRankNum.textSize = 18f
            } else {
                holder.txtRankNum.text = "${position + 1}"
                holder.txtRankNum.textSize = 14f
            }

            holder.txtName.text = item.userName
            holder.txtDesc.text = "${item.activeDays} hari aktif • ${item.totalPages} hlm"
            holder.txtScore.text = item.totalMinutes.toString()
            holder.txtInitials.text = item.initials

            // Avatar colors
            val avatarColors = listOf(
                "#FDF2E9" to "#E28743",
                "#EBF5FB" to "#2980B9",
                "#E9F7EF" to "#27AE60"
            )
            val colorPair = avatarColors[position % avatarColors.size]
            holder.layAvatar.background?.setTint(Color.parseColor(colorPair.first))
            holder.txtInitials.setTextColor(Color.parseColor(colorPair.second))

            // Highlight current user
            if (item.userId == currentUserId) {
                holder.layRow.setBackgroundColor(Color.parseColor("#FFF3E0")) // Orange tint
                holder.txtName.setTextColor(Color.parseColor("#FF6D00"))
            } else {
                holder.layRow.setBackgroundColor(Color.TRANSPARENT)
                holder.txtName.setTextColor(Color.parseColor("#212121"))
            }
        }

        override fun getItemCount(): Int = list.size
    }
}

package com.quran.labs.androidquran.ui.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.R
import androidx.lifecycle.lifecycleScope
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StravaQuranFragment : Fragment() {

    private var isDurationMode = true // true: duration, false: pages
    private var cachedCompetitors: List<LeaderboardItem>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_strava_quran, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupStreak(view)
        setupChart(view)
        setupToggleButtons(view)
    }

    private fun setupToggleButtons(rootView: View) {
        val btnDuration = rootView.findViewById<TextView>(R.id.btn_mode_duration)
        val btnPages = rootView.findViewById<TextView>(R.id.btn_mode_pages)

        btnDuration.setOnClickListener {
            if (!isDurationMode) {
                isDurationMode = true
                updateToggleUI(btnDuration, btnPages)
                setupChart(rootView)
            }
        }

        btnPages.setOnClickListener {
            if (isDurationMode) {
                isDurationMode = false
                updateToggleUI(btnDuration, btnPages)
                setupChart(rootView)
            }
        }

        updateToggleUI(btnDuration, btnPages)
    }

    private fun updateToggleUI(btnDuration: TextView, btnPages: TextView) {
        if (isDurationMode) {
            btnDuration.setTextColor(Color.WHITE)
            btnDuration.background = rootViewBackground(0xFFFF6D00.toInt())
            
            btnPages.setTextColor(Color.parseColor("#666666"))
            btnPages.background = null
        } else {
            btnPages.setTextColor(Color.WHITE)
            btnPages.background = rootViewBackground(0xFFFF6D00.toInt())
            
            btnDuration.setTextColor(Color.parseColor("#666666"))
            btnDuration.background = null
        }
    }

    private fun rootViewBackground(color: Int): android.graphics.drawable.GradientDrawable {
        val shape = android.graphics.drawable.GradientDrawable()
        shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        shape.cornerRadius = 8f * resources.displayMetrics.density
        shape.setColor(color)
        return shape
    }

    private fun setupStreak(rootView: View) {
        val context = context ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        var streak = 0
        val cal = Calendar.getInstance()
        
        // Loop back up to 30 days to calculate consecutive reading streak
        for (i in 0 until 30) {
            val dateStr = dateFormat.format(cal.time)
            val dur = prefs.getLong("strava_duration_$dateStr", 0L)
            
            if (dur > 0) {
                streak++
            } else {
                // If it is today and duration is 0, we can continue checking yesterday in case they haven't read today yet
                if (i == 0) {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    continue
                }
                break
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val txtStreakTitle = rootView.findViewById<TextView>(R.id.txt_streak_title)
        if (streak > 0) {
            txtStreakTitle.text = "🔥 $streak Hari Beruntun!"
        } else {
            txtStreakTitle.text = "🌱 Mulai Mengaji Hari Ini!"
        }
    }

    private fun setupChart(rootView: View) {
        val context = context ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.forLanguageTag("id")) // Indonesia day names: Sen, Sel, etc.

        // Get the rolling 7 days (ending today)
        val days = mutableListOf<DayData>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6) // Start 6 days ago

        var totalWeeklyDuration = 0L
        var totalWeeklyPages = 0L

        for (i in 0..6) {
            val dateStr = dateFormat.format(cal.time)
            val dayName = dayLabelFormat.format(cal.time)
            
            val durationSeconds = prefs.getLong("strava_duration_$dateStr", 0L)
            val durationMinutes = (durationSeconds + 59) / 60 // Round up to nearest minute
            val pagesRead = prefs.getInt("strava_pages_$dateStr", 0)
            
            totalWeeklyDuration += durationMinutes
            totalWeeklyPages += pagesRead
            
            days.add(DayData(dayName, durationMinutes, pagesRead.toLong()))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Find max value for scaling bar heights
        val maxValue = days.maxOfOrNull { if (isDurationMode) it.duration else it.pages } ?: 0L
        val limitMax = if (maxValue == 0L) 1L else maxValue

        val density = resources.displayMetrics.density
        val maxBarHeightDp = 100f // Maximum visual height of bar
        val minBarHeightDp = 6f // Minimum visual height so it's not completely invisible

        for (i in 0..6) {
            val dayData = days[i]
            
            val valViewId = resources.getIdentifier("txt_val_${i + 1}", "id", context.packageName)
            val barViewId = resources.getIdentifier("bar_day_${i + 1}", "id", context.packageName)
            val lblViewId = resources.getIdentifier("txt_lbl_${i + 1}", "id", context.packageName)

            val tvVal = rootView.findViewById<TextView>(valViewId)
            val vBar = rootView.findViewById<View>(barViewId)
            val tvLbl = rootView.findViewById<TextView>(lblViewId)

            if (tvVal != null && vBar != null && tvLbl != null) {
                tvLbl.text = dayData.name
                
                val rawValue = if (isDurationMode) dayData.duration else dayData.pages
                tvVal.text = if (isDurationMode) "${rawValue}m" else "${rawValue}hlm"
                
                // Calculate dynamic height
                val heightDp = if (rawValue == 0L) {
                    minBarHeightDp
                } else {
                    minBarHeightDp + ((rawValue.toFloat() / limitMax) * (maxBarHeightDp - minBarHeightDp))
                }
                
                val params = vBar.layoutParams
                params.height = (heightDp * density).toInt()
                vBar.layoutParams = params
                
                // Highlight today's bar with custom color
                if (i == 6) {
                    vBar.backgroundTintList = ColorStateList.valueOf(0xFFFF6D00.toInt()) // Today: Orange
                    tvLbl.setTypeface(null, android.graphics.Typeface.BOLD)
                    tvLbl.setTextColor(0xFFFF6D00.toInt())
                } else {
                    vBar.backgroundTintList = ColorStateList.valueOf(0xFF80CBC4.toInt()) // Others: Teal
                    tvLbl.setTypeface(null, android.graphics.Typeface.NORMAL)
                    tvLbl.setTextColor(0xFF999999.toInt())
                }
            }
        }

        // Setup the dynamic leaderboard
        setupLeaderboard(rootView, totalWeeklyDuration, totalWeeklyPages)
    }

    private fun setupLeaderboard(rootView: View, myWeeklyDuration: Long, myWeeklyPages: Long) {
        val context = context ?: return
        val sessionManager = SessionManager(context)
        val currentUserId = sessionManager.getUserId()
        
        val myName = if (sessionManager.isLoggedIn()) {
            sessionManager.getUserName()
        } else {
            "Saya (Anda)"
        }

        fun getLevel(dur: Long): String = when {
            dur >= 60 -> "Level 4"
            dur >= 30 -> "Level 3"
            dur >= 15 -> "Level 2"
            else -> "Level 1"
        }

        // Initial local user entry
        val myLevel = getLevel(myWeeklyDuration)
        val initialList = mutableListOf(
            LeaderboardItem(myName, myWeeklyDuration, myWeeklyPages, myLevel, true)
        )

        val cached = cachedCompetitors
        if (cached != null) {
            renderLeaderboardUI(rootView, cached)
        } else {
            renderLeaderboardUI(rootView, initialList)
        }

        // Fetch real server users from backend API
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getLeaderboard(period = "weekly", limit = 10)
                if (response.isSuccessful) {
                    val apiItems = response.body()?.data
                    if (!apiItems.isNullOrEmpty()) {
                        val realList = mutableListOf<LeaderboardItem>()
                        var meFoundInApi = false

                        for (item in apiItems) {
                            val isMe = (currentUserId > 0 && item.userId == currentUserId)
                            if (isMe) meFoundInApi = true
                            
                            val nameStr = if (isMe) myName else item.userName
                            val levelStr = getLevel(item.totalMinutes.toLong())
                            realList.add(
                                LeaderboardItem(
                                    name = nameStr,
                                    duration = item.totalMinutes.toLong(),
                                    pages = item.totalPages.toLong(),
                                    level = levelStr,
                                    isMe = isMe
                                )
                            )
                        }

                        if (!meFoundInApi && sessionManager.isLoggedIn()) {
                            realList.add(LeaderboardItem(myName, myWeeklyDuration, myWeeklyPages, myLevel, true))
                        }

                        cachedCompetitors = realList
                        renderLeaderboardUI(rootView, realList)
                    }
                }
            } catch (_: Exception) {
                // Network/API fallback
            }
        }
    }

    private fun renderLeaderboardUI(rootView: View, list: List<LeaderboardItem>) {
        val context = context ?: return
        val sortedList = if (isDurationMode) {
            list.sortedByDescending { it.duration }
        } else {
            list.sortedByDescending { it.pages }
        }

        val medals = listOf("🥇", "🥈", "🥉", "4️⃣", "5️⃣")

        for (i in 0..2) {
            val rankId = resources.getIdentifier("txt_rank_${i + 1}", "id", context.packageName)
            val nameId = resources.getIdentifier("name_rank_${i + 1}", "id", context.packageName)
            val descId = resources.getIdentifier("desc_rank_${i + 1}", "id", context.packageName)
            val valId = resources.getIdentifier("val_rank_${i + 1}", "id", context.packageName)
            val bgId = resources.getIdentifier("bg_rank_${i + 1}", "id", context.packageName)

            val tvRank = rootView.findViewById<TextView>(rankId)
            val tvName = rootView.findViewById<TextView>(nameId)
            val tvDesc = rootView.findViewById<TextView>(descId)
            val tvVal = rootView.findViewById<TextView>(valId)
            val vBg = rootView.findViewById<View>(bgId)

            if (tvRank != null && tvName != null && tvDesc != null && tvVal != null && vBg != null) {
                if (i < sortedList.size) {
                    val item = sortedList[i]
                    tvRank.text = medals.getOrElse(i) { "${i + 1}" }
                    tvName.text = item.name
                    
                    val km = String.format(Locale.US, "%.1f", item.pages * 0.4f)
                    tvDesc.text = "${item.level} • $km km (halaman)"
                    tvVal.text = if (isDurationMode) "${item.duration} Menit" else "${item.pages} Halaman"

                    if (item.isMe) {
                        vBg.setBackgroundColor(0xFFFFF3E0.toInt()) // Light Orange
                    } else {
                        vBg.setBackgroundColor(Color.WHITE)
                    }
                    vBg.visibility = View.VISIBLE
                } else {
                    vBg.visibility = View.INVISIBLE
                }
            }
        }
    }

    data class LeaderboardItem(
        val name: String,
        val duration: Long,
        val pages: Long,
        val level: String,
        val isMe: Boolean
    )

    data class DayData(
        val name: String,
        val duration: Long,
        val pages: Long
    )

    companion object {
        fun newInstance(): StravaQuranFragment = StravaQuranFragment()
    }
}

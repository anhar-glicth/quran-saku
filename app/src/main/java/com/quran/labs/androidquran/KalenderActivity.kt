package com.quran.labs.androidquran

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quran.labs.androidquran.adhan.HijriCalendarHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class KalenderActivity : AppCompatActivity() {

    private lateinit var tvMonthTitle: TextView
    private lateinit var tvHijriRange: TextView
    private lateinit var rvCalendarGrid: RecyclerView
    private lateinit var adapter: CalendarGridAdapter

    private lateinit var tvDetailDateGregorian: TextView
    private lateinit var tvDetailDateHijri: TextView
    private lateinit var tvDetailFastingName: TextView
    private lateinit var tvDetailFastingDesc: TextView
    private lateinit var tvDetailFastingBadge: TextView
    private lateinit var layoutFastingBadge: View

    private var currentMonthCalendar: Calendar = Calendar.getInstance()
    private var selectedDate: Calendar = Calendar.getInstance()

    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    private val fullDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kalender)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Bind Views
        tvMonthTitle = findViewById(R.id.tv_month_title)
        tvHijriRange = findViewById(R.id.tv_hijri_range)
        rvCalendarGrid = findViewById(R.id.rv_calendar_grid)

        tvDetailDateGregorian = findViewById(R.id.tv_detail_date_gregorian)
        tvDetailDateHijri = findViewById(R.id.tv_detail_date_hijri)
        tvDetailFastingName = findViewById(R.id.tv_detail_fasting_name)
        tvDetailFastingDesc = findViewById(R.id.tv_detail_fasting_desc)
        tvDetailFastingBadge = findViewById(R.id.tv_detail_fasting_badge)
        layoutFastingBadge = findViewById(R.id.layout_fasting_badge)

        // Setup Grid RecyclerView
        rvCalendarGrid.layoutManager = GridLayoutManager(this, 7)
        adapter = CalendarGridAdapter(this) { day ->
            day.date?.let {
                selectedDate = it
                updateDetailCard(day)
                adapter.notifyDataSetChanged()
            }
        }
        rvCalendarGrid.adapter = adapter

        // Bind Switch Buttons
        findViewById<ImageButton>(R.id.btn_prev_month).setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, -1)
            loadMonthData()
        }
        findViewById<ImageButton>(R.id.btn_next_month).setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, 1)
            loadMonthData()
        }

        // Initialize display with current month and selected day (today)
        loadMonthData()
        
        // Find and select today's day item to pre-populate details
        val todayDay = adapter.days.firstOrNull { 
            it.date != null && isSameDay(it.date, selectedDate)
        }
        todayDay?.let { updateDetailCard(it) } ?: run {
            // Fallback detail card
            val year = selectedDate.get(Calendar.YEAR)
            val month = selectedDate.get(Calendar.MONTH)
            val day = selectedDate.get(Calendar.DAY_OF_MONTH)
            val dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK)
            val (_, hMonth, hDay) = HijriCalendarHelper.gregorianToHijri(year, month + 1, day)
            val fastingInfo = HijriCalendarHelper.getFastingInfo(year, month + 1, day, dayOfWeek)
            updateDetailCardForCustomDate(selectedDate, hDay, hMonth, fastingInfo)
        }
    }

    private fun loadMonthData() {
        // Month Title
        tvMonthTitle.text = monthFormat.format(currentMonthCalendar.time)

        // Generate Days
        val days = generateDaysForMonth(currentMonthCalendar)
        adapter.setDaysList(days)

        // Find Hijri month range
        val activeDays = days.filter { it.date != null }
        if (activeDays.isNotEmpty()) {
            val firstDay = activeDays.first().date!!
            val lastDay = activeDays.last().date!!
            
            val (_, fMonth, _) = HijriCalendarHelper.gregorianToHijri(
                firstDay.get(Calendar.YEAR), firstDay.get(Calendar.MONTH) + 1, firstDay.get(Calendar.DAY_OF_MONTH)
            )
            val (lYear, lMonth, _) = HijriCalendarHelper.gregorianToHijri(
                lastDay.get(Calendar.YEAR), lastDay.get(Calendar.MONTH) + 1, lastDay.get(Calendar.DAY_OF_MONTH)
            )

            val firstMonthName = HijriCalendarHelper.getMonthName(fMonth)
            val lastMonthName = HijriCalendarHelper.getMonthName(lMonth)
            
            tvHijriRange.text = if (firstMonthName == lastMonthName) {
                "$firstMonthName $lYear H"
            } else {
                "$firstMonthName - $lastMonthName $lYear H"
            }
        }
    }

    private fun generateDaysForMonth(calendar: Calendar): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()

        val cal = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Sunday = 1, Monday = 2. We want leading empty cells matching (firstDayOfWeek - 1)
        val leadingEmptyCells = firstDayOfWeek - 1

        for (i in 0 until leadingEmptyCells) {
            days.add(CalendarDay(0, null, null, false, false, null))
        }

        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..totalDays) {
            val dayCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
            val (_, hMonth, hDay) = HijriCalendarHelper.gregorianToHijri(year, month + 1, day)
            val fastingInfo = HijriCalendarHelper.getFastingInfo(year, month + 1, day, dayOfWeek)

            days.add(CalendarDay(
                dayNumber = day,
                hijriDayNumber = hDay,
                date = dayCal,
                isCurrentMonth = true,
                isFasting = fastingInfo.isFasting,
                fastingInfo = fastingInfo
            ))
        }

        return days
    }

    private fun updateDetailCard(day: CalendarDay) {
        val date = day.date ?: return
        val hDay = day.hijriDayNumber ?: 1
        val fastingInfo = day.fastingInfo ?: return

        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH)
        val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)
        val (_, hMonth, _) = HijriCalendarHelper.gregorianToHijri(year, month + 1, dayOfMonth)

        updateDetailCardForCustomDate(date, hDay, hMonth, fastingInfo)
    }

    private fun updateDetailCardForCustomDate(
        date: Calendar,
        hijriDay: Int,
        hijriMonth: Int,
        fastingInfo: HijriCalendarHelper.FastingInfo
    ) {
        tvDetailDateGregorian.text = fullDateFormat.format(date.time)
        tvDetailDateHijri.text = "$hijriDay ${HijriCalendarHelper.getMonthName(hijriMonth)} ${HijriCalendarHelper.gregorianToHijri(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH)).first} H"

        tvDetailFastingBadge.text = fastingInfo.badge
        tvDetailFastingName.text = fastingInfo.name
        tvDetailFastingDesc.text = fastingInfo.description

        // Customize badge color based on type
        when (fastingInfo.badge) {
            "Puasa Wajib" -> {
                layoutFastingBadge.setBackgroundColor(0xFF2E7D32.toInt()) // Green
                layoutFastingBadge.visibility = View.VISIBLE
            }
            "Puasa Sunnah" -> {
                layoutFastingBadge.setBackgroundColor(0xFFFF6D00.toInt()) // Orange
                layoutFastingBadge.visibility = View.VISIBLE
            }
            "Diharamkan" -> {
                layoutFastingBadge.setBackgroundColor(0xFFD32F2F.toInt()) // Red
                layoutFastingBadge.visibility = View.VISIBLE
            }
            else -> {
                layoutFastingBadge.visibility = View.GONE
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    // --- Inner Grid Cell Adapter ---
    class CalendarGridAdapter(
        private val context: Context,
        private val onItemClick: (CalendarDay) -> Unit
    ) : RecyclerView.Adapter<CalendarGridAdapter.DayViewHolder>() {

        val days = ArrayList<CalendarDay>()

        fun setDaysList(list: List<CalendarDay>) {
            days.clear()
            days.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val v = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false)
            return DayViewHolder(v)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val day = days[position]
            holder.bind(day, context as KalenderActivity)
        }

        override fun getItemCount(): Int = days.size

        inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvGregorian: TextView = itemView.findViewById(R.id.tv_gregorian_day)
            private val tvHijri: TextView = itemView.findViewById(R.id.tv_hijri_day)
            private val viewBg: View = itemView.findViewById(R.id.view_day_background)
            private val viewSelection: View = itemView.findViewById(R.id.view_day_selection)

            fun bind(day: CalendarDay, activity: KalenderActivity) {
                if (day.dayNumber == 0) {
                    tvGregorian.text = ""
                    tvHijri.text = ""
                    viewBg.setBackgroundResource(0)
                    viewSelection.setBackgroundResource(0)
                    itemView.isClickable = false
                    return
                }

                itemView.isClickable = true
                tvGregorian.text = day.dayNumber.toString()
                tvHijri.text = day.hijriDayNumber?.toString() ?: ""

                val date = day.date!!

                // Set Text Color red if Sunday
                if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    tvGregorian.setTextColor(0xFFD32F2F.toInt())
                } else {
                    tvGregorian.setTextColor(0xFF212121.toInt())
                }

                // Highlight Fasting Day
                if (day.isFasting) {
                    viewBg.setBackgroundResource(R.drawable.bg_calendar_day_fasting)
                } else {
                    viewBg.setBackgroundResource(0)
                }

                // Highlight Selected Day
                if (activity.isSameDay(date, activity.selectedDate)) {
                    viewSelection.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                } else {
                    viewSelection.setBackgroundResource(0)
                }

                itemView.setOnClickListener {
                    onItemClick(day)
                }
            }
        }
    }

    data class CalendarDay(
        val dayNumber: Int,
        val hijriDayNumber: Int?,
        val date: Calendar?,
        val isCurrentMonth: Boolean,
        val isFasting: Boolean,
        val fastingInfo: HijriCalendarHelper.FastingInfo?
    )
}

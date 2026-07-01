package com.quran.labs.androidquran

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quran.labs.androidquran.adhan.HijriCalendarHelper
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.SessionManager
import com.quran.labs.androidquran.model.EventItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KalenderActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
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

    // Event views
    private lateinit var viewEventDivider: View
    private lateinit var layoutEventDetail: View
    private lateinit var layoutDynamicEventsContainer: LinearLayout
    private lateinit var btnManageEvent: Button

    private var currentMonthCalendar: Calendar = Calendar.getInstance()
    private var selectedDate: Calendar = Calendar.getInstance()

    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    private val fullDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val monthEvents = ArrayList<EventItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kalender)

        sessionManager = SessionManager(this)

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

        // Event views bind
        viewEventDivider = findViewById(R.id.view_event_divider)
        layoutEventDetail = findViewById(R.id.layout_event_detail)
        layoutDynamicEventsContainer = findViewById(R.id.layout_dynamic_events_container)
        btnManageEvent = findViewById(R.id.btn_manage_date_event)

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

        // Setup Admin controls
        if (sessionManager.isLoggedIn() && sessionManager.getUserRole() == "admin") {
            btnManageEvent.visibility = View.VISIBLE
            btnManageEvent.setOnClickListener {
                val formattedDate = dbDateFormat.format(selectedDate.time)
                val existingEvent = monthEvents.find { it.eventDate == formattedDate }
                showAddEventDialog(existingEvent, formattedDate)
            }
        } else {
            btnManageEvent.visibility = View.GONE
        }

        // Initialize display with current month and selected day (today)
        loadMonthData()
    }

    private fun loadMonthData() {
        // Month Title
        tvMonthTitle.text = monthFormat.format(currentMonthCalendar.time)

        val year = currentMonthCalendar.get(Calendar.YEAR)
        val month = currentMonthCalendar.get(Calendar.MONTH) + 1 // 1-indexed

        // Fetch monthly events from API
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getMonthlyEvents(year = year, month = month)
                if (response.isSuccessful) {
                    val events = response.body()?.data ?: emptyList()
                    runOnUiThread {
                        monthEvents.clear()
                        monthEvents.addAll(events)
                        renderCalendarGrid()
                    }
                } else {
                    runOnUiThread { renderCalendarGrid() }
                }
            } catch (e: Exception) {
                runOnUiThread { renderCalendarGrid() }
            }
        }
    }

    private fun renderCalendarGrid() {
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

        // Select selected date details
        val selectedDay = adapter.days.firstOrNull { 
            it.date != null && isSameDay(it.date, selectedDate)
        }
        selectedDay?.let { updateDetailCard(it) } ?: run {
            val y = selectedDate.get(Calendar.YEAR)
            val m = selectedDate.get(Calendar.MONTH)
            val d = selectedDate.get(Calendar.DAY_OF_MONTH)
            val dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK)
            val (_, hMonth, hDay) = HijriCalendarHelper.gregorianToHijri(y, m + 1, d)
            val fastingInfo = HijriCalendarHelper.getFastingInfo(y, m + 1, d, dayOfWeek)
            updateDetailCardForCustomDate(selectedDate, hDay, hMonth, fastingInfo)
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
        val leadingEmptyCells = firstDayOfWeek - 1

        for (i in 0 until leadingEmptyCells) {
            days.add(CalendarDay(0, null, null, false, false, null, false))
        }

        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..totalDays) {
            val dayCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
            val (_, hMonth, hDay) = HijriCalendarHelper.gregorianToHijri(year, month + 1, day)
            val fastingInfo = HijriCalendarHelper.getFastingInfo(year, month + 1, day, dayOfWeek)

            val formattedDate = dbDateFormat.format(dayCal.time)
            val hasEvent = monthEvents.any { it.eventDate == formattedDate }

            days.add(CalendarDay(
                dayNumber = day,
                hijriDayNumber = hDay,
                date = dayCal,
                isCurrentMonth = true,
                isFasting = fastingInfo.isFasting,
                fastingInfo = fastingInfo,
                hasEvent = hasEvent
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showAdminEventOptionsDialog(event: EventItem, dateStr: String) {
        val options = arrayOf("✏️ Edit Kegiatan", "❌ Hapus Kegiatan")
        AlertDialog.Builder(this)
            .setTitle("Pilihan Admin")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddEventDialog(event, dateStr)
                    1 -> confirmDeleteEvent(event)
                }
            }
            .show()
    }

    private fun confirmDeleteEvent(event: EventItem) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kegiatan")
            .setMessage("Apakah Anda yakin ingin menghapus kegiatan \"${event.title}\"?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                deleteCalendarEvent(event.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteCalendarEvent(id: Int) {
        val adminUserId = sessionManager.getUserId()
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.deleteEvent(
                    userId = adminUserId,
                    id = id
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    runOnUiThread {
                        Toast.makeText(this@KalenderActivity, "Kegiatan berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadMonthData()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@KalenderActivity, "Gagal menghapus kegiatan: " + response.body()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@KalenderActivity, "Koneksi server gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

        // Customize fasting badge color
        when (fastingInfo.badge) {
            "Puasa Wajib" -> {
                layoutFastingBadge.setBackgroundColor(0xFF2E7D32.toInt())
                layoutFastingBadge.visibility = View.VISIBLE
            }
            "Puasa Sunnah" -> {
                layoutFastingBadge.setBackgroundColor(0xFFFF6D00.toInt())
                layoutFastingBadge.visibility = View.VISIBLE
            }
            "Diharamkan" -> {
                layoutFastingBadge.setBackgroundColor(0xFFD32F2F.toInt())
                layoutFastingBadge.visibility = View.VISIBLE
            }
            else -> {
                layoutFastingBadge.visibility = View.GONE
            }
        }

        // Bind event detail if any
        val formattedDate = dbDateFormat.format(date.time)
        val eventsForDay = monthEvents.filter { it.eventDate == formattedDate }

        layoutDynamicEventsContainer.removeAllViews()

        if (eventsForDay.isNotEmpty()) {
            viewEventDivider.visibility = View.VISIBLE
            layoutEventDetail.visibility = View.VISIBLE

            for (event in eventsForDay) {
                val eventContainer = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(12))
                    }
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                }

                val tvTitle = TextView(this).apply {
                    text = event.title
                    setTextColor(Color.parseColor("#212121"))
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                val tvSpeaker = TextView(this).apply {
                    text = "Pembicara: ${event.speaker}"
                    setTextColor(Color.parseColor("#555555"))
                    textSize = 12f
                    setPadding(0, dp(2), 0, 0)
                }

                val tvInfo = TextView(this).apply {
                    text = "Waktu: ${event.timeRange} • Lokasi: ${event.location}"
                    setTextColor(Color.parseColor("#666666"))
                    textSize = 11f
                    setPadding(0, dp(2), 0, 0)
                }

                eventContainer.addView(tvTitle)
                eventContainer.addView(tvSpeaker)
                eventContainer.addView(tvInfo)

                // If user is admin, allow clicking on the event item to edit or delete it
                if (sessionManager.isLoggedIn() && sessionManager.getUserRole() == "admin") {
                    eventContainer.isClickable = true
                    eventContainer.isFocusable = true
                    val typedValue = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                    eventContainer.setBackgroundResource(typedValue.resourceId)

                    eventContainer.setOnClickListener {
                        showAdminEventOptionsDialog(event, formattedDate)
                    }
                }

                layoutDynamicEventsContainer.addView(eventContainer)
            }
        } else {
            viewEventDivider.visibility = View.GONE
            layoutEventDetail.visibility = View.GONE
        }
    }

    private fun showAddEventDialog(eventToEdit: EventItem?, preselectedDate: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.et_event_title)
        val spCategory = dialogView.findViewById<Spinner>(R.id.sp_event_category)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_event_desc)
        val etSpeaker = dialogView.findViewById<EditText>(R.id.et_event_speaker)
        val etDate = dialogView.findViewById<EditText>(R.id.et_event_date)
        val etTime = dialogView.findViewById<EditText>(R.id.et_event_time)
        val etLocation = dialogView.findViewById<EditText>(R.id.et_event_location)
        val etImageUrl = dialogView.findViewById<EditText>(R.id.et_event_image_url)
        val cbFeatured = dialogView.findViewById<CheckBox>(R.id.cb_featured)

        // Setup category spinner
        val categories = listOf("Kajian", "Webinar", "Workshop", "Sosial")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = spinnerAdapter

        etDate.setText(preselectedDate)
        etDate.isEnabled = false // Lock to the selected calendar date

        // Fill data if editing
        if (eventToEdit != null) {
            etTitle.setText(eventToEdit.title)
            val catIndex = categories.indexOf(eventToEdit.category)
            if (catIndex >= 0) spCategory.setSelection(catIndex)
            etDesc.setText(eventToEdit.description)
            etSpeaker.setText(eventToEdit.speaker)
            etTime.setText(eventToEdit.timeRange)
            etLocation.setText(eventToEdit.location)
            etImageUrl.setText(eventToEdit.imageUrl ?: "")
            cbFeatured.isChecked = eventToEdit.isFeatured
        }

        AlertDialog.Builder(this)
            .setTitle(if (eventToEdit == null) "➕ Tambah Event Kalender" else "✏️ Edit Event")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val title = etTitle.text.toString().trim()
                val cat = spCategory.selectedItem.toString()
                val desc = etDesc.text.toString().trim()
                val speaker = etSpeaker.text.toString().trim()
                val time = etTime.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val imgUrl = etImageUrl.text.toString().trim()
                val isFeaturedVal = if (cbFeatured.isChecked) 1 else 0

                if (title.isNotEmpty() && desc.isNotEmpty() && speaker.isNotEmpty()) {
                    saveCalendarEvent(eventToEdit?.id ?: 0, title, cat, desc, preselectedDate, time, speaker, location, isFeaturedVal, imgUrl)
                } else {
                    Toast.makeText(this, "Field penting tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveCalendarEvent(
        id: Int, title: String, cat: String, desc: String, date: String,
        time: String, speaker: String, location: String, featured: Int, imageUrl: String
    ) {
        val adminUserId = sessionManager.getUserId()
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.saveEvent(
                    userId = adminUserId, id = id, title = title, category = cat,
                    description = desc, eventDate = date, timeRange = time,
                    speaker = speaker, location = location, isFeatured = featured,
                    imageUrl = imageUrl
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    runOnUiThread {
                        Toast.makeText(this@KalenderActivity, "Event berhasil disimpan", Toast.LENGTH_SHORT).show()
                        loadMonthData()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@KalenderActivity, "Gagal menyimpan: " + response.body()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@KalenderActivity, "Koneksi server gagal", Toast.LENGTH_SHORT).show()
                }
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
            private val viewDot: View = itemView.findViewById(R.id.view_event_dot)

            fun bind(day: CalendarDay, activity: KalenderActivity) {
                if (day.dayNumber == 0) {
                    tvGregorian.text = ""
                    tvHijri.text = ""
                    viewBg.setBackgroundResource(0)
                    viewSelection.setBackgroundResource(0)
                    viewDot.visibility = View.GONE
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

                // Highlight Event Dot
                if (day.hasEvent) {
                    viewDot.visibility = View.VISIBLE
                } else {
                    viewDot.visibility = View.GONE
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
        val fastingInfo: HijriCalendarHelper.FastingInfo?,
        val hasEvent: Boolean
    )
}

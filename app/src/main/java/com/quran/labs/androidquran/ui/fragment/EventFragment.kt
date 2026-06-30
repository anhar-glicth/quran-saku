package com.quran.labs.androidquran.ui.fragment

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.SessionManager
import com.quran.labs.androidquran.model.EventItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var rvEvents: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var edtSearch: EditText

    // Featured views
    private lateinit var layoutFeatured: View
    private lateinit var tvFeaturedTitle: TextView
    private lateinit var tvFeaturedDesc: TextView
    private lateinit var tvFeaturedTime: TextView
    private lateinit var tvFeaturedBadge: TextView
    private lateinit var btnRegisterFeatured: View

    private val allEvents = mutableListOf<EventItem>()
    private val displayedEvents = mutableListOf<EventItem>()
    private lateinit var adapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        rvEvents = view.findViewById(R.id.rv_events)
        progress = view.findViewById(R.id.progress_events)
        tvEmpty = view.findViewById(R.id.tv_events_empty)
        fabAdd = view.findViewById(R.id.fab_add_event)
        edtSearch = view.findViewById(R.id.edt_search_events)

        // Featured binds
        layoutFeatured = view.findViewById(R.id.layout_featured_event)
        tvFeaturedTitle = view.findViewById(R.id.tv_featured_title)
        tvFeaturedDesc = view.findViewById(R.id.tv_featured_desc)
        tvFeaturedTime = view.findViewById(R.id.tv_featured_time)
        tvFeaturedBadge = view.findViewById(R.id.tv_featured_badge)
        btnRegisterFeatured = view.findViewById(R.id.btn_register_featured)

        rvEvents.layoutManager = LinearLayoutManager(context)
        adapter = EventAdapter(
            displayedEvents,
            isAdmin = sessionManager.isLoggedIn() && sessionManager.getUserRole() == "admin",
            onAction = { event -> registerForEvent(event) },
            onDelete = { event -> confirmDeleteEvent(event) }
        )
        rvEvents.adapter = adapter

        // Admin controls
        if (sessionManager.isLoggedIn() && sessionManager.getUserRole() == "admin") {
            fabAdd.visibility = View.VISIBLE
            fabAdd.setOnClickListener {
                showAddEventDialog(null)
            }
        } else {
            fabAdd.visibility = View.GONE
        }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterEvents()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadEvents()
    }

    private fun loadEvents() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        layoutFeatured.visibility = View.GONE
        rvEvents.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getEvents()
                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    activity?.runOnUiThread {
                        progress.visibility = View.GONE
                        if (list.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                        } else {
                            rvEvents.visibility = View.VISIBLE
                            allEvents.clear()
                            allEvents.addAll(list)
                            filterEvents()
                        }
                    }
                } else {
                    showError("Gagal memuat event (${response.code()})")
                }
            } catch (e: Exception) {
                showError("Tidak dapat terhubung ke server.")
            }
        }
    }

    private fun showError(msg: String) {
        activity?.runOnUiThread {
            progress.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = msg
            layoutFeatured.visibility = View.GONE
            rvEvents.visibility = View.GONE
        }
    }

    private fun filterEvents() {
        val query = edtSearch.text.toString().trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allEvents
        } else {
            allEvents.filter {
                it.title.lowercase().contains(query) ||
                it.description.lowercase().contains(query) ||
                it.speaker.lowercase().contains(query) ||
                it.category.lowercase().contains(query)
            }
        }

        // Bind Featured (take first featured event, if any)
        val featured = filtered.find { it.isFeatured }
        if (featured != null) {
            layoutFeatured.visibility = View.VISIBLE
            tvFeaturedTitle.text = featured.title
            tvFeaturedDesc.text = featured.description
            tvFeaturedTime.text = "${formatDateIndo(featured.eventDate)} • ${featured.timeRange}"
            tvFeaturedBadge.text = featured.category.uppercase()
            btnRegisterFeatured.setOnClickListener {
                registerForEvent(featured)
            }
        } else {
            layoutFeatured.visibility = View.GONE
        }

        // Bind remaining upcoming list (all except the featured one)
        val upcomingList = if (featured != null) {
            filtered.filter { it.id != featured.id }
        } else {
            filtered
        }

        displayedEvents.clear()
        displayedEvents.addAll(upcomingList)
        adapter.notifyDataSetChanged()
    }

    private fun registerForEvent(event: EventItem) {
        Toast.makeText(context, "Berhasil mendaftar ke kegiatan: ${event.title}! 🌟", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDeleteEvent(event: EventItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Hapus Event")
            .setMessage("Apakah Anda yakin ingin menghapus event \"${event.title}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteEvent(event)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteEvent(event: EventItem) {
        val adminUserId = sessionManager.getUserId()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.deleteEvent(userId = adminUserId, id = event.id)
                if (response.isSuccessful && response.body()?.success == true) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Event berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadEvents()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Gagal menghapus event", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddEventDialog(eventToEdit: EventItem?) {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_event, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.et_event_title)
        val spCategory = dialogView.findViewById<Spinner>(R.id.sp_event_category)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_event_desc)
        val etSpeaker = dialogView.findViewById<EditText>(R.id.et_event_speaker)
        val etDate = dialogView.findViewById<EditText>(R.id.et_event_date)
        val etTime = dialogView.findViewById<EditText>(R.id.et_event_time)
        val etLocation = dialogView.findViewById<EditText>(R.id.et_event_location)
        val cbFeatured = dialogView.findViewById<CheckBox>(R.id.cb_featured)

        // Setup category spinner
        val categories = listOf("Kajian", "Webinar", "Workshop", "Sosial")
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = spinnerAdapter

        // Date Picker Setup
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            etDate.setText(format.format(calendar.time))
        }

        etDate.setOnClickListener {
            DatePickerDialog(context, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Fill data if editing
        if (eventToEdit != null) {
            etTitle.setText(eventToEdit.title)
            val catIndex = categories.indexOf(eventToEdit.category)
            if (catIndex >= 0) spCategory.setSelection(catIndex)
            etDesc.setText(eventToEdit.description)
            etSpeaker.setText(eventToEdit.speaker)
            etDate.setText(eventToEdit.eventDate)
            etTime.setText(eventToEdit.timeRange)
            etLocation.setText(eventToEdit.location)
            cbFeatured.isChecked = eventToEdit.isFeatured
        }

        AlertDialog.Builder(context)
            .setTitle(if (eventToEdit == null) "➕ Tambah Event Baru" else "✏️ Edit Event")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val title = etTitle.text.toString().trim()
                val cat = spCategory.selectedItem.toString()
                val desc = etDesc.text.toString().trim()
                val speaker = etSpeaker.text.toString().trim()
                val date = etDate.text.toString().trim()
                val time = etTime.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val isFeaturedVal = if (cbFeatured.isChecked) 1 else 0

                if (title.isNotEmpty() && desc.isNotEmpty() && date.isNotEmpty() && speaker.isNotEmpty()) {
                    saveEvent(eventToEdit?.id ?: 0, title, cat, desc, date, time, speaker, location, isFeaturedVal)
                } else {
                    Toast.makeText(context, "Field penting tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveEvent(
        id: Int, title: String, cat: String, desc: String, date: String,
        time: String, speaker: String, location: String, featured: Int
    ) {
        val adminUserId = sessionManager.getUserId()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.saveEvent(
                    userId = adminUserId, id = id, title = title, category = cat,
                    description = desc, eventDate = date, timeRange = time,
                    speaker = speaker, location = location, isFeatured = featured
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Event berhasil disimpan", Toast.LENGTH_SHORT).show()
                        loadEvents()
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Gagal menyimpan event: " + response.body()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Tidak dapat menghubungkan ke server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatDateIndo(dateStr: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = format.parse(dateStr) ?: return dateStr
            SimpleDateFormat("d MMM yyyy", Locale("id")).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    inner class EventAdapter(
        private val list: List<EventItem>,
        private val isAdmin: Boolean,
        private val onAction: (EventItem) -> Unit,
        private val onDelete: (EventItem) -> Unit
    ) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tv_event_title)
            val tvCat: TextView = view.findViewById(R.id.tv_event_category)
            val tvSpeaker: TextView = view.findViewById(R.id.tv_event_speaker)
            val tvTime: TextView = view.findViewById(R.id.tv_event_time)
            val tvLoc: TextView = view.findViewById(R.id.tv_event_location)
            val btnAction: TextView = view.findViewById(R.id.btn_event_action)
            val btnDelete: TextView = view.findViewById(R.id.btn_event_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvTitle.text = item.title
            holder.tvCat.text = item.category.uppercase()
            holder.tvSpeaker.text = "Bersama " + item.speaker
            holder.tvTime.text = "${formatDateIndo(item.eventDate)} • ${item.timeRange}"
            holder.tvLoc.text = item.location

            holder.btnAction.setOnClickListener { onAction(item) }

            if (isAdmin) {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnDelete.setOnClickListener { onDelete(item) }
                // Also support edit on row long click
                holder.itemView.setOnLongClickListener {
                    showAddEventDialog(item)
                    true
                }
            } else {
                holder.btnDelete.visibility = View.GONE
                holder.itemView.setOnLongClickListener(null)
            }
        }

        override fun getItemCount(): Int = list.size
    }
}

package com.quran.labs.androidquran.ui.fragment

import android.content.Intent
import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

    // Photo pick helpers
    private var pendingPhotoPreview: ImageView? = null
    private var pendingImageUrlField: EditText? = null
    private var pendingPhotoPlaceholder: TextView? = null
    private var selectedPhotoUri: Uri? = null
    private lateinit var photoPickLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoPickLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedPhotoUri = it
                pendingPhotoPreview?.setImageURI(it)
                pendingPhotoPlaceholder?.visibility = View.GONE
                // Also clear URL field since a local image was picked
                pendingImageUrlField?.setText("")
            }
        }
    }

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
            onAction = { event -> showEventDetail(event) },
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

    override fun onResume() {
        super.onResume()
        // loadEvents dipanggil sekali di onViewCreated; tidak perlu reload di setiap onResume
        // agar tidak terjadi network call berulang saat back dari EventDetail
    }

    private fun loadEvents() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        layoutFeatured.visibility = View.GONE
        rvEvents.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getEvents()
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    progress.visibility = View.GONE
                    if (list.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rvEvents.visibility = View.VISIBLE
                        allEvents.clear()
                        allEvents.addAll(list)
                        filterEvents()
                    }
                } else {
                    showError("Gagal memuat event (${response.code()})")
                }
            } catch (e: Exception) {
                if (isAdded) {
                    showError("Tidak dapat terhubung ke server.")
                }
            }
        }
    }

    private fun showError(msg: String) {
        progress.visibility = View.GONE
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = msg
        layoutFeatured.visibility = View.GONE
        rvEvents.visibility = View.GONE
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
                showEventDetail(featured)
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

    private fun showEventDetail(event: EventItem) {
        val ctx = context ?: return
        try {
            val intent = Intent(ctx, com.quran.labs.androidquran.EventDetailActivity::class.java).apply {
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_ID, event.id)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_TITLE, event.title)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_DESC, event.description)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_DATE, event.eventDate)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_TIME, event.timeRange)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_SPEAKER, event.speaker)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_LOCATION, event.location)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_CATEGORY, event.category)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_IMAGE_URL, event.imageUrl)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_LINK_URL, event.linkUrl)
                putExtra(com.quran.labs.androidquran.EventDetailActivity.EXTRA_EVENT_IS_FEATURED, event.isFeatured)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal membuka detail event: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
                    Toast.makeText(context, "Event berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadEvents()
                } else {
                    Toast.makeText(context, "Gagal menghapus event", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menghapus event", Toast.LENGTH_SHORT).show()
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
        val etLinkUrl = dialogView.findViewById<EditText>(R.id.et_event_link_url)
        val etImageUrl = dialogView.findViewById<EditText>(R.id.et_event_image_url)
        val imgPhotoPreview = dialogView.findViewById<ImageView>(R.id.img_event_photo_preview)
        val tvPhotoPlaceholder = dialogView.findViewById<TextView>(R.id.tv_photo_placeholder)
        val btnPickPhoto = dialogView.findViewById<Button>(R.id.btn_pick_photo)
        val cbFeatured = dialogView.findViewById<CheckBox>(R.id.cb_featured)

        // Reset state for this dialog instance
        selectedPhotoUri = null
        pendingPhotoPreview = imgPhotoPreview
        pendingImageUrlField = etImageUrl
        pendingPhotoPlaceholder = tvPhotoPlaceholder

        // Tombol pilih foto dari galeri
        btnPickPhoto.setOnClickListener {
            photoPickLauncher.launch("image/*")
        }

        // Preview dari URL ketika user mengetik
        etImageUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s.toString().trim()
                if (url.startsWith("http") && selectedPhotoUri == null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                conn.doInput = true; conn.connect()
                                BitmapFactory.decodeStream(conn.inputStream)
                            }
                            imgPhotoPreview.setImageBitmap(bitmap)
                            tvPhotoPlaceholder.visibility = View.GONE
                        } catch (_: Exception) {}
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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
            etLinkUrl.setText(eventToEdit.linkUrl ?: "")
            etImageUrl.setText(eventToEdit.imageUrl ?: "")
            cbFeatured.isChecked = eventToEdit.isFeatured

            // Load existing photo preview
            val existingUrl = eventToEdit.imageUrl
            if (!existingUrl.isNullOrEmpty()) {
                tvPhotoPlaceholder.visibility = View.GONE
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            val conn = java.net.URL(existingUrl).openConnection() as java.net.HttpURLConnection
                            conn.doInput = true; conn.connect()
                            BitmapFactory.decodeStream(conn.inputStream)
                        }
                        imgPhotoPreview.setImageBitmap(bitmap)
                    } catch (_: Exception) {}
                }
            }
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
                val linkUrlVal = etLinkUrl.text.toString().trim()
                val imgUrl = etImageUrl.text.toString().trim()
                val isFeaturedVal = if (cbFeatured.isChecked) 1 else 0
                val localUri = selectedPhotoUri

                if (title.isNotEmpty() && desc.isNotEmpty() && date.isNotEmpty() && speaker.isNotEmpty()) {
                    if (localUri != null) {
                        // Upload local photo first, then save event
                        uploadPhotoThenSave(localUri, eventToEdit?.id ?: 0, title, cat, desc, date, time, speaker, location, linkUrlVal, isFeaturedVal)
                    } else {
                        saveEvent(eventToEdit?.id ?: 0, title, cat, desc, date, time, speaker, location, linkUrlVal, isFeaturedVal, imgUrl)
                    }
                } else {
                    Toast.makeText(context, "Field penting tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun uploadPhotoThenSave(
        uri: android.net.Uri,
        id: Int, title: String, cat: String, desc: String, date: String,
        time: String, speaker: String, location: String, linkUrl: String, featured: Int
    ) {
        val ctx = context ?: return
        Toast.makeText(ctx, "Mengunggah foto...", Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uploadedUrl = withContext(Dispatchers.IO) {
                    // Read bytes from URI
                    val inputStream = ctx.contentResolver.openInputStream(uri) ?: return@withContext null
                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    // Upload via HTTP multipart to PHP
                    val serverBase = AuthClient.BASE_URL
                    val boundary = "Boundary_${System.currentTimeMillis()}"
                    val url = java.net.URL("${serverBase}auth/upload_event_photo.php")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                    val output = conn.outputStream
                    val writer = java.io.PrintStream(output, true, "UTF-8")
                    writer.print("--$boundary\r\n")
                    writer.print("Content-Disposition: form-data; name=\"photo\"; filename=\"event_photo.jpg\"\r\n")
                    writer.print("Content-Type: image/jpeg\r\n\r\n")
                    writer.flush()
                    output.write(bytes)
                    output.flush()
                    writer.print("\r\n--$boundary--\r\n")
                    writer.flush()

                    val responseCode = conn.responseCode
                    if (responseCode == 200) {
                        val responseText = conn.inputStream.bufferedReader().readText()
                        val json = org.json.JSONObject(responseText)
                        if (json.optBoolean("success")) json.optString("url") else null
                    } else null
                }

                if (!uploadedUrl.isNullOrEmpty()) {
                    saveEvent(id, title, cat, desc, date, time, speaker, location, linkUrl, featured, uploadedUrl)
                } else {
                    Toast.makeText(ctx, "Gagal mengunggah foto. Coba masukkan URL langsung.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "Error upload: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveEvent(
        id: Int, title: String, cat: String, desc: String, date: String,
        time: String, speaker: String, location: String, linkUrl: String, featured: Int, imageUrl: String
    ) {
        val adminUserId = sessionManager.getUserId()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.saveEvent(
                    userId = adminUserId, id = id, title = title, category = cat,
                    description = desc, eventDate = date, timeRange = time,
                    speaker = speaker, location = location, linkUrl = linkUrl, isFeatured = featured,
                    imageUrl = imageUrl
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(context, "Event berhasil disimpan", Toast.LENGTH_SHORT).show()
                    loadEvents()
                } else {
                    Toast.makeText(context, "Gagal menyimpan event: " + response.body()?.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Tidak dapat menghubungkan ke server", Toast.LENGTH_SHORT).show()
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
            val imgBanner: ImageView = view.findViewById(R.id.img_event_banner)
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

            // Load banner image dynamically from URL using coroutine
            if (!item.imageUrl.isNullOrEmpty()) {
                val currentUrl = item.imageUrl
                holder.imgBanner.tag = currentUrl
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val connection = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                            connection.doInput = true
                            connection.connect()
                            val input = connection.inputStream
                            android.graphics.BitmapFactory.decodeStream(input)
                        }
                        if (holder.imgBanner.tag == currentUrl) {
                            holder.imgBanner.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        if (holder.imgBanner.tag == currentUrl) {
                            holder.imgBanner.setImageResource(R.drawable.bg_home_verse_card)
                        }
                    }
                }
            } else {
                holder.imgBanner.setImageResource(R.drawable.bg_home_verse_card)
            }

            holder.btnAction.setOnClickListener { onAction(item) }
            holder.itemView.setOnClickListener { onAction(item) }

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

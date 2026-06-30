package com.quran.labs.androidquran

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.io.InputStream

class DoaActivity : AppCompatActivity() {

    private lateinit var rvDoa: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var adapter: DoaListAdapter
    private var allDoaItems = mutableListOf<DoaItem>()
    private var filteredDoaItems = mutableListOf<DoaItem>()
    private var currentBookIdFilter: String = "ALL" // "ALL", "0", "1", "2", "SHALAT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doa)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "Doa-Doa"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Setup Search
        etSearch = EditText(this).apply {
            hint = "Cari doa harian..."
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#B0BEC5"))
            background = null
            setSingleLine(true)
        }
        toolbar.addView(etSearch)

        rvDoa = findViewById(R.id.rv_doa_harian)
        rvDoa.layoutManager = LinearLayoutManager(this)
        
        adapter = DoaListAdapter(filteredDoaItems) { item ->
            val intent = Intent(this, DoaDetailActivity::class.java).apply {
                putExtra("doa_id", item.id)
                putExtra("doa_title", item.title)
            }
            startActivity(intent)
        }
        rvDoa.adapter = adapter

        // Setup Tabs
        val tabSemua = findViewById<TextView>(R.id.tab_semua)
        val tabHarian = findViewById<TextView>(R.id.tab_harian)
        val tabPagi = findViewById<TextView>(R.id.tab_pagi)
        val tabShalat = findViewById<TextView>(R.id.tab_shalat)
        val tabQuran = findViewById<TextView>(R.id.tab_quran)

        val tabs = listOf(tabSemua, tabHarian, tabPagi, tabShalat, tabQuran)

        fun selectTab(selectedTab: TextView, filter: String) {
            currentBookIdFilter = filter
            for (tab in tabs) {
                if (tab == selectedTab) {
                    tab.setBackgroundResource(R.drawable.bg_segmented_selected)
                    tab.setTextColor(Color.WHITE)
                } else {
                    tab.setBackgroundResource(R.drawable.bg_segmented_control)
                    tab.setTextColor(Color.parseColor("#666666"))
                }
            }
            filterList()
        }

        tabSemua.setOnClickListener { selectTab(tabSemua, "ALL") }
        tabHarian.setOnClickListener { selectTab(tabHarian, "2") }
        tabPagi.setOnClickListener { selectTab(tabPagi, "1") }
        tabShalat.setOnClickListener { selectTab(tabShalat, "SHALAT") }
        tabQuran.setOnClickListener { selectTab(tabQuran, "0") }

        loadDoaFromJson()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterList()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadDoaFromJson() {
        try {
            val inputStream: InputStream = assets.open("json/prayer/prayer.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            allDoaItems.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val bookId = obj.getString("book_id")
                
                var titleText = ""
                val titleArray = obj.getJSONArray("title")
                for (j in 0 until titleArray.length()) {
                    val tObj = titleArray.getJSONObject(j)
                    if (tObj.getString("language") == "bahasa") {
                        titleText = tObj.getString("text")
                        break
                    }
                }
                
                allDoaItems.add(DoaItem(id, bookId, titleText))
            }
            filterList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun filterList() {
        val query = etSearch.text.toString().trim().lowercase()
        filteredDoaItems.clear()

        for (item in allDoaItems) {
            // Check Book Filter
            val matchesBook = when (currentBookIdFilter) {
                "ALL" -> true
                "SHALAT" -> item.bookId == "2" && (
                    item.title.contains("Shalat", true) || 
                    item.title.contains("Wudhu", true) || 
                    item.title.contains("Sujud", true) || 
                    item.title.contains("Tasyahud", true) || 
                    item.title.contains("Qunut", true)
                )
                else -> item.bookId == currentBookIdFilter
            }

            // Check Query Filter
            val matchesQuery = query.isEmpty() || item.title.lowercase().contains(query)

            if (matchesBook && matchesQuery) {
                filteredDoaItems.add(item)
            }
        }
        adapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    data class DoaItem(val id: String, val bookId: String, val title: String)

    inner class DoaListAdapter(
        private val list: List<DoaItem>,
        private val onItemClick: (DoaItem) -> Unit
    ) : RecyclerView.Adapter<DoaListAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNumber: TextView = view.findViewById(R.id.tv_doa_number)
            val tvTitle: TextView = view.findViewById(R.id.tv_doa_title)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_doa_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvNumber.text = (position + 1).toString()
            holder.tvTitle.text = item.title
            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount(): Int = list.size
    }
}

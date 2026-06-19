package com.quran.labs.androidquran

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.quran.labs.androidquran.database.NotesDbHelper

class CatatanActivity : AppCompatActivity() {

    private lateinit var rvNotes: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var dbHelper: NotesDbHelper
    private val notesList = mutableListOf<WorshipNote>()
    private lateinit var adapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catatan)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvNotes = findViewById(R.id.rv_notes)
        layoutEmpty = findViewById(R.id.layout_empty_notes)
        fabAdd = findViewById(R.id.fab_add_note)

        dbHelper = NotesDbHelper(this)
        rvNotes.layoutManager = LinearLayoutManager(this)

        adapter = NotesAdapter(notesList) { clickedNote ->
            val intent = Intent(this, CatatanWriteActivity::class.java).apply {
                putExtra("note_id", clickedNote.id)
            }
            startActivity(intent)
        }
        rvNotes.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, CatatanWriteActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        notesList.clear()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            NotesDbHelper.TABLE_NOTES,
            null,
            null,
            null,
            null,
            null,
            "${NotesDbHelper.KEY_ID} DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.KEY_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.KEY_TITLE))
                val content = cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.KEY_CONTENT))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.KEY_DATE))
                notesList.add(WorshipNote(id, title, content, date))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        adapter.notifyDataSetChanged()

        if (notesList.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvNotes.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rvNotes.visibility = View.VISIBLE
        }
    }

    data class WorshipNote(
        val id: Int,
        val title: String,
        val content: String,
        val date: String
    )

    inner class NotesAdapter(
        private val list: List<WorshipNote>,
        private val onItemClick: (WorshipNote) -> Unit
    ) : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtTitle: TextView = view.findViewById(R.id.txt_note_title)
            val txtContent: TextView = view.findViewById(R.id.txt_note_content)
            val txtDate: TextView = view.findViewById(R.id.txt_note_date)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_note, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.txtTitle.text = item.title
            holder.txtContent.text = item.content
            holder.txtDate.text = item.date
            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount(): Int = list.size
    }
}

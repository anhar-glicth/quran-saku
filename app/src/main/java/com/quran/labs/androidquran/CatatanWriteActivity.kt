package com.quran.labs.androidquran

import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.quran.labs.androidquran.database.NotesDbHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatatanWriteActivity : AppCompatActivity() {

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText
    private lateinit var btnDelete: View
    private lateinit var btnSave: View
    private lateinit var dbHelper: NotesDbHelper
    private var noteId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catatan_write)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        edtTitle = findViewById(R.id.edt_note_title)
        edtContent = findViewById(R.id.edt_note_content)
        btnDelete = findViewById(R.id.btn_delete_note)
        btnSave = findViewById(R.id.btn_save_note)

        dbHelper = NotesDbHelper(this)

        noteId = intent.getIntExtra("note_id", -1)
        if (noteId != -1) {
            supportActionBar?.title = "Edit Catatan"
            btnDelete.visibility = View.VISIBLE
            loadNoteData(noteId)
        } else {
            supportActionBar?.title = "Tulis Catatan Baru"
            btnDelete.visibility = View.GONE
        }

        btnSave.setOnClickListener { saveNote() }
        btnDelete.setOnClickListener { deleteNote() }
    }

    private fun loadNoteData(id: Int) {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            NotesDbHelper.TABLE_NOTES,
            null,
            "${NotesDbHelper.KEY_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            val title = cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.KEY_TITLE))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.KEY_CONTENT))
            edtTitle.setText(title)
            edtContent.setText(content)
        }
        cursor.close()
        db.close()
    }

    private fun saveNote() {
        val title = edtTitle.text.toString().trim()
        val content = edtContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Judul dan Isi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
        val currentDate = dateFormat.format(Date())

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(NotesDbHelper.KEY_TITLE, title)
            put(NotesDbHelper.KEY_CONTENT, content)
            put(NotesDbHelper.KEY_DATE, currentDate)
        }

        if (noteId != -1) {
            // Update
            db.update(
                NotesDbHelper.TABLE_NOTES,
                values,
                "${NotesDbHelper.KEY_ID} = ?",
                arrayOf(noteId.toString())
            )
            Toast.makeText(this, "Catatan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        } else {
            // Insert
            db.insert(NotesDbHelper.TABLE_NOTES, null, values)
            Toast.makeText(this, "Catatan berhasil disimpan!", Toast.LENGTH_SHORT).show()
        }
        db.close()
        finish()
    }

    private fun deleteNote() {
        if (noteId != -1) {
            val db = dbHelper.writableDatabase
            db.delete(
                NotesDbHelper.TABLE_NOTES,
                "${NotesDbHelper.KEY_ID} = ?",
                arrayOf(noteId.toString())
            )
            db.close()
            Toast.makeText(this, "Catatan berhasil dihapus!", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}

package com.quran.labs.androidquran

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.io.InputStream
import java.nio.charset.Charset

class DzikirReadActivity : AppCompatActivity() {

    private lateinit var rvDzikir: RecyclerView
    private lateinit var adapter: DzikirAdapter
    private val itemList = mutableListOf<DzikirItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dzikir_read)

        val category = intent.getStringExtra("category") ?: "pagi"
        val title = intent.getStringExtra("title") ?: "Dzikir"

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvDzikir = findViewById(R.id.rv_dzikir)
        rvDzikir.layoutManager = LinearLayoutManager(this)

        loadData(category)

        adapter = DzikirAdapter(itemList)
        rvDzikir.adapter = adapter
    }

    private fun loadData(category: String) {
        itemList.clear()
        try {
            if (category == "pagi" || category == "sore") {
                val jsonString = loadJSONFromAsset("json/prayer/matsuratData.json")
                if (jsonString != null) {
                    val jsonArray = JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val prayerId = obj.optString("prayer_id")

                        if (prayerId == category) {
                            val textArray = obj.getJSONArray("text")
                            var arabic = ""
                            var translation = ""
                            for (j in 0 until textArray.length()) {
                                val textObj = textArray.getJSONObject(j)
                                val lang = textObj.optString("language")
                                if (lang == "arabic") {
                                    arabic = textObj.optString("text")
                                } else if (lang == "bahasa") {
                                    translation = textObj.optString("text")
                                }
                            }

                            val sourceArray = obj.optJSONArray("source")
                            var source = ""
                            if (sourceArray != null && sourceArray.length() > 0) {
                                for (j in 0 until sourceArray.length()) {
                                    val srcObj = sourceArray.getJSONObject(j)
                                    if (srcObj.optString("language") == "bahasa") {
                                        source = srcObj.optString("text")
                                    }
                                }
                            }

                            val notesArray = obj.optJSONArray("notes")
                            var notes = ""
                            if (notesArray != null && notesArray.length() > 0) {
                                for (j in 0 until notesArray.length()) {
                                    val noteObj = notesArray.getJSONObject(j)
                                    if (noteObj.optString("language") == "bahasa") {
                                        notes = noteObj.optString("text")
                                    }
                                }
                            }

                            var targetCount = 1
                            if (notes.contains("100x")) {
                                targetCount = 100
                            } else if (notes.contains("33x")) {
                                targetCount = 33
                            } else if (notes.contains("10x")) {
                                targetCount = 10
                            } else if (notes.contains("7x")) {
                                targetCount = 7
                            } else if (notes.contains("3x")) {
                                targetCount = 3
                            } else if (notes.contains("4x")) {
                                targetCount = 4
                            }

                            itemList.add(
                                DzikirItem(
                                    arabic = arabic,
                                    translation = translation,
                                    notes = if (notes.isEmpty()) "Baca 1x" else notes,
                                    source = if (source.isEmpty()) "Al-Matsurat (Imam Hasan Al-Banna)" else source,
                                    targetCount = targetCount
                                )
                            )
                        }
                    }
                }
            } else if (category == "hadits") {
                val jsonString = loadJSONFromAsset("json/hadith/nawawi40.json")
                if (jsonString != null) {
                    val jsonArray = JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val textArray = obj.getJSONArray("text")
                        
                        var arabic = ""
                        var translation = ""
                        for (j in 0 until textArray.length()) {
                            val textObj = textArray.getJSONObject(j)
                            val lang = textObj.optString("language")
                            if (lang == "arabic") {
                                arabic = textObj.optString("text")
                            } else if (lang == "bahasa") {
                                translation = textObj.optString("text")
                            }
                        }

                        val indexStr = obj.optString("index")
                        itemList.add(
                            DzikirItem(
                                arabic = arabic,
                                translation = translation,
                                notes = "Hadits Ke-$indexStr",
                                source = "Hadits Arba'in Nawawi",
                                targetCount = 1
                            )
                        )
                    }
                }
            } else {
                val jsonString = loadJSONFromAsset("json/prayer/prayerData.json")
                if (jsonString != null) {
                    val jsonArray = JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val bookId = obj.optString("book_id")
                        val prayerId = obj.optString("prayer_id")

                        val matchesCategory = when (category) {
                            "shalat" -> prayerId == "29"
                            "quran" -> bookId == "0" || bookId == "2"
                            else -> false
                        }

                        if (matchesCategory) {
                            val textArray = obj.getJSONArray("text")
                            var arabic = ""
                            var translation = ""
                            for (j in 0 until textArray.length()) {
                                val textObj = textArray.getJSONObject(j)
                                val lang = textObj.optString("language")
                                if (lang == "arabic") {
                                    arabic = textObj.optString("text")
                                } else if (lang == "bahasa") {
                                    translation = textObj.optString("text")
                                }
                            }

                            val sourceArray = obj.optJSONArray("source")
                            var source = ""
                            if (sourceArray != null && sourceArray.length() > 0) {
                                for (j in 0 until sourceArray.length()) {
                                    val srcObj = sourceArray.getJSONObject(j)
                                    if (srcObj.optString("language") == "bahasa") {
                                        source = srcObj.optString("text")
                                    }
                                }
                            }

                            val notesArray = obj.optJSONArray("notes")
                            var notes = ""
                            if (notesArray != null && notesArray.length() > 0) {
                                for (j in 0 until notesArray.length()) {
                                    val noteObj = notesArray.getJSONObject(j)
                                    if (noteObj.optString("language") == "bahasa") {
                                        notes = noteObj.optString("text")
                                    }
                                }
                            }

                            var targetCount = 1
                            if (notes.contains("33x")) {
                                targetCount = 33
                            } else if (notes.contains("3x")) {
                                targetCount = 3
                            } else if (notes.contains("10x")) {
                                targetCount = 10
                            } else if (notes.contains("100x")) {
                                targetCount = 100
                            } else if (notes.contains("7x")) {
                                targetCount = 7
                            } else if (notes.contains("4x")) {
                                targetCount = 4
                            }

                            itemList.add(
                                DzikirItem(
                                    arabic = arabic,
                                    translation = translation,
                                    notes = if (notes.isEmpty()) "Dibaca 1x" else notes,
                                    source = if (source.isEmpty()) "Sunnah Rasulullah" else source,
                                    targetCount = targetCount
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadJSONFromAsset(fileName: String): String? {
        return try {
            val inputStream: InputStream = assets.open(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class DzikirItem(
        val arabic: String,
        val translation: String,
        val notes: String,
        val source: String,
        val targetCount: Int,
        var currentCount: Int = 0
    )

    inner class DzikirAdapter(private val list: List<DzikirItem>) :
        RecyclerView.Adapter<DzikirAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtArabic: TextView = view.findViewById(R.id.txt_arabic)
            val txtTranslation: TextView = view.findViewById(R.id.txt_translation)
            val txtNotes: TextView = view.findViewById(R.id.txt_notes)
            val txtSource: TextView = view.findViewById(R.id.txt_source)
            val txtCount: TextView = view.findViewById(R.id.txt_count)
            val btnCounterBg: View = view.findViewById(R.id.btn_counter_bg)
            val btnCounter: View = view.findViewById(R.id.btn_counter_container)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dzikir, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.txtArabic.text = item.arabic
            holder.txtTranslation.text = item.translation
            holder.txtNotes.text = item.notes
            holder.txtSource.text = item.source
            holder.txtCount.text = "${item.currentCount}/${item.targetCount}"

            updateCounterUI(holder, item)

            holder.btnCounter.setOnClickListener {
                if (item.currentCount < item.targetCount) {
                    item.currentCount++
                    holder.txtCount.text = "${item.currentCount}/${item.targetCount}"
                    updateCounterUI(holder, item)

                    // Haptic feedback - aman di semua versi Android
                    if (item.currentCount == item.targetCount) {
                        holder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    } else {
                        holder.itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                }
            }
        }

        private fun updateCounterUI(holder: ViewHolder, item: DzikirItem) {
            if (item.currentCount == item.targetCount) {
                holder.btnCounterBg.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9")) // Light Green
                holder.txtCount.textColor = android.graphics.Color.parseColor("#2E7D32") // Dark Green
                holder.txtCount.text = "✓"
            } else {
                holder.btnCounterBg.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF0E6")) // Light Orange
                holder.txtCount.textColor = android.graphics.Color.parseColor("#ffff6d00") // Orange
            }
        }

        private var TextView.textColor: Int
            get() = currentTextColor
            set(value) {
                setTextColor(value)
            }

        override fun getItemCount(): Int = list.size
    }
}

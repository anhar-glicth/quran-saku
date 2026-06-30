package com.quran.labs.androidquran.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.model.DoaHarianItem

class DoaHarianAdapter(
    private var items: List<DoaHarianItem>
) : RecyclerView.Adapter<DoaHarianAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView       = view.findViewById(R.id.tv_doa_title)
        val tvCategory: TextView    = view.findViewById(R.id.tv_doa_category)
        val tvArabic: TextView      = view.findViewById(R.id.tv_doa_arabic)
        val tvLatin: TextView       = view.findViewById(R.id.tv_doa_latin)
        val tvTranslation: TextView = view.findViewById(R.id.tv_doa_translation)
        val tvSource: TextView      = view.findViewById(R.id.tv_doa_source)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doa_harian, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text       = item.title
        holder.tvArabic.text      = item.arabic
        holder.tvLatin.text       = item.latin
        holder.tvTranslation.text = "\"${item.translation}\""
        holder.tvSource.text      = item.source
        holder.tvCategory.text    = categoryLabel(item.category)
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<DoaHarianItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun categoryLabel(category: String): String = when (category) {
        "pagi"   -> "🌅 Pagi"
        "sore"   -> "🌙 Sore"
        "harian" -> "☀️ Harian"
        "shalat" -> "🕌 Shalat"
        "quran"  -> "📖 Al-Quran"
        else     -> category.replaceFirstChar { it.uppercase() }
    }
}

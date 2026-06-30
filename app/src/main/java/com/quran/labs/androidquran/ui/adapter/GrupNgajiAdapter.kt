package com.quran.labs.androidquran.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.model.GrupNgaji

class GrupNgajiAdapter(
    private var items: List<GrupNgaji>,
    private val onItemClick: (GrupNgaji) -> Unit
) : RecyclerView.Adapter<GrupNgajiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView          = view.findViewById(R.id.txt_group_name)
        val tvCode: TextView          = view.findViewById(R.id.txt_group_code)
        val tvProgressPct: TextView   = view.findViewById(R.id.txt_group_progress_pct)
        val progressIndicator: LinearProgressIndicator = view.findViewById(R.id.progress_indicator)
        val tvDesc: TextView          = view.findViewById(R.id.txt_group_desc)
        val tvInitials: TextView      = view.findViewById(R.id.txt_group_initials)
        val flAvatar: FrameLayout     = view.findViewById(R.id.lay_group_avatar)
        
        val layLastReadRelay: View    = view.findViewById(R.id.lay_last_read_relay)
        val tvLastReadDesc: TextView  = view.findViewById(R.id.txt_last_read_relay_desc)
        val tvStatusBadge: TextView   = view.findViewById(R.id.txt_member_status_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grup_ngaji_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvCode.text = "ID: ${item.groupCode}"
        holder.tvDesc.text = "Target: ${item.targetKhatam}x Khatam (${item.durationDays} Hari)"

        // Set Initials
        val initials = if (item.name.isNotEmpty()) item.name.take(2).uppercase() else "G"
        holder.tvInitials.text = initials

        // Member status badge
        if (item.memberStatus == "pending") {
            holder.tvStatusBadge.visibility = View.VISIBLE
            holder.tvStatusBadge.text = "Menunggu Acc"
            holder.tvStatusBadge.setTextColor(Color.parseColor("#E65100"))
            holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
        } else {
            holder.tvStatusBadge.visibility = View.GONE
        }

        // Last read relay info
        if (item.lastReaderName != null && item.lastReaderName.isNotEmpty()) {
            holder.layLastReadRelay.visibility = View.VISIBLE
            holder.tvLastReadDesc.text = "Terakhir dibaca oleh ${item.lastReaderName}: Halaman ${item.currentPage}"
        } else {
            holder.layLastReadRelay.visibility = View.GONE
        }

        // Custom click listener
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<GrupNgaji>) {
        items = newList
        notifyDataSetChanged()
    }
}

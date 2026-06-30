package com.quran.labs.androidquran.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.model.DoaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DoaAdapter(
    private val items: MutableList<DoaItem>,
    private val onLike: (DoaItem, Int) -> Unit,
    private val onAamiin: (DoaItem, Int) -> Unit
) : RecyclerView.Adapter<DoaAdapter.DoaViewHolder>() {

    inner class DoaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarBg: View        = itemView.findViewById(R.id.avatar_bg)
        val txtInitials: TextView = itemView.findViewById(R.id.txt_doa_initials)
        val txtUsername: TextView = itemView.findViewById(R.id.txt_doa_username)
        val txtTime: TextView     = itemView.findViewById(R.id.txt_doa_time)
        val txtContent: TextView  = itemView.findViewById(R.id.txt_doa_content)
        val btnLike: LinearLayout = itemView.findViewById(R.id.btn_like)
        val btnAamiin: LinearLayout= itemView.findViewById(R.id.btn_aamiin)
        val txtLikeCount: TextView = itemView.findViewById(R.id.txt_like_count)
        val txtAamiinCount: TextView= itemView.findViewById(R.id.txt_aamiin_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doa_card, parent, false)
        return DoaViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoaViewHolder, position: Int) {
        val item = items[position]

        // Inisial & warna avatar
        val nameParts = item.userName.trim().split(" ")
        val initials = nameParts.take(2).joinToString("") { it.take(1).uppercase() }
        holder.txtInitials.text = initials

        // Warna avatar: hash dari userId agar konsisten per orang
        val avatarColors = listOf(
            "#FDF2E9" to "#E28743",
            "#EBF5FB" to "#2980B9",
            "#E9F7EF" to "#27AE60",
            "#FEF9E7" to "#F39C12",
            "#F5EEF8" to "#8E44AD"
        )
        val colorPair = avatarColors[item.userId % avatarColors.size]
        holder.avatarBg.background?.setTint(Color.parseColor(colorPair.first))
        holder.txtInitials.setTextColor(Color.parseColor(colorPair.second))

        holder.txtUsername.text = item.userName
        holder.txtTime.text = getRelativeTime(item.createdAt)
        holder.txtContent.text = item.latinText

        // Update reaction counts
        updateReactionUI(holder, item)

        // Click listeners
        holder.btnLike.setOnClickListener {
            onLike(item, position)
        }
        holder.btnAamiin.setOnClickListener {
            onAamiin(item, position)
        }
    }

    fun updateItem(position: Int, newItem: DoaItem) {
        items[position] = newItem
        notifyItemChanged(position)
    }

    fun prependItem(item: DoaItem) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun replaceAll(newItems: List<DoaItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun updateReactionUI(holder: DoaViewHolder, item: DoaItem) {
        holder.txtLikeCount.text = " ${item.likeCount}"
        holder.txtAamiinCount.text = " ${item.aamiinCount}"

        // Highlight if user already reacted
        if (item.isLiked) {
            holder.txtLikeCount.setTextColor(Color.parseColor("#E53935"))
        } else {
            holder.txtLikeCount.setTextColor(Color.parseColor("#555555"))
        }
        if (item.isAaminned) {
            holder.txtAamiinCount.setTextColor(Color.parseColor("#E28743"))
        } else {
            holder.txtAamiinCount.setTextColor(Color.parseColor("#555555"))
        }
    }

    private fun getRelativeTime(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date: Date = sdf.parse(dateStr) ?: return dateStr
            val now = Date()
            val diffMs = now.time - date.time
            val diffMin = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)

            when {
                diffMin < 1    -> "Baru saja"
                diffMin < 60   -> "$diffMin menit lalu"
                diffHours < 24 -> "$diffHours jam lalu"
                diffDays < 7   -> "$diffDays hari lalu"
                else           -> SimpleDateFormat("d MMM", Locale("id")).format(date)
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun getItemCount(): Int = items.size
}

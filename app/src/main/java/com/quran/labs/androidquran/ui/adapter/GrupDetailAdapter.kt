package com.quran.labs.androidquran.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.model.GrupMember
import com.quran.labs.androidquran.model.ReadingRelay

class GrupDetailAdapter(
    private val onApproveClick: ((GrupMember) -> Unit)? = null,
    private val onRejectClick: ((GrupMember) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mode = MODE_RELAY // 0: Relay, 1: Member
    private var relayList: List<ReadingRelay> = emptyList()
    private var memberList: List<GrupMember> = emptyList()

    companion object {
        const val MODE_RELAY = 0
        const val MODE_MEMBER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return mode
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == MODE_RELAY) {
            val view = inflater.inflate(R.layout.item_grup_relay_row, parent, false)
            RelayViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_grup_member, parent, false)
            MemberViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == MODE_RELAY) {
            val relayHolder = holder as RelayViewHolder
            val item = relayList[position]
            relayHolder.tvName.text = item.userName
            relayHolder.tvInitials.text = if (item.userName.isNotEmpty()) item.userName.take(2).uppercase() else "U"
            relayHolder.tvSurah.text = if (item.surahName != null) {
                "Membaca: ${item.surahName} Hlm ${item.pageNumber}"
            } else {
                "Membaca Quran Saku Hlm ${item.pageNumber}"
            }
            relayHolder.tvPage.text = "Hlm ${item.pageNumber}"
        } else {
            val memberHolder = holder as MemberViewHolder
            val item = memberList[position]
            memberHolder.tvName.text = item.name
            memberHolder.tvInitials.text = if (item.name.isNotEmpty()) item.name.take(2).uppercase() else "U"
            memberHolder.tvDesc.text = "Halaman Terakhir: ${item.lastPageRead}"
            memberHolder.tvProgress.text = "${item.lastPageRead} hlm"

            if (item.role == "admin") {
                memberHolder.tvName.text = "${item.name} 👑 (Admin)"
                memberHolder.tvName.setTextColor(Color.parseColor("#E65100"))
            } else {
                memberHolder.tvName.setTextColor(Color.parseColor("#212121"))
            }

            // Hide action buttons in active list view (they are only for pending requests)
            memberHolder.btnApprove.visibility = View.GONE
            memberHolder.btnReject.visibility = View.GONE
            memberHolder.tvProgress.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return if (mode == MODE_RELAY) relayList.size else memberList.size
    }

    fun setRelayData(data: List<ReadingRelay>) {
        this.relayList = data
        this.mode = MODE_RELAY
        notifyDataSetChanged()
    }

    fun setMemberData(data: List<GrupMember>) {
        this.memberList = data
        this.mode = MODE_MEMBER
        notifyDataSetChanged()
    }

    // Relay ViewHolder
    class RelayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.txt_relay_name)
        val tvInitials: TextView = view.findViewById(R.id.txt_relay_initials)
        val tvSurah: TextView = view.findViewById(R.id.txt_relay_time)
        val tvPage: TextView = view.findViewById(R.id.txt_relay_page)
    }

    // Member ViewHolder
    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.txt_member_name)
        val tvInitials: TextView = view.findViewById(R.id.txt_member_initials)
        val tvDesc: TextView = view.findViewById(R.id.txt_member_desc)
        val tvProgress: TextView = view.findViewById(R.id.txt_member_progress)
        val btnApprove: TextView = view.findViewById(R.id.btn_approve)
        val btnReject: TextView = view.findViewById(R.id.btn_reject)
    }
}

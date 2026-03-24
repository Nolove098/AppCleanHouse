package com.example.appcleanhouse.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.R
import com.example.appcleanhouse.models.ChatRoom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter hiển thị danh sách phòng chat trong CleanerDashboard / CustomerChatList.
 */
class ChatRoomAdapter(
    private val currentUserRole: String,
    private val onClick: (ChatRoom) -> Unit
) : ListAdapter<ChatRoom, ChatRoomAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatRoom>() {
            override fun areItemsTheSame(old: ChatRoom, new: ChatRoom) = old.id == new.id
            override fun areContentsTheSame(old: ChatRoom, new: ChatRoom) = old == new
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInitial: TextView = view.findViewById(R.id.tvAvatarInitial)
        private val tvName: TextView = view.findViewById(R.id.tvPartnerName)
        private val tvLastMsg: TextView = view.findViewById(R.id.tvLastMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvTimestamp)

        fun bind(room: ChatRoom) {
            val partnerName = if (currentUserRole == "cleaner") room.customerName else room.cleanerName
            val partnerId = if (currentUserRole == "cleaner") room.customerId else room.cleanerId
            val displayName = partnerName.ifBlank { partnerId.ifBlank { "N/A" } }

            tvName.text = displayName
            tvInitial.text = displayName.firstOrNull()?.uppercase() ?: "?"
            tvLastMsg.text = room.lastMessage.ifEmpty { "Tap to start chatting..." }
            tvTime.text = if (room.lastTimestamp > 0) formatTime(room.lastTimestamp) else ""

            itemView.setOnClickListener { onClick(room) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_room, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

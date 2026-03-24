package com.example.appcleanhouse.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.R
import com.example.appcleanhouse.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter cho RecyclerView hiển thị tin nhắn realtime.
 * Phân biệt bubble gửi (sent) và bubble nhận (received) dựa trên senderId.
 */
class RealtimeChatAdapter(
    private val currentUserId: String
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val VIEW_SENT = 0
        private const val VIEW_RECEIVED = 1

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = old.id == new.id
            override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
        }

        fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_SENT else VIEW_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_SENT) {
            SentVH(inflater.inflate(R.layout.item_chat_sent, parent, false))
        } else {
            ReceivedVH(inflater.inflate(R.layout.item_chat_received, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is SentVH -> holder.bind(msg)
            is ReceivedVH -> holder.bind(msg)
        }
    }

    class SentVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.text
            tvTime.text = formatTime(msg.timestamp)
        }
    }

    class ReceivedVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSenderName: TextView = view.findViewById(R.id.tvSenderName)
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(msg: ChatMessage) {
            tvSenderName.text = msg.senderName
            tvMessage.text = msg.text
            tvTime.text = formatTime(msg.timestamp)
        }
    }
}

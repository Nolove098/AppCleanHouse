package com.example.appcleanhouse.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.R
import com.example.appcleanhouse.models.AiChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter cho AI Chat sử dụng ListAdapter + DiffUtil để cập nhật mượt mà.
 * Hỗ trợ 2 loại ViewType: tin nhắn User (bên phải) và Bot (bên trái).
 */
class SmartMessageAdapter : ListAdapter<AiChatMessage, SmartMessageAdapter.MessageViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_BOT = 1

        /** DiffUtil callback để so sánh items hiệu quả */
        private val DiffCallback = object : DiffUtil.ItemCallback<AiChatMessage>() {
            override fun areItemsTheSame(oldItem: AiChatMessage, newItem: AiChatMessage): Boolean =
                oldItem.timestamp == newItem.timestamp && oldItem.isUser == newItem.isUser

            override fun areContentsTheSame(oldItem: AiChatMessage, newItem: AiChatMessage): Boolean =
                oldItem == newItem
        }

        /** Format timestamp thành giờ:phút */
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isUser) TYPE_USER else TYPE_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == TYPE_USER) R.layout.item_message_user else R.layout.item_message_bot
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /** ViewHolder dùng chung cho cả User và Bot messages */
    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val tvMessage: TextView = view.findViewById(
            if (view.findViewById<TextView?>(R.id.tvUserMessage) != null)
                R.id.tvUserMessage
            else
                R.id.tvBotMessage
        )

        private val tvTimestamp: TextView? = view.findViewById(R.id.tvTimestamp)

        fun bind(message: AiChatMessage) {
            tvMessage.text = message.text
            tvTimestamp?.text = timeFormat.format(Date(message.timestamp))

            // Error styling cho bot messages
            if (message.isError && !message.isUser) {
                tvMessage.setTextColor(0xFFDC2626.toInt()) // red-600
                tvMessage.setBackgroundResource(R.drawable.bg_bubble_error)
            } else if (!message.isUser) {
                // Reset về style mặc định cho bot (tránh ViewHolder tái sử dụng bị dính màu error)
                tvMessage.setTextColor(tvMessage.context.getColor(R.color.slate_700))
                tvMessage.setBackgroundResource(R.drawable.bg_bubble_bot)
            }
        }
    }
}

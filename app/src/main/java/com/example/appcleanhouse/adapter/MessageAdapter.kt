package com.example.appcleanhouse.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.R
import com.example.appcleanhouse.models.ChatMessage

class MessageAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutSent: View     = itemView.findViewById(R.id.layoutSent)
        val layoutReceived: View = itemView.findViewById(R.id.layoutReceived)
        val tvSentMessage: TextView     = itemView.findViewById(R.id.tvSentMessage)
        val tvSentTime: TextView        = itemView.findViewById(R.id.tvSentTime)
        val tvReceivedMessage: TextView = itemView.findViewById(R.id.tvReceivedMessage)
        val tvReceivedTime: TextView    = itemView.findViewById(R.id.tvReceivedTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isMe = message.senderId == currentUserId

        if (isMe) {
            holder.layoutSent.visibility     = View.VISIBLE
            holder.layoutReceived.visibility = View.GONE
            holder.tvSentMessage.text        = message.text
            holder.tvSentTime.text           = message.timeDisplay
        } else {
            holder.layoutSent.visibility     = View.GONE
            holder.layoutReceived.visibility = View.VISIBLE
            holder.tvReceivedMessage.text    = message.text
            holder.tvReceivedTime.text       = message.timeDisplay
        }
    }
}

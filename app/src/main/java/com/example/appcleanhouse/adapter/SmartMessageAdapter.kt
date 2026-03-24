package com.example.appcleanhouse.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.R

/** Simple data class for smart-chat messages */
data class SmartMessage(
    val role: String,   // "user" or "model"
    val text: String,
    val isError: Boolean = false
)

class SmartMessageAdapter(
    private val messages: List<SmartMessage>
) : RecyclerView.Adapter<SmartMessageAdapter.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_BOT  = 1
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(
            if (view.findViewById<TextView?>(R.id.tvUserMessage) != null)
                R.id.tvUserMessage
            else
                R.id.tvBotMessage
        )
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].role == "user") TYPE_USER else TYPE_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == TYPE_USER) R.layout.item_message_user else R.layout.item_message_bot
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.tvMessage.text = msg.text

        // Error styling for bot messages
        if (msg.isError && msg.role == "model") {
            holder.tvMessage.setTextColor(0xFFDC2626.toInt()) // red-600
            holder.tvMessage.setBackgroundResource(R.drawable.bg_bubble_error)
        }
    }

    override fun getItemCount() = messages.size
}

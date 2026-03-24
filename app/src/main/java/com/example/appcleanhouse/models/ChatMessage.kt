package com.example.appcleanhouse.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "", // "customer" or "cleaner"
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Trả về giờ:phút cho hiển thị trong UI */
    val timeDisplay: String
        get() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}


package com.example.appcleanhouse.models

/**
 * Data class đại diện cho một tin nhắn trong phiên chat AI.
 * @param text Nội dung tin nhắn
 * @param isUser true nếu tin nhắn của người dùng, false nếu của bot
 * @param isError true nếu tin nhắn là thông báo lỗi
 * @param timestamp Thời gian gửi tin nhắn (epoch millis)
 */
data class AiChatMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

package com.example.appcleanhouse.models

data class ChatRoom(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val cleanerId: String = "",
    val cleanerName: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L,
    val orderId: String = ""
)

package com.example.chatapp.data

data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0,
    var isSentByMe: Boolean = false,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false
)
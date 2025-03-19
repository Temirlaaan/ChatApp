package com.example.chatapp.data


import java.io.Serializable

data class ChatMetadata(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastMessageSenderId: String = "",
    val lastActive: Any? = null
) : Serializable
package com.example.chatapp.data

import java.io.Serializable

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val isOnline: Boolean = false,
    val lastActive: Long = 0
) : Serializable
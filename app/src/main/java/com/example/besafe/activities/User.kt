package com.example.besafe.activities

data class User(
    val admin: Boolean = false,
    val createdAt: Double = 0.0,
    val currentUserPhoneNumber: String = "",
    val email: String = "",
    val firstFriendPhoneNumber: String = "",
    val fourthFriendPhoneNumber: String = "",
    val name: String = "",
    val pin: String = "",
    val secondFriendPhoneNumber: String = "",
    val thirdFriendPhoneNumber: String = "",
    val userDescription: String = "",
    val userId: String = ""
)

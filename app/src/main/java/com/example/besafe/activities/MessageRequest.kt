package com.example.besafe.activities

data class MessageRequest(
    val phoneNumbers: List<String>,
    val isTest: Boolean,
    val location: String,
    val name: String
)

package com.example.food_project

data class AgentChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

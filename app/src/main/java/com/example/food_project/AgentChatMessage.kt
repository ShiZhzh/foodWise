package com.example.food_project

data class AgentAdviceDetail(
    val riskLevel: String,
    val actionItems: List<String>
)

data class AgentChatMessage(
    val role: String,
    val content: String,
    val adviceDetail: AgentAdviceDetail? = null,
    var isDetailExpanded: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

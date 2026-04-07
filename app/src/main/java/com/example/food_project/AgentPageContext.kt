package com.example.food_project

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

data class AgentPageContext(
    val pageKey: String,
    val pageTitle: String,
    val contextType: String,
    val contextPayload: Map<String, Any?> = emptyMap(),
    val contextSummary: String = ""
) {
    fun toJson(): String = gson.toJson(this)

    companion object {
        private val gson = Gson()

        fun fromJson(raw: String?): AgentPageContext? {
            if (raw.isNullOrBlank()) return null
            return try {
                gson.fromJson(raw, AgentPageContext::class.java)
            } catch (_: JsonSyntaxException) {
                null
            }
        }
    }
}

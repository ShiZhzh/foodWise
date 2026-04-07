package com.example.food_project

import android.content.Context
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AgentApiResponse(
    val success: Boolean,
    val sessionId: String,
    val answer: String,
    val riskLevel: String,
    val actionItems: List<String>,
    val sources: List<String>,
    val traceId: String,
    val raw: JSONObject
)

class AgentApiClient(private val context: Context) {

    fun sendMessage(
        userMessage: String,
        sessionId: String?,
        pageContext: AgentPageContext
    ): AgentApiResponse {
        val fixedSessionId = sessionId?.takeIf { it.isNotBlank() } ?: "s_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val userId = getUserId()

        val selectedRoute = AgentRouteSelector.select(pageContext)
        return try {
            when (selectedRoute) {
                AgentRoute.ADVICE_FROM_DISH -> callAdviceFromDish(userId, fixedSessionId, userMessage, pageContext)
                AgentRoute.ADVICE_FROM_MENU -> callAdviceFromMenu(userId, fixedSessionId, userMessage, pageContext)
                AgentRoute.CHAT -> callChat(userId, fixedSessionId, userMessage, pageContext)
            }
        } catch (_: Exception) {
            callChat(userId, fixedSessionId, userMessage, pageContext)
        }
    }

    private fun callAdviceFromDish(
        userId: String,
        sessionId: String,
        userMessage: String,
        pageContext: AgentPageContext
    ): AgentApiResponse {
        val dishes = AgentRouteSelector.extractDishes(pageContext)
        if (dishes.isEmpty()) {
            throw IllegalStateException("dish context is empty")
        }
        val body = FormBody.Builder()
            .add("userId", userId)
            .add("sessionId", sessionId)
            .add("mealType", "unknown")
            .add("dishes", JSONArray(dishes).toString())
            .add("message", userMessage)
            .build()

        val request = ApiHelper.buildAuthRequest("/api/agent/advice/from-dish", context)
            .post(body)
            .build()
        return execute(request, sessionId)
    }

    private fun callAdviceFromMenu(
        userId: String,
        sessionId: String,
        userMessage: String,
        pageContext: AgentPageContext
    ): AgentApiResponse {
        val menuItems = AgentRouteSelector.extractMenuItems(pageContext)
        val ocrText = AgentRouteSelector.extractOcrText(pageContext)
        if (menuItems.isEmpty() && ocrText.isBlank()) {
            throw IllegalStateException("menu context is empty")
        }

        val body = FormBody.Builder()
            .add("userId", userId)
            .add("sessionId", sessionId)
            .add("mealType", "unknown")
            .add("ocrText", ocrText)
            .add("menuItems", JSONArray(menuItems).toString())
            .add("message", userMessage)
            .build()

        val request = ApiHelper.buildAuthRequest("/api/agent/advice/from-menu", context)
            .post(body)
            .build()
        return execute(request, sessionId)
    }

    private fun callChat(
        userId: String,
        sessionId: String,
        userMessage: String,
        pageContext: AgentPageContext
    ): AgentApiResponse {
        val contextualMessage = buildContextualMessage(userMessage, pageContext)
        val body = FormBody.Builder()
            .add("userId", userId)
            .add("sessionId", sessionId)
            .add("message", contextualMessage)
            .build()

        val request = ApiHelper.buildAuthRequest("/api/agent/chat", context)
            .post(body)
            .build()
        return execute(request, sessionId)
    }

    private fun execute(request: Request, fallbackSessionId: String): AgentApiResponse {
        val response = ApiHelper.getAIClient().newCall(request).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}")
        }
        val json = JSONObject(body)
        if (!json.optBoolean("success", false)) {
            throw IllegalStateException(json.optString("error", "agent request failed"))
        }

        return AgentApiResponse(
            success = true,
            sessionId = json.optString("sessionId", fallbackSessionId),
            answer = json.optString("answer", ""),
            riskLevel = json.optString("riskLevel", ""),
            actionItems = parseActionItems(json.optJSONArray("actionItems")),
            sources = parseStringArray(json.optJSONArray("sources")),
            traceId = json.optString("traceId", ""),
            raw = json
        )
    }

    private fun parseActionItems(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val result = mutableListOf<String>()
        for (index in 0 until array.length()) {
            val item = array.opt(index)
            when (item) {
                is JSONObject -> {
                    val title = item.optString("title").trim()
                    val how = item.optString("how").trim()
                    val text = when {
                        title.isNotEmpty() && how.isNotEmpty() -> "$title：$how"
                        title.isNotEmpty() -> title
                        how.isNotEmpty() -> how
                        else -> item.toString()
                    }
                    if (text.isNotBlank()) result.add(text)
                }

                else -> {
                    val text = item?.toString()?.trim().orEmpty()
                    if (text.isNotEmpty()) result.add(text)
                }
            }
        }
        return result
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val out = mutableListOf<String>()
        for (index in 0 until array.length()) {
            val text = array.optString(index).trim()
            if (text.isNotEmpty()) out.add(text)
        }
        return out
    }

    private fun getUserId(): String {
        val pref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return pref.getString("userId", "") ?: ""
    }

    private fun buildContextualMessage(userMessage: String, pageContext: AgentPageContext): String {
        val summary = pageContext.contextSummary.trim()
        val payload = runCatching {
            JSONObject(pageContext.contextPayload).toString()
        }.getOrElse {
            "{}"
        }
        return buildString {
            append("当前页面: ")
            append(pageContext.pageTitle)
            append(" (")
            append(pageContext.pageKey)
            append(")\n")
            if (summary.isNotEmpty()) {
                append("页面摘要: ")
                append(summary)
                append("\n")
            }
            append("页面上下文: ")
            append(payload)
            append("\n")
            append("用户问题: ")
            append(userMessage)
        }
    }
}

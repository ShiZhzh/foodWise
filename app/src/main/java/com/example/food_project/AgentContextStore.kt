package com.example.food_project

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AgentContextStore {
    private const val MAX_CACHE_SIZE = 20
    private val cache = ConcurrentHashMap<String, AgentPageContext>()

    fun put(context: AgentPageContext): String {
        if (cache.size > MAX_CACHE_SIZE) {
            cache.clear()
        }
        val token = "ctx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        cache[token] = context
        return token
    }

    fun take(token: String?): AgentPageContext? {
        if (token.isNullOrBlank()) return null
        return cache.remove(token)
    }
}


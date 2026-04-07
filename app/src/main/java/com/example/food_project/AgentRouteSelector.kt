package com.example.food_project

enum class AgentRoute {
    CHAT,
    ADVICE_FROM_DISH,
    ADVICE_FROM_MENU
}
object AgentRouteSelector {
    fun select(pageContext: AgentPageContext): AgentRoute {
        return when (pageContext.contextType.lowercase()) {
            "dish" -> if (extractDishes(pageContext).isNotEmpty()) AgentRoute.ADVICE_FROM_DISH else AgentRoute.CHAT
            "menu" -> if (extractMenuItems(pageContext).isNotEmpty() || extractOcrText(pageContext).isNotBlank()) {
                AgentRoute.ADVICE_FROM_MENU
            } else {
                AgentRoute.CHAT
            }

            else -> AgentRoute.CHAT
        }
    }

    fun extractDishes(pageContext: AgentPageContext): List<Map<String, Any?>> {
        val raw = pageContext.contextPayload["dishes"]
        if (raw !is List<*>) return emptyList()
        val out = mutableListOf<Map<String, Any?>>()
        for (item in raw) {
            if (item is Map<*, *>) {
                val mapped = item.entries.associate { (k, v) -> k.toString() to v }
                out.add(mapped)
            }
        }
        return out
    }

    fun extractMenuItems(pageContext: AgentPageContext): List<String> {
        val raw = pageContext.contextPayload["menuItems"]
        if (raw !is List<*>) return emptyList()
        return raw.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
    }

    fun extractOcrText(pageContext: AgentPageContext): String {
        return pageContext.contextPayload["ocrText"]?.toString()?.trim().orEmpty()
    }
}

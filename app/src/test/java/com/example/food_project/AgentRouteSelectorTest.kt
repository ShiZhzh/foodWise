package com.example.food_project

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentRouteSelectorTest {

    @Test
    fun select_shouldUseDishRouteWhenDishPayloadExists() {
        val context = AgentPageContext(
            pageKey = "dish_recognition",
            pageTitle = "菜品识别",
            contextType = "dish",
            contextPayload = mapOf(
                "dishes" to listOf(mapOf("dishName" to "宫保鸡丁", "confidence" to 0.91))
            )
        )

        assertEquals(AgentRoute.ADVICE_FROM_DISH, AgentRouteSelector.select(context))
    }

    @Test
    fun select_shouldUseMenuRouteWhenMenuPayloadExists() {
        val context = AgentPageContext(
            pageKey = "menu_recognition",
            pageTitle = "菜单识别",
            contextType = "menu",
            contextPayload = mapOf(
                "ocrText" to "宫保鸡丁\n鱼香肉丝",
                "menuItems" to listOf("宫保鸡丁", "鱼香肉丝")
            )
        )

        assertEquals(AgentRoute.ADVICE_FROM_MENU, AgentRouteSelector.select(context))
    }

    @Test
    fun select_shouldFallbackToChatWithoutContext() {
        val context = AgentPageContext(
            pageKey = "main_dashboard",
            pageTitle = "首页",
            contextType = "general",
            contextPayload = emptyMap()
        )

        assertEquals(AgentRoute.CHAT, AgentRouteSelector.select(context))
    }
}

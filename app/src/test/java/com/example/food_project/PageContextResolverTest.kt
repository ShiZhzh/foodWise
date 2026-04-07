package com.example.food_project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageContextResolverTest {

    @Test
    fun resolve_shouldReturnStableMappingForKnownActivity() {
        val context = PageContextResolver.resolve(MainActivity::class.java.simpleName)
        assertEquals("main_dashboard", context.pageKey)
        assertEquals("首页", context.pageTitle)
        assertEquals("general", context.contextType)
        assertTrue(context.contextSummary.contains("首页"))
    }

    @Test
    fun resolve_shouldPreferProvidedContextWhenNotBlank() {
        val provided = AgentPageContext(
            pageKey = "custom_key",
            pageTitle = "自定义页",
            contextType = "dish",
            contextPayload = mapOf("dishes" to listOf(mapOf("dishName" to "番茄炒蛋"))),
            contextSummary = "自定义摘要"
        )

        val context = PageContextResolver.resolve(DishRecognitionActivity::class.java.simpleName, provided)
        assertEquals("custom_key", context.pageKey)
        assertEquals("自定义页", context.pageTitle)
        assertEquals("dish", context.contextType)
        assertEquals("自定义摘要", context.contextSummary)
    }
}

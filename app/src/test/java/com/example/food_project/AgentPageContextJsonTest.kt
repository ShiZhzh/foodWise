package com.example.food_project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPageContextJsonTest {

    @Test
    fun toJsonAndFromJson_shouldKeepCoreFieldsAndPayload() {
        val source = AgentPageContext(
            pageKey = "dish_recognition",
            pageTitle = "菜品图片识别",
            contextType = "dish",
            contextPayload = mapOf(
                "dishes" to listOf(
                    mapOf("dishName" to "西红柿鸡蛋", "calorie" to "150")
                ),
                "recognizedCount" to 1
            ),
            contextSummary = "识别到1个菜品"
        )

        val parsed = AgentPageContext.fromJson(source.toJson())
        assertNotNull(parsed)
        parsed!!
        assertEquals("dish_recognition", parsed.pageKey)
        assertEquals("菜品图片识别", parsed.pageTitle)
        assertEquals("dish", parsed.contextType)
        assertEquals("识别到1个菜品", parsed.contextSummary)

        val dishes = AgentRouteSelector.extractDishes(parsed)
        assertTrue(dishes.isNotEmpty())
        assertEquals("西红柿鸡蛋", dishes.first()["dishName"])
    }
}

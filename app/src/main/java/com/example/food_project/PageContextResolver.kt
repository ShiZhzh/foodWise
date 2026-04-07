package com.example.food_project

data class AgentPageMeta(
    val pageKey: String,
    val pageTitle: String,
    val contextType: String
)

object PageContextResolver {
    private val defaultMeta = AgentPageMeta(
        pageKey = "general",
        pageTitle = "健康助手",
        contextType = "general"
    )

    private val pageMetaByActivityName = mapOf(
        MainActivity::class.java.simpleName to AgentPageMeta("main_dashboard", "首页", "general"),
        FoodAnalysisActivity::class.java.simpleName to AgentPageMeta("food_analysis", "菜品分析", "general"),
        FoodRecommendActivity::class.java.simpleName to AgentPageMeta("food_recommend", "AI美食推荐", "general"),
        HealthManagementActivity::class.java.simpleName to AgentPageMeta("health_management", "健康管理", "general"),
        DishRecognitionActivity::class.java.simpleName to AgentPageMeta("dish_recognition", "菜品图片识别", "dish"),
        MenuRecognitionActivity::class.java.simpleName to AgentPageMeta("menu_recognition", "菜单识别", "menu"),
        TodayIntakeActivity::class.java.simpleName to AgentPageMeta("today_intake", "今日摄入记录", "general"),
        UserDetailActivity::class.java.simpleName to AgentPageMeta("user_detail", "用户详情", "general"),
        DishDetailActivity::class.java.simpleName to AgentPageMeta("dish_detail", "菜品详情", "general"),
        AgentChatActivity::class.java.simpleName to AgentPageMeta("agent_chat", "健康助手", "general")
    )

    fun resolve(activityName: String, provided: AgentPageContext? = null): AgentPageContext {
        val meta = pageMetaByActivityName[activityName] ?: defaultMeta
        val payload = provided?.contextPayload ?: emptyMap()

        val key = provided?.pageKey?.takeIf { it.isNotBlank() } ?: meta.pageKey
        val title = provided?.pageTitle?.takeIf { it.isNotBlank() } ?: meta.pageTitle
        val type = provided?.contextType?.takeIf { it.isNotBlank() } ?: meta.contextType
        val summary = provided?.contextSummary?.takeIf { it.isNotBlank() } ?: "当前页面：$title"

        return AgentPageContext(
            pageKey = key,
            pageTitle = title,
            contextType = type,
            contextPayload = payload,
            contextSummary = summary
        )
    }
}

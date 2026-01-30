package com.example.food_project

/**
 * AI推荐缓存单例
 * 在应用运行期间（不退出程序）全局共享，只加载一次
 */
object DietRecommendCache {
    private var hasLoadedForUser: String? = null  // 已加载的用户ID
    private val cachedDishes = mutableListOf<DietDish>()
    private var cachedOverallReason: String = ""
    
    /**
     * 检查是否已为指定用户加载过推荐
     */
    fun hasLoaded(userId: String): Boolean {
        return hasLoadedForUser == userId && cachedDishes.isNotEmpty()
    }
    
    /**
     * 获取缓存的菜品列表（返回副本，避免外部修改）
     */
    fun getDishes(): List<DietDish> {
        return cachedDishes.toList()
    }
    
    /**
     * 获取缓存的总体推荐理由
     */
    fun getOverallReason(): String {
        return cachedOverallReason
    }
    
    /**
     * 保存推荐数据到缓存
     */
    fun saveDishes(userId: String, dishes: List<DietDish>, overallReason: String) {
        hasLoadedForUser = userId
        cachedDishes.clear()
        cachedDishes.addAll(dishes)
        cachedOverallReason = overallReason
    }
    
    /**
     * 清空缓存（用于手动刷新或用户登出）
     */
    fun clear() {
        hasLoadedForUser = null
        cachedDishes.clear()
        cachedOverallReason = ""
    }
}

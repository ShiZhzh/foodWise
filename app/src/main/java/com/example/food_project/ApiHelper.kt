package com.example.food_project

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ApiHelper {
    private const val BASE_URL = "http://10.0.2.2:5000"
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // 获取存储的Token
    fun getToken(context: Context): String? {
        val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("token", null)
    }
    
    // 保存Token
    fun saveToken(context: Context, token: String) {
        val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("token", token).apply()
    }
    
    // 清除Token（退出登录）
    fun clearToken(context: Context) {
        val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("token").apply()
    }
    
    // 构建带Token的请求
    fun buildAuthRequest(url: String, context: Context): Request.Builder {
        val token = getToken(context)
        return Request.Builder()
            .url("$BASE_URL$url")
            .apply {
                if (!token.isNullOrEmpty()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
    }
    
    // 获取OkHttpClient实例
    fun getClient(): OkHttpClient = okHttpClient
    
    // 获取完整URL
    fun getUrl(endpoint: String): String = "$BASE_URL$endpoint"
}

package com.example.food_project

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class FoodRecommendActivity : AppCompatActivity() {
    // 只保留核心控件，去掉多余声明
    private lateinit var rgRecommendType: RadioGroup
    private lateinit var btnGetRecommend: Button
    private lateinit var tvRecommendEmpty: TextView
    private lateinit var btnRefreshRecommend: Button
    private lateinit var actvDishSearch: AutoCompleteTextView
    private lateinit var btnSearchRelated: Button
    private lateinit var tvRelatedEmpty: TextView
    private lateinit var btnBack: android.view.View

    // 新增：存储四个RadioButton的FrameLayout容器
    private lateinit var frameWeightLoss: FrameLayout
    private lateinit var frameWeightGain: FrameLayout
    private lateinit var frameHomeCooking: FrameLayout
    private lateinit var frameQuickMeal: FrameLayout

    // 推荐类型（仅用于临时存储选择状态）
    private var selectedRecommendType: String = ""

    // 简化历史菜品（仅用于搜索框提示）
    private val historyDishes = arrayOf("番茄炒蛋", "清蒸鱼", "宫保鸡丁", "凉拌黄瓜", "红烧肉")

    // 定义高度常量（dp转px）
    private val normalHeightDp = 120
    private val expandedHeightDp = 180
    private var normalHeightPx = 0
    private var expandedHeightPx = 0

    private val okHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_recommend)

        // 计算px值
        normalHeightPx = (normalHeightDp * resources.displayMetrics.density).toInt()
        expandedHeightPx = (expandedHeightDp * resources.displayMetrics.density).toInt()

        // 绑定核心控件
        initViews()
        // 初始化搜索框提示（仅保留基础功能）
        initSearchBox()
        // 设置按钮点击事件（空实现，仅提示）
        setButtonListeners()
        // 单选按钮组监听
        setRadioGroupListener()
    }

    private fun initViews() {
        rgRecommendType = findViewById(R.id.rg_recommend_type)
        btnGetRecommend = findViewById(R.id.btn_get_recommend)
        tvRecommendEmpty = findViewById(R.id.tv_recommend_empty)
        btnRefreshRecommend = findViewById(R.id.btn_refresh_recommend)
        actvDishSearch = findViewById(R.id.actv_dish_search)
        btnSearchRelated = findViewById(R.id.btn_search_related)
        tvRelatedEmpty = findViewById(R.id.tv_related_empty)
        btnBack = findViewById(R.id.btn_back)

        // 获取FrameLayout和RadioButton
        frameWeightLoss = findViewById(R.id.frame_weight_loss)
        frameWeightGain = findViewById(R.id.frame_weight_gain)
        frameHomeCooking = findViewById(R.id.frame_home_cooking)
        frameQuickMeal = findViewById(R.id.frame_quick_meal)

        // 修改：为FrameLayout设置点击监听，使用RadioGroup.check()方法触发选中
        frameWeightLoss.setOnClickListener {
            rgRecommendType.check(R.id.rb_weight_loss)
        }
        frameWeightGain.setOnClickListener {
            rgRecommendType.check(R.id.rb_weight_gain)
        }
        frameHomeCooking.setOnClickListener {
            rgRecommendType.check(R.id.rb_home_cooking)
        }
        frameQuickMeal.setOnClickListener {
            rgRecommendType.check(R.id.rb_quick_meal)
        }
    }

    // 简化搜索框：仅保留历史菜品提示，去掉复杂逻辑
    private fun initSearchBox() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, historyDishes)
        actvDishSearch.setAdapter(adapter)
        actvDishSearch.threshold = 1 // 输入1个字符提示
    }

    // 单选按钮组监听：记录选择状态并调整高度
    private fun setRadioGroupListener() {
        rgRecommendType.setOnCheckedChangeListener { _, checkedId ->
            selectedRecommendType = when (checkedId) {
                R.id.rb_weight_loss -> {
                    updateFrameHeights(frameWeightLoss)
                    "减重低卡"
                }
                R.id.rb_weight_gain -> {
                    updateFrameHeights(frameWeightGain)
                    "增重高蛋白"
                }
                R.id.rb_home_cooking -> {
                    updateFrameHeights(frameHomeCooking)
                    "家常菜"
                }
                R.id.rb_quick_meal -> {
                    updateFrameHeights(frameQuickMeal)
                    "快手餐"
                }
                else -> {
                    resetAllFrameHeights()
                    ""
                }
            }
        }
    }

    // 修改：更新FrameLayout高度和透明度的方法
    private fun updateFrameHeights(selectedFrame: FrameLayout) {
        listOf(frameWeightLoss, frameWeightGain, frameHomeCooking, frameQuickMeal).forEach { frame ->
            val layoutParams = frame.layoutParams

            if (frame == selectedFrame) {
                // 选中的：变大 + 移除半透明遮罩
                layoutParams.height = expandedHeightPx
                // 找到半透明遮罩View（第二个子View）并隐藏
                if (frame.childCount >= 2) {
                    frame.getChildAt(1).visibility = android.view.View.GONE
                }
            } else {
                // 未选中的：正常大小 + 显示半透明遮罩
                layoutParams.height = normalHeightPx
                // 恢复半透明遮罩
                if (frame.childCount >= 2) {
                    frame.getChildAt(1).visibility = android.view.View.VISIBLE
                }
            }

            frame.layoutParams = layoutParams
        }
    }

    // 修改：重置所有Frame为正常高度并恢复遮罩
    private fun resetAllFrameHeights() {
        listOf(frameWeightLoss, frameWeightGain, frameHomeCooking, frameQuickMeal).forEach { frame ->
            val layoutParams = frame.layoutParams
            layoutParams.height = normalHeightPx
            frame.layoutParams = layoutParams

            // 恢复半透明遮罩
            if (frame.childCount >= 2) {
                frame.getChildAt(1).visibility = android.view.View.VISIBLE
            }
        }
    }

    // 按钮点击事件：实现网络请求
    private fun setButtonListeners() {
        // 返回按钮（添加日志确认触发）
        btnBack.setOnClickListener {
            android.util.Log.d("FoodRecommend", "返回按钮被点击")
            finish()
        }

        // 获取推荐菜品：从后端获取数据
        btnGetRecommend.setOnClickListener {
            if (selectedRecommendType.isEmpty()) {
                Toast.makeText(this, "请选择推荐类型", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 显示加载状态
            tvRecommendEmpty.text = "正在加载推荐菜品..."
            btnRefreshRecommend.visibility = Button.GONE

            Thread {
                try {
                    val encodedType = URLEncoder.encode(selectedRecommendType, "UTF-8")
                    val request = Request.Builder()
                        .url("http://10.0.2.2:5000/api/getRecommendDishes?type=$encodedType")
                        .get()
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val responseBody = response.body?.string()

                    runOnUiThread {
                        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                            try {
                                val json = JSONObject(responseBody)
                                if (json.has("error")) {
                                    showDefaultRecommend()
                                } else {
                                    val dishes = json.getJSONArray("dishes")
                                    val dishList = mutableListOf<String>()
                                    for (i in 0 until dishes.length()) {
                                        dishList.add(dishes.getString(i))
                                    }
                                    showRecommendResult(dishList)
                                }
                            } catch (e: Exception) {
                                showDefaultRecommend()
                            }
                        } else {
                            showDefaultRecommend()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { showDefaultRecommend() }
                }
            }.start()
        }

        // 换一批：重新请求
        btnRefreshRecommend.setOnClickListener {
            btnGetRecommend.performClick()
        }

        // 搜索相关菜品：从后端获取数据
        btnSearchRelated.setOnClickListener {
            val dishName = actvDishSearch.text.toString().trim()
            if (dishName.isEmpty()) {
                Toast.makeText(this, "请输入/选择菜品名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 显示加载状态
            tvRelatedEmpty.text = "正在搜索相关菜品..."

            Thread {
                try {
                    val encodedDish = URLEncoder.encode(dishName, "UTF-8")
                    val request = Request.Builder()
                        .url("http://10.0.2.2:5000/api/getRelatedDishes?dishName=$encodedDish")
                        .get()
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val responseBody = response.body?.string()

                    runOnUiThread {
                        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                            try {
                                val json = JSONObject(responseBody)
                                if (json.has("error")) {
                                    showDefaultRelated(dishName)
                                } else {
                                    val related = json.getJSONArray("relatedDishes")
                                    val relatedList = mutableListOf<String>()
                                    for (i in 0 until related.length()) {
                                        relatedList.add(related.getString(i))
                                    }
                                    showRelatedResult(dishName, relatedList)
                                }
                            } catch (e: Exception) {
                                showDefaultRelated(dishName)
                            }
                        } else {
                            showDefaultRelated(dishName)
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { showDefaultRelated(dishName) }
                }
            }.start()
        }
    }

    // 显示推荐结果
    private fun showRecommendResult(dishes: List<String>) {
        val result = dishes.joinToString("、")
        tvRecommendEmpty.text = "推荐菜品：$result"
        btnRefreshRecommend.visibility = Button.VISIBLE
        Toast.makeText(this, "获取成功", Toast.LENGTH_SHORT).show()
    }

    // 修改：显示获取失败提示（不再显示默认推荐）
    private fun showDefaultRecommend() {
        tvRecommendEmpty.text = "获取失败：无法连接服务器或服务器返回错误"
        btnRefreshRecommend.visibility = Button.GONE
        Toast.makeText(this, "获取推荐失败", Toast.LENGTH_SHORT).show()
    }

    // 显示相关菜品结果
    private fun showRelatedResult(dishName: String, relatedDishes: List<String>) {
        val result = relatedDishes.joinToString("、")
        tvRelatedEmpty.text = "与「$dishName」相关的菜品：$result"
        Toast.makeText(this, "搜索成功", Toast.LENGTH_SHORT).show()
    }

    // 修改：显示获取失败提示（不再显示默认相关菜品）
    private fun showDefaultRelated(dishName: String) {
        tvRelatedEmpty.text = "获取失败：无法连接服务器或服务器返回错误"
        Toast.makeText(this, "搜索相关菜品失败", Toast.LENGTH_SHORT).show()
    }
}
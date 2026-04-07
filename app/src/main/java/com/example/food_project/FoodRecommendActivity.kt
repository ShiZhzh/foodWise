package com.example.food_project

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import android.util.Log // 新增：导入日志类

class FoodRecommendActivity : AppCompatActivity() {
    private lateinit var rgRecommendType: RadioGroup
    private lateinit var etUserNote: EditText
    private lateinit var btnGetRecommend: Button
    private lateinit var tvRecommendEmpty: TextView
    private lateinit var btnRefreshRecommend: Button
    private lateinit var actvIngredientSearch: AutoCompleteTextView
    private lateinit var etIngredientNote: EditText
    private lateinit var btnSearchByIngredient: Button
    private lateinit var tvIngredientEmpty: TextView
    private lateinit var btnBack: android.view.View

    private lateinit var frameWeightLoss: FrameLayout
    private lateinit var frameWeightGain: FrameLayout
    private lateinit var frameHomeCooking: FrameLayout
    private lateinit var frameQuickMeal: FrameLayout

    private var selectedRecommendType: String = ""
    private val historyIngredients = arrayOf("西红柿", "鸡蛋", "土豆", "黄瓜", "鸡胸肉", "三文鱼")

    private val normalHeightDp = 120
    private val expandedHeightDp = 180
    private var normalHeightPx = 0
    private var expandedHeightPx = 0

    private var loginUserId: String = ""
    
    // 存储推荐结果
    private val typeRecommendDishes = mutableListOf<DishInfo>()
    private val ingredientRecommendDishes = mutableListOf<DishInfo>()

    data class DishInfo(
        val dishName: String,
        val cookingMethod: String,
        val calories: String,
        val protein: String? = null,
        val fiber: String? = null,
        val sodium: String,
        val nutritionHighlight: String? = null,
        val difficulty: String,
        val cookingTime: String
    )

    companion object {
        private const val TAG = "FoodRecommend" // 日志标签
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_recommend)
        AgentEntryBinder.bind(this)

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        loginUserId = sharedPref.getString("userId", "") ?: ""

        normalHeightPx = (normalHeightDp * resources.displayMetrics.density).toInt()
        expandedHeightPx = (expandedHeightDp * resources.displayMetrics.density).toInt()

        initViews()
        initSearchBox()
        setButtonListeners()
        setRadioGroupListener()
    }

    private fun initViews() {
        rgRecommendType = findViewById(R.id.rg_recommend_type)
        etUserNote = findViewById(R.id.et_user_note)
        btnGetRecommend = findViewById(R.id.btn_get_recommend)
        tvRecommendEmpty = findViewById(R.id.tv_recommend_empty)
        btnRefreshRecommend = findViewById(R.id.btn_refresh_recommend)
        actvIngredientSearch = findViewById(R.id.actv_ingredient_search)
        etIngredientNote = findViewById(R.id.et_ingredient_note)
        btnSearchByIngredient = findViewById(R.id.btn_search_by_ingredient)
        tvIngredientEmpty = findViewById(R.id.tv_ingredient_empty)
        btnBack = findViewById(R.id.btn_back)

        frameWeightLoss = findViewById(R.id.frame_weight_loss)
        frameWeightGain = findViewById(R.id.frame_weight_gain)
        frameHomeCooking = findViewById(R.id.frame_home_cooking)
        frameQuickMeal = findViewById(R.id.frame_quick_meal)

        frameWeightLoss.setOnClickListener { rgRecommendType.check(R.id.rb_weight_loss) }
        frameWeightGain.setOnClickListener { rgRecommendType.check(R.id.rb_weight_gain) }
        frameHomeCooking.setOnClickListener { rgRecommendType.check(R.id.rb_home_cooking) }
        frameQuickMeal.setOnClickListener { rgRecommendType.check(R.id.rb_quick_meal) }
    }

    private fun initSearchBox() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, historyIngredients)
        actvIngredientSearch.setAdapter(adapter)
        actvIngredientSearch.threshold = 1
    }

    private fun setRadioGroupListener() {
        rgRecommendType.setOnCheckedChangeListener { _, checkedId ->
            selectedRecommendType = when (checkedId) {
                R.id.rb_weight_loss -> {
                    updateFrameHeights(frameWeightLoss)
                    "低卡"
                }
                R.id.rb_weight_gain -> {
                    updateFrameHeights(frameWeightGain)
                    "高蛋白"
                }
                R.id.rb_home_cooking -> {
                    updateFrameHeights(frameHomeCooking)
                    "高纤维"
                }
                R.id.rb_quick_meal -> {
                    updateFrameHeights(frameQuickMeal)
                    "高钙"
                }
                else -> {
                    resetAllFrameHeights()
                    ""
                }
            }
        }
    }

    private fun updateFrameHeights(selectedFrame: FrameLayout) {
        listOf(frameWeightLoss, frameWeightGain, frameHomeCooking, frameQuickMeal).forEach { frame ->
            val layoutParams = frame.layoutParams
            if (frame == selectedFrame) {
                layoutParams.height = expandedHeightPx
                if (frame.childCount >= 2) {
                    frame.getChildAt(1).visibility = android.view.View.GONE
                }
            } else {
                layoutParams.height = normalHeightPx
                if (frame.childCount >= 2) {
                    frame.getChildAt(1).visibility = android.view.View.VISIBLE
                }
            }
            frame.layoutParams = layoutParams
        }
    }

    private fun resetAllFrameHeights() {
        listOf(frameWeightLoss, frameWeightGain, frameHomeCooking, frameQuickMeal).forEach { frame ->
            val layoutParams = frame.layoutParams
            layoutParams.height = normalHeightPx
            frame.layoutParams = layoutParams
            if (frame.childCount >= 2) {
                frame.getChildAt(1).visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun setButtonListeners() {
        btnBack.setOnClickListener { finish() }

        // 根据类型获取AI推荐
        btnGetRecommend.setOnClickListener {
            if (selectedRecommendType.isEmpty()) {
                Toast.makeText(this, "请选择推荐类型", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userNote = etUserNote.text.toString().trim()
            Log.d(TAG, "开始AI推荐，类型: $selectedRecommendType, 用户备注: $userNote, userId: $loginUserId")
            
            tvRecommendEmpty.text = "正在AI推荐菜品...\n(AI思考中，请稍候20-60秒)"
            btnRefreshRecommend.visibility = Button.GONE
            typeRecommendDishes.clear()

            Thread {
                try {
                    val requestBody = FormBody.Builder()
                        .add("dishType", selectedRecommendType)
                        .add("userId", loginUserId)
                        .add("userNote", userNote)
                        .build()

                    val url = ApiHelper.getUrl("/api/ai/recommendByType")
                    Log.d(TAG, "请求URL: $url")
                    
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    Log.d(TAG, "发送AI类型推荐请求...")
                    val startTime = System.currentTimeMillis()
                    val response = ApiHelper.getAIClient().newCall(request).execute()
                    val elapsedTime = System.currentTimeMillis() - startTime
                    
                    val responseBody = response.body?.string()
                    Log.d(TAG, "收到响应，耗时: ${elapsedTime}ms")
                    Log.d(TAG, "响应状态码: ${response.code}")
                    
                    if (!responseBody.isNullOrEmpty()) {
                        Log.d(TAG, "响应内容: $responseBody")
                    }

                    runOnUiThread {
                        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                            try {
                                val json = JSONObject(responseBody)
                                if (json.optBoolean("success", false)) {
                                    val dishes = json.getJSONArray("dishes")
                                    Log.d(TAG, "推荐菜品数量: ${dishes.length()}")
                                    
                                    for (i in 0 until dishes.length()) {
                                        val dish = dishes.getJSONObject(i)
                                        val dishName = dish.getString("dishName")
                                        Log.d(TAG, "解析菜品 $i: $dishName")
                                        
                                        typeRecommendDishes.add(DishInfo(
                                            dishName,
                                            dish.getString("cookingMethod"),
                                            dish.getString("calories"),
                                            dish.optString("protein", null),
                                            dish.optString("fiber", null),
                                            dish.getString("sodium"),
                                            dish.optString("nutritionHighlight", null),
                                            dish.getString("difficulty"),
                                            dish.getString("cookingTime")
                                        ))
                                    }
                                    showTypeRecommendResult()
                                } else {
                                    val error = json.optString("error", "未知错误")
                                    Log.e(TAG, "AI推荐失败: $error")
                                    showDefaultTypeRecommend()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "JSON解析异常", e)
                                showDefaultTypeRecommend()
                            }
                        } else {
                            Log.e(TAG, "响应失败，HTTP ${response.code}: ${response.message}")
                            showDefaultTypeRecommend()
                        }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    Log.e(TAG, "AI推荐超时", e)
                    runOnUiThread { 
                        tvRecommendEmpty.text = "AI推荐超时：服务器响应时间过长(>60秒)，请稍后重试"
                        Toast.makeText(this, "AI推荐超时，请重试", Toast.LENGTH_LONG).show()
                    }
                } catch (e: java.net.UnknownHostException) {
                    Log.e(TAG, "无法连接到服务器: 主机未知", e)
                    runOnUiThread {
                        tvRecommendEmpty.text = "无法连接AI服务器：请检查网络设置"
                        Toast.makeText(this, "无法连接服务器", Toast.LENGTH_LONG).show()
                    }
                } catch (e: java.net.ConnectException) {
                    Log.e(TAG, "连接服务器失败", e)
                    runOnUiThread {
                        tvRecommendEmpty.text = "连接AI服务器失败：服务器可能未启动"
                        Toast.makeText(this, "连接失败", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AI推荐异常: ${e.javaClass.simpleName} - ${e.message}", e)
                    runOnUiThread { showDefaultTypeRecommend() }
                }
            }.start()
        }

        btnRefreshRecommend.setOnClickListener {
            btnGetRecommend.performClick()
        }

        // 根据食材获取AI推荐
        btnSearchByIngredient.setOnClickListener {
            val ingredient = actvIngredientSearch.text.toString().trim()
            if (ingredient.isEmpty()) {
                Toast.makeText(this, "请输入食材名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userNote = etIngredientNote.text.toString().trim()
            Log.d(TAG, "开始AI食材推荐，食材: $ingredient, 用户备注: $userNote, userId: $loginUserId")
            
            tvIngredientEmpty.text = "正在AI推荐菜品...\n(AI思考中，请稍候20-60秒)"
            ingredientRecommendDishes.clear()

            Thread {
                try {
                    val requestBody = FormBody.Builder()
                        .add("ingredient", ingredient)
                        .add("userId", loginUserId)
                        .add("userNote", userNote)
                        .build()

                    val url = ApiHelper.getUrl("/api/ai/recommendByIngredient")
                    Log.d(TAG, "请求URL: $url")
                    
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    Log.d(TAG, "发送AI食材推荐请求...")
                    val startTime = System.currentTimeMillis()
                    val response = ApiHelper.getAIClient().newCall(request).execute()
                    val elapsedTime = System.currentTimeMillis() - startTime
                    
                    val responseBody = response.body?.string()
                    Log.d(TAG, "收到响应，耗时: ${elapsedTime}ms")
                    Log.d(TAG, "响应状态码: ${response.code}")
                    
                    if (!responseBody.isNullOrEmpty()) {
                        Log.d(TAG, "响应内容: $responseBody")
                    }

                    runOnUiThread {
                        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                            try {
                                val json = JSONObject(responseBody)
                                if (json.optBoolean("success", false)) {
                                    val dishes = json.getJSONArray("dishes")
                                    Log.d(TAG, "推荐菜品数量: ${dishes.length()}")
                                    
                                    for (i in 0 until dishes.length()) {
                                        val dish = dishes.getJSONObject(i)
                                        val dishName = dish.getString("dishName")
                                        Log.d(TAG, "解析菜品 $i: $dishName")
                                        
                                        ingredientRecommendDishes.add(DishInfo(
                                            dishName,
                                            dish.getString("cookingMethod"),
                                            dish.getString("calories"),
                                            null,
                                            null,
                                            dish.getString("sodium"),
                                            null,
                                            dish.getString("difficulty"),
                                            dish.getString("cookingTime")
                                        ))
                                    }
                                    showIngredientRecommendResult(ingredient)
                                } else {
                                    val error = json.optString("error", "未知错误")
                                    Log.e(TAG, "AI食材推荐失败: $error")
                                    showDefaultIngredientRecommend(ingredient)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "JSON解析异常", e)
                                showDefaultIngredientRecommend(ingredient)
                            }
                        } else {
                            Log.e(TAG, "响应失败，HTTP ${response.code}: ${response.message}")
                            showDefaultIngredientRecommend(ingredient)
                        }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    Log.e(TAG, "AI食材推荐超时", e)
                    runOnUiThread { 
                        tvIngredientEmpty.text = "AI推荐超时：服务器响应时间过长(>60秒)，请稍后重试"
                        Toast.makeText(this, "AI推荐超时，请重试", Toast.LENGTH_LONG).show()
                    }
                } catch (e: java.net.UnknownHostException) {
                    Log.e(TAG, "无法连接到服务器: 主机未知", e)
                    runOnUiThread {
                        tvIngredientEmpty.text = "无法连接AI服务器：请检查网络设置"
                        Toast.makeText(this, "无法连接服务器", Toast.LENGTH_LONG).show()
                    }
                } catch (e: java.net.ConnectException) {
                    Log.e(TAG, "连接服务器失败", e)
                    runOnUiThread {
                        tvIngredientEmpty.text = "连接AI服务器失败：服务器可能未启动"
                        Toast.makeText(this, "连接失败", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AI食材推荐异常: ${e.javaClass.simpleName} - ${e.message}", e)
                    runOnUiThread { showDefaultIngredientRecommend(ingredient) }
                }
            }.start()
        }
    }

    private fun showTypeRecommendResult() {
        val result = typeRecommendDishes.joinToString("、") { it.dishName }
        tvRecommendEmpty.text = "推荐菜品：$result\n点击查看详情"
        tvRecommendEmpty.setOnClickListener {
            showDishListDialog(typeRecommendDishes)
        }
        btnRefreshRecommend.visibility = Button.VISIBLE
        Toast.makeText(this, "AI推荐成功", Toast.LENGTH_SHORT).show()
    }

    private fun showDefaultTypeRecommend() {
        tvRecommendEmpty.text = "获取失败：无法连接AI服务器或返回错误"
        tvRecommendEmpty.setOnClickListener(null)
        btnRefreshRecommend.visibility = Button.GONE
        Toast.makeText(this, "AI推荐失败", Toast.LENGTH_SHORT).show()
    }

    private fun showIngredientRecommendResult(ingredient: String) {
        val result = ingredientRecommendDishes.joinToString("、") { it.dishName }
        tvIngredientEmpty.text = "基于「$ingredient」推荐：$result\n点击查看详情"
        tvIngredientEmpty.setOnClickListener {
            showDishListDialog(ingredientRecommendDishes)
        }
        Toast.makeText(this, "AI推荐成功", Toast.LENGTH_SHORT).show()
    }

    private fun showDefaultIngredientRecommend(ingredient: String) {
        tvIngredientEmpty.text = "获取失败：无法连接AI服务器或返回错误"
        tvIngredientEmpty.setOnClickListener(null)
        Toast.makeText(this, "AI推荐失败", Toast.LENGTH_SHORT).show()
    }

    // 显示菜品列表选择对话框
    private fun showDishListDialog(dishes: List<DishInfo>) {
        val dishNames = dishes.map { it.dishName }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择菜品查看详情")
            .setItems(dishNames) { _, which ->
                val selectedDish = dishes[which]
                openDishDetail(selectedDish)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 跳转到菜品详情页
    private fun openDishDetail(dish: DishInfo) {
        val intent = Intent(this, DishDetailActivity::class.java)
        intent.putExtra("dishName", dish.dishName)
        intent.putExtra("cookingMethod", dish.cookingMethod)
        intent.putExtra("calories", dish.calories)
        intent.putExtra("protein", dish.protein)
        intent.putExtra("fiber", dish.fiber)
        intent.putExtra("sodium", dish.sodium)
        intent.putExtra("nutritionHighlight", dish.nutritionHighlight)
        intent.putExtra("difficulty", dish.difficulty)
        intent.putExtra("cookingTime", dish.cookingTime)
        startActivity(intent)
    }
}

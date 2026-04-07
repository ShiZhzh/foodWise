package com.example.food_project

import android.os.Bundle
import android.widget.Button
import androidx.cardview.widget.CardView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.util.Log
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.core.content.ContextCompat
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class DietDish(
    val name: String, 
    val calorie: String,
    val cookingMethod: String = "",
    val reason: String = ""
)

class DietAdapter(
    private val dishList: List<DietDish>,
    private val onItemClick: (DietDish) -> Unit
) : RecyclerView.Adapter<DietAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_dish_name)
        val tvCalorie: TextView = itemView.findViewById(R.id.tv_dish_calorie)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diet_recommend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dish = dishList[position]
        holder.tvName.text = dish.name
        holder.tvCalorie.text = dish.calorie
        holder.itemView.setOnClickListener {
            onItemClick(dish)
        }
    }

    override fun getItemCount(): Int {
        return dishList.size
    }
}

class HealthManagementActivity : AppCompatActivity() {
    private lateinit var etWeight: EditText
    private lateinit var btnSaveWeight: Button
    private lateinit var tvWeightStatus: TextView
    private lateinit var chartWeight: LineChart
    private lateinit var tvWeightStats: TextView
    private lateinit var rvDietRecommend: RecyclerView
    private lateinit var cardTodayIntake: CardView
    private lateinit var btnBack: android.view.View
    private lateinit var tvUserName: TextView
    private lateinit var tvUserId: TextView
    private lateinit var cardUserInfo: CardView
    private lateinit var btnRefreshDiet: Button

    private var loginUserId: String = ""
    private var loginUsername: String = ""
    
    companion object {
        private const val TAG = "HealthManagement"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_management)
        AgentEntryBinder.bind(this)
        
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        loginUserId = sharedPref.getString("userId", "") ?: ""
        loginUsername = sharedPref.getString("username", "") ?: ""

        initViews()
        setupWeightChart()
        
        tvUserName.text = if (loginUsername.isEmpty()) "用户未登录" else loginUsername
        tvUserId.text = if (loginUserId.isEmpty()) "ID: 未设置" else "ID: $loginUserId"
        
        checkAndLoadDietRecommend()
        loadTodayWeight()
        loadWeightHistory()
        setButtonListeners()
        
        cardUserInfo.setOnClickListener {
            startActivity(Intent(this, UserDetailActivity::class.java))
        }
    }

    private fun initViews() {
        etWeight = findViewById(R.id.et_weight)
        btnSaveWeight = findViewById(R.id.btn_save_weight)
        tvWeightStatus = findViewById(R.id.tv_weight_status)
        chartWeight = findViewById(R.id.chart_weight)
        tvWeightStats = findViewById(R.id.tv_weight_stats)
        rvDietRecommend = findViewById(R.id.rv_diet_recommend)
        cardTodayIntake = findViewById(R.id.card_today_intake)
        btnBack = findViewById(R.id.btn_back)
        tvUserName = findViewById(R.id.tv_user_name)
        tvUserId = findViewById(R.id.tv_user_id)
        cardUserInfo = findViewById(R.id.card_user_info)
        btnRefreshDiet = findViewById(R.id.btn_refresh_diet)
    }

    private fun setupWeightChart() {
        chartWeight.apply {
            description.isEnabled = true
            description.text = "近30天体重趋势"
            description.textSize = 12f
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textSize = 10f
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun checkAndLoadDietRecommend() {
        if (DietRecommendCache.hasLoaded(loginUserId)) {
            // 使用全局缓存的数据
            Log.d(TAG, "使用全局缓存的AI推荐数据")
            val cachedDishes = DietRecommendCache.getDishes()
            val overallReason = DietRecommendCache.getOverallReason()
            
            initDietListWithData(cachedDishes)
            if (overallReason.isNotEmpty()) {
                Toast.makeText(this, "AI推荐(已缓存): $overallReason", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 首次加载，从服务器获取
            Log.d(TAG, "首次加载，从服务器获取AI推荐")
            loadAIDietRecommend(loginUserId)
        }
    }

    private fun loadTodayWeight() {
        if (loginUserId.isEmpty()) return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        Thread {
            try {
                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/weight/getByDate?userId=$loginUserId&recordDate=$today"))
                    .get()
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                val record = json.getJSONObject("record")
                                val weight = record.getDouble("weight")
                                etWeight.setText(weight.toString())
                                tvWeightStatus.text = "今日已记录体重: ${weight}kg"
                            } else {
                                tvWeightStatus.text = "今日尚未记录体重"
                            }
                        } catch (e: Exception) {
                            tvWeightStatus.text = "加载失败"
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvWeightStatus.text = "网络异常"
                }
            }
        }.start()
    }

    private fun loadWeightHistory() {
        if (loginUserId.isEmpty()) return

        Thread {
            try {
                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/weight/getHistory?userId=$loginUserId&days=30"))
                    .get()
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                val dataArray = json.getJSONArray("data")
                                if (dataArray.length() > 0) {
                                    drawWeightChart(dataArray)
                                    loadWeightStatistics()
                                } else {
                                    showEmptyChart()
                                    tvWeightStats.text = "暂无体重记录"
                                }
                            } else {
                                showEmptyChart()
                            }
                        } catch (e: Exception) {
                            showEmptyChart()
                        }
                    } else {
                        showEmptyChart()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { showEmptyChart() }
            }
        }.start()
    }

    private fun drawWeightChart(dataArray: org.json.JSONArray) {
        val entries = ArrayList<Entry>()
        val dates = ArrayList<String>()

        for (i in 0 until dataArray.length()) {
            val item = dataArray.getJSONObject(i)
            val weight = item.getDouble("weight").toFloat()
            val date = item.getString("date")
            entries.add(Entry(i.toFloat(), weight))
            dates.add(date.substring(5))
        }

        val dataSet = LineDataSet(entries, "体重 (kg)").apply {
            color = ContextCompat.getColor(this@HealthManagementActivity, android.R.color.holo_orange_dark)
            setCircleColor(ContextCompat.getColor(this@HealthManagementActivity, android.R.color.holo_orange_dark))
            lineWidth = 2.5f
            circleRadius = 5f
            setDrawCircleHole(true)
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@HealthManagementActivity, android.R.color.holo_orange_light)
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(true)
        }

        chartWeight.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < dates.size) dates[index] else ""
            }
        }

        chartWeight.data = LineData(dataSet)
        chartWeight.animateX(1000)
        chartWeight.invalidate()
    }

    private fun showEmptyChart() {
        chartWeight.clear()
        chartWeight.setNoDataText("暂无体重数据")
        chartWeight.invalidate()
    }

    private fun loadWeightStatistics() {
        Thread {
            try {
                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/weight/getStatistics?userId=$loginUserId&days=30"))
                    .get()
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                val stats = json.getJSONObject("statistics")
                                val minWeight = stats.getDouble("minWeight")
                                val maxWeight = stats.getDouble("maxWeight")
                                val avgWeight = stats.getDouble("avgWeight")
                                val latestWeight = stats.getDouble("latestWeight")
                                val weightChange = stats.getDouble("weightChange")
                                val trend = stats.getString("trend")

                                val trendText = when (trend) {
                                    "up" -> "↑ 上升"
                                    "down" -> "↓ 下降"
                                    else -> "→ 持平"
                                }

                                val changeText = if (weightChange > 0) "+$weightChange" else "$weightChange"

                                tvWeightStats.text = """
                                    近30天统计:
                                    最新: ${latestWeight}kg | 平均: ${String.format("%.1f", avgWeight)}kg
                                    最低: ${minWeight}kg | 最高: ${maxWeight}kg
                                    变化: ${changeText}kg $trendText
                                """.trimIndent()
                            }
                        } catch (e: Exception) {
                            tvWeightStats.text = "统计加载失败"
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { showEmptyChart() }
            }
        }.start()
    }

    private fun setButtonListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSaveWeight.setOnClickListener {
            val weightStr = etWeight.text.toString().trim()

            if (weightStr.isEmpty()) {
                Toast.makeText(this, "请输入体重", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weight = weightStr.toDoubleOrNull()
            if (weight == null || weight <= 0 || weight > 500) {
                Toast.makeText(this, "请输入有效的体重（1-500kg）", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveWeightToServer(weight)
        }

        cardTodayIntake.setOnClickListener {
            startActivity(Intent(this, TodayIntakeActivity::class.java))
        }
        
        btnRefreshDiet.setOnClickListener {
            Log.d(TAG, "用户点击刷新AI推荐按钮")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("刷新推荐")
                .setMessage("确定要重新获取AI推荐吗？\n这将花费20-60秒时间。")
                .setPositiveButton("确定") { _, _ ->
                    // 清空全局缓存
                    DietRecommendCache.clear()
                    
                    val emptyList = listOf(DietDish("加载中...", "AI推荐生成中，请稍候"))
                    val layoutManager = LinearLayoutManager(this)
                    layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                    rvDietRecommend.layoutManager = layoutManager
                    rvDietRecommend.adapter = DietAdapter(emptyList) { }
                    
                    loadAIDietRecommend(loginUserId)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun saveWeightToServer(weight: Double) {
        if (loginUserId.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        btnSaveWeight.isEnabled = false
        btnSaveWeight.text = "保存中..."

        Thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("userId", loginUserId)
                    .add("recordDate", today)
                    .add("weight", weight.toString())
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/weight/add"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    btnSaveWeight.isEnabled = true
                    btnSaveWeight.text = "保存体重"

                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                Toast.makeText(this, "体重记录已保存", Toast.LENGTH_SHORT).show()
                                tvWeightStatus.text = "今日已记录体重: ${weight}kg"
                                loadWeightHistory()
                            } else {
                                Toast.makeText(this, "保存失败: ${json.optString("error")}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "保存失败: 数据解析错误", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "保存失败: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnSaveWeight.isEnabled = true
                    btnSaveWeight.text = "保存体重"
                    Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun loadAIDietRecommend(userId: String) {
        if (userId.isEmpty()) {
            Log.w(TAG, "loadAIDietRecommend: userId为空，无法加载推荐")
            showEmptyDietList()
            return
        }

        Log.d(TAG, "开始加载AI推荐，userId: $userId")
        
        Thread {
            try {
                val url = ApiHelper.getUrl("/api/ai/getDietRecommend?userId=$userId")
                Log.d(TAG, "请求URL: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                Log.d(TAG, "发送AI推荐请求...")
                val startTime = System.currentTimeMillis()
                val response = ApiHelper.getAIClient().newCall(request).execute()
                val elapsedTime = System.currentTimeMillis() - startTime
                
                val responseBody = response.body?.string()
                Log.d(TAG, "收到响应，耗时: ${elapsedTime}ms")
                Log.d(TAG, "响应状态码: ${response.code}")
                Log.d(TAG, "响应内容长度: ${responseBody?.length ?: 0} 字符")
                
                if (!responseBody.isNullOrEmpty()) {
                    Log.d(TAG, "响应内容: $responseBody")
                }

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            Log.d(TAG, "JSON解析成功")
                            
                            if (json.optBoolean("success", false)) {
                                Log.d(TAG, "AI推荐成功标志为true")
                                
                                val dishes = json.getJSONArray("recommendDishes")
                                val overallReason = json.optString("overallReason", "")
                                Log.d(TAG, "推荐菜品数量: ${dishes.length()}")
                                Log.d(TAG, "总体推荐理由: $overallReason")
                                
                                val dishList = mutableListOf<DietDish>()
                                for (i in 0 until dishes.length()) {
                                    val dish = dishes.getJSONObject(i)
                                    val dishName = dish.getString("dishName")
                                    Log.d(TAG, "解析菜品 $i: $dishName")
                                    

                                    dishList.add(DietDish(
                                        dishName,
                                        dish.getString("calorie"),
                                        dish.getString("cookingMethod"),
                                        dish.getString("reason")
                                    ))
                                }
                                
                                DietRecommendCache.saveDishes(userId, dishList, overallReason)
                                
                                initDietListWithData(dishList)
                                Log.d(TAG, "AI推荐列表初始化完成并已保存到全局缓存")
                                
                                if (overallReason.isNotEmpty()) {
                                    Toast.makeText(this, "AI推荐: $overallReason", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val error = json.optString("error", "未知错误")
                                Log.e(TAG, "AI推荐失败，服务器返回错误: $error")
                                showEmptyDietListWithMessage("服务器错误: $error")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON解析异常", e)
                            Log.e(TAG, "异常详情: ${e.javaClass.simpleName} - ${e.message}")
                            e.printStackTrace()
                            showEmptyDietListWithMessage("数据解析失败: ${e.message}")
                        }
                    } else {
                        Log.e(TAG, "响应失败或为空")
                        Log.e(TAG, "isSuccessful: ${response.isSuccessful}")
                        Log.e(TAG, "responseBody.isNullOrEmpty: ${responseBody.isNullOrEmpty()}")
                        showEmptyDietListWithMessage("HTTP ${response.code}: ${response.message}")
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "AI推荐请求超时", e)
                runOnUiThread {
                    val emptyList = listOf(DietDish("AI推荐超时", "服务器响应时间过长(>60秒)"))
                    val layoutManager = LinearLayoutManager(this)
                    layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                    rvDietRecommend.layoutManager = layoutManager
                    rvDietRecommend.adapter = DietAdapter(emptyList) { }
                    Toast.makeText(this, "AI推荐超时，请稍后刷新页面重试", Toast.LENGTH_LONG).show()
                }
            } catch (e: java.net.UnknownHostException) {
                Log.e(TAG, "无法连接到服务器: 主机未知", e)
                runOnUiThread {
                    showEmptyDietListWithMessage("无法连接服务器: 请检查网络设置")
                }
            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "连接服务器失败", e)
                runOnUiThread {
                    showEmptyDietListWithMessage("连接失败: 服务器可能未启动")
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI推荐请求异常", e)
                Log.e(TAG, "异常类型: ${e.javaClass.simpleName}")
                Log.e(TAG, "异常消息: ${e.message}")
                e.printStackTrace()
                runOnUiThread { 
                    showEmptyDietListWithMessage("网络异常: ${e.javaClass.simpleName}") 
                }
            }
        }.start()
    }

    private fun initDietListWithData(dishes: List<DietDish>) {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvDietRecommend.layoutManager = layoutManager
        rvDietRecommend.adapter = DietAdapter(dishes) { dish ->
            showDishDetailDialog(dish)
        }
    }

    private fun showDishDetailDialog(dish: DietDish) {
        val message = StringBuilder()
        message.append("热量: ${dish.calorie}\n\n")
        message.append("制作方法:\n${dish.cookingMethod}\n\n")
        message.append("推荐理由:\n${dish.reason}")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(dish.name)
            .setMessage(message.toString())
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showEmptyDietListWithMessage(message: String) {
        Log.w(TAG, "显示错误消息: $message")
        val emptyList = listOf(DietDish("获取失败", message))
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvDietRecommend.layoutManager = layoutManager
        rvDietRecommend.adapter = DietAdapter(emptyList) { }
        Toast.makeText(this, "AI推荐: $message", Toast.LENGTH_LONG).show()
    }

    private fun showEmptyDietList() {
        showEmptyDietListWithMessage("AI推荐服务异常")
    }
}

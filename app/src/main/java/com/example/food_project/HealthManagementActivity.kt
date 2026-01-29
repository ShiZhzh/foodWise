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
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

// 饮食推荐数据类
class DietDish(val name: String, val calorie: String)

// 饮食推荐适配器（完全基础语法，无简化）
class DietAdapter(private val dishList: List<DietDish>) : RecyclerView.Adapter<DietAdapter.ViewHolder>() {
    // 内部类ViewHolder（基础写法）
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
    }

    override fun getItemCount(): Int {
        return dishList.size
    }
}

// 核心Activity
class HealthManagementActivity : AppCompatActivity() {
    // 修改：正确声明所有控件
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

    private var loginUserId: String = ""
    private var loginUsername: String = "" // 新增：存储用户名

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_management)
        
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        loginUserId = sharedPref.getString("userId", "") ?: ""
        loginUsername = sharedPref.getString("username", "") ?: "" // 新增：读取用户名

        initViews()
        setupWeightChart()
        
        // 修改：显示用户名而不是用户ID
        tvUserName.text = if (loginUsername.isEmpty()) {
            "用户未登录"
        } else {
            loginUsername  // 直接显示用户名，如"张三"
        }
        tvUserId.text = if (loginUserId.isEmpty()) {
            "ID: 未设置"
        } else {
            "ID: $loginUserId"   // 显示完整的用户ID
        }
        
        loadDietRecommend(loginUserId)
        loadTodayWeight()
        loadWeightHistory()
        setButtonListeners()
    }

    // 修改：绑定正确的控件ID（与布局文件匹配）
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
    }

    // 新增：初始化体重趋势图表
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

    // 新增:加载今日体重
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

    // 新增：加载体重历史（近30天）
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

    // 新增：绘制体重趋势图
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

    // 新增：显示空图表
    private fun showEmptyChart() {
        chartWeight.clear()
        chartWeight.setNoDataText("暂无体重数据")
        chartWeight.invalidate()
    }

    // 新增：加载体重统计信息
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
                runOnUiThread {
                    tvWeightStats.text = "网络异常"
                }
            }
        }.start()
    }

    private fun setButtonListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // 修改：保存体重按钮
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
    }

    // 新增：保存体重到服务器
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

    private fun loadDietRecommend(userId: String) {
        Thread {
            try {
                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/getDietRecommend?userId=$userId"))
                    .get()
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                val dishes = json.getJSONArray("recommendDishes")
                                val dishList = mutableListOf<DietDish>()
                                for (i in 0 until dishes.length()) {
                                    val dish = dishes.getJSONObject(i)
                                    dishList.add(DietDish(
                                        dish.getString("dishName"),
                                        dish.getString("calorie")
                                    ))
                                }
                                initDietListWithData(dishList)
                            } else {
                                showEmptyDietList()
                            }
                        } catch (e: Exception) {
                            showEmptyDietList()
                        }
                    } else {
                        showEmptyDietList()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { showEmptyDietList() }
            }
        }.start()
    }

    private fun initDietListWithData(dishes: List<DietDish>) {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvDietRecommend.layoutManager = layoutManager
        rvDietRecommend.adapter = DietAdapter(dishes)
    }

    private fun showEmptyDietList() {
        val emptyList = listOf(DietDish("获取失败", "服务器连接失败"))
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvDietRecommend.layoutManager = layoutManager
        rvDietRecommend.adapter = DietAdapter(emptyList)
        Toast.makeText(this, "智能餐饮推荐获取失败", Toast.LENGTH_SHORT).show()
    }
}
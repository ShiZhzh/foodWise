package com.example.food_project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var tvUserInfo: TextView
    private lateinit var chartCalorie: LineChart
    private lateinit var btnFoodAnalysis: Button
    private val okHttpClient = OkHttpClient()
    private var userId: String = ""
    private var username: String = "" // 新增

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userId = sharedPref.getString("userId", "") ?: ""
        username = sharedPref.getString("username", "") ?: "" // 新增
        
        if (userId.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        loadCalorieData()
    }

    private fun initViews() {
        tvUserInfo = findViewById(R.id.tv_user_info)
        chartCalorie = findViewById(R.id.chart_calorie)
        btnFoodAnalysis = findViewById(R.id.btn_food_analysis)

        // 修改：显示用户名和ID
        tvUserInfo.text = if (username.isEmpty()) {
            "用户ID: $userId"
        } else {
            "欢迎, $username (ID: $userId)"
        }

        btnFoodAnalysis.setOnClickListener {
            val intent = Intent(this, FoodAnalysisActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btn_food_recommend).setOnClickListener {
            val intent = Intent(this, FoodRecommendActivity::class.java)
            startActivity(intent)
        }
	    findViewById<Button>(R.id.btn_health_management).setOnClickListener {
            val intent = Intent(this, HealthManagementActivity::class.java)
            startActivity(intent)
            // 删掉原来的Toast提示
        }


        // 初始化图表样式
        setupChart()
    }

    private fun setupChart() {
        chartCalorie.apply {
            description.isEnabled = true
            description.text = "近7天卡路里摄入趋势"
            description.textSize = 12f
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            // X轴设置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textSize = 10f
            }

            // 左Y轴设置
            axisLeft.apply {
                setDrawGridLines(true)
                textSize = 10f
                axisMinimum = 0f
            }

            // 右Y轴禁用
            axisRight.isEnabled = false

            // 图例设置
            legend.apply {
                isEnabled = true
                textSize = 12f
            }
        }
    }

    private fun loadCalorieData() {
        chartCalorie.clear()
        chartCalorie.invalidate()

        Thread {
            try {
                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/getCalorieHistory?userId=$userId"))
                    .get()
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                val dataList = json.getJSONArray("data")
                                if (dataList.length() == 0) {
                                    showErrorOnChart("暂无数据")
                                } else {
                                    drawChart(dataList)
                                }
                            } else {
                                showErrorOnChart("查询失败: ${json.optString("error")}")
                            }
                        } catch (e: Exception) {
                            showErrorOnChart("数据解析错误")
                        }
                    } else {
                        showErrorOnChart("HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showErrorOnChart("网络异常")
                }
            }
        }.start()
    }

    private fun drawChart(dataList: org.json.JSONArray) {
        val entries = ArrayList<Entry>()
        val dates = ArrayList<String>()

        // 解析数据
        for (i in 0 until dataList.length()) {
            val item = dataList.getJSONObject(i)
            val calorie = item.getString("calorie").toFloatOrNull() ?: 0f
            val date = item.getString("date")
            entries.add(Entry(i.toFloat(), calorie))
            dates.add(date.substring(5)) // 只显示月-日
        }

        // 创建数据集
        val dataSet = LineDataSet(entries, "卡路里摄入 (kcal)").apply {
            color = ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_dark)
            setCircleColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_dark))
            lineWidth = 2.5f
            circleRadius = 5f
            setDrawCircleHole(true)
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_light)
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER // 平滑曲线
            setDrawValues(true)
        }

        // 设置X轴标签
        chartCalorie.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < dates.size) dates[index] else ""
            }
        }

        // 应用数据
        val lineData = LineData(dataSet)
        chartCalorie.data = lineData
        chartCalorie.animateX(1000) // 1秒动画
        chartCalorie.invalidate()
    }

    private fun showErrorOnChart(message: String) {
        chartCalorie.clear()
        chartCalorie.setNoDataText(message)
        chartCalorie.invalidate()
    }
}
package com.example.food_project

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog // 新增导入
import java.util.*

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class TodayIntakeActivity : AppCompatActivity() {
    // 控件声明
    private lateinit var btnBack: android.view.View // 类型改为 View
    private lateinit var etFoodName: EditText
    private lateinit var etCalorie: EditText
    private lateinit var btnAddFood: Button
    private lateinit var tvTotalCalorie: TextView
    private lateinit var tvFoodList: TextView
    private lateinit var btnClearAll: Button

    // 登录用户ID
    private var loginUserId: String = ""
    // 今日摄入列表（改用数据类列表）
    data class IntakeRecord(
        val id: Int,
        val foodName: String,
        val calorie: Int,
        val timestamp: String
    )
    private val foodList = mutableListOf<IntakeRecord>()

    private val okHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_intake)
        AgentEntryBinder.bind(this)

        // 读取登录ID
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        loginUserId = sharedPref.getString("userId", "") ?: ""

        // 绑定控件
        initViews()
        // 设置点击事件
        setButtonListeners()
        // 从服务器加载今日记录
        loadTodayDataFromServer()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etFoodName = findViewById(R.id.et_food_name)
        etCalorie = findViewById(R.id.et_calorie)
        btnAddFood = findViewById(R.id.btn_add_food)
        tvTotalCalorie = findViewById(R.id.tv_total_calorie)
        tvFoodList = findViewById(R.id.tv_food_list)
        btnClearAll = findViewById(R.id.btn_clear_all)
    }

    // 新增：从服务器加载今日摄入记录
    private fun loadTodayDataFromServer() {
        if (loginUserId.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val todayDate = getTodayDate()

        Thread {
            try {
                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/getTodayIntake?userId=$loginUserId&date=$todayDate"))
                    .get()
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                foodList.clear()
                                val records = json.getJSONArray("records")
                                for (i in 0 until records.length()) {
                                    val record = records.getJSONObject(i)
                                    val id = record.getInt("id")
                                    val foodName = record.getString("foodName")
                                    val calorieStr = record.getString("calorie")
                                    val timestamp = record.optString("timestamp", "")
                                    val calorie = calorieStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                                    foodList.add(IntakeRecord(id, foodName, calorie, timestamp))
                                }
                                foodList.sortByDescending { it.timestamp }
                                updateFoodList()
                                if (records.length() > 0) {
                                    Toast.makeText(this, "已加载今日记录", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "加载失败: ${json.optString("error")}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "数据解析失败", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "加载失败: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // 删除或注释掉原来的loadTodayData()方法
    // private fun loadTodayData() { ... }

    // 删除或注释掉原来的saveTodayData()方法（不再需要本地保存）
    // private fun saveTodayData() { ... }

    // 获取今日日期（格式：2026-01-24）
    private fun getTodayDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    // 更新食物列表和总计热量显示
    private fun updateFoodList() {
        // 计算总计热量
        var total = 0
        val listSb = StringBuilder()
        for (record in foodList) {
            total += record.calorie
            // 格式化时间戳显示
            val timeStr = formatTimestamp(record.timestamp)
            listSb.append("${record.foodName}：${record.calorie} kcal\n")
            listSb.append("时间：$timeStr  ")
            listSb.append("[删除 ID:${record.id}]\n\n")
        }

        // 更新UI
        tvTotalCalorie.text = "今日总计摄入：$total kcal"
        tvFoodList.text = if (listSb.isEmpty()) "暂无摄入记录" else listSb.toString()
        
        // 设置点击事件处理删除
        tvFoodList.setOnClickListener {
            showDeleteDialog()
        }
    }

    // 格式化时间戳
    private fun formatTimestamp(timestamp: String): String {
        if (timestamp.isEmpty()) return "未知时间"
        return try {
            // 如果是完整的日期时间格式（如：2025-01-24 14:30:25），只显示时间部分
            if (timestamp.length > 10) {
                timestamp.substring(11, 19) // 提取 HH:mm:ss
            } else {
                timestamp
            }
        } catch (e: Exception) {
            "未知时间"
        }
    }

    private fun setButtonListeners() {
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }

        // 添加摄入项
        btnAddFood.setOnClickListener {
            val foodName = etFoodName.text.toString().trim()
            val calorieStr = etCalorie.text.toString().trim()

            if (foodName.isEmpty() || calorieStr.isEmpty()) {
                Toast.makeText(this, "请输入食物名称和热量", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val calorie = calorieStr.toIntOrNull()
            if (calorie == null || calorie < 0) {
                Toast.makeText(this, "请输入有效的热量数值", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            syncToServerAndUpdate(foodName, calorie)
        }

        // 清空今日记录
        btnClearAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("确认清空")
                .setMessage("确定要清空今日所有摄入记录吗？\n此操作将同步清空服务器数据，无法恢复。")
                .setPositiveButton("确定") { _, _ ->
                    clearTodayDataFromServer()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    // 显示删除对话框
    private fun showDeleteDialog() {
        if (foodList.isEmpty()) {
            Toast.makeText(this, "暂无记录", Toast.LENGTH_SHORT).show()
            return
        }

        // 构建选项列表
        val items = foodList.map { 
            "${it.foodName} (${it.calorie} kcal) - ${formatTimestamp(it.timestamp)}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择要删除的记录")
            .setItems(items) { _, which ->
                val recordToDelete = foodList[which]
                confirmAndDeleteRecord(recordToDelete)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 确认并删除记录
    private fun confirmAndDeleteRecord(record: IntakeRecord) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除「${record.foodName}」吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteRecordFromServer(record.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 从服务器删除单条记录
    private fun deleteRecordFromServer(recordId: Int) {
        Thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("recordId", recordId.toString())
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/deleteIntakeRecord"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                foodList.removeAll { it.id == recordId }
                                updateFoodList()
                                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "删除失败: ${json.optString("error")}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "删除失败: 数据解析错误", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "删除失败: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // 修改：同步到服务器后更新本地显示
    private fun syncToServerAndUpdate(foodName: String, calorie: Int) {
        Thread {
            try {
                val todayDate = getTodayDate()
                val requestBody = FormBody.Builder()
                    .add("userId", loginUserId)
                    .add("date", todayDate)
                    .add("foodName", foodName)
                    .add("calorie", calorie.toString())
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/addIntake"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                loadTodayDataFromServer()
                                etFoodName.setText("")
                                etCalorie.setText("")
                                Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "同步失败: ${json.optString("error")}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "同步失败: 数据解析错误", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "同步失败: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "同步失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // 新增：清空服务器今日记录
    private fun clearTodayDataFromServer() {
        if (loginUserId.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val todayDate = getTodayDate()

        Thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("userId", loginUserId)
                    .add("date", todayDate)
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/clearTodayIntake"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                foodList.clear()
                                updateFoodList()
                                Toast.makeText(this, "已清空今日记录", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "清空失败: ${json.optString("error")}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "清空失败: 数据解析错误", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "清空失败: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "清空失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // 删除原来的syncToServer()方法
    // private fun syncToServer(foodName: String, calorie: Int) { ... }
}

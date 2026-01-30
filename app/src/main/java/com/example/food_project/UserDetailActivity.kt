package com.example.food_project

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class UserDetailActivity : AppCompatActivity() {
    private lateinit var btnBack: android.view.View
    private lateinit var tvUserName: TextView
    private lateinit var tvUserId: TextView
    
    // 长期目标相关
    private lateinit var tvGoalStatus: TextView
    private lateinit var tvTargetWeight: TextView
    private lateinit var tvNutritionPlan: TextView
    private lateinit var tvPlanDesc: TextView
    private lateinit var btnSetGoal: Button
    private lateinit var btnDeleteGoal: Button
    
    // 今日日程相关
    private lateinit var tvScheduleList: TextView
    private lateinit var btnAddSchedule: Button
    private lateinit var btnRefreshSchedule: Button
    
    private var loginUserId: String = ""
    private var loginUsername: String = ""
    private val scheduleList = mutableListOf<ScheduleItem>()
    
    data class ScheduleItem(
        val id: Int,
        val datetime: String,
        val content: String,
        val isCompleted: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)
        
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        loginUserId = sharedPref.getString("userId", "") ?: ""
        loginUsername = sharedPref.getString("username", "") ?: ""
        
        initViews()
        setButtonListeners()
        loadUserGoal()
        loadTodaySchedule()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvUserName = findViewById(R.id.tv_user_name)
        tvUserId = findViewById(R.id.tv_user_id)
        
        tvGoalStatus = findViewById(R.id.tv_goal_status)
        tvTargetWeight = findViewById(R.id.tv_target_weight)
        tvNutritionPlan = findViewById(R.id.tv_nutrition_plan)
        tvPlanDesc = findViewById(R.id.tv_plan_desc)
        btnSetGoal = findViewById(R.id.btn_set_goal)
        btnDeleteGoal = findViewById(R.id.btn_delete_goal)
        
        tvScheduleList = findViewById(R.id.tv_schedule_list)
        btnAddSchedule = findViewById(R.id.btn_add_schedule)
        btnRefreshSchedule = findViewById(R.id.btn_refresh_schedule)
        
        tvUserName.text = loginUsername
        tvUserId.text = "ID: $loginUserId"
    }

    private fun setButtonListeners() {
        btnBack.setOnClickListener { finish() }
        
        btnSetGoal.setOnClickListener { showSetGoalDialog() }
        btnDeleteGoal.setOnClickListener { confirmDeleteGoal() }
        btnAddSchedule.setOnClickListener { showAddScheduleDialog() }
        btnRefreshSchedule.setOnClickListener { loadTodaySchedule() }
        
        tvScheduleList.setOnClickListener { showScheduleOptionsDialog() }
    }

    // 加载用户长期目标
    private fun loadUserGoal() {
        Thread {
            try {
                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/goal/get?userId=$loginUserId"))
                    .get()
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                val goal = json.getJSONObject("goal")
                                val targetWeight = goal.getDouble("targetWeight")
                                val nutritionPlan = goal.getString("nutritionPlan")
                                val planDesc = goal.optString("planDesc", "暂无描述")
                                
                                tvGoalStatus.text = "已设置目标"
                                tvTargetWeight.text = "目标体重: ${targetWeight}kg"
                                tvNutritionPlan.text = "营养计划: $nutritionPlan"
                                tvPlanDesc.text = "计划描述: $planDesc"
                                btnDeleteGoal.visibility = Button.VISIBLE
                            } else {
                                showNoGoalStatus()
                            }
                        } catch (e: Exception) {
                            showNoGoalStatus()
                        }
                    } else {
                        showNoGoalStatus()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { showNoGoalStatus() }
            }
        }.start()
    }

    private fun showNoGoalStatus() {
        tvGoalStatus.text = "未设置长期目标"
        tvTargetWeight.text = "点击下方按钮设置目标"
        tvNutritionPlan.text = ""
        tvPlanDesc.text = ""
        btnDeleteGoal.visibility = Button.GONE
    }

    // 显示设置目标对话框
    private fun showSetGoalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_goal, null)
        val etTargetWeight = dialogView.findViewById<EditText>(R.id.et_target_weight)
        val etNutritionPlan = dialogView.findViewById<EditText>(R.id.et_nutrition_plan)
        val etPlanDesc = dialogView.findViewById<EditText>(R.id.et_plan_desc)
        
        AlertDialog.Builder(this)
            .setTitle("设置长期目标")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val targetWeight = etTargetWeight.text.toString().trim()
                val nutritionPlan = etNutritionPlan.text.toString().trim()
                val planDesc = etPlanDesc.text.toString().trim()
                
                if (targetWeight.isEmpty() || nutritionPlan.isEmpty()) {
                    Toast.makeText(this, "请填写目标体重和营养计划", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                saveGoalToServer(targetWeight, nutritionPlan, planDesc)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 保存目标到服务器
    private fun saveGoalToServer(targetWeight: String, nutritionPlan: String, planDesc: String) {
        Thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("userId", loginUserId)
                    .add("targetWeight", targetWeight)
                    .add("nutritionPlan", nutritionPlan)
                    .add("planDesc", planDesc)
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/goal/set"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                Toast.makeText(this, "目标设置成功", Toast.LENGTH_SHORT).show()
                                loadUserGoal()
                            } else {
                                Toast.makeText(this, "设置失败: ${json.optString("error")}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "设置失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // 确认删除目标
    private fun confirmDeleteGoal() {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除当前目标吗？")
            .setPositiveButton("删除") { _, _ -> deleteGoalFromServer() }
            .setNegativeButton("取消", null)
            .show()
    }

    // 从服务器删除目标
    private fun deleteGoalFromServer() {
        Thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("userId", loginUserId)
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/goal/delete"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                Toast.makeText(this, "目标已删除", Toast.LENGTH_SHORT).show()
                                loadUserGoal()
                            } else {
                                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // 加载今日日程
    private fun loadTodaySchedule() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        Thread {
            try {
                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/schedule/getByDate?userId=$loginUserId&date=$today"))
                    .get()
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                scheduleList.clear()
                                val schedules = json.getJSONArray("schedules")
                                for (i in 0 until schedules.length()) {
                                    val item = schedules.getJSONObject(i)
                                    scheduleList.add(ScheduleItem(
                                        item.getInt("scheduleId"),
                                        item.getString("scheduleDatetime"),
                                        item.getString("scheduleContent"),
                                        item.optBoolean("isCompleted", false)
                                    ))
                                }
                                updateScheduleDisplay()
                            } else {
                                tvScheduleList.text = "暂无今日日程"
                            }
                        } catch (e: Exception) {
                            tvScheduleList.text = "加载失败"
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvScheduleList.text = "网络异常"
                }
            }
        }.start()
    }

    // 更新日程显示
    private fun updateScheduleDisplay() {
        if (scheduleList.isEmpty()) {
            tvScheduleList.text = "暂无今日日程"
            return
        }
        
        val sb = StringBuilder()
        for (schedule in scheduleList) {
            val time = schedule.datetime.substring(11, 16)
            val status = if (schedule.isCompleted) "[已完成]" else "[未完成]"
            sb.append("$time $status ${schedule.content}\n\n")
        }
        tvScheduleList.text = sb.toString()
    }

    // 显示添加日程对话框
    private fun showAddScheduleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_schedule, null)
        val etTime = dialogView.findViewById<EditText>(R.id.et_schedule_time)
        val etContent = dialogView.findViewById<EditText>(R.id.et_schedule_content)
        
        // 默认填充当前时间
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        etTime.setText(currentTime)
        
        AlertDialog.Builder(this)
            .setTitle("添加今日日程")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val time = etTime.text.toString().trim()
                val content = etContent.text.toString().trim()
                
                if (time.isEmpty() || content.isEmpty()) {
                    Toast.makeText(this, "请填写时间和内容", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                addScheduleToServer(time, content)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 添加日程到服务器
    private fun addScheduleToServer(time: String, content: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val datetime = "$today $time:00"
        
        Thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("userId", loginUserId)
                    .add("scheduleDatetime", datetime)
                    .add("scheduleContent", content)
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/schedule/add"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                Toast.makeText(this, "日程添加成功", Toast.LENGTH_SHORT).show()
                                loadTodaySchedule()
                            } else {
                                Toast.makeText(this, "添加失败", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "添加失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // 显示日程操作选项
    private fun showScheduleOptionsDialog() {
        if (scheduleList.isEmpty()) {
            Toast.makeText(this, "暂无日程", Toast.LENGTH_SHORT).show()
            return
        }
        
        val items = scheduleList.map {
            val time = it.datetime.substring(11, 16)
            val status = if (it.isCompleted) "[已完成]" else "[未完成]"
            "$time $status ${it.content}"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("选择日程")
            .setItems(items) { _, which ->
                showScheduleActionDialog(scheduleList[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 显示单个日程操作
    private fun showScheduleActionDialog(schedule: ScheduleItem) {
        val actions = if (schedule.isCompleted) {
            arrayOf("标记为未完成", "删除")
        } else {
            arrayOf("标记为已完成", "删除")
        }
        
        AlertDialog.Builder(this)
            .setTitle(schedule.content)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> updateScheduleStatus(schedule.id, !schedule.isCompleted)
                    1 -> deleteSchedule(schedule.id)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 更新日程状态
    private fun updateScheduleStatus(scheduleId: Int, isCompleted: Boolean) {
        Thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("scheduleId", scheduleId.toString())
                    .add("isCompleted", isCompleted.toString())
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/schedule/updateStatus"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this, "状态已更新", Toast.LENGTH_SHORT).show()
                        loadTodaySchedule()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // 删除日程
    private fun deleteSchedule(scheduleId: Int) {
        Thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("scheduleId", scheduleId.toString())
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/schedule/delete"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this, "日程已删除", Toast.LENGTH_SHORT).show()
                        loadTodaySchedule()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}

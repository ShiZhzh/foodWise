package com.example.food_project

import android.os.Bundle
import android.util.Log
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var etAge: EditText
    private lateinit var etHeight: EditText
    private lateinit var etWeight: EditText
    private lateinit var cbHypertension: CheckBox
    private lateinit var cbDiabetes: CheckBox
    private lateinit var etTastePreferences: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnBack: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setButtonListeners()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        rgGender = findViewById(R.id.rg_gender)
        etAge = findViewById(R.id.et_age)
        etHeight = findViewById(R.id.et_height)
        etWeight = findViewById(R.id.et_weight)
        cbHypertension = findViewById(R.id.cb_hypertension)
        cbDiabetes = findViewById(R.id.cb_diabetes)
        etTastePreferences = findViewById(R.id.et_taste_preferences)
        btnRegister = findViewById(R.id.btn_register)
        btnBack = findViewById(R.id.btn_back)
    }

    private fun setButtonListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            handleRegister()
        }
    }

    private fun handleRegister() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val ageStr = etAge.text.toString().trim()
        val heightStr = etHeight.text.toString().trim()
        val weightStr = etWeight.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
            ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "请填写所有必填项", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "密码长度不能少于6位", Toast.LENGTH_SHORT).show()
            return
        }

        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rb_male -> "男"
            R.id.rb_female -> "女"
            else -> {
                Toast.makeText(this, "请选择性别", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 新增：智能转换身高单位
        val heightValue = heightStr.toFloatOrNull()
        if (heightValue == null || heightValue <= 0) {
            Toast.makeText(this, "请输入有效的身高", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 如果输入的是厘米（大于10），自动转换为米
        val heightInMeters = if (heightValue > 10) {
            String.format("%.2f", heightValue / 100)
        } else {
            String.format("%.2f", heightValue)
        }

        // 新增：验证体重范围
        val weightValue = weightStr.toFloatOrNull()
        if (weightValue == null || weightValue <= 0 || weightValue > 500) {
            Toast.makeText(this, "请输入有效的体重（1-500kg）", Toast.LENGTH_SHORT).show()
            return
        }

        val tastePreferences = etTastePreferences.text.toString().trim()

        btnRegister.isEnabled = false
        btnRegister.text = "注册中..."

        Thread {
            try {
                // 构建请求体
                val requestBody = FormBody.Builder()
                    .add("username", username)
                    .add("password", password)
                    .add("gender", gender)
                    .add("age", ageStr)
                    .add("height", heightInMeters) // 使用转换后的米单位
                    .add("weight", weightStr)
                    .add("hasHypertension", cbHypertension.isChecked.toString())
                    .add("hasDiabetes", cbDiabetes.isChecked.toString())
                    .apply {
                        if (tastePreferences.isNotEmpty()) {
                            add("tastePreferences", tastePreferences)
                        }
                    }
                    .build()

                // 打印请求参数日志
                Log.d("RegisterActivity", "注册请求参数:")
                Log.d("RegisterActivity", "username: $username")
                Log.d("RegisterActivity", "password: ${password.replace(Regex("."), "*")}")
                Log.d("RegisterActivity", "gender: $gender")
                Log.d("RegisterActivity", "age: $ageStr")
                Log.d("RegisterActivity", "height: $heightInMeters (原始输入: $heightStr)")
                Log.d("RegisterActivity", "weight: $weightStr")
                Log.d("RegisterActivity", "hasHypertension: ${cbHypertension.isChecked}")
                Log.d("RegisterActivity", "hasDiabetes: ${cbDiabetes.isChecked}")
                Log.d("RegisterActivity", "tastePreferences: $tastePreferences")

                val url = ApiHelper.getUrl("/api/user/register")
                Log.d("RegisterActivity", "请求URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                // 打印响应日志
                Log.d("RegisterActivity", "HTTP状态码: ${response.code}")
                Log.d("RegisterActivity", "响应内容: $responseBody")

                runOnUiThread {
                    btnRegister.isEnabled = true
                    btnRegister.text = "注册"

                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.optBoolean("success", false)) {
                                val token = json.getString("token")
                                val userId = json.getString("userId")
                                
                                ApiHelper.saveToken(this, token)
                                val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                sharedPref.edit()
                                    .putString("userId", userId)
                                    .putString("username", username)
                                    .apply()

                                Toast.makeText(this, "注册成功，即将进入主页", Toast.LENGTH_SHORT).show()
                                
                                val intent = android.content.Intent(this, MainActivity::class.java)
                                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                val errorMsg = json.optString("error", "未知错误")
                                Log.e("RegisterActivity", "注册失败: $errorMsg")
                                Toast.makeText(this, "注册失败: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e("RegisterActivity", "JSON解析失败", e)
                            Toast.makeText(this, "数据解析失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // HTTP 500 错误详细信息
                        val errorDetail = if (response.code == 500) {
                            "服务器内部错误，可能原因：\n" +
                            "1. 后端未正确处理注册参数\n" +
                            "2. 数据库连接失败\n" +
                            "3. 用户名已存在\n" +
                            "响应内容: ${responseBody?.take(200) ?: "无"}"
                        } else {
                            "HTTP ${response.code}"
                        }
                        
                        Log.e("RegisterActivity", "注册失败: $errorDetail")
                        Toast.makeText(this, "注册失败: $errorDetail", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "网络请求异常", e)
                runOnUiThread {
                    btnRegister.isEnabled = true
                    btnRegister.text = "注册"
                    Toast.makeText(this, "网络错误: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}

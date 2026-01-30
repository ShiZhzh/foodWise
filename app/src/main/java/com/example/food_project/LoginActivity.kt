package com.example.food_project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvLoginTitle: ImageView
    private lateinit var cardUsername: View
    private lateinit var cardPassword: View
    private lateinit var tvSlogan: TextView
    private lateinit var tvTeam: TextView
    private lateinit var tvGoRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 新增：清空上一个用户的缓存
        DietRecommendCache.clear()

        initViews()
        applyAnimations()
        setButtonListeners()
    }

    private fun initViews() {
        tvLoginTitle = findViewById(R.id.tv_login_title)
        cardUsername = findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_username)
        cardPassword = findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_password)
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvSlogan = findViewById(R.id.tv_slogan)
        tvTeam = findViewById(R.id.tv_team)
        tvGoRegister = findViewById(R.id.tv_go_register)
    }

    private fun applyAnimations() {
        val scaleFadeIn = AnimationUtils.loadAnimation(this, R.anim.scale_fade_in)
        val slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        val slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        val slideInBottom = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)

        tvLoginTitle.startAnimation(scaleFadeIn)
        
        cardUsername.postDelayed({
            cardUsername.startAnimation(slideInLeft)
        }, 300)
        
        cardPassword.postDelayed({
            cardPassword.startAnimation(slideInRight)
        }, 500)
        
        btnLogin.postDelayed({
            btnLogin.startAnimation(slideInBottom)
        }, 700)
        
        tvGoRegister.postDelayed({
            tvGoRegister.alpha = 1f
            tvGoRegister.startAnimation(slideInBottom)
        }, 900)
        
        tvSlogan.postDelayed({
            tvSlogan.alpha = 1f
            tvSlogan.startAnimation(slideInBottom)
        }, 1100)
        
        tvTeam.postDelayed({
            tvTeam.alpha = 1f
            tvTeam.startAnimation(slideInBottom)
        }, 1300)
    }

    private fun setButtonListeners() {
        btnLogin.setOnClickListener { v ->
            v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    handleLogin()
                }
                .start()
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "登录中..."

        Thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("username", username)
                    .add("password", password)
                    .build()

                val request = Request.Builder()
                    .url(ApiHelper.getUrl("/api/user/login"))
                    .post(requestBody)
                    .build()

                val response = ApiHelper.getClient().newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "登录"

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

                                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "登录失败: ${json.optString("error", "用户名或密码错误")}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "数据解析失败", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "登录失败: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "登录"
                    Toast.makeText(this, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}

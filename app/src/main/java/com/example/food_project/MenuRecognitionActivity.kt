package com.example.food_project

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*;
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

class MenuRecognitionActivity : AppCompatActivity() {
    private lateinit var ivMenu: ImageView
    private lateinit var tvLoading: TextView
    private lateinit var rvMenuItems: RecyclerView
    private val okHttpClient = OkHttpClient()
    private val menuItems = mutableListOf<String>()
    private lateinit var adapter: MenuDishAdapter

    // 百度API密钥
    private val API_KEY = "dMrpDLIFCVO9S2kNF1wsL501"
    private val SECRET_KEY = "r04IBpCsn1Mx9lH9oMnYtJYtVx6d86FK"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            ivMenu.setImageURI(uri)
            recognizeMenu(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_recognition)

        ivMenu = findViewById(R.id.iv_menu)
        tvLoading = findViewById(R.id.tv_loading)
        rvMenuItems = findViewById(R.id.rv_menu_items)

        adapter = MenuDishAdapter(menuItems) { dishName ->
            queryDishInfo(dishName)
        }
        rvMenuItems.layoutManager = LinearLayoutManager(this)
        rvMenuItems.adapter = adapter

        findViewById<Button>(R.id.btn_choose_menu).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun recognizeMenu(uri: Uri) {
        tvLoading.text = "正在识别菜单..."
        tvLoading.visibility = View.VISIBLE
        menuItems.clear()
        adapter.notifyDataSetChanged()
        
        Thread {
            try {
                // 步骤1: 获取Access Token
                val accessToken = getAccessToken()
                if (accessToken.isNullOrEmpty()) {
                    runOnUiThread {
                        tvLoading.visibility = View.GONE
                        Toast.makeText(this, "获取Token失败", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                // 步骤2: 将图片转为Base64
                val file = uriToFile(uri)
                if (file == null) {
                    runOnUiThread {
                        tvLoading.visibility = View.GONE
                        Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }
                val imageBase64 = encodeFileToBase64(file)

                // 步骤3: 调用百度OCR API
                val ocrUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token=$accessToken"
                
                val requestBody = FormBody.Builder()
                    .add("image", imageBase64)
                    .add("detect_direction", "false")
                    .add("paragraph", "false")
                    .add("probability", "false")
                    .build()

                val request = Request.Builder()
                    .url(ocrUrl)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    tvLoading.visibility = View.GONE
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.has("error_code")) {
                                Toast.makeText(this, "识别失败: ${json.getString("error_msg")}", Toast.LENGTH_LONG).show()
                            } else {
                                // 解析OCR结果
                                val wordsResult = json.getJSONArray("words_result")
                                for (i in 0 until wordsResult.length()) {
                                    val item = wordsResult.getJSONObject(i)
                                    val text = item.getString("words").trim()
                                    // 过滤掉价格等无关信息,只保留菜品名
                                    if (text.isNotEmpty() && !text.matches(Regex(".*\\d+.*元.*"))) {
                                        menuItems.add(text)
                                    }
                                }
                                adapter.notifyDataSetChanged()
                                if (menuItems.isEmpty()) {
                                    Toast.makeText(this, "未识别到菜品信息", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "识别成功,点击菜品查询详情", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "识别失败: 数据解析错误", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "查询失败: 服务器无响应 (HTTP ${response.code})", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvLoading.visibility = View.GONE
                    Toast.makeText(this, "查询失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    // 获取百度Access Token
    private fun getAccessToken(): String? {
        return try {
            val url = "https://aip.baidubce.com/oauth/2.0/token?" +
                    "grant_type=client_credentials&" +
                    "client_id=$API_KEY&" +
                    "client_secret=$SECRET_KEY"

            val request = Request.Builder()
                .url(url)
                .post(FormBody.Builder().build())
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val json = JSONObject(responseBody)
                json.getString("access_token")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 将图片文件转为Base64
    private fun encodeFileToBase64(file: File): String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun queryDishInfo(dishName: String) {
        Toast.makeText(this, "正在查询 $dishName...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                // URL编码菜品名称
                val encodedDishName = URLEncoder.encode(dishName, "UTF-8")
                val request = Request.Builder()
                    .url("http://10.0.2.2:5000/api/getDishInfo?dishName=$encodedDishName")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.has("error")) {
                                showDishDialog(dishName, "查询失败", json.getString("error"))
                            } else {
                                val calorie = json.getString("calorie")
                                val allergens = json.getString("allergens")
                                showDishDialog(dishName, calorie, allergens)
                            }
                        } catch (e: Exception) {
                            showDishDialog(dishName, "查询失败", "数据解析错误")
                        }
                    } else {
                        showDishDialog(dishName, "查询失败", "服务器无响应 (HTTP ${response.code})")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showDishDialog(dishName, "查询失败", e.message ?: "网络错误")
                }
            }
        }.start()
    }

    private fun showDishDialog(name: String, calorie: String, allergens: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(name)
            .setMessage("卡路里: $calorie\n过敏源: $allergens")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "temp_menu.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

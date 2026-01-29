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
import androidx.cardview.widget.CardView
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class DishRecognitionActivity : AppCompatActivity() {
    private lateinit var ivDish: ImageView
    private lateinit var tvLoading: TextView
    private lateinit var cardResult: CardView
    private lateinit var tvDishName: TextView
    private lateinit var tvCalorie: TextView
    private lateinit var tvAllergens: TextView
    private val okHttpClient = OkHttpClient()

    // 百度API密钥(与菜单识别共用)
    private val API_KEY = "dMrpDLIFCVO9S2kNF1wsL501"
    private val SECRET_KEY = "r04IBpCsn1Mx9lH9oMnYtJYtVx6d86FK"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            ivDish.setImageURI(uri)
            recognizeDish(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dish_recognition)

        ivDish = findViewById(R.id.iv_dish)
        tvLoading = findViewById(R.id.tv_loading)
        cardResult = findViewById(R.id.card_result)
        tvDishName = findViewById(R.id.tv_dish_name)
        tvCalorie = findViewById(R.id.tv_calorie)
        tvAllergens = findViewById(R.id.tv_allergens)

        findViewById<Button>(R.id.btn_choose_dish).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun recognizeDish(uri: Uri) {
        tvLoading.text = "正在识别菜品..."
        tvLoading.visibility = View.VISIBLE
        cardResult.visibility = View.GONE

        Thread {
            try {
                val file = uriToFile(uri)
                if (file == null) {
                    runOnUiThread {
                        tvLoading.visibility = View.GONE
                        Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                // 方案1: 使用百度菜品识别API(需要单独申请)
                // 方案2: 直接发送给你的后端服务器识别
                // 这里演示方案2,发送Base64图片给后端
                val imageBase64 = encodeFileToBase64(file)
                
                val requestBody = FormBody.Builder()
                    .add("dishImage", imageBase64)
                    .build()

                val request = Request.Builder()
                    .url("http://10.0.2.2:5000/api/recognizeDish")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    tvLoading.visibility = View.GONE
                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(responseBody)
                            if (json.has("error")) {
                                tvDishName.text = "识别失败"
                                tvCalorie.text = "错误: ${json.getString("error")}"
                                tvAllergens.text = "提示: 服务器未找到该菜品信息"
                                cardResult.visibility = View.VISIBLE
                            } else {
                                val dishName = json.getString("dishName")
                                val calorie = json.getString("calorie")
                                val allergens = json.getString("allergens")

                                tvDishName.text = "菜品名称: $dishName"
                                tvCalorie.text = "卡路里: $calorie"
                                tvAllergens.text = "过敏源: $allergens"
                                cardResult.visibility = View.VISIBLE
                            }
                        } catch (e: Exception) {
                            tvDishName.text = "识别失败"
                            tvCalorie.text = "错误: 数据解析失败"
                            tvAllergens.text = "提示: 服务器返回格式异常"
                            cardResult.visibility = View.VISIBLE
                        }
                    } else {
                        tvDishName.text = "查询失败"
                        tvCalorie.text = "错误: HTTP ${response.code}"
                        tvAllergens.text = "提示: 服务器无响应或返回错误"
                        cardResult.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvLoading.visibility = View.GONE
                    tvDishName.text = "查询失败"
                    tvCalorie.text = "错误: ${e.message}"
                    tvAllergens.text = "提示: 网络异常,请检查服务器连接"
                    cardResult.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun encodeFileToBase64(file: File): String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "temp_dish.jpg")
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

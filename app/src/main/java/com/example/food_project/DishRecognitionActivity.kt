package com.example.food_project

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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
    private lateinit var llDishesContainer: LinearLayout
    private val okHttpClient = OkHttpClient()
    private var currentImageBitmap: Bitmap? = null

    // 百度API密钥(与菜单识别共用)
    private val API_KEY = "dMrpDLIFCVO9S2kNF1wsL501"
    private val SECRET_KEY = "r04IBpCsn1Mx9lH9oMnYtJYtVx6d86FK"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageBitmap = uriToBitmap(uri)
            ivDish.setImageBitmap(currentImageBitmap)
            recognizeDish(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dish_recognition)

        ivDish = findViewById(R.id.iv_dish)
        tvLoading = findViewById(R.id.tv_loading)
        cardResult = findViewById(R.id.card_result)
        llDishesContainer = findViewById(R.id.ll_dishes_container)

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
                                showErrorResult("识别失败: ${json.getString("error")}")
                            } else if (json.getBoolean("success")) {
                                val dishCount = json.getInt("dishCount")
                                val dishesArray = json.getJSONArray("dishes")
                                
                                // 绘制边界框
                                drawBoundingBoxes(dishesArray)
                                
                                // 显示菜品信息
                                displayDishes(dishesArray, dishCount)
                            } else {
                                showErrorResult("识别失败: 未找到菜品")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showErrorResult("数据解析失败: ${e.message}")
                        }
                    } else {
                        showErrorResult("HTTP ${response.code}: 服务器无响应")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvLoading.visibility = View.GONE
                    showErrorResult("网络异常: ${e.message}")
                }
            }
        }.start()
    }

    private fun drawBoundingBoxes(dishesArray: org.json.JSONArray) {
        currentImageBitmap?.let { originalBitmap ->
            val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)
            val paint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
            }
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 40f
                isAntiAlias = true
            }

            val colors = arrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN)
            var colorIndex = 0

            for (i in 0 until dishesArray.length()) {
                val dish = dishesArray.getJSONObject(i)
                
                // 检查是否有bbox字段且不为null
                if (!dish.isNull("bbox")) {
                    val bboxArray = dish.getJSONArray("bbox")
                    val dishName = dish.getString("dishName")
                    val confidence = dish.getDouble("confidence")
                    
                    val left = bboxArray.getInt(0).toFloat()
                    val top = bboxArray.getInt(1).toFloat()
                    val right = bboxArray.getInt(2).toFloat()
                    val bottom = bboxArray.getInt(3).toFloat()

                    // 绘制边界框
                    paint.color = colors[colorIndex % colors.size]
                    canvas.drawRect(left, top, right, bottom, paint)

                    // 绘制标签背景
                    val label = "$dishName ${(confidence * 100).toInt()}%"
                    val textWidth = textPaint.measureText(label)
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(left, top - 50, left + textWidth + 20, top, paint)

                    // 绘制标签文字
                    canvas.drawText(label, left + 10, top - 15, textPaint)
                    paint.style = Paint.Style.STROKE
                    
                    colorIndex++
                }
            }

            ivDish.setImageBitmap(mutableBitmap)
        }
    }

    private fun displayDishes(dishesArray: org.json.JSONArray, dishCount: Int) {
        llDishesContainer.removeAllViews()
        
        // 添加标题
        val titleView = TextView(this).apply {
            text = "识别到 $dishCount 个菜品："
            textSize = 18f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 20)
        }
        llDishesContainer.addView(titleView)

        for (i in 0 until dishesArray.length()) {
            val dish = dishesArray.getJSONObject(i)
            val dishName = dish.getString("dishName")
            val calorie = dish.getString("calorie")
            val allergens = dish.getString("allergens")
            val confidence = dish.getDouble("confidence")
            val modelType = if (dish.has("modelType")) dish.getString("modelType") else "unknown"
            val hasBbox = !dish.isNull("bbox")

            val dishCard = createDishCard(i + 1, dishName, calorie, allergens, confidence, modelType, hasBbox)
            llDishesContainer.addView(dishCard)
        }

        cardResult.visibility = View.VISIBLE
    }

    private fun createDishCard(
        index: Int, 
        dishName: String, 
        calorie: String, 
        allergens: String, 
        confidence: Double,
        modelType: String,
        hasBbox: Boolean
    ): View {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 8f
            cardElevation = 4f
            setContentPadding(16, 16, 16, 16)
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvDishName = TextView(this).apply {
            text = "菜品 $index: $dishName"
            textSize = 16f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val tvModelInfo = TextView(this).apply {
            val modelTypeText = when(modelType) {
                "detection" -> "检测模型"
                "classification" -> "分类模型"
                else -> "未知模型"
            }
            val bboxText = if (hasBbox) "有定位" else "无定位"
            text = "识别方式: $modelTypeText ($bboxText)"
            textSize = 12f
            setTextColor(Color.GRAY)
        }

        val tvConfidence = TextView(this).apply {
            text = "置信度: ${(confidence * 100).toInt()}%"
            textSize = 14f
            setTextColor(Color.GRAY)
        }

        val tvCalorie = TextView(this).apply {
            text = "卡路里: $calorie"
            textSize = 14f
            setTextColor(Color.parseColor("#FF6B35"))
        }

        val tvAllergens = TextView(this).apply {
            text = "过敏源: $allergens"
            textSize = 14f
            setTextColor(if (allergens == "无") Color.GREEN else Color.RED)
        }

        contentLayout.addView(tvDishName)
        contentLayout.addView(tvModelInfo)
        contentLayout.addView(tvConfidence)
        contentLayout.addView(tvCalorie)
        contentLayout.addView(tvAllergens)
        cardView.addView(contentLayout)

        return cardView
    }

    private fun showErrorResult(message: String) {
        llDishesContainer.removeAllViews()
        val errorView = TextView(this).apply {
            text = message
            textSize = 16f
            setTextColor(Color.RED)
            setPadding(16, 16, 16, 16)
        }
        llDishesContainer.addView(errorView)
        cardResult.visibility = View.VISIBLE
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun encodeFileToBase64(file: File): String {
        // 先解码为Bitmap
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val baos = java.io.ByteArrayOutputStream()

        // 逐步压缩，直到小于4MB（Base64后约5.3MB）
        var quality = 90
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        
        while (baos.toByteArray().size > 4 * 1024 * 1024 && quality > 10) {
            baos.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        }

        val compressedBytes = baos.toByteArray()
        bitmap.recycle()
        baos.close()
        
        return Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
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

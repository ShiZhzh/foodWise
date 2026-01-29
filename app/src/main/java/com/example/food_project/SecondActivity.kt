package com.example.food_project

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.widget.ImageView // 新增：引入图片控件类

class SecondActivity : AppCompatActivity() {
    // 新增：声明图片控件变量
    private lateinit var ivFood: ImageView

    // 用于接收相册返回的图片
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            android.util.Log.d("FoodApp", "选择的图片Uri：$uri")
            // 核心：通过Uri加载图片到ImageView
            ivFood.setImageURI(uri)
        } else {
            android.util.Log.d("FoodApp", "未选择图片，Uri为null")
            ivFood.setImageDrawable(null) // 未选图时清空图片控件
            ivFood.setBackgroundResource(android.R.color.darker_gray) // 恢复灰色背景
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        // 新增：找到图片控件（ImageView）
        ivFood = findViewById(R.id.iv_food)

        // 1. 返回首页按钮（类型改为 View 以兼容）
        val btnBack: android.view.View = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        // 2. 选择菜品图片按钮（原有逻辑不变）
        val btnChoosePhoto: Button = findViewById(R.id.btn_choose_photo)
        btnChoosePhoto.setOnClickListener {
            val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    // 用于申请权限（原有逻辑不变）
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            android.widget.Toast.makeText(this, "需要权限才能打开相册", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 打开相册的方法（原有逻辑不变）
    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }
}
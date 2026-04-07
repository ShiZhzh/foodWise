package com.example.food_project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.view.View // 新增导入
import androidx.appcompat.app.AppCompatActivity

class FoodAnalysisActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_analysis)
        AgentEntryBinder.bind(this)

        findViewById<Button>(R.id.btn_menu_recognition).setOnClickListener {
            startActivity(Intent(this, MenuRecognitionActivity::class.java))
        }

        findViewById<Button>(R.id.btn_dish_recognition).setOnClickListener {
            startActivity(Intent(this, DishRecognitionActivity::class.java))
        }

        // 修改为 View 以兼容布局文件中可能是 Button 或 ImageView 的情况
        findViewById<android.view.View>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }
}

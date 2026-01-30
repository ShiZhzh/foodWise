package com.example.food_project

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DishDetailActivity : AppCompatActivity() {
    private lateinit var tvDishName: TextView
    private lateinit var tvCookingMethod: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvFiber: TextView
    private lateinit var tvSodium: TextView
    private lateinit var tvNutritionHighlight: TextView
    private lateinit var tvDifficulty: TextView
    private lateinit var tvCookingTime: TextView
    private lateinit var btnBack: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dish_detail)

        initViews()
        loadDishData()
    }

    private fun initViews() {
        tvDishName = findViewById(R.id.tv_dish_name)
        tvCookingMethod = findViewById(R.id.tv_cooking_method)
        tvCalories = findViewById(R.id.tv_calories)
        tvProtein = findViewById(R.id.tv_protein)
        tvFiber = findViewById(R.id.tv_fiber)
        tvSodium = findViewById(R.id.tv_sodium)
        tvNutritionHighlight = findViewById(R.id.tv_nutrition_highlight)
        tvDifficulty = findViewById(R.id.tv_difficulty)
        tvCookingTime = findViewById(R.id.tv_cooking_time)
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener { finish() }
    }

    private fun loadDishData() {
        val dishName = intent.getStringExtra("dishName") ?: "未知菜品"
        val cookingMethod = intent.getStringExtra("cookingMethod") ?: "暂无制作方法"
        val calories = intent.getStringExtra("calories") ?: "未知"
        val protein = intent.getStringExtra("protein")
        val fiber = intent.getStringExtra("fiber")
        val sodium = intent.getStringExtra("sodium") ?: "未知"
        val nutritionHighlight = intent.getStringExtra("nutritionHighlight")
        val difficulty = intent.getStringExtra("difficulty") ?: "未知"
        val cookingTime = intent.getStringExtra("cookingTime") ?: "未知"

        tvDishName.text = dishName
        tvCookingMethod.text = cookingMethod
        tvCalories.text = "热量：${calories} kcal"
        
        if (!protein.isNullOrEmpty()) {
            tvProtein.text = "蛋白质：${protein}g"
            tvProtein.visibility = TextView.VISIBLE
        } else {
            tvProtein.visibility = TextView.GONE
        }

        if (!fiber.isNullOrEmpty()) {
            tvFiber.text = "纤维：${fiber}g"
            tvFiber.visibility = TextView.VISIBLE
        } else {
            tvFiber.visibility = TextView.GONE
        }

        tvSodium.text = "钠：${sodium}mg"
        
        if (!nutritionHighlight.isNullOrEmpty()) {
            tvNutritionHighlight.text = "营养亮点：$nutritionHighlight"
            tvNutritionHighlight.visibility = TextView.VISIBLE
        } else {
            tvNutritionHighlight.visibility = TextView.GONE
        }

        tvDifficulty.text = "难度：$difficulty"
        tvCookingTime.text = "烹饪时间：${cookingTime}分钟"
    }
}

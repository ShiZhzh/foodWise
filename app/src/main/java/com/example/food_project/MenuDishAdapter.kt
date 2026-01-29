package com.example.food_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MenuDishAdapter(
    private val dishes: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<MenuDishAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDishName: TextView = view.findViewById(R.id.tv_dish_name)
        val tvDishStatus: TextView = view.findViewById(R.id.tv_dish_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_dish, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dishName = dishes[position]
        holder.tvDishName.text = dishName
        holder.itemView.setOnClickListener {
            onItemClick(dishName)
        }
    }

    override fun getItemCount() = dishes.size
}

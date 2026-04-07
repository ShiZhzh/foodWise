package com.example.food_project

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AgentChatAdapter(
    private val items: List<AgentChatMessage>,
    private val onDetailToggle: (Int) -> Unit
) : RecyclerView.Adapter<AgentChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: FrameLayout = itemView.findViewById(R.id.message_root)
        val text: TextView = itemView.findViewById(R.id.tv_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_agent_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = items[position]

        val layoutParams = holder.text.layoutParams as FrameLayout.LayoutParams
        if (item.role == "user") {
            layoutParams.gravity = Gravity.END
            holder.text.setBackgroundResource(R.drawable.bg_agent_message_user)
        } else {
            layoutParams.gravity = Gravity.START
            holder.text.setBackgroundResource(R.drawable.bg_agent_message_assistant)
        }
        holder.text.layoutParams = layoutParams

        if (item.adviceDetail != null) {
            holder.text.text = buildFoldText(item)
            holder.text.setTextColor(if (item.isDetailExpanded) Color.parseColor("#165B3E") else Color.parseColor("#1B9A63"))
            holder.text.isClickable = true
            holder.text.isFocusable = true
            holder.text.setOnClickListener { onDetailToggle(position) }
            holder.root.setOnClickListener { onDetailToggle(position) }
        } else {
            holder.text.text = item.content
            holder.text.setTextColor(Color.parseColor("#1B1B1B"))
            holder.text.isClickable = false
            holder.text.isFocusable = false
            holder.text.setOnClickListener(null)
            holder.root.setOnClickListener(null)
        }
    }

    private fun buildFoldText(item: AgentChatMessage): String {
        val detail = item.adviceDetail ?: return item.content
        if (!item.isDetailExpanded) {
            return "展开风险等级与建议行动"
        }

        val risk = detail.riskLevel.ifBlank { "未提供" }
        val actions = if (detail.actionItems.isEmpty()) {
            "- 暂无建议行动"
        } else {
            detail.actionItems.joinToString("\n") { "- $it" }
        }

        return "收起风险等级与建议行动\n\n风险等级：$risk\n\n建议行动：\n$actions"
    }

    override fun getItemCount(): Int = items.size
}

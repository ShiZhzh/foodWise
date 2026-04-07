package com.example.food_project

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AgentChatAdapter(
    private val items: List<AgentChatMessage>
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
        holder.text.text = item.content

        val layoutParams = holder.text.layoutParams as FrameLayout.LayoutParams
        if (item.role == "user") {
            layoutParams.gravity = Gravity.END
            holder.text.setBackgroundResource(R.drawable.bg_agent_message_user)
        } else {
            layoutParams.gravity = Gravity.START
            holder.text.setBackgroundResource(R.drawable.bg_agent_message_assistant)
        }
        holder.text.layoutParams = layoutParams
    }

    override fun getItemCount(): Int = items.size
}

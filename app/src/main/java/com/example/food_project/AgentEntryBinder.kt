package com.example.food_project

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView

object AgentEntryBinder {
    private const val TASKBAR_TAG = "agent_taskbar"
    private const val LOG_TAG = "AgentEntryBinder"

    fun bind(activity: Activity) {
        runCatching {
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
            if (root.findViewWithTag<View>(TASKBAR_TAG) != null) return

            val contentView = root.getChildAt(0)
            val taskbar = LayoutInflater.from(activity).inflate(R.layout.include_agent_taskbar, root, false)
            taskbar.tag = TASKBAR_TAG

            taskbar.findViewById<View>(R.id.btn_agent_entry).setOnClickListener {
                try {
                    val providerContext = runCatching {
                        (activity as? AgentContextProvider)?.buildAgentPageContext()
                    }.getOrNull()
                    val resolved = PageContextResolver.resolve(activity.javaClass.simpleName, providerContext)
                    val contextToken = AgentContextStore.put(resolved)
                    val lightweight = resolved.copy(contextPayload = emptyMap())
                    val intent = Intent(activity, AgentChatActivity::class.java).apply {
                        putExtra(AgentChatActivity.EXTRA_PAGE_KEY, resolved.pageKey)
                        putExtra(AgentChatActivity.EXTRA_PAGE_TITLE, resolved.pageTitle)
                        putExtra(AgentChatActivity.EXTRA_CONTEXT_TOKEN, contextToken)
                        putExtra(AgentChatActivity.EXTRA_CONTEXT_JSON, lightweight.toJson())
                        putExtra(AgentChatActivity.EXTRA_SOURCE_ACTIVITY, activity.javaClass.simpleName)
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "open agent failed, fallback start", e)
                    val fallbackIntent = Intent(activity, AgentChatActivity::class.java).apply {
                        putExtra(AgentChatActivity.EXTRA_PAGE_KEY, "general")
                        putExtra(AgentChatActivity.EXTRA_PAGE_TITLE, "健康助手")
                        putExtra(AgentChatActivity.EXTRA_SOURCE_ACTIVITY, activity.javaClass.simpleName)
                    }
                    activity.startActivity(fallbackIntent)
                }
            }
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
            root.addView(taskbar, layoutParams)

            taskbar.post {
                applyBottomSafePadding(contentView, taskbar.height + dp(activity, 8f))
            }
        }.onFailure { e ->
            Log.e(LOG_TAG, "bind taskbar failed for ${activity.javaClass.simpleName}", e)
        }
    }

    private fun applyBottomSafePadding(contentView: View?, extraBottomPadding: Int) {
        if (contentView == null) return

        val target = findScrollable(contentView) ?: contentView
        val originalBottom = (target.getTag(R.id.tag_agent_original_padding_bottom) as? Int)
            ?: target.paddingBottom.also { target.setTag(R.id.tag_agent_original_padding_bottom, it) }

        target.setPadding(
            target.paddingLeft,
            target.paddingTop,
            target.paddingRight,
            originalBottom + extraBottomPadding
        )

        if (target is ScrollView || target is NestedScrollView || target is RecyclerView || target is ListView) {
            target.clipToPadding = false
        }
    }

    private fun findScrollable(view: View): View? {
        if (view is ScrollView || view is NestedScrollView || view is RecyclerView || view is ListView) {
            return view
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                val found = findScrollable(view.getChildAt(index))
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

    private fun dp(activity: Activity, value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            activity.resources.displayMetrics
        ).toInt()
    }
}

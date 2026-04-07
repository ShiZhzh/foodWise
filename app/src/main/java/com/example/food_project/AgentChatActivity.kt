package com.example.food_project

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

class AgentChatActivity : AppCompatActivity() {
    private val tag = "AgentChatActivity"

    private lateinit var chatRoot: View
    private lateinit var inputBar: View
    private lateinit var btnBack: TextView
    private lateinit var tvPageTag: TextView
    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private val messageList = mutableListOf<AgentChatMessage>()
    private lateinit var chatAdapter: AgentChatAdapter
    private lateinit var apiClient: AgentApiClient

    private var currentSessionId: String? = null
    private lateinit var pageContext: AgentPageContext
    private var inputBarBaseBottomPadding = 0
    private var rvBaseBottomPadding = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(tag, "onCreate start")
        setContentView(R.layout.activity_agent_chat)

        var safeMode = false
        runCatching {
            initContext()
        }.onFailure { e ->
            Log.e(tag, "initContext failed, fallback to safe mode", e)
            pageContext = AgentPageContext(
                pageKey = "general",
                pageTitle = "健康助手",
                contextType = "general",
                contextSummary = "当前页面：健康助手"
            )
            safeMode = true
        }

        initViews()
        initList()
        initListeners()
        Log.i(tag, "onCreate ui initialized, safeMode=$safeMode, page=${pageContext.pageKey}")
        if (safeMode) {
            Toast.makeText(this, "健康助手启动异常，已回退到安全模式", Toast.LENGTH_SHORT).show()
            addAssistantMessage("已进入安全模式。你可以继续提问，我会先按通用健康建议回复。")
        } else {
            addAssistantMessage("你好，我是你的健康助手。请告诉我你现在想解决什么饮食问题。")
        }
    }

    private fun initContext() {
        val contextToken = intent.getStringExtra(EXTRA_CONTEXT_TOKEN)
        val fromStore = AgentContextStore.take(contextToken)
        val rawContextJson = intent.getStringExtra(EXTRA_CONTEXT_JSON)
        val pageKey = intent.getStringExtra(EXTRA_PAGE_KEY).orEmpty()
        val pageTitle = intent.getStringExtra(EXTRA_PAGE_TITLE).orEmpty()

        val parsed = fromStore ?: AgentPageContext.fromJson(rawContextJson)
        val provided = parsed ?: AgentPageContext(
            pageKey = pageKey,
            pageTitle = pageTitle,
            contextType = "general",
            contextSummary = "当前页面：${if (pageTitle.isBlank()) "健康助手" else pageTitle}"
        )

        pageContext = PageContextResolver.resolve(
            activityName = intent.getStringExtra(EXTRA_SOURCE_ACTIVITY).orEmpty(),
            provided = provided
        )
    }

    private fun initViews() {
        chatRoot = requireView(R.id.chat_root, "chat_root")
        inputBar = requireView(R.id.chat_input_bar, "chat_input_bar")
        btnBack = requireView(R.id.btn_back, "btn_back")
        tvPageTag = requireView(R.id.tv_page_tag, "tv_page_tag")
        rvChat = requireView(R.id.rv_chat, "rv_chat")
        etMessage = requireView(R.id.et_message, "et_message")
        btnSend = requireView(R.id.btn_send, "btn_send")
        inputBarBaseBottomPadding = inputBar.paddingBottom
        rvBaseBottomPadding = rvChat.paddingBottom

        tvPageTag.text = "页面识别：${pageContext.pageTitle}"
        apiClient = AgentApiClient(this)
        setupKeyboardSyncLift()
    }

    private fun initList() {
        chatAdapter = AgentChatAdapter(messageList)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = chatAdapter
    }

    private fun initListeners() {
        btnBack.setOnClickListener { finish() }
        btnSend.setOnClickListener { sendMessage() }
    }

    private fun sendMessage() {
        val input = etMessage.text.toString().trim()
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入消息", Toast.LENGTH_SHORT).show()
            return
        }

        addUserMessage(input)
        etMessage.setText("")
        setSendingState(true)

        Thread {
            try {
                val response = apiClient.sendMessage(
                    userMessage = input,
                    sessionId = currentSessionId,
                    pageContext = pageContext
                )
                currentSessionId = response.sessionId

                val finalText = buildString {
                    append(response.answer)
                    if (response.riskLevel.isNotBlank()) {
                        append("\n\n风险等级：")
                        append(response.riskLevel)
                    }
                    if (response.actionItems.isNotEmpty()) {
                        append("\n\n建议行动：")
                        response.actionItems.forEach { item ->
                            append("\n- ")
                            append(item)
                        }
                    }
                }

                runOnUiThread {
                    addAssistantMessage(finalText)
                    setSendingState(false)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    addAssistantMessage("当前请求失败，已建议稍后重试。错误信息：${e.message}")
                    setSendingState(false)
                }
            }
        }.start()
    }

    private fun addUserMessage(content: String) {
        messageList.add(AgentChatMessage(role = "user", content = content))
        chatAdapter.notifyItemInserted(messageList.lastIndex)
        rvChat.scrollToPosition(messageList.lastIndex)
    }

    private fun addAssistantMessage(content: String) {
        messageList.add(AgentChatMessage(role = "assistant", content = content))
        chatAdapter.notifyItemInserted(messageList.lastIndex)
        rvChat.scrollToPosition(messageList.lastIndex)
    }

    private fun setSendingState(isSending: Boolean) {
        btnSend.isEnabled = !isSending
        btnSend.text = if (isSending) "发送中..." else "发送"
        etMessage.isEnabled = !isSending
    }

    private fun setupKeyboardSyncLift() {
        ViewCompat.setOnApplyWindowInsetsListener(chatRoot) { _, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val barsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val lift = max(0, imeBottom - barsBottom)

            inputBar.translationY = -lift.toFloat()
            inputBar.setPadding(
                inputBar.paddingLeft,
                inputBar.paddingTop,
                inputBar.paddingRight,
                inputBarBaseBottomPadding + barsBottom
            )
            rvChat.setPadding(
                rvChat.paddingLeft,
                rvChat.paddingTop,
                rvChat.paddingRight,
                rvBaseBottomPadding + lift
            )
            insets
        }
        ViewCompat.requestApplyInsets(chatRoot)
    }

    private fun <T : android.view.View> requireView(id: Int, name: String): T {
        val view = findViewById<T>(id)
        if (view == null) {
            Log.e(tag, "required view missing: $name")
            throw IllegalStateException("Missing view: $name")
        }
        return view
    }

    companion object {
        const val EXTRA_PAGE_KEY = "agent_page_key"
        const val EXTRA_PAGE_TITLE = "agent_page_title"
        const val EXTRA_CONTEXT_TOKEN = "agent_context_token"
        const val EXTRA_CONTEXT_JSON = "agent_context_json"
        const val EXTRA_SOURCE_ACTIVITY = "agent_source_activity"
    }
}

package com.example.appcleanhouse

import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.models.ChatRoom
import com.example.appcleanhouse.adapter.RealtimeChatAdapter
import com.example.appcleanhouse.repository.ChatRepository
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject

/**
 * Màn hình Chat thời gian thực giữa Customer và Cleaner.
 * Dữ liệu được đồng bộ realtime qua Firestore snapshot listener.
 */
class RealtimeChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FrameLayout
    private lateinit var adapter: RealtimeChatAdapter

    private var chatRoomId: String = ""
    private var partnerName: String = ""
    private var currentUserName: String = ""
    private var currentUserRole: String = "customer"
    private var messageListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_realtime_chat)

        // Get extras
        chatRoomId = intent.getStringExtra("CHAT_ROOM_ID") ?: ""
        partnerName = intent.getStringExtra("PARTNER_NAME") ?: "Chat"
        currentUserRole = intent.getStringExtra("USER_ROLE") ?: "customer"

        // For creating a new chat room on-the-fly
        val customerId = intent.getStringExtra("CUSTOMER_ID") ?: ""
        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: ""
        val cleanerId = intent.getStringExtra("CLEANER_ID") ?: ""
        val cleanerName = intent.getStringExtra("CLEANER_NAME") ?: ""
        val orderId = intent.getStringExtra("ORDER_ID") ?: ""

        // Init views
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvPartnerName = findViewById<TextView>(R.id.tvChatPartnerName)

        tvPartnerName.text = partnerName

        val currentUserId = FirebaseAuthRepository.currentUserId
        adapter = RealtimeChatAdapter(currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter

        resolvePartnerDisplayName(
            tvPartnerName = tvPartnerName,
            fallbackCustomerId = customerId,
            fallbackCleanerId = cleanerId
        )

        btnBack.setOnClickListener { finish() }

        // Lấy tên user hiện tại
        FirestoreRepository.getUserProfile(currentUserId) { user ->
            currentUserName = user?.fullName ?: "User"
        }

        // Nếu đã có chatRoomId -> listen ngay
        if (chatRoomId.isNotEmpty()) {
            startListening()
        } else {
            // Tạo hoặc tìm phòng chat
            val cId = if (currentUserRole == "cleaner") currentUserId else customerId
            val cName = if (currentUserRole == "cleaner") "" else customerName
            val clId = if (currentUserRole == "cleaner") currentUserId else cleanerId
            val clName = if (currentUserRole == "cleaner") currentUserName else cleanerName

            ChatRepository.getOrCreateChatRoom(
                customerId = if (currentUserRole == "customer") currentUserId else customerId,
                customerName = if (currentUserRole == "customer") currentUserName else customerName,
                cleanerId = if (currentUserRole == "cleaner") currentUserId else cleanerId,
                cleanerName = if (currentUserRole == "cleaner") currentUserName else cleanerName,
                orderId = orderId,
                onResult = { room ->
                    chatRoomId = room.id
                    runOnUiThread { startListening() }
                },
                onFailure = { err -> 
                    runOnUiThread {
                        android.widget.Toast.makeText(this@RealtimeChatActivity, "Init chat room failed: $err", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // Send button
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            
            if (chatRoomId.isEmpty()) {
                android.widget.Toast.makeText(this, "Đang kết nối phòng chat, vui lòng thử lại...", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ChatRepository.sendMessage(
                chatRoomId = chatRoomId,
                senderId = currentUserId,
                senderName = currentUserName,
                senderRole = currentUserRole,
                text = text,
                onFailure = { err ->
                    runOnUiThread {
                        android.widget.Toast.makeText(this@RealtimeChatActivity, "Send failed: $err", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            )
            etMessage.setText("")
        }

        // Bắt sự kiện nhấn phím Enter trên bàn phím (đặc biệt hữu ích khi dùng máy ảo)
        etMessage.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN) {
                if (!event.isShiftPressed) {
                    btnSend.performClick()
                    return@setOnKeyListener true
                }
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        if (chatRoomId.isNotBlank()) {
            NotificationHelper.activeChatRoomId = chatRoomId
        }
    }

    override fun onPause() {
        super.onPause()
        if (NotificationHelper.activeChatRoomId == chatRoomId) {
            NotificationHelper.activeChatRoomId = null
        }
    }

    private fun startListening() {
        messageListener = ChatRepository.listenMessages(chatRoomId) { messages ->
            runOnUiThread {
                adapter.submitList(messages.toList()) {
                    if (messages.isNotEmpty()) {
                        rvMessages.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }
    }

    private fun resolvePartnerDisplayName(
        tvPartnerName: TextView,
        fallbackCustomerId: String,
        fallbackCleanerId: String
    ) {
        if (partnerName.isNotBlank() && !partnerName.equals("customer", true) && !partnerName.equals("user", true)) {
            return
        }

        if (chatRoomId.isBlank()) {
            if (currentUserRole == "cleaner") {
                resolveCustomerNameById(fallbackCustomerId) { resolved ->
                    runOnUiThread { tvPartnerName.text = resolved }
                }
            }
            return
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("chat_rooms")
            .document(chatRoomId)
            .get()
            .addOnSuccessListener { doc ->
                val room = doc.toObject<ChatRoom>()
                if (room == null) return@addOnSuccessListener

                if (currentUserRole == "cleaner") {
                    val directName = room.customerName.trim()
                    if (directName.isNotBlank()) {
                        runOnUiThread { tvPartnerName.text = directName }
                    } else {
                        val customerId = room.customerId.ifBlank { fallbackCustomerId }
                        resolveCustomerNameById(customerId) { resolved ->
                            runOnUiThread { tvPartnerName.text = resolved }
                        }
                    }
                } else {
                    val directName = room.cleanerName.trim()
                    if (directName.isNotBlank()) {
                        runOnUiThread { tvPartnerName.text = directName }
                    } else {
                        val cleanerId = room.cleanerId.ifBlank { fallbackCleanerId }
                        runOnUiThread { tvPartnerName.text = cleanerId.ifBlank { "Chat" } }
                    }
                }
            }
    }

    private fun resolveCustomerNameById(customerId: String, onResolved: (String) -> Unit) {
        if (customerId.isBlank()) {
            onResolved(partnerName.ifBlank { "Chat" })
            return
        }

        FirestoreRepository.getUserProfile(customerId) { user ->
            val resolved = user?.fullName?.trim().orEmpty().ifBlank { customerId }
            onResolved(resolved)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (NotificationHelper.activeChatRoomId == chatRoomId) {
            NotificationHelper.activeChatRoomId = null
        }
        messageListener?.remove()
    }
}

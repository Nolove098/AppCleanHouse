package com.example.appcleanhouse.repository

import com.example.appcleanhouse.models.ChatMessage
import com.example.appcleanhouse.models.ChatRoom
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * Repository xử lý chat thời gian thực giữa Customer và Cleaner.
 * Collections: chat_rooms, chat_rooms/{roomId}/messages
 */
object ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val chatRoomsCol get() = db.collection("chat_rooms")

    // ─── CHAT ROOMS ─────────────────────────────────────────────────

    /**
     * Tìm hoặc tạo phòng chat giữa customer và cleaner cho 1 order.
     */
    fun getOrCreateChatRoom(
        customerId: String,
        customerName: String,
        cleanerId: String,
        cleanerName: String,
        orderId: String,
        onResult: (ChatRoom) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        // Tìm phòng chat hiện có cho cặp customer-cleaner-order
        chatRoomsCol
            .whereEqualTo("customerId", customerId)
            .whereEqualTo("cleanerId", cleanerId)
            .whereEqualTo("orderId", orderId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val existing = snapshot.documents.first()
                    val room = existing.toObject(ChatRoom::class.java)?.copy(id = existing.id)
                    if (room != null) {
                        onResult(room)
                    } else {
                        onFailure("Không thể đọc phòng chat")
                    }
                } else {
                    // Tạo phòng chat mới
                    val docRef = chatRoomsCol.document()
                    val newRoom = ChatRoom(
                        id = docRef.id,
                        customerId = customerId,
                        customerName = customerName,
                        cleanerId = cleanerId,
                        cleanerName = cleanerName,
                        orderId = orderId
                    )
                    docRef.set(newRoom)
                        .addOnSuccessListener { onResult(newRoom) }
                        .addOnFailureListener { onFailure(it.message ?: "Không thể tạo phòng chat") }
                }
            }
            .addOnFailureListener { onFailure(it.message ?: "Lỗi tìm phòng chat") }
    }

    /**
     * Lấy danh sách phòng chat cho user.
     */
    fun listenChatRooms(
        userId: String,
        role: String,
        onResult: (List<ChatRoom>) -> Unit
    ): ListenerRegistration {
        val field = if (role == "cleaner") "cleanerId" else "customerId"
        return chatRoomsCol
            .whereEqualTo(field, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val rooms = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.lastTimestamp }
                onResult(rooms)
            }
    }

    // ─── MESSAGES ───────────────────────────────────────────────────

    /**
     * Gửi tin nhắn vào phòng chat.
     */
    fun sendMessage(
        chatRoomId: String,
        senderId: String,
        senderName: String,
        senderRole: String,
        text: String,
        onFailure: (String) -> Unit = {}
    ) {
        val messagesCol = chatRoomsCol.document(chatRoomId).collection("messages")
        val docRef = messagesCol.document()
        val message = ChatMessage(
            id = docRef.id,
            chatRoomId = chatRoomId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        docRef.set(message)
            .addOnSuccessListener {
                // Cập nhật last message & timestamp của phòng chat
                chatRoomsCol.document(chatRoomId).update(
                    mapOf(
                        "lastMessage" to text,
                        "lastTimestamp" to message.timestamp
                    )
                )
            }
            .addOnFailureListener { onFailure(it.message ?: "Gửi tin nhắn thất bại") }
    }

    /**
     * Lắng nghe tin nhắn realtime trong phòng chat.
     */
    fun listenMessages(
        chatRoomId: String,
        onResult: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        return chatRoomsCol.document(chatRoomId).collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }.sortedBy { it.timestamp }
                onResult(messages)
            }
    }
}

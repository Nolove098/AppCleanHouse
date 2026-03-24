package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.adapter.ChatRoomAdapter
import com.example.appcleanhouse.models.ChatRoom
import com.example.appcleanhouse.repository.ChatRepository
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.firebase.firestore.ListenerRegistration

/**
 * Dashboard dành cho Cleaner (nhân viên).
 * Hiển thị danh sách phòng chat với khách hàng.
 */
class CleanerDashboardActivity : AppCompatActivity() {

    private lateinit var rvChatRooms: RecyclerView
    private lateinit var adapter: ChatRoomAdapter
    private lateinit var tvChatCount: TextView
    private var roomsListener: ListenerRegistration? = null
    private var latestRooms: List<ChatRoom> = emptyList()
    private val customerNameCache = mutableMapOf<String, String>()
    private val customerNameRequests = mutableSetOf<String>()
    private val roomMessageCache = mutableMapOf<String, Long>()
    private var hasObservedChatRooms = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleaner_dashboard)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        tvChatCount = findViewById(R.id.tvChatCount)
        val emptyState = findViewById<View>(R.id.emptyState)
        rvChatRooms = findViewById(R.id.rvChatRooms)

        val currentUserId = FirebaseAuthRepository.currentUserId

        // Show welcome name
        FirestoreRepository.getUserProfile(currentUserId) { user ->
            runOnUiThread {
                tvWelcome.text = "Welcome back, ${user?.fullName ?: "Cleaner"}! 👋"
            }
        }

        // Setup chat rooms list
        adapter = ChatRoomAdapter("cleaner") { room ->
            val intent = Intent(this, RealtimeChatActivity::class.java)
            intent.putExtra("CHAT_ROOM_ID", room.id)
            intent.putExtra("PARTNER_NAME", room.customerName.ifBlank { room.customerId })
            intent.putExtra("USER_ROLE", "cleaner")
            startActivity(intent)
        }
        rvChatRooms.layoutManager = LinearLayoutManager(this)
        rvChatRooms.adapter = adapter

        // Setup Bottom Navigation
        setupBottomNavigation()

        // Fetch Cleaner ID then Listen for chat rooms
        FirestoreRepository.cleanersCol.whereEqualTo("authUid", currentUserId).get()
            .addOnSuccessListener { snapshot ->
                val actualCleanerId = if (!snapshot.isEmpty) snapshot.documents.first().id else currentUserId
                roomsListener = ChatRepository.listenChatRooms(
                    userId = actualCleanerId,
                    role = "cleaner",
                    onResult = { rooms ->
                        if (hasObservedChatRooms) {
                            rooms.forEach { room ->
                                val previousTimestamp = roomMessageCache[room.id]
                                if (previousTimestamp != null && room.lastTimestamp > previousTimestamp) {
                                    NotificationHelper.notifyChatMessage(this, room, "cleaner")
                                }
                            }
                        }
                        roomMessageCache.clear()
                        rooms.forEach { room -> roomMessageCache[room.id] = room.lastTimestamp }
                        hasObservedChatRooms = true

                        latestRooms = rooms
                        runOnUiThread {
                            submitResolvedRooms(rooms)
                            tvChatCount.text = "${rooms.size} active chats"
                            emptyState.visibility = if (rooms.isEmpty()) View.VISIBLE else View.GONE
                            rvChatRooms.visibility = if (rooms.isEmpty()) View.GONE else View.VISIBLE
                        }
                        resolveMissingCustomerNames(rooms)
                    }
                )
            }
    }

    private fun submitResolvedRooms(rooms: List<ChatRoom>) {
        val resolvedRooms = rooms.map { room ->
            if (room.customerName.isNotBlank()) {
                customerNameCache[room.customerId] = room.customerName
                room
            } else {
                val cachedName = customerNameCache[room.customerId].orEmpty()
                if (cachedName.isNotBlank()) room.copy(customerName = cachedName) else room
            }
        }
        adapter.submitList(resolvedRooms)
    }

    private fun resolveMissingCustomerNames(rooms: List<ChatRoom>) {
        val missingCustomerIds = rooms
            .filter { it.customerName.isBlank() && it.customerId.isNotBlank() }
            .map { it.customerId }
            .distinct()

        missingCustomerIds.forEach { customerId ->
            if (customerNameCache[customerId].isNullOrBlank().not() || customerNameRequests.contains(customerId)) {
                return@forEach
            }

            customerNameRequests.add(customerId)
            FirestoreRepository.getUserProfile(customerId) { user ->
                val resolvedName = user?.fullName?.trim().orEmpty()
                if (resolvedName.isNotBlank()) {
                    customerNameCache[customerId] = resolvedName
                    runOnUiThread { submitResolvedRooms(latestRooms) }
                }
                customerNameRequests.remove(customerId)
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_cleaner_chats
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_cleaner_chats -> true
                R.id.nav_cleaner_jobs -> {
                    startActivity(Intent(this, CleanerJobBoardActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        roomsListener?.remove()
        roomMessageCache.clear()
    }
}

package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.repository.ChatRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.button.MaterialButton

/**
 * Hiển thị chi tiết Đơn dọn dẹp để Cleaner nhận việc và đổi trạng thái.
 */
class CleanerJobDetailActivity : AppCompatActivity() {

    private lateinit var tvJobStatus: TextView
    private lateinit var tvServiceName: TextView
    private lateinit var tvJobDate: TextView
    private lateinit var tvJobAddress: TextView
    private lateinit var tvJobPrice: TextView
    private lateinit var btnUpdateStatus: MaterialButton
    private lateinit var btnChatCustomer: MaterialButton

    private var currentOrder: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleaner_job_detail)

        tvJobStatus = findViewById(R.id.tvJobStatus)
        tvServiceName = findViewById(R.id.tvServiceName)
        tvJobDate = findViewById(R.id.tvJobDate)
        tvJobAddress = findViewById(R.id.tvJobAddress)
        tvJobPrice = findViewById(R.id.tvJobPrice)
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus)
        btnChatCustomer = findViewById(R.id.btnChatCustomer)
        
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val orderId = intent.getStringExtra("ORDER_ID") ?: return

        // Load order details
        FirestoreRepository.ordersCol.document(orderId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val order = snapshot.toObject(Order::class.java)?.copy(id = snapshot.id)
            if (order != null) {
                currentOrder = order
                bindOrder(order)
            }
        }
    }

    private fun bindOrder(order: Order) {
        val service = MockData.MOCK_SERVICES.find { it.id == order.serviceId }
        tvServiceName.text = service?.name ?: "Dịch vụ"
        tvJobDate.text = "${order.date} • ${order.time}"
        tvJobAddress.text = order.address
        tvJobPrice.text = "$${order.totalPrice}"

        tvJobStatus.text = order.status

        when (order.status) {
            "Upcoming" -> {
                tvJobStatus.setTextColor(ContextCompat.getColor(this, R.color.blue_700))
                btnUpdateStatus.text = "Start Cleaning (In Progress)"
                btnUpdateStatus.isEnabled = true
                btnUpdateStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.app_primary))
                btnUpdateStatus.setOnClickListener { updateOrderStatus(order.id, "In Progress") }
            }
            "In Progress" -> {
                tvJobStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_700))
                btnUpdateStatus.text = "Finish Job (Complete)"
                btnUpdateStatus.isEnabled = true
                btnUpdateStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.green_600))
                btnUpdateStatus.setOnClickListener { updateOrderStatus(order.id, "Completed") }
            }
            "Completed" -> {
                tvJobStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600))
                btnUpdateStatus.text = "Job Completed"
                btnUpdateStatus.isEnabled = false
                btnUpdateStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.slate_400))
            }
            "Cancelled" -> {
                tvJobStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error))
                btnUpdateStatus.text = "Job Cancelled"
                btnUpdateStatus.isEnabled = false
                btnUpdateStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.slate_400))
            }
        }

        btnChatCustomer.setOnClickListener {
            btnChatCustomer.isEnabled = false // prevent double click
            // Fetch customer profile to get their real name
            FirestoreRepository.getUserProfile(order.userId) { customer ->
                val customerName = customer?.fullName ?: "Customer"
                ChatRepository.getOrCreateChatRoom(
                    customerId = order.userId,
                    customerName = customerName,
                    cleanerId = order.cleanerId,
                    cleanerName = order.cleanerName,
                    orderId = order.id,
                    onResult = { room ->
                        btnChatCustomer.isEnabled = true
                        val intent = Intent(this, RealtimeChatActivity::class.java)
                        intent.putExtra("CHAT_ROOM_ID", room.id)
                        intent.putExtra("PARTNER_NAME", customerName)
                        intent.putExtra("USER_ROLE", "cleaner")
                        startActivity(intent)
                    },
                    onFailure = { err ->
                        btnChatCustomer.isEnabled = true
                        Toast.makeText(this, "Không thể mở chat: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun updateOrderStatus(orderId: String, newStatus: String) {
        FirestoreRepository.ordersCol.document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }
}

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
    private lateinit var tvCustomerInfo: TextView
    private lateinit var stepUpcoming: TextView
    private lateinit var stepInProgress: TextView
    private lateinit var stepCompleted: TextView
    private lateinit var btnUpdateStatus: MaterialButton
    private lateinit var btnChatCustomer: MaterialButton

    private var currentOrder: Order? = null
    private var customerName: String = "Customer"
    private var customerPhone: String = ""
    private var loadedCustomerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleaner_job_detail)

        tvJobStatus = findViewById(R.id.tvJobStatus)
        tvServiceName = findViewById(R.id.tvServiceName)
        tvJobDate = findViewById(R.id.tvJobDate)
        tvJobAddress = findViewById(R.id.tvJobAddress)
        tvJobPrice = findViewById(R.id.tvJobPrice)
        tvCustomerInfo = findViewById(R.id.tvCustomerInfo)
        stepUpcoming = findViewById(R.id.stepUpcoming)
        stepInProgress = findViewById(R.id.stepInProgress)
        stepCompleted = findViewById(R.id.stepCompleted)
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

        if (loadedCustomerId != order.userId) {
            loadedCustomerId = order.userId
            loadCustomer(order.userId)
        }
        tvCustomerInfo.text = if (customerPhone.isBlank()) {
            "Customer: $customerName"
        } else {
            "Customer: $customerName • $customerPhone"
        }

        tvJobStatus.text = order.status
        updateProgressSteps(order.status)

        when (order.status) {
            "Upcoming" -> {
                tvJobStatus.setTextColor(ContextCompat.getColor(this, R.color.blue_700))
                tvJobStatus.setBackgroundResource(R.drawable.bg_status_upcoming)
                btnUpdateStatus.text = "Start Cleaning (In Progress)"
                btnUpdateStatus.isEnabled = true
                btnUpdateStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.app_primary))
                btnUpdateStatus.setOnClickListener { updateOrderStatus(order.id, "In Progress") }
            }
            "In Progress" -> {
                tvJobStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_700))
                tvJobStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
                btnUpdateStatus.text = "Finish Job (Complete)"
                btnUpdateStatus.isEnabled = true
                btnUpdateStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.green_600))
                btnUpdateStatus.setOnClickListener { updateOrderStatus(order.id, "Completed") }
            }
            "Completed" -> {
                tvJobStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600))
                tvJobStatus.setBackgroundResource(R.drawable.bg_status_completed)
                btnUpdateStatus.text = "Job Completed"
                btnUpdateStatus.isEnabled = false
                btnUpdateStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.slate_400))
            }
            "Cancelled" -> {
                tvJobStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error))
                tvJobStatus.setBackgroundResource(R.drawable.bg_status_cancelled)
                btnUpdateStatus.text = "Job Cancelled"
                btnUpdateStatus.isEnabled = false
                btnUpdateStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.slate_400))
            }
        }

        btnChatCustomer.text = "Chat with $customerName"

        btnChatCustomer.setOnClickListener {
            btnChatCustomer.isEnabled = false // prevent double click
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

    private fun loadCustomer(customerId: String) {
        FirestoreRepository.getUserProfile(customerId) { customer ->
            customerName = customer?.fullName?.ifBlank { "Customer" } ?: "Customer"
            customerPhone = customer?.phone.orEmpty()
            runOnUiThread {
                tvCustomerInfo.text = if (customerPhone.isBlank()) {
                    "Customer: $customerName"
                } else {
                    "Customer: $customerName • $customerPhone"
                }
                btnChatCustomer.text = "Chat with $customerName"
            }
        }
    }

    private fun updateProgressSteps(status: String) {
        fun activate(view: TextView) {
            view.setBackgroundResource(R.drawable.bg_step_active)
            view.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
        }

        fun idle(view: TextView) {
            view.setBackgroundResource(R.drawable.bg_step_idle)
            view.setTextColor(ContextCompat.getColor(this, R.color.slate_500))
        }

        idle(stepUpcoming)
        idle(stepInProgress)
        idle(stepCompleted)

        when (status) {
            "Upcoming" -> activate(stepUpcoming)
            "In Progress" -> {
                activate(stepUpcoming)
                activate(stepInProgress)
            }
            "Completed" -> {
                activate(stepUpcoming)
                activate(stepInProgress)
                activate(stepCompleted)
            }
            "Cancelled" -> {
                activate(stepUpcoming)
                stepInProgress.setBackgroundResource(R.drawable.bg_status_cancelled)
                stepInProgress.setTextColor(ContextCompat.getColor(this, R.color.status_error))
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

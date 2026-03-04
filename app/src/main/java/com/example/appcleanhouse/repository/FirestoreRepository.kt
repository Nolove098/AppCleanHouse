package com.example.appcleanhouse.repository

import com.example.appcleanhouse.models.Cleaner
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.models.Service
import com.example.appcleanhouse.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

/**
 * Repository xử lý toàn bộ tương tác với Cloud Firestore.
 * Collections: users, services, cleaners, orders
 */
object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // ─── Collection References ─────────────────────────────────────
    private val usersCol    get() = db.collection("users")
    private val servicesCol get() = db.collection("services")
    private val cleanersCol get() = db.collection("cleaners")
    private val ordersCol   get() = db.collection("orders")

    // ─── USER ──────────────────────────────────────────────────────

    /** Lưu/cập nhật thông tin người dùng lên Firestore */
    fun saveUserProfile(user: User, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        usersCol.document(user.uid)
            .set(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Lỗi khi lưu thông tin người dùng") }
    }

    /** Lấy thông tin người dùng theo UID */
    fun getUserProfile(userId: String, onResult: (User?) -> Unit) {
        usersCol.document(userId).get()
            .addOnSuccessListener { doc -> onResult(doc.toObject<User>()) }
            .addOnFailureListener { onResult(null) }
    }

    // ─── SERVICES ──────────────────────────────────────────────────

    /** Lấy danh sách tất cả services từ Firestore */
    fun getServices(onResult: (List<Service>) -> Unit, onFailure: (String) -> Unit = {}) {
        servicesCol.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toObject<Service>() }
                onResult(list)
            }
            .addOnFailureListener { onFailure(it.message ?: "Không thể tải dịch vụ") }
    }

    // ─── CLEANERS ──────────────────────────────────────────────────

    /** Lấy danh sách tất cả cleaners từ Firestore */
    fun getCleaners(onResult: (List<Cleaner>) -> Unit, onFailure: (String) -> Unit = {}) {
        cleanersCol.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toObject<Cleaner>() }
                onResult(list)
            }
            .addOnFailureListener { onFailure(it.message ?: "Không thể tải danh sách nhân viên") }
    }

    // ─── ORDERS ────────────────────────────────────────────────────

    /** Tạo đơn hàng mới. ID tự động sinh bởi Firestore */
    fun createOrder(
        order: Order,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val docRef = ordersCol.document() // auto-generated ID
        val orderWithId = order.copy(id = docRef.id)
        docRef.set(orderWithId)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { onFailure(it.message ?: "Không thể tạo đơn hàng") }
    }

    /** Lấy danh sách orders của một user, sắp xếp mới nhất lên trên */
    fun getUserOrders(
        userId: String,
        onResult: (List<Order>) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        ordersCol.whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toObject<Order>() }
                onResult(list)
            }
            .addOnFailureListener { onFailure(it.message ?: "Không thể tải lịch sử đơn hàng") }
    }

    /** Cập nhật trạng thái đơn hàng */
    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        ordersCol.document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Không thể cập nhật trạng thái") }
    }

    /** Cập nhật rating cho đơn hàng */
    fun rateOrder(
        orderId: String,
        rating: Int,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        ordersCol.document(orderId)
            .update("rating", rating)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Không thể lưu đánh giá") }
    }

    // ─── SEED DATA ─────────────────────────────────────────────────

    /**
     * Seed dữ liệu mẫu (services + cleaners) lên Firestore.
     * Chỉ cần gọi 1 lần khi khởi tạo app.
     * Hàm này kiểm tra nếu đã có data thì không seed lại.
     */
    fun seedInitialData(onDone: () -> Unit = {}) {
        servicesCol.get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                onDone()
                return@addOnSuccessListener
            }
            // Seed Services
            val services = listOf(
                mapOf("id" to "s1", "name" to "Deep Clean",     "description" to "Thorough cleaning for every corner including appliances and baseboards.", "pricePerHour" to 45, "rating" to 4.9),
                mapOf("id" to "s2", "name" to "Standard",       "description" to "Regular maintenance cleaning for keeping your home fresh.",               "pricePerHour" to 30, "rating" to 4.7),
                mapOf("id" to "s3", "name" to "Laundry",        "description" to "Washing, drying, and folding of clothes and linens.",                    "pricePerHour" to 25, "rating" to 4.8),
                mapOf("id" to "s4", "name" to "Carpet",         "description" to "Deep steam cleaning to remove stains and allergens.",                    "pricePerHour" to 60, "rating" to 4.6)
            )
            val batch = db.batch()
            services.forEach { s ->
                batch.set(servicesCol.document(s["id"] as String), s)
            }

            // Seed Cleaners
            val cleaners = listOf(
                mapOf("id" to "c1", "name" to "Phan Khai",    "rating" to 4.9, "jobCount" to 142, "specialty" to "Deep Clean Specialist"),
                mapOf("id" to "c2", "name" to "Hung Nguyen",  "rating" to 4.8, "jobCount" to 98,  "specialty" to "Fast & Efficient"),
                mapOf("id" to "c3", "name" to "Khoa",         "rating" to 5.0, "jobCount" to 215, "specialty" to "Pet Friendly"),
                mapOf("id" to "c4", "name" to "Khoi",         "rating" to 4.7, "jobCount" to 76,  "specialty" to "Eco-Friendly Products")
            )
            cleaners.forEach { c ->
                batch.set(cleanersCol.document(c["id"] as String), c)
            }

            batch.commit().addOnCompleteListener { onDone() }
        }
    }
}

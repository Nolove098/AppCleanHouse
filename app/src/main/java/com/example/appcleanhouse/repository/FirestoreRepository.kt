package com.example.appcleanhouse.repository

import com.example.appcleanhouse.models.Cleaner
import com.example.appcleanhouse.models.CleanerAvailability
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.models.PaymentCard
import com.example.appcleanhouse.models.Review
import com.example.appcleanhouse.models.Service
import com.example.appcleanhouse.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Repository xử lý toàn bộ tương tác với Cloud Firestore.
 * Collections: users, services, cleaners, orders
 */
object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // ─── Collection References ─────────────────────────────────────
    val usersCol    get() = db.collection("users")
    val servicesCol get() = db.collection("services")
    val cleanersCol get() = db.collection("cleaners")
    val ordersCol   get() = db.collection("orders")
    val reviewsCol  get() = db.collection("reviews")
    val availabilityCol get() = db.collection("cleaner_availability")

    // ─── USER ──────────────────────────────────────────────────────

    /** Lưu/cập nhật thông tin người dùng lên Firestore */
    fun saveUserProfile(user: User, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        usersCol.document(user.uid)
            .set(user, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Lỗi khi lưu thông tin người dùng") }
    }

    /** Lấy thông tin người dùng theo UID */
    fun getUserProfile(userId: String, onResult: (User?) -> Unit) {
        usersCol.document(userId).get()
            .addOnSuccessListener { doc -> onResult(doc.toObject<User>()) }
            .addOnFailureListener { onResult(null) }
    }

    /** Cập nhật phương thức thanh toán ưu tiên của người dùng */
    fun updateUserPaymentMethod(
        userId: String,
        paymentMethod: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        usersCol.document(userId)
            .update("paymentMethod", paymentMethod)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Không thể cập nhật phương thức thanh toán") }
    }

    private fun paymentCardsCol(userId: String) = usersCol.document(userId).collection("payment_cards")

    fun listenUserPaymentCards(
        userId: String,
        onResult: (List<PaymentCard>) -> Unit,
        onFailure: (String) -> Unit = {}
    ): ListenerRegistration {
        return paymentCardsCol(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onFailure(error.message ?: "Không thể tải danh sách thẻ")
                onResult(emptyList())
                return@addSnapshotListener
            }
            val cards = snapshot?.documents
                ?.mapNotNull { doc -> doc.toObject<PaymentCard>()?.copy(id = doc.id) }
                ?.sortedWith(compareByDescending<PaymentCard> { it.isDefault }.thenByDescending { it.createdAt })
                ?: emptyList()
            onResult(cards)
        }
    }

    fun addPaymentCard(
        userId: String,
        card: PaymentCard,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val docRef = paymentCardsCol(userId).document()
        val payload = card.copy(
            id = docRef.id,
            createdAt = if (card.createdAt > 0L) card.createdAt else System.currentTimeMillis()
        )
        docRef.set(payload)
            .addOnSuccessListener {
                if (payload.isDefault) {
                    setDefaultPaymentCard(userId, payload.id, onSuccess, onFailure)
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { onFailure(it.message ?: "Không thể thêm thẻ") }
    }

    fun deletePaymentCard(
        userId: String,
        cardId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        paymentCardsCol(userId).document(cardId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Không thể xoá thẻ") }
    }

    fun setDefaultPaymentCard(
        userId: String,
        cardId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        paymentCardsCol(userId).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    val shouldDefault = doc.id == cardId
                    batch.update(doc.reference, "isDefault", shouldDefault)
                }
                batch.update(usersCol.document(userId), "paymentMethod", "card")
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it.message ?: "Không thể đặt thẻ mặc định") }
            }
            .addOnFailureListener { onFailure(it.message ?: "Không thể đặt thẻ mặc định") }
    }

    fun ensureDefaultPaymentCards(
        userId: String,
        onDone: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        paymentCardsCol(userId).limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    onDone()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                val now = System.currentTimeMillis()
                val seedCards = listOf(
                    PaymentCard(type = "Visa", last4 = "4242", expiry = "12/24", isDefault = true, createdAt = now),
                    PaymentCard(type = "Mastercard", last4 = "8888", expiry = "06/25", isDefault = false, createdAt = now - 1)
                )
                seedCards.forEach { card ->
                    val doc = paymentCardsCol(userId).document()
                    batch.set(doc, card.copy(id = doc.id))
                }
                batch.update(usersCol.document(userId), "paymentMethod", "card")
                batch.commit()
                    .addOnSuccessListener { onDone() }
                    .addOnFailureListener { onFailure(it.message ?: "Không thể khởi tạo thẻ mặc định") }
            }
            .addOnFailureListener { onFailure(it.message ?: "Không thể kiểm tra thẻ thanh toán") }
    }

    fun updateUserFcmToken(
        userId: String,
        token: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        usersCol.document(userId)
            .set(
                mapOf(
                    "fcmToken" to token,
                    "fcmTokenUpdatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Không thể cập nhật FCM token") }
    }

    fun clearUserFcmToken(
        userId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        usersCol.document(userId)
            .set(
                mapOf(
                    "fcmToken" to FieldValue.delete(),
                    "fcmTokenUpdatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Không thể xoá FCM token") }
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

    /** Lắng nghe realtime danh sách cleaners từ Firestore */
    fun listenCleaners(
        onResult: (List<Cleaner>) -> Unit,
        onFailure: (String) -> Unit = {}
    ): ListenerRegistration {
        return cleanersCol.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onFailure(error.message ?: "Không thể lắng nghe danh sách nhân viên")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject<Cleaner>() } ?: emptyList()
            onResult(list)
        }
    }

    fun getCleanerIdByAuthUid(
        authUid: String,
        onResult: (String?) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        cleanersCol.whereEqualTo("authUid", authUid).limit(1).get()
            .addOnSuccessListener { snapshot ->
                val cleanerId = snapshot.documents.firstOrNull()?.id
                onResult(cleanerId)
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Không thể tải thông tin cleaner")
                onResult(null)
            }
    }

    fun getCleanerAvailability(
        cleanerId: String,
        onResult: (CleanerAvailability) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        availabilityCol.document(cleanerId).get()
            .addOnSuccessListener { doc ->
                val availabilityMap = parseAvailabilityMap(doc.get("availabilityByDate"))
                val legacyDays = doc.get("availableDays") as? List<*> ?: emptyList<Any>()
                val legacyTimeSlots = doc.get("availableTimeSlots") as? List<*> ?: emptyList<Any>()

                val normalizedMap = when {
                    availabilityMap.isNotEmpty() -> CleanerAvailability.normalizeAvailabilityMap(availabilityMap)
                    legacyDays.isNotEmpty() || legacyTimeSlots.isNotEmpty() -> CleanerAvailability.availabilityFromLegacy(
                        availableDays = legacyDays.filterIsInstance<String>(),
                        availableTimeSlots = legacyTimeSlots.filterIsInstance<String>()
                    )
                    else -> emptyMap()
                }

                onResult(
                    CleanerAvailability(
                        cleanerId = cleanerId,
                        availabilityByDate = normalizedMap,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                )
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Không thể tải lịch rảnh")
                onResult(
                    CleanerAvailability(
                        cleanerId = cleanerId,
                        availabilityByDate = emptyMap()
                    )
                )
            }
    }

    fun saveCleanerAvailability(
        availability: CleanerAvailability,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val normalized = availability.copy(
            availabilityByDate = CleanerAvailability.normalizeAvailabilityMap(availability.availabilityByDate)
                .mapValues { (_, slots) -> slots.distinct() }
                .filterValues { it.isNotEmpty() },
            updatedAt = System.currentTimeMillis()
        )
        availabilityCol.document(normalized.cleanerId)
            .set(normalized)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Không thể lưu lịch rảnh") }
    }

    private fun parseAvailabilityMap(rawValue: Any?): Map<String, List<String>> {
        val rawMap = rawValue as? Map<*, *> ?: return emptyMap()
        return rawMap.mapNotNull { (key, value) ->
            val dateKey = key as? String ?: return@mapNotNull null
            val slots = (value as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            dateKey to slots
        }.toMap()
    }

    // ─── ORDERS ────────────────────────────────────────────────────

    /** Tạo đơn hàng mới. ID tự động sinh bởi Firestore */
    fun createOrder(
        order: Order,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val docRef = ordersCol.document() // auto-generated ID
        val orderWithId = order.copy(
            id = docRef.id,
            timestamp = if (order.timestamp > 0L) order.timestamp else System.currentTimeMillis()
        )
        docRef.set(orderWithId)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { onFailure(it.message ?: "Không thể tạo đơn hàng") }
    }

    /** Lấy danh sách orders của một user theo thời gian thực, sắp xếp mới nhất lên trên */
    fun getUserOrdersRealtime(
        userId: String,
        onResult: (List<Order>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return ordersCol.whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull(::normalizeOrderSnapshot)
                    .sortedByDescending { it.timestamp }
                onResult(list)
            }
    }

    fun resolveOrderTimestamp(
        storedTimestamp: Long?,
        date: String,
        time: String,
        fallbackTimestamp: Long = 0L
    ): Long {
        if (storedTimestamp != null && storedTimestamp > 0L) return storedTimestamp

        val parsedTimestamp = parseOrderDateTime(date, time)
        if (parsedTimestamp != null) return parsedTimestamp

        return fallbackTimestamp
    }

    private fun parseOrderDateTime(date: String, time: String): Long? {
        if (date.isBlank()) return null
        val patterns = listOf(
            "MMM dd, yyyy hh:mm a",
            "MMM dd, yyyy"
        )

        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.ENGLISH).parse(
                    if (pattern.contains("hh:mm a")) "$date $time" else date
                )
            }.getOrNull()
            if (parsed != null) {
                return parsed.time
            }
        }

        return null
    }

    fun getCleanerBookedOrders(
        cleanerId: String,
        date: String,
        onResult: (List<Order>) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        ordersCol
            .whereEqualTo("cleanerId", cleanerId)
            .whereEqualTo("date", date)
            .whereIn("status", listOf("Upcoming", "In Progress"))
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull(::normalizeOrderSnapshot)
                onResult(list)
            }
            .addOnFailureListener { onFailure(it.message ?: "Không thể tải lịch đã đặt") }
    }

    /** Lấy chi tiết order theo orderId */
    fun getOrderById(
        orderId: String,
        onResult: (Order?) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        ordersCol.document(orderId).get()
            .addOnSuccessListener { doc -> onResult(normalizeOrderSnapshot(doc)) }
            .addOnFailureListener {
                onFailure(it.message ?: "Không thể tải chi tiết đơn hàng")
                onResult(null)
            }
    }

    private fun normalizeOrderSnapshot(doc: DocumentSnapshot): Order? {
        val order = doc.toObject<Order>() ?: return null
        val normalizedTimestamp = resolveOrderTimestamp(
            storedTimestamp = doc.getLong("timestamp"),
            date = doc.getString("date").orEmpty(),
            time = doc.getString("time").orEmpty(),
            fallbackTimestamp = order.timestamp
        )

        val storedTimestamp = doc.getLong("timestamp")
        if ((storedTimestamp == null || storedTimestamp <= 0L) && normalizedTimestamp > 0L) {
            ordersCol.document(doc.id).update("timestamp", normalizedTimestamp)
        }

        return order.copy(id = doc.id, timestamp = normalizedTimestamp)
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

    /** Lưu review cho order và cập nhật rating của order trong cùng một batch. */
    fun saveReview(
        review: Review,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val reviewDocId = review.orderId.ifBlank { review.id }
        if (reviewDocId.isBlank()) {
            onFailure("Thiếu thông tin đơn hàng để lưu đánh giá")
            return
        }

        val normalizedReview = review.copy(
            id = reviewDocId,
            orderId = reviewDocId,
            comment = review.comment.trim()
        )

        val batch = db.batch()
        batch.set(reviewsCol.document(reviewDocId), normalizedReview)
        batch.update(ordersCol.document(reviewDocId), "rating", normalizedReview.rating)
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Không thể lưu đánh giá") }
    }

    /** Lấy review theo orderId. */
    fun getReviewByOrderId(
        orderId: String,
        onResult: (Review?) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        reviewsCol.document(orderId).get()
            .addOnSuccessListener { doc -> onResult(doc.toObject<Review>()) }
            .addOnFailureListener {
                onFailure(it.message ?: "Không thể tải đánh giá")
                onResult(null)
            }
    }

    /** Lấy danh sách review của cleaner, mới nhất trước. */
    fun getReviewsForCleaner(
        cleanerId: String,
        onResult: (List<Review>) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        reviewsCol.whereEqualTo("cleanerId", cleanerId).get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents
                    .mapNotNull { it.toObject<Review>() }
                    .sortedByDescending { it.createdAt }
                onResult(list)
            }
            .addOnFailureListener { onFailure(it.message ?: "Không thể tải danh sách đánh giá") }
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
                mapOf(
                    "id" to "c1", "name" to "Phan Khai", "rating" to 4.9, "jobCount" to 142,
                    "specialty" to "Deep Clean Specialist", "experience" to "6 yrs",
                    "about" to "Khai is known for deep-clean precision and kitchen revival.",
                    "pricePerHour" to 48, "tags" to listOf("Deep Clean", "Kitchen", "Sanitize"), "distanceKm" to 1.8
                ),
                mapOf(
                    "id" to "c2", "name" to "Hung Nguyen", "rating" to 4.8, "jobCount" to 98,
                    "specialty" to "Fast & Efficient", "experience" to "4 yrs",
                    "about" to "Hung specializes in fast but meticulous standard cleaning.",
                    "pricePerHour" to 42, "tags" to listOf("Standard", "Speed", "Organize"), "distanceKm" to 3.2
                ),
                mapOf(
                    "id" to "c3", "name" to "Khoa Tran", "rating" to 5.0, "jobCount" to 215,
                    "specialty" to "Pet Friendly", "experience" to "7 yrs",
                    "about" to "Khoa is a premium cleaner trusted by pet owners.",
                    "pricePerHour" to 55, "tags" to listOf("Pet Friendly", "Allergy", "Odor Care"), "distanceKm" to 2.4
                ),
                mapOf(
                    "id" to "c4", "name" to "Khoi Le", "rating" to 4.7, "jobCount" to 76,
                    "specialty" to "Eco-Friendly Products", "experience" to "3 yrs",
                    "about" to "Khoi focuses on eco-safe products and gentle textile care.",
                    "pricePerHour" to 40, "tags" to listOf("Eco", "Laundry", "Carpet"), "distanceKm" to 4.6
                )
            )
            cleaners.forEach { c ->
                batch.set(cleanersCol.document(c["id"] as String), c)
            }

            batch.commit().addOnCompleteListener { onDone() }
        }
    }

    // ─── USER ROLE ──────────────────────────────────────────────────

    /** Lấy role của user hiện tại từ Firestore */
    fun getUserRole(userId: String, onResult: (String) -> Unit) {
        usersCol.document(userId).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "customer"
                onResult(role)
            }
            .addOnFailureListener { onResult("customer") } // Default to customer
    }

    // ─── SEED CLEANER ACCOUNTS ──────────────────────────────────────

    /**
     * Seed 4 cleaner accounts tuần tự.
     * Nếu account tồn tại, đăng nhập tạm để lấy UID và cập nhật Firestore.
     * Cuối cùng sign out để không dính session.
     */
    fun seedCleanerAccounts() {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

        data class CleanerSeed(
            val email: String, val password: String,
            val name: String, val phone: String, val cleanerDocId: String
        )

        val accounts = listOf(
            CleanerSeed("cleaner1@cleanhouse.com", "cleaner123", "Hung Nguyen", "0900000001", "c2"),
            CleanerSeed("cleaner2@cleanhouse.com", "cleaner123", "Phan Khai", "0900000002", "c1"),
            CleanerSeed("cleaner3@cleanhouse.com", "cleaner123", "Khoa Tran", "0900000003", "c3"),
            CleanerSeed("cleaner4@cleanhouse.com", "cleaner123", "Khoi Le", "0900000004", "c4")
        )

        // Sign out first so we don't conflict with current session
        auth.signOut()

        fun syncFirestore(uid: String, acc: CleanerSeed, onDone: () -> Unit) {
            val user = com.example.appcleanhouse.models.User(
                uid = uid, email = acc.email, fullName = acc.name,
                phone = acc.phone, role = "cleaner"
            )
            usersCol.document(uid).set(user).addOnCompleteListener {
                cleanersCol.document(acc.cleanerDocId).update("authUid", uid)
                    .addOnCompleteListener { onDone() }
            }
        }

        fun processNext(index: Int) {
            if (index >= accounts.size) {
                // Done with all 4
                auth.signOut()
                return
            }
            val acc = accounts[index]
            
            // Try create
            auth.createUserWithEmailAndPassword(acc.email, acc.password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: run { processNext(index + 1); return@addOnSuccessListener }
                    syncFirestore(uid, acc) { processNext(index + 1) }
                }
                .addOnFailureListener {
                    // Likely exists, so sign in to get UID
                    auth.signInWithEmailAndPassword(acc.email, acc.password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid ?: run { processNext(index + 1); return@addOnSuccessListener }
                            syncFirestore(uid, acc) { processNext(index + 1) }
                        }
                        .addOnFailureListener {
                            // Proceed anyway
                            processNext(index + 1)
                        }
                }
        }

        // Bỏ qua check snapshot size để chạy force-sync 1 lần
        processNext(0)
    }
}

package com.example.appcleanhouse.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Repository xử lý toàn bộ logic Firebase Authentication.
 * Singleton object để dùng chung toàn app.
 */
object FirebaseAuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /** Người dùng hiện tại (null nếu chưa đăng nhập) */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** UID của người dùng hiện tại */
    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    /** Kiểm tra đã đăng nhập chưa */
    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Đăng nhập bằng email & password.
     * @param onSuccess Callback khi thành công, trả về FirebaseUser
     * @param onFailure Callback khi thất bại, trả về thông báo lỗi
     */
    fun login(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onFailure("Email và mật khẩu không được để trống")
            return
        }

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let { onSuccess(it) }
                    ?: onFailure("Đăng nhập thất bại, vui lòng thử lại")
            }
            .addOnFailureListener { e ->
                val msg = when {
                    e.message?.contains("no user record") == true -> "Email chưa được đăng ký"
                    e.message?.contains("password is invalid") == true -> "Mật khẩu không đúng"
                    e.message?.contains("badly formatted") == true -> "Định dạng email không hợp lệ"
                    else -> "Đăng nhập thất bại: ${e.message}"
                }
                onFailure(msg)
            }
    }

    /**
     * Đăng ký tài khoản mới bằng email & password.
     * @param onSuccess Callback khi thành công, trả về FirebaseUser
     * @param onFailure Callback khi thất bại, trả về thông báo lỗi
     */
    fun register(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onFailure("Email và mật khẩu không được để trống")
            return
        }
        if (password.length < 6) {
            onFailure("Mật khẩu phải có ít nhất 6 ký tự")
            return
        }

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let { onSuccess(it) }
                    ?: onFailure("Đăng ký thất bại, vui lòng thử lại")
            }
            .addOnFailureListener { e ->
                val msg = when {
                    e.message?.contains("email address is already") == true -> "Email này đã được sử dụng"
                    e.message?.contains("badly formatted") == true -> "Định dạng email không hợp lệ"
                    else -> "Đăng ký thất bại: ${e.message}"
                }
                onFailure(msg)
            }
    }

    /**
     * Đăng xuất khỏi tài khoản hiện tại.
     */
    fun logout() {
        val userId = auth.currentUser?.uid
        if (!userId.isNullOrBlank()) {
            FirestoreRepository.clearUserFcmToken(userId)
        }
        FirebaseMessaging.getInstance().deleteToken()
        auth.signOut()
    }
}

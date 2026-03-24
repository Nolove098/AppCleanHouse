package com.example.appcleanhouse

import android.util.Log
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    fun syncCurrentUserToken() {
        val userId = FirebaseAuthRepository.currentUserId
        if (userId.isBlank()) return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isBlank()) return@addOnSuccessListener
                FirestoreRepository.updateUserFcmToken(
                    userId = userId,
                    token = token,
                    onFailure = { error -> Log.w(TAG, "Failed to save FCM token: $error") }
                )
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to fetch FCM token", error)
            }
    }
}

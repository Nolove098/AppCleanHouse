package com.example.appcleanhouse

import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return

        val userId = FirebaseAuthRepository.currentUserId
        if (userId.isBlank()) return

        FirestoreRepository.updateUserFcmToken(
            userId = userId,
            token = token,
            onFailure = { error -> Log.w(TAG, "Failed to sync refreshed token: $error") }
        )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        NotificationHelper.ensureChannels(this)

        val data = remoteMessage.data
        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "AppCleanHouse"
        val body = data["body"]
            ?: remoteMessage.notification?.body
            ?: "Bạn có thông báo mới"

        val type = data["type"].orEmpty()
        val notificationId = data["notificationId"]?.toIntOrNull() ?: System.currentTimeMillis().toInt()

        val pendingIntent = when (type) {
            "booking_update" -> {
                val orderId = data["orderId"].orEmpty()
                if (orderId.isBlank()) null else PendingIntent.getActivity(
                    this,
                    (orderId + "fcm-booking").hashCode(),
                    android.content.Intent(this, BookingDetailActivity::class.java).apply {
                        putExtra("ORDER_ID", orderId)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags()
                )
            }
            "chat_message" -> {
                val roomId = data["chatRoomId"].orEmpty()
                if (roomId.isBlank() || NotificationHelper.activeChatRoomId == roomId) {
                    null
                } else {
                    PendingIntent.getActivity(
                        this,
                        (roomId + "fcm-chat").hashCode(),
                        android.content.Intent(this, RealtimeChatActivity::class.java).apply {
                            putExtra("CHAT_ROOM_ID", roomId)
                            putExtra("PARTNER_NAME", data["partnerName"].orEmpty())
                            putExtra("USER_ROLE", data["userRole"].orEmpty())
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags()
                    )
                }
            }
            "new_job" -> {
                val orderId = data["orderId"].orEmpty()
                if (orderId.isBlank()) null else PendingIntent.getActivity(
                    this,
                    (orderId + "fcm-job").hashCode(),
                    android.content.Intent(this, CleanerJobDetailActivity::class.java).apply {
                        putExtra("ORDER_ID", orderId)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags()
                )
            }
            else -> null
        }

        val channelId = when (type) {
            "booking_update" -> NotificationHelper.CHANNEL_BOOKINGS
            "chat_message" -> NotificationHelper.CHANNEL_CHATS
            "new_job" -> NotificationHelper.CHANNEL_JOBS
            else -> NotificationHelper.CHANNEL_BOOKINGS
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(
                if (channelId == NotificationHelper.CHANNEL_CHATS) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .apply {
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_IMMUTABLE
    }

    companion object {
        private const val TAG = "AppFcmService"
    }
}

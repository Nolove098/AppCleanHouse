package com.example.appcleanhouse

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.models.ChatRoom
import com.example.appcleanhouse.models.Order

object NotificationHelper {

    const val CHANNEL_BOOKINGS = "bookings"
    const val CHANNEL_CHATS = "chats"
    const val CHANNEL_JOBS = "jobs"

    var activeChatRoomId: String? = null

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(CHANNEL_BOOKINGS, "Booking Updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Order status updates for customers"
            },
            NotificationChannel(CHANNEL_CHATS, "Chat Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "New realtime chat messages"
            },
            NotificationChannel(CHANNEL_JOBS, "Cleaner Jobs", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "New jobs assigned to cleaners"
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun requestPermissionIfNeeded(activity: Activity, launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun notifyBookingStatus(context: Context, order: Order, previousStatus: String) {
        if (previousStatus == order.status) return
        if (!canNotify(context)) return

        val content = when (order.status) {
            "In Progress" -> "Your booking on ${order.date} at ${order.time} is now in progress."
            "Completed" -> "Your booking on ${order.date} at ${order.time} has been completed."
            "Cancelled" -> "Your booking on ${order.date} at ${order.time} was cancelled."
            else -> "Your booking status changed to ${order.status}."
        }

        val intent = Intent(context, BookingDetailActivity::class.java).apply {
            putExtra("ORDER_ID", order.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            order.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags()
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BOOKINGS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Booking Updated")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(order.id.hashCode(), notification)
    }

    fun notifyNewCleanerJob(context: Context, order: Order) {
        if (!canNotify(context)) return

        val content = "New job scheduled for ${order.date} at ${order.time}."
        val intent = Intent(context, CleanerJobDetailActivity::class.java).apply {
            putExtra("ORDER_ID", order.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (order.id + "job").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags()
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_JOBS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New Assigned Job")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify((order.id + "job").hashCode(), notification)
    }

    fun notifyChatMessage(context: Context, room: ChatRoom, currentUserRole: String) {
        if (!canNotify(context)) return
        if (activeChatRoomId == room.id) return
        if (room.lastMessage.isBlank()) return

        val partnerName = if (currentUserRole == "cleaner") {
            room.customerName.ifBlank { room.customerId }
        } else {
            room.cleanerName.ifBlank { room.cleanerId }
        }

        val intent = Intent(context, RealtimeChatActivity::class.java).apply {
            putExtra("CHAT_ROOM_ID", room.id)
            putExtra("PARTNER_NAME", partnerName)
            putExtra("USER_ROLE", currentUserRole)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (room.id + "chat").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags()
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CHATS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(partnerName)
            .setContentText(room.lastMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(room.lastMessage))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify((room.id + "chat").hashCode(), notification)
    }

    private fun canNotify(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun pendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}
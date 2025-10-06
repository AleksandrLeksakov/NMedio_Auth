package ru.netology.nmedia.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.random.Random
import ru.netology.nmedia.R
import ru.netology.nmedia.api.AuthApi
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.PushMessage

class PushService : FirebaseMessagingService() {

    private val gson = Gson()

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val data = message.data["content"]
            val pushMessage = gson.fromJson(data, PushMessage::class.java)

            handlePush(pushMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handlePush(pushMessage: PushMessage) {
        val myId = AppAuth.getInstance().getMyId()
        val recipientId = pushMessage.recipientId

        println("📱 Received push: recipientId=$recipientId, myId=$myId, content=${pushMessage.content}")

        when {
            // Массовая рассылка - показываем уведомление
            recipientId == null -> {
                println("📢 Mass notification - showing")
                showNotification(pushMessage.content)
            }
            // Уведомление для текущего пользователя
            recipientId == myId -> {
                println("👤 Personal notification - showing")
                showNotification(pushMessage.content)
            }
            // Сервер считает что у нас анонимная аутентификация или другая
            recipientId == 0L || recipientId != myId -> {
                println("🔄 Wrong recipient (expected: $recipientId, actual: $myId) - resending token")
                // Переотправляем push token
                sendPushToken()
            }
        }
    }

    private fun showNotification(content: String) {
        // Проверяем разрешение для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("❌ No notification permission, skipping notification: $content")
                return
            }
        }

        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            NotificationManagerCompat.from(this)
                .notify(Random.nextInt(100_000), notification)

            println("✅ Notification shown: $content")
        } catch (e: SecurityException) {
            println("❌ SecurityException when showing notification: ${e.message}")
        } catch (e: Exception) {
            println("❌ Error showing notification: ${e.message}")
        }
    }

    private fun sendPushToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                println("🔄 Resending push token: $token")
                sendPushTokenToServer(token)
            } else {
                println("❌ Failed to get FCM token for resending")
            }
        }
    }

    private fun sendPushTokenToServer(token: String) {
        // Вариант 1: Использовать suspend функцию с корутинами
        GlobalScope.launch {
            try {
                val response = AuthApi.service.sendPushToken(token)
                if (response.isSuccessful) {
                    println("✅ Push token sent to server successfully")
                } else {
                    println("❌ Failed to send push token: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                println("❌ Error sending push token: ${e.message}")
            }
        }
    }

    override fun onNewToken(token: String) {
        println("🆕 New FCM token: $token")
        sendPushTokenToServer(token)
    }

    companion object {
        private const val CHANNEL_ID = "messages"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.channel_description)
                }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
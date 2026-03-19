package com.vishruthdev.destiny.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vishruthdev.destiny.DestinyApplication
import com.vishruthdev.destiny.reminder.ReminderNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DestinyFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as DestinyApplication
        serviceScope.launch {
            app.pushTokenRepository.syncCurrentToken(explicitToken = token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: return
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return
        val reminderType = message.data["reminderType"].orEmpty()
        val itemId = message.data["itemId"].orEmpty()
        val dueAtMillis = message.data["dueAtMillis"]?.toLongOrNull()
        val notificationId = if (reminderType.isNotBlank() && itemId.isNotBlank() && dueAtMillis != null) {
            ReminderNotificationManager.notificationId(
                type = reminderType,
                itemId = itemId,
                dueAtMillis = dueAtMillis
            )
        } else {
            message.messageId?.hashCode() ?: System.currentTimeMillis().toInt()
        }

        ReminderNotificationManager.showPushNotification(
            context = applicationContext,
            notificationId = notificationId,
            title = title,
            body = body
        )
    }
}

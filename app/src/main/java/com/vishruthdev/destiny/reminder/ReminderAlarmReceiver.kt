package com.vishruthdev.destiny.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        ReminderNotificationManager.showPushNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            body = body
        )
    }

    companion object {
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_BODY = "reminder_body"
        const val EXTRA_NOTIFICATION_ID = "reminder_notification_id"
    }
}

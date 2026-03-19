package com.vishruthdev.destiny.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vishruthdev.destiny.MainActivity

object ReminderNotificationManager {
    private const val CHANNEL_ID = "destiny_reminders"
    private const val CHANNEL_NAME = "Destiny Reminders"
    private const val CHANNEL_DESCRIPTION = "Habit and revision reminders"

    private const val ALARM_CHANNEL_ID = "destiny_alarms"
    private const val ALARM_CHANNEL_NAME = "Destiny Alarms"
    private const val ALARM_CHANNEL_DESCRIPTION = "Habit and revision alarms (2 min before)"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

        val reminderChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmChannel = NotificationChannel(
            ALARM_CHANNEL_ID,
            ALARM_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = ALARM_CHANNEL_DESCRIPTION
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
        }

        notificationManager.createNotificationChannel(reminderChannel)
        notificationManager.createNotificationChannel(alarmChannel)
    }

    fun showPushNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return
        createChannels(context)

        val contentIntent = launchAppPendingIntent(context, notificationId)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun showAlarmNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return
        createChannels(context)

        val contentIntent = launchAppPendingIntent(context, notificationId)
        val fullScreenIntent = launchAppPendingIntent(context, notificationId + 1)

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setFullScreenIntent(fullScreenIntent, true)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun showHabitNotification(
        context: Context,
        habitId: String,
        habitName: String,
        dueAtMillis: Long
    ) {
        val formattedTime = DateFormat.getTimeFormat(context).format(dueAtMillis)
        showPushNotification(
            context = context,
            notificationId = notificationId("habit", habitId, dueAtMillis),
            title = "Habit in 10 minutes",
            body = "$habitName starts at $formattedTime."
        )
    }

    fun showRevisionNotification(
        context: Context,
        topicId: String,
        topicName: String,
        revisionDay: Int,
        dueAtMillis: Long
    ) {
        val formattedTime = DateFormat.getTimeFormat(context).format(dueAtMillis)
        showPushNotification(
            context = context,
            notificationId = notificationId("revision", topicId, dueAtMillis),
            title = "Revision in 10 minutes",
            body = "Day $revisionDay for $topicName starts at $formattedTime."
        )
    }

    fun notificationId(
        type: String,
        itemId: String,
        dueAtMillis: Long
    ): Int {
        return "$type:$itemId:$dueAtMillis".hashCode()
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun launchAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

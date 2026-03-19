package com.vishruthdev.destiny.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives BOOT_COMPLETED to trigger Application creation,
 * which starts ReminderScheduleManager and reschedules all alarms.
 */
class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Application.onCreate() handles rescheduling via ReminderScheduleManager
        }
    }
}

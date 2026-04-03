package com.vishruthdev.destiny.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.vishruthdev.destiny.data.HabitWithStats
import com.vishruthdev.destiny.data.RevisionTopicWithProgress

class ReminderScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun scheduleAll(
        habits: List<HabitWithStats>,
        revisions: List<RevisionTopicWithProgress>
    ) {
        cancelAllTracked()

        val nowMillis = System.currentTimeMillis()
        val newRequestCodes = mutableSetOf<Int>()

        for (habit in habits) {
            scheduleHabitReminders(habit, nowMillis, newRequestCodes)
        }

        for (revision in revisions) {
            scheduleRevisionReminders(revision, nowMillis, newRequestCodes)
        }

        saveTrackedCodes(newRequestCodes)
        Log.d(TAG, "Scheduled ${newRequestCodes.size} alarms (${habits.size} habits, ${revisions.size} revisions)")
    }

    private fun scheduleHabitReminders(
        habit: HabitWithStats,
        nowMillis: Long,
        codes: MutableSet<Int>
    ) {
        if (!habit.alarmEnabled) return

        val todayStart = dayStartMillis(nowMillis)
        val dueToday = todayStart + timeOffsetMillis(habit.startHour, habit.startMinute)

        // Calculate notification trigger (10 min before)
        val notifyToday = dueToday - NOTIFY_LEAD_MS
        val (notifyAt, notifyDueAt) = if (notifyToday > nowMillis) {
            notifyToday to dueToday
        } else {
            (notifyToday + ONE_DAY_MS) to (dueToday + ONE_DAY_MS)
        }

        // Calculate alarm trigger (2 min before)
        val alarmToday = dueToday - ALARM_LEAD_MS
        val (alarmAt, alarmDueAt) = if (alarmToday > nowMillis) {
            alarmToday to dueToday
        } else {
            (alarmToday + ONE_DAY_MS) to (dueToday + ONE_DAY_MS)
        }

        // 10 min notification
        if (
            habit.startDateMillis <= dayStartMillis(notifyDueAt) &&
            shouldScheduleHabitReminderForDueDay(
                todayState = habit.todayState,
                dueAtMillis = notifyDueAt,
                nowMillis = nowMillis
            )
        ) {
            val code = habitNotifyRequestCode(habit.id)
            setAlarm(
                requestCode = code,
                triggerAtMillis = notifyAt,
                title = "Habit in 10 minutes",
                body = "${habit.name} starts soon.",
                notificationId = ReminderNotificationManager.notificationId("habit_notify", habit.id, notifyDueAt),
                isAlarm = false
            )
            codes.add(code)
        }

        // 2 min device alarm
        if (
            habit.startDateMillis <= dayStartMillis(alarmDueAt) &&
            shouldScheduleHabitReminderForDueDay(
                todayState = habit.todayState,
                dueAtMillis = alarmDueAt,
                nowMillis = nowMillis
            )
        ) {
            val code = habitAlarmRequestCode(habit.id)
            setAlarm(
                requestCode = code,
                triggerAtMillis = alarmAt,
                title = "Time for ${habit.name}",
                body = "${habit.name} starts now!",
                notificationId = ReminderNotificationManager.notificationId("habit_alarm", habit.id, alarmDueAt),
                isAlarm = true
            )
            codes.add(code)
        }
    }

    private fun scheduleRevisionReminders(
        revision: RevisionTopicWithProgress,
        nowMillis: Long,
        codes: MutableSet<Int>
    ) {
        if (!revision.alarmEnabled) return
        val nextDay = findNextSchedulableRevisionDay(revision.dayStates) ?: return

        val dayOffsetMillis = (nextDay - 1).coerceAtLeast(0) * ONE_DAY_MS
        val timeOffset = timeOffsetMillis(revision.revisionHour, revision.revisionMinute)
        val dueAtMillis = revision.startDateMillis + dayOffsetMillis + timeOffset

        // 10 min notification
        val notifyAt = dueAtMillis - NOTIFY_LEAD_MS
        if (notifyAt > nowMillis) {
            val code = revisionNotifyRequestCode(revision.id)
            setAlarm(
                requestCode = code,
                triggerAtMillis = notifyAt,
                title = "Revision in 10 minutes",
                body = "Day $nextDay for ${revision.name} starts soon.",
                notificationId = ReminderNotificationManager.notificationId("rev_notify", revision.id, dueAtMillis),
                isAlarm = false
            )
            codes.add(code)
        }

        // 2 min device alarm
        val alarmAt = dueAtMillis - ALARM_LEAD_MS
        if (alarmAt > nowMillis) {
            val code = revisionAlarmRequestCode(revision.id)
            setAlarm(
                requestCode = code,
                triggerAtMillis = alarmAt,
                title = "Time for ${revision.name}",
                body = "Day $nextDay revision starts now!",
                notificationId = ReminderNotificationManager.notificationId("rev_alarm", revision.id, dueAtMillis),
                isAlarm = true
            )
            codes.add(code)
        }
    }

    private fun setAlarm(
        requestCode: Int,
        triggerAtMillis: Long,
        title: String,
        body: String,
        notificationId: Int,
        isAlarm: Boolean
    ) {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_BODY, body)
            putExtra(ReminderAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_ALARM, isAlarm)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (isAlarm) {
            // Use setAlarmClock for device alarms — guarantees firing and shows alarm icon
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent),
                pendingIntent
            )
        } else {
            val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun cancelAllTracked() {
        val codes = prefs.getStringSet(KEY_ACTIVE_CODES, emptySet()) ?: emptySet()
        for (codeStr in codes) {
            val code = codeStr.toIntOrNull() ?: continue
            val intent = Intent(appContext, ReminderAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
        prefs.edit().remove(KEY_ACTIVE_CODES).apply()
    }

    private fun saveTrackedCodes(codes: Set<Int>) {
        prefs.edit()
            .putStringSet(KEY_ACTIVE_CODES, codes.map { it.toString() }.toSet())
            .apply()
    }

    private fun timeOffsetMillis(hour: Int, minute: Int): Long {
        return ((hour * 60L) + minute) * 60 * 1000L
    }

    private fun habitNotifyRequestCode(id: String) = "habit_notify:$id".hashCode()
    private fun habitAlarmRequestCode(id: String) = "habit_alarm:$id".hashCode()
    private fun revisionNotifyRequestCode(id: String) = "revision_notify:$id".hashCode()
    private fun revisionAlarmRequestCode(id: String) = "revision_alarm:$id".hashCode()

    private companion object {
        const val TAG = "ReminderScheduler"
        const val PREFS_NAME = "reminder_alarms"
        const val KEY_ACTIVE_CODES = "active_codes"
        const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
        const val NOTIFY_LEAD_MS = 10 * 60 * 1000L
        const val ALARM_LEAD_MS = 2 * 60 * 1000L
    }
}

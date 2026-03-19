package com.vishruthdev.destiny.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.vishruthdev.destiny.data.HabitWithStats
import com.vishruthdev.destiny.data.RevisionDayState
import com.vishruthdev.destiny.data.RevisionTopicWithProgress
import java.util.Calendar

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
            val code = scheduleHabitReminder(habit, nowMillis)
            if (code != null) newRequestCodes.add(code)
        }

        for (revision in revisions) {
            val code = scheduleRevisionReminder(revision, nowMillis)
            if (code != null) newRequestCodes.add(code)
        }

        saveTrackedCodes(newRequestCodes)
        Log.d(TAG, "Scheduled ${newRequestCodes.size} alarms (${habits.size} habits, ${revisions.size} revisions)")
    }

    private fun scheduleHabitReminder(habit: HabitWithStats, nowMillis: Long): Int? {
        val todayStart = dayStartMillis(nowMillis)
        val dueToday = todayStart + timeOffsetMillis(habit.startHour, habit.startMinute)
        val triggerToday = dueToday - REMINDER_LEAD_MS

        val (triggerAt, dueAt) = if (triggerToday > nowMillis) {
            triggerToday to dueToday
        } else {
            (triggerToday + ONE_DAY_MS) to (dueToday + ONE_DAY_MS)
        }

        // Don't schedule if the habit hasn't started yet
        if (habit.startDateMillis > dayStartMillis(dueAt)) return null

        val requestCode = habitRequestCode(habit.id)
        setAlarm(
            requestCode = requestCode,
            triggerAtMillis = triggerAt,
            title = "Habit in 10 minutes",
            body = "${habit.name} starts soon.",
            notificationId = ReminderNotificationManager.notificationId("habit", habit.id, dueAt)
        )
        return requestCode
    }

    private fun scheduleRevisionReminder(
        revision: RevisionTopicWithProgress,
        nowMillis: Long
    ): Int? {
        val nextDay = findNextSchedulableDay(revision) ?: return null

        val dayOffsetMillis = (nextDay - 1).coerceAtLeast(0) * ONE_DAY_MS
        val timeOffset = timeOffsetMillis(revision.revisionHour, revision.revisionMinute)
        val dueAtMillis = revision.startDateMillis + dayOffsetMillis + timeOffset
        val triggerAtMillis = dueAtMillis - REMINDER_LEAD_MS

        if (triggerAtMillis <= nowMillis) return null

        val requestCode = revisionRequestCode(revision.id)
        setAlarm(
            requestCode = requestCode,
            triggerAtMillis = triggerAtMillis,
            title = "Revision in 10 minutes",
            body = "Day $nextDay for ${revision.name} starts soon.",
            notificationId = ReminderNotificationManager.notificationId("revision", revision.id, dueAtMillis)
        )
        return requestCode
    }

    private fun findNextSchedulableDay(revision: RevisionTopicWithProgress): Int? {
        for (dayProgress in revision.dayStates) {
            when (dayProgress.state) {
                RevisionDayState.Completed -> continue
                RevisionDayState.Active -> return dayProgress.day
                RevisionDayState.InProgress -> return dayProgress.day
                RevisionDayState.Overdue -> return null
                RevisionDayState.Locked -> {
                    val allPreviousCompleted = revision.dayStates
                        .filter { it.day < dayProgress.day }
                        .all { it.state == RevisionDayState.Completed }
                    return if (allPreviousCompleted) dayProgress.day else null
                }
            }
        }
        return null
    }

    private fun setAlarm(
        requestCode: Int,
        triggerAtMillis: Long,
        title: String,
        body: String,
        notificationId: Int
    ) {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_BODY, body)
            putExtra(ReminderAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

    private fun dayStartMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun timeOffsetMillis(hour: Int, minute: Int): Long {
        return ((hour * 60L) + minute) * 60 * 1000L
    }

    private fun habitRequestCode(id: String) = "habit_alarm:$id".hashCode()
    private fun revisionRequestCode(id: String) = "revision_alarm:$id".hashCode()

    private companion object {
        const val TAG = "ReminderScheduler"
        const val PREFS_NAME = "reminder_alarms"
        const val KEY_ACTIVE_CODES = "active_codes"
        const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
        const val REMINDER_LEAD_MS = 10 * 60 * 1000L
    }
}

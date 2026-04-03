package com.vishruthdev.destiny.reminder

import com.vishruthdev.destiny.data.HabitCompletionState
import com.vishruthdev.destiny.data.RevisionDayProgress
import com.vishruthdev.destiny.data.RevisionDayState
import java.util.Calendar

internal fun shouldScheduleHabitReminderForDueDay(
    todayState: HabitCompletionState,
    dueAtMillis: Long,
    nowMillis: Long
): Boolean {
    val todayStart = dayStartMillis(nowMillis)
    val dueDayStart = dayStartMillis(dueAtMillis)
    return dueDayStart != todayStart || todayState == HabitCompletionState.NotStarted
}

internal fun findNextSchedulableRevisionDay(
    dayStates: List<RevisionDayProgress>
): Int? {
    for (dayProgress in dayStates) {
        when (dayProgress.state) {
            RevisionDayState.Completed -> continue
            RevisionDayState.Active -> return dayProgress.day
            RevisionDayState.InProgress -> return null
            RevisionDayState.Locked -> {
                val allPreviousCompleted = dayStates
                    .filter { it.day < dayProgress.day }
                    .all { it.state == RevisionDayState.Completed }
                return if (allPreviousCompleted) dayProgress.day else null
            }
        }
    }
    return null
}

internal fun dayStartMillis(timeMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timeMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

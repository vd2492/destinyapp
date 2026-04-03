package com.vishruthdev.destiny.reminder

import com.vishruthdev.destiny.data.HabitCompletionState
import com.vishruthdev.destiny.data.RevisionDayProgress
import com.vishruthdev.destiny.data.RevisionDayState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderPolicyTest {

    @Test
    fun `habit reminder is skipped when today's habit is already completed`() {
        val now = millisFor(2026, Calendar.APRIL, 3, 9, 0)
        val dueAt = millisFor(2026, Calendar.APRIL, 3, 10, 0)

        assertFalse(
            shouldScheduleHabitReminderForDueDay(
                todayState = HabitCompletionState.Completed,
                dueAtMillis = dueAt,
                nowMillis = now
            )
        )
    }

    @Test
    fun `habit reminder still schedules for tomorrow even when today is complete`() {
        val now = millisFor(2026, Calendar.APRIL, 3, 22, 0)
        val dueAtTomorrow = millisFor(2026, Calendar.APRIL, 4, 10, 0)

        assertTrue(
            shouldScheduleHabitReminderForDueDay(
                todayState = HabitCompletionState.Completed,
                dueAtMillis = dueAtTomorrow,
                nowMillis = now
            )
        )
    }

    @Test
    fun `revision reminder skips in progress day`() {
        val result = findNextSchedulableRevisionDay(
            listOf(
                RevisionDayProgress(day = 1, state = RevisionDayState.InProgress),
                RevisionDayProgress(day = 2, state = RevisionDayState.Locked)
            )
        )

        assertEquals(null, result)
    }

    @Test
    fun `revision reminder schedules next future locked day once previous days are complete`() {
        val result = findNextSchedulableRevisionDay(
            listOf(
                RevisionDayProgress(day = 1, state = RevisionDayState.Completed),
                RevisionDayProgress(day = 2, state = RevisionDayState.Locked),
                RevisionDayProgress(day = 4, state = RevisionDayState.Locked)
            )
        )

        assertEquals(2, result)
    }

    private fun millisFor(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int
    ): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

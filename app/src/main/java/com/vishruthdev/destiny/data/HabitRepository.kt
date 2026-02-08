package com.vishruthdev.destiny.data

import com.vishruthdev.destiny.data.local.HabitCompletionEntity
import com.vishruthdev.destiny.data.local.HabitDao
import com.vishruthdev.destiny.data.local.HabitEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

class HabitRepository(private val habitDao: HabitDao) {

    fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private val oneDayMillis = 24 * 60 * 60 * 1000L
    private val thirtyDaysMillis = 30 * oneDayMillis

    /** All habits with today's completion status for the home screen. */
    fun getTodayHabitsWithCompletion(): Flow<List<HabitWithCompletion>> {
        return habitDao.getHabitsWithTodayCompletion(todayStartMillis()).map { rows ->
            rows.map { row ->
                HabitWithCompletion(
                    id = row.id,
                    name = row.name,
                    completedToday = row.completedToday == 1
                )
            }
        }
    }

    /** All habits with streak and 30-day completion rate for the Habits screen. */
    fun getHabitsWithStats(): Flow<List<HabitWithStats>> = flow {
        combine(
            habitDao.getAllHabits(),
            habitDao.getCompletionCountFlow()
        ) { habits, _ -> habits }.collect { habits ->
            val withStats = habits.map { habit ->
                val completionDates = habitDao.getCompletionDatesForHabitOnce(habit.id)
                val streak = computeStreak(completionDates, todayStartMillis())
                val count = habitDao.getCompletionCountInRange(
                    habit.id,
                    habit.createdAtMillis,
                    habit.createdAtMillis + thirtyDaysMillis
                )
                val completionRate = ((count.toFloat() / 30) * 100).toInt().coerceIn(0, 100)
                HabitWithStats(
                    id = habit.id,
                    name = habit.name,
                    streakDays = streak,
                    completionRatePercent = completionRate
                )
            }
            emit(withStats)
        }
    }

    private fun computeStreak(completionDatesDesc: List<Long>, todayStart: Long): Int {
        if (completionDatesDesc.isEmpty()) return 0
        val sorted = completionDatesDesc.sortedDescending()
        var streak = 0
        var expectedDay = todayStart
        for (date in sorted) {
            if (date == expectedDay) {
                streak++
                expectedDay -= oneDayMillis
            } else if (date < expectedDay) {
                break
            }
            // if date > expectedDay, we're past a gap, stop
        }
        return streak
    }

    suspend fun setCompletedToday(habitId: String, completed: Boolean) {
        val today = todayStartMillis()
        if (completed) {
            habitDao.insertCompletion(HabitCompletionEntity(habitId = habitId, dateMillis = today))
        } else {
            habitDao.deleteCompletion(habitId, today)
        }
    }

    suspend fun addHabit(name: String): String {
        val id = UUID.randomUUID().toString()
        val entity = HabitEntity(
            id = id,
            name = name,
            createdAtMillis = todayStartMillis()
        )
        habitDao.insertHabit(entity)
        return id
    }

    suspend fun deleteHabit(habitId: String) {
        habitDao.deleteCompletionsForHabit(habitId)
        habitDao.deleteHabit(habitId)
    }
}

data class HabitWithCompletion(
    val id: String,
    val name: String,
    val completedToday: Boolean
)

data class HabitWithStats(
    val id: String,
    val name: String,
    val streakDays: Int,
    val completionRatePercent: Int
)

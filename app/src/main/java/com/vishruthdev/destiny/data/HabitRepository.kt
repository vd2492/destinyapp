package com.vishruthdev.destiny.data

import com.vishruthdev.destiny.data.local.HabitCompletionEntity
import com.vishruthdev.destiny.data.local.HabitDao
import com.vishruthdev.destiny.data.local.HabitEntity
import com.vishruthdev.destiny.data.local.RevisionCompletionEntity
import com.vishruthdev.destiny.data.local.RevisionTopicEntity
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
    private val revisionScheduleDays = listOf(1, 2, 4, 7)

    /** All habits with today's completion status for the home screen. */
    fun getTodayHabitsWithCompletion(): Flow<List<HabitWithCompletion>> {
        return habitDao.getHabitsWithTodayCompletion(
            todayStartMillis = todayStartMillis(),
            nowMillis = Calendar.getInstance().timeInMillis
        ).map { rows ->
            rows.map { row ->
                HabitWithCompletion(
                    id = row.id,
                    name = row.name,
                    completedToday = row.completedToday == 1,
                    startHour = row.startHour,
                    startMinute = row.startMinute
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
                    habit.startDateMillis,
                    habit.startDateMillis + thirtyDaysMillis
                )
                val completionRate = ((count.toFloat() / 30) * 100).toInt().coerceIn(0, 100)
                HabitWithStats(
                    id = habit.id,
                    name = habit.name,
                    streakDays = streak,
                    completionRatePercent = completionRate,
                    startDateMillis = habit.startDateMillis,
                    startHour = habit.startHour,
                    startMinute = habit.startMinute
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

    suspend fun addHabit(
        name: String,
        startDateMillis: Long,
        startHour: Int,
        startMinute: Int
    ): String {
        val id = UUID.randomUUID().toString()
        val entity = HabitEntity(
            id = id,
            name = name,
            createdAtMillis = Calendar.getInstance().timeInMillis,
            startDateMillis = startDateMillis,
            startHour = startHour.coerceIn(0, 23),
            startMinute = startMinute.coerceIn(0, 59)
        )
        habitDao.insertHabit(entity)
        return id
    }

    suspend fun deleteHabit(habitId: String) {
        habitDao.deleteCompletionsForHabit(habitId)
        habitDao.deleteHabit(habitId)
    }

    fun getRevisionTopicsWithProgress(): Flow<List<RevisionTopicWithProgress>> {
        return combine(
            habitDao.getAllRevisionTopics(),
            habitDao.getAllRevisionCompletions()
        ) { topics, completions ->
            val completionsByTopic = completions.groupBy { it.topicId }
            val now = Calendar.getInstance().timeInMillis
            topics.map { topic ->
                val completedDays = completionsByTopic[topic.id].orEmpty()
                    .map { it.revisionDay }
                    .toSet()
                val dayStates = buildRevisionDayStates(
                    startDateMillis = topic.startDateMillis,
                    revisionHour = topic.revisionHour,
                    revisionMinute = topic.revisionMinute,
                    completedDays = completedDays,
                    nowMillis = now
                )
                RevisionTopicWithProgress(
                    id = topic.id,
                    name = topic.name,
                    dayStates = dayStates,
                    startDateMillis = topic.startDateMillis,
                    revisionHour = topic.revisionHour,
                    revisionMinute = topic.revisionMinute
                )
            }
        }
    }

    private fun buildRevisionDayStates(
        startDateMillis: Long,
        revisionHour: Int,
        revisionMinute: Int,
        completedDays: Set<Int>,
        nowMillis: Long
    ): List<RevisionDayProgress> {
        return revisionScheduleDays.map { day ->
            val previousDays = revisionScheduleDays.takeWhile { it < day }
            val dueDateMillis = calculateRevisionDueAt(
                startDateMillis = startDateMillis,
                revisionHour = revisionHour,
                revisionMinute = revisionMinute,
                revisionDay = day
            )
            val state = when {
                completedDays.contains(day) -> RevisionDayState.Completed
                previousDays.any { it !in completedDays } -> RevisionDayState.Locked
                nowMillis >= dueDateMillis -> RevisionDayState.Active
                else -> RevisionDayState.Locked
            }
            RevisionDayProgress(day = day, state = state)
        }
    }

    private fun calculateRevisionDueAt(
        startDateMillis: Long,
        revisionHour: Int,
        revisionMinute: Int,
        revisionDay: Int
    ): Long {
        val dayOffsetMillis = (revisionDay - 1).coerceAtLeast(0) * oneDayMillis
        val timeOffsetMillis = ((revisionHour * 60L) + revisionMinute) * 60 * 1000L
        return startDateMillis + dayOffsetMillis + timeOffsetMillis
    }

    suspend fun addRevisionTopic(
        name: String,
        startDateMillis: Long,
        revisionHour: Int,
        revisionMinute: Int
    ): String {
        val id = UUID.randomUUID().toString()
        val entity = RevisionTopicEntity(
            id = id,
            name = name,
            createdAtMillis = Calendar.getInstance().timeInMillis,
            startDateMillis = startDateMillis,
            revisionHour = revisionHour.coerceIn(0, 23),
            revisionMinute = revisionMinute.coerceIn(0, 59)
        )
        habitDao.insertRevisionTopic(entity)
        return id
    }

    suspend fun completeActiveRevision(topicId: String): Int? {
        val topic = habitDao.getRevisionTopicById(topicId) ?: return null
        val completedDays = habitDao.getCompletedRevisionDaysForTopic(topicId).toSet()
        val now = Calendar.getInstance().timeInMillis

        val nextActiveDay = revisionScheduleDays.firstOrNull { day ->
            val previousDays = revisionScheduleDays.takeWhile { it < day }
            val dueDateMillis = calculateRevisionDueAt(
                startDateMillis = topic.startDateMillis,
                revisionHour = topic.revisionHour,
                revisionMinute = topic.revisionMinute,
                revisionDay = day
            )
            day !in completedDays &&
                previousDays.all { it in completedDays } &&
                now >= dueDateMillis
        } ?: return null

        habitDao.insertRevisionCompletion(
            RevisionCompletionEntity(
                topicId = topicId,
                revisionDay = nextActiveDay,
                completedAtMillis = Calendar.getInstance().timeInMillis
            )
        )
        return nextActiveDay
    }

    suspend fun deleteRevisionTopic(topicId: String) {
        habitDao.deleteRevisionCompletionsForTopic(topicId)
        habitDao.deleteRevisionTopic(topicId)
    }

    suspend fun clearAll() {
        habitDao.deleteAllRevisionCompletions()
        habitDao.deleteAllRevisionTopics()
        habitDao.deleteAllCompletions()
        habitDao.deleteAllHabits()
    }
}

data class HabitWithCompletion(
    val id: String,
    val name: String,
    val completedToday: Boolean,
    val startHour: Int,
    val startMinute: Int
)

data class HabitWithStats(
    val id: String,
    val name: String,
    val streakDays: Int,
    val completionRatePercent: Int,
    val startDateMillis: Long,
    val startHour: Int,
    val startMinute: Int
)

enum class RevisionDayState {
    Completed,
    Active,
    Locked
}

data class RevisionDayProgress(
    val day: Int,
    val state: RevisionDayState
)

data class RevisionTopicWithProgress(
    val id: String,
    val name: String,
    val dayStates: List<RevisionDayProgress>,
    val startDateMillis: Long,
    val revisionHour: Int,
    val revisionMinute: Int
) {
    val activeDay: Int?
        get() = dayStates.firstOrNull { it.state == RevisionDayState.Active }?.day
}

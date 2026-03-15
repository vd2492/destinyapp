package com.vishruthdev.destiny.data

import com.vishruthdev.destiny.data.local.HabitCompletionEntity
import com.vishruthdev.destiny.data.local.HabitDao
import com.vishruthdev.destiny.data.local.HabitEntity
import com.vishruthdev.destiny.data.local.RevisionCompletionEntity
import com.vishruthdev.destiny.data.local.RevisionTopicEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
        return combine(
            habitDao.getAllHabits(),
            habitDao.getAllHabitCompletions()
        ) { habits, completions ->
            val now = Calendar.getInstance().timeInMillis
            val todayStart = todayStartMillis(now)
            val completionDatesByHabit = completions
                .groupBy { it.habitId }
                .mapValues { (_, entries) -> entries.map { it.dateMillis }.toSet() }

            habits
                .filter { habit ->
                    habit.startDateMillis <= todayStart &&
                        now >= calculateHabitDueAt(todayStart, habit.startHour, habit.startMinute)
                }
                .map { habit ->
                    val completionDates = completionDatesByHabit[habit.id].orEmpty()
                    val history = calculateHabitHistory(
                        startDateMillis = habit.startDateMillis,
                        completionDates = completionDates,
                        nowMillis = now
                    )

                    HabitWithCompletion(
                        id = habit.id,
                        name = habit.name,
                        completedToday = todayStart in completionDates,
                        startHour = habit.startHour,
                        startMinute = habit.startMinute,
                        missedDaysCount = history.missedDaysCount,
                        latestMissedDateMillis = history.latestMissedDateMillis
                    )
                }
        }
    }

    /** All habits with streak and 30-day completion rate for the Habits screen. */
    fun getHabitsWithStats(): Flow<List<HabitWithStats>> {
        return combine(
            habitDao.getAllHabits(),
            habitDao.getAllHabitCompletions()
        ) { habits, completions ->
            val now = Calendar.getInstance().timeInMillis
            val completionDatesByHabit = completions
                .groupBy { it.habitId }
                .mapValues { (_, entries) -> entries.map { it.dateMillis }.toSet() }

            habits.map { habit ->
                val completionDates = completionDatesByHabit[habit.id].orEmpty()
                val streak = computeHabitStreak(
                    completionDates = completionDates,
                    startDateMillis = habit.startDateMillis,
                    latestExpectedDateMillis = latestExpectedHabitDate(
                        startDateMillis = habit.startDateMillis,
                        startHour = habit.startHour,
                        startMinute = habit.startMinute,
                        nowMillis = now
                    )
                )
                val trackedCompletions = completionDates.count { date ->
                    date >= habit.startDateMillis && date < habit.startDateMillis + thirtyDaysMillis
                }
                val completionRate = ((trackedCompletions.toFloat() / 30) * 100).toInt().coerceIn(0, 100)
                val history = calculateHabitHistory(
                    startDateMillis = habit.startDateMillis,
                    completionDates = completionDates,
                    nowMillis = now
                )

                HabitWithStats(
                    id = habit.id,
                    name = habit.name,
                    streakDays = streak,
                    completionRatePercent = completionRate,
                    startDateMillis = habit.startDateMillis,
                    startHour = habit.startHour,
                    startMinute = habit.startMinute,
                    missedDaysCount = history.missedDaysCount,
                    latestMissedDateMillis = history.latestMissedDateMillis
                )
            }
        }
    }

    private fun computeHabitStreak(
        completionDates: Set<Long>,
        startDateMillis: Long,
        latestExpectedDateMillis: Long?
    ): Int {
        val expectedDate = latestExpectedDateMillis ?: return 0
        var streak = 0
        var currentDay = expectedDate

        while (currentDay >= startDateMillis && currentDay in completionDates) {
            streak++
            currentDay -= oneDayMillis
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
            val overdueAtMillis = todayStartMillis(dueDateMillis) + oneDayMillis
            val state = when {
                completedDays.contains(day) -> RevisionDayState.Completed
                previousDays.any { it !in completedDays } -> RevisionDayState.Locked
                nowMillis >= overdueAtMillis -> RevisionDayState.Overdue
                nowMillis >= dueDateMillis -> RevisionDayState.Active
                else -> RevisionDayState.Locked
            }
            RevisionDayProgress(day = day, state = state)
        }
    }

    private fun calculateHabitHistory(
        startDateMillis: Long,
        completionDates: Set<Long>,
        nowMillis: Long
    ): HabitHistory {
        val todayStart = todayStartMillis(nowMillis)
        val lastCompletedDayThatCanBeMissed = todayStart - oneDayMillis
        if (lastCompletedDayThatCanBeMissed < startDateMillis) {
            return HabitHistory()
        }

        var currentDay = startDateMillis
        var missedDaysCount = 0
        var latestMissedDateMillis: Long? = null

        while (currentDay <= lastCompletedDayThatCanBeMissed) {
            if (currentDay !in completionDates) {
                missedDaysCount++
                latestMissedDateMillis = currentDay
            }
            currentDay += oneDayMillis
        }

        return HabitHistory(
            missedDaysCount = missedDaysCount,
            latestMissedDateMillis = latestMissedDateMillis
        )
    }

    private fun latestExpectedHabitDate(
        startDateMillis: Long,
        startHour: Int,
        startMinute: Int,
        nowMillis: Long
    ): Long? {
        val todayStart = todayStartMillis(nowMillis)
        if (todayStart < startDateMillis) return null

        val todayDueAtMillis = calculateHabitDueAt(todayStart, startHour, startMinute)
        val latestExpectedDay = if (nowMillis >= todayDueAtMillis) {
            todayStart
        } else {
            todayStart - oneDayMillis
        }

        return latestExpectedDay.takeIf { it >= startDateMillis }
    }

    private fun calculateHabitDueAt(
        dayStartMillis: Long,
        startHour: Int,
        startMinute: Int
    ): Long {
        val timeOffsetMillis = ((startHour * 60L) + startMinute) * 60 * 1000L
        return dayStartMillis + timeOffsetMillis
    }

    private fun todayStartMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
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

    suspend fun resetRevisionTopicFromToday(topicId: String): Boolean {
        val topic = habitDao.getRevisionTopicById(topicId) ?: return false
        habitDao.deleteRevisionCompletionsForTopic(topicId)
        habitDao.insertRevisionTopic(
            topic.copy(
                startDateMillis = todayStartMillis()
            )
        )
        return true
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
    val startMinute: Int,
    val missedDaysCount: Int = 0,
    val latestMissedDateMillis: Long? = null
)

data class HabitWithStats(
    val id: String,
    val name: String,
    val streakDays: Int,
    val completionRatePercent: Int,
    val startDateMillis: Long,
    val startHour: Int,
    val startMinute: Int,
    val missedDaysCount: Int = 0,
    val latestMissedDateMillis: Long? = null
)

enum class RevisionDayState {
    Completed,
    Active,
    Overdue,
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

    val overdueDay: Int?
        get() = dayStates.firstOrNull { it.state == RevisionDayState.Overdue }?.day

    val actionableDay: Int?
        get() = overdueDay ?: activeDay

    val requiresAttention: Boolean
        get() = actionableDay != null

    val isCompleted: Boolean
        get() = dayStates.all { it.state == RevisionDayState.Completed }
}

private data class HabitHistory(
    val missedDaysCount: Int = 0,
    val latestMissedDateMillis: Long? = null
)

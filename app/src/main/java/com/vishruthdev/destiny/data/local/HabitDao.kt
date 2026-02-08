package com.vishruthdev.destiny.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Result of habits joined with today's completion. */
data class HabitWithTodayCompletionRow(
    val id: String,
    val name: String,
    val completedToday: Int
)

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY createdAtMillis ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query(
        """
        SELECT h.id AS id, h.name AS name,
        CASE WHEN c.dateMillis IS NOT NULL THEN 1 ELSE 0 END AS completedToday
        FROM habits h
        LEFT JOIN habit_completions c ON h.id = c.habitId AND c.dateMillis = :todayStartMillis
        ORDER BY h.createdAtMillis ASC
        """
    )
    fun getHabitsWithTodayCompletion(todayStartMillis: Long): Flow<List<HabitWithTodayCompletionRow>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateMillis = :dateMillis")
    suspend fun deleteCompletion(habitId: String, dateMillis: Long)

    @Query("SELECT dateMillis FROM habit_completions WHERE habitId = :habitId ORDER BY dateMillis DESC")
    fun getCompletionDatesForHabit(habitId: String): Flow<List<Long>>

    @Query("SELECT dateMillis FROM habit_completions WHERE habitId = :habitId ORDER BY dateMillis DESC")
    suspend fun getCompletionDatesForHabitOnce(habitId: String): List<Long>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND dateMillis >= :fromMillis AND dateMillis < :toMillis")
    suspend fun getCompletionCountInRange(habitId: String, fromMillis: Long, toMillis: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND dateMillis = :dateMillis)")
    suspend fun isCompletedOn(habitId: String, dateMillis: Long): Boolean

    /** Emits when habit_completions table changes (for refreshing stats). */
    @Query("SELECT COUNT(*) FROM habit_completions")
    fun getCompletionCountFlow(): Flow<Int>

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteCompletionsForHabit(habitId: String)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: String)
}

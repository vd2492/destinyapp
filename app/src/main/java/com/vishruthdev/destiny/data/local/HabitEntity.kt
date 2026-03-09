package com.vishruthdev.destiny.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtMillis: Long,
    val startDateMillis: Long,
    val startHour: Int,
    val startMinute: Int
)

@Entity(tableName = "habit_completions", primaryKeys = ["habitId", "dateMillis"])
data class HabitCompletionEntity(
    val habitId: String,
    val dateMillis: Long
)

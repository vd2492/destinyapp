package com.vishruthdev.destiny

import android.app.Application
import com.vishruthdev.destiny.data.HabitRepository
import com.vishruthdev.destiny.data.local.AppDatabase
import com.vishruthdev.destiny.data.local.DatabaseProvider
import com.vishruthdev.destiny.data.local.HabitDao
import com.vishruthdev.destiny.data.local.HabitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DestinyApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val habitRepository: HabitRepository by lazy {
        val db = DatabaseProvider.get(this)
        HabitRepository(db.habitDao()).also { repo ->
            applicationScope.launch(Dispatchers.IO) {
                seedDefaultHabitsIfNeeded(db.habitDao())
            }
        }
    }

    private suspend fun seedDefaultHabitsIfNeeded(habitDao: HabitDao) {
        val existing = habitDao.getAllHabits().first()
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            listOf(
                HabitEntity("workout", "Workout", now),
                HabitEntity("read", "Read 20 mins", now),
                HabitEntity("revise", "Revise 30 mins", now)
            ).forEach { habitDao.insertHabit(it) }
        }
    }
}

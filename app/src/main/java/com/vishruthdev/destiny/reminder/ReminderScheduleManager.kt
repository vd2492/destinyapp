package com.vishruthdev.destiny.reminder

import android.util.Log
import com.vishruthdev.destiny.data.HabitRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReminderScheduleManager(
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Reminder scheduling failed", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var observeJob: Job? = null

    fun start() {
        if (observeJob != null) return
        observeJob = scope.launch {
            combine(
                habitRepository.getHabitsWithStats(),
                habitRepository.getRevisionTopicsWithProgress()
            ) { habits, revisions -> habits to revisions }
                .collect { (habits, revisions) ->
                    reminderScheduler.scheduleAll(habits, revisions)
                }
        }
    }

    private companion object {
        const val TAG = "ReminderScheduleManager"
    }
}

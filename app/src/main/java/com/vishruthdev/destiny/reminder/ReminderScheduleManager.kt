package com.vishruthdev.destiny.reminder

import android.util.Log
import com.vishruthdev.destiny.data.HabitRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class ReminderScheduleManager(
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Reminder scheduling failed", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var observeJob: Job? = null

    // scheduleAll is cancel-then-reschedule against a shared tracked-code set, so the
    // observer and refresh() must never run it concurrently.
    private val scheduleMutex = Mutex()

    fun start() {
        if (observeJob != null) return
        observeJob = scope.launch {
            combine(
                habitRepository.getHabitsWithStats(),
                habitRepository.getRevisionTopicsWithProgress()
            ) { habits, revisions -> habits to revisions }
                .collect { (habits, revisions) ->
                    scheduleMutex.withLock {
                        reminderScheduler.scheduleAll(habits, revisions)
                    }
                }
        }
    }

    /**
     * Re-runs scheduling once against the current data. Used after the user grants
     * exact-alarm access, since alarms queued before the grant are still inexact and
     * the observing flow only re-emits when the underlying data changes.
     */
    fun refresh() {
        scope.launch {
            val loaded = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
                val habits = habitRepository.getHabitsWithStats().first()
                val revisions = habitRepository.getRevisionTopicsWithProgress().first()
                habits to revisions
            }
            if (loaded == null) {
                Log.w(TAG, "Timed out loading data for reminder refresh")
                return@launch
            }
            scheduleMutex.withLock {
                reminderScheduler.scheduleAll(loaded.first, loaded.second)
            }
        }
    }

    private companion object {
        const val TAG = "ReminderScheduleManager"
        const val LOAD_TIMEOUT_MS = 15_000L
    }
}

package com.vishruthdev.destiny.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishruthdev.destiny.data.HabitCompletionState
import com.vishruthdev.destiny.data.HabitRepository
import com.vishruthdev.destiny.data.RevisionTopicWithProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitUiState(
    val id: String,
    val label: String,
    val state: HabitCompletionState,
    val startHour: Int,
    val startMinute: Int,
    val missedDaysCount: Int,
    val latestMissedDateMillis: Long?
)

data class HomeUiState(
    val habits: List<HabitUiState>,
    val dueCount: Int,
    val totalHabitsCount: Int,
    val progressPercent: Int,
    val dueRevisions: List<RevisionTopicWithProgress> = emptyList(),
    val hasRevisionTopics: Boolean = false,
    val showAllCompletedState: Boolean = false,
    val undoCountdownSeconds: Int = -1
)

class HomeViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        HomeUiState(
            habits = emptyList(),
            dueCount = 0,
            totalHabitsCount = 0,
            progressPercent = 0
        )
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var celebrationJob: Job? = null
    private var countdownJob: Job? = null
    private var isFirstEmission = true

    init {
        viewModelScope.launch {
            repository.getTodayHabitsWithCompletion().collect { withCompletion ->
                val habits = withCompletion.map { h ->
                    HabitUiState(
                        id = h.id,
                        label = h.name,
                        state = h.state,
                        startHour = h.startHour,
                        startMinute = h.startMinute,
                        missedDaysCount = h.missedDaysCount,
                        latestMissedDateMillis = h.latestMissedDateMillis
                    )
                }
                val total = habits.size
                val progressPercent = if (total == 0) 0 else (habits.count { it.state == HabitCompletionState.Completed }.toFloat() / total * 100).toInt()
                val allCompleted = total > 0 && progressPercent == 100

                celebrationJob?.cancel()
                if (allCompleted) {
                    if (isFirstEmission) {
                        // Already completed on app launch — show immediately, no undo
                        _state.update { it.copy(showAllCompletedState = true, undoCountdownSeconds = -1) }
                    } else {
                        // Just completed during this session — delay then show with undo
                        celebrationJob = viewModelScope.launch {
                            delay(3000L)
                            _state.update { it.copy(showAllCompletedState = true, undoCountdownSeconds = 10) }
                            startUndoCountdown()
                        }
                    }
                } else {
                    countdownJob?.cancel()
                    _state.update { it.copy(showAllCompletedState = false, undoCountdownSeconds = -1) }
                }
                isFirstEmission = false

                _state.update {
                    it.copy(
                        habits = habits,
                        totalHabitsCount = total,
                        progressPercent = progressPercent
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getRevisionTopicsWithProgress().collect { topics ->
                val dueRevisions = topics
                    .filter { it.requiresAttention }
                    .sortedWith(
                        compareByDescending<RevisionTopicWithProgress> { it.inProgressDay != null }
                            .thenByDescending { it.activeDay != null }
                            .thenBy { it.actionableDay ?: Int.MAX_VALUE }
                    )
                _state.update {
                    it.copy(
                        dueCount = dueRevisions.size,
                        dueRevisions = dueRevisions,
                        hasRevisionTopics = topics.isNotEmpty()
                    )
                }
            }
        }
    }

    private fun startUndoCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in 9 downTo 0) {
                delay(1000L)
                _state.update { it.copy(undoCountdownSeconds = remaining) }
            }
            _state.update { it.copy(undoCountdownSeconds = -1) }
        }
    }

    fun toggleHabit(id: String) {
        viewModelScope.launch {
            val current = _state.value.habits.find { it.id == id } ?: return@launch
            val nextState = when (current.state) {
                HabitCompletionState.NotStarted -> HabitCompletionState.InProgress
                HabitCompletionState.InProgress -> HabitCompletionState.Completed
                HabitCompletionState.Completed -> HabitCompletionState.NotStarted
            }
            repository.setHabitStateToday(id, nextState)
        }
    }

    fun undoAllHabitsToday() {
        celebrationJob?.cancel()
        countdownJob?.cancel()
        viewModelScope.launch {
            _state.value.habits.forEach { habit ->
                repository.setHabitStateToday(habit.id, HabitCompletionState.NotStarted)
            }
            _state.update { it.copy(showAllCompletedState = false, undoCountdownSeconds = -1) }
        }
    }

    fun startRevision(topicId: String) {
        viewModelScope.launch {
            repository.startRevision(topicId)
        }
    }

    fun completeRevision(topicId: String) {
        viewModelScope.launch {
            repository.completeActiveRevision(topicId)
        }
    }
}

class HomeViewModelFactory(
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

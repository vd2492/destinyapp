package com.vishruthdev.destiny.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishruthdev.destiny.data.HabitRepository
import com.vishruthdev.destiny.data.HabitWithCompletion
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
    val completed: Boolean
)

data class HomeUiState(
    val habits: List<HabitUiState>,
    val dueCount: Int,
    val totalHabitsCount: Int,
    val progressPercent: Int,
    val showAllCompletedState: Boolean = false,
    val undoCountdownSeconds: Int = -1
)

class HomeViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(emptyList(), 0, 0, 0))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var celebrationJob: Job? = null
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getTodayHabitsWithCompletion().collect { withCompletion ->
                val habits = withCompletion.map { h ->
                    HabitUiState(id = h.id, label = h.name, completed = h.completedToday)
                }
                val dueCount = habits.count { !it.completed }
                val total = habits.size
                val progressPercent = if (total == 0) 0 else (habits.count { it.completed }.toFloat() / total * 100).toInt()
                val allCompleted = total > 0 && progressPercent == 100

                celebrationJob?.cancel()
                if (allCompleted) {
                    celebrationJob = viewModelScope.launch {
                        delay(3000L)
                        _state.update { it.copy(showAllCompletedState = true, undoCountdownSeconds = 10) }
                        startUndoCountdown()
                    }
                } else {
                    countdownJob?.cancel()
                    _state.update { it.copy(showAllCompletedState = false, undoCountdownSeconds = -1) }
                }

                _state.update {
                    it.copy(
                        habits = habits,
                        dueCount = dueCount,
                        totalHabitsCount = total,
                        progressPercent = progressPercent
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
            _state.update { it.copy(showAllCompletedState = false, undoCountdownSeconds = -1) }
        }
    }

    fun toggleHabit(id: String) {
        viewModelScope.launch {
            val current = _state.value.habits.find { it.id == id } ?: return@launch
            repository.setCompletedToday(id, !current.completed)
        }
    }

    fun undoAllHabitsToday() {
        celebrationJob?.cancel()
        countdownJob?.cancel()
        viewModelScope.launch {
            _state.value.habits.forEach { habit ->
                repository.setCompletedToday(habit.id, false)
            }
            _state.update { it.copy(showAllCompletedState = false, undoCountdownSeconds = -1) }
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

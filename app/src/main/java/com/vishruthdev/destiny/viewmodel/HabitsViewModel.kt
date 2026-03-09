package com.vishruthdev.destiny.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishruthdev.destiny.data.HabitRepository
import com.vishruthdev.destiny.data.HabitWithStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class HabitStartOption {
    Today,
    Tomorrow,
    Custom
}

data class HabitsUiState(
    val habits: List<HabitWithStats>,
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val newHabitName: String = "",
    val startOption: HabitStartOption = HabitStartOption.Today,
    val customStartDateMillis: Long = 0L,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val deleteMode: Boolean = false
)

class HabitsViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HabitsUiState(habits = emptyList()))
    val state: StateFlow<HabitsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getHabitsWithStats().collect { habits ->
                _state.update { it.copy(habits = habits) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun showAddDialog() {
        val now = Calendar.getInstance()
        _state.update {
            it.copy(
                showAddDialog = true,
                newHabitName = "",
                startOption = HabitStartOption.Today,
                customStartDateMillis = repository.todayStartMillis(),
                startHour = now.get(Calendar.HOUR_OF_DAY),
                startMinute = now.get(Calendar.MINUTE)
            )
        }
    }

    fun dismissAddDialog() {
        _state.update { it.copy(showAddDialog = false, newHabitName = "") }
    }

    fun updateNewHabitName(name: String) {
        _state.update { it.copy(newHabitName = name) }
    }

    fun updateStartOption(option: HabitStartOption) {
        _state.update { it.copy(startOption = option) }
    }

    fun updateCustomStartDate(dateStartMillis: Long) {
        _state.update { it.copy(customStartDateMillis = dateStartMillis) }
    }

    fun updateStartTime(hour: Int, minute: Int) {
        _state.update {
            it.copy(
                startHour = hour.coerceIn(0, 23),
                startMinute = minute.coerceIn(0, 59)
            )
        }
    }

    fun addHabit() {
        val name = _state.value.newHabitName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val todayStart = repository.todayStartMillis()
            val startDateMillis = when (_state.value.startOption) {
                HabitStartOption.Today -> todayStart
                HabitStartOption.Tomorrow -> todayStart + (24 * 60 * 60 * 1000L)
                HabitStartOption.Custom -> _state.value.customStartDateMillis
            }

            repository.addHabit(
                name = name,
                startDateMillis = startDateMillis,
                startHour = _state.value.startHour,
                startMinute = _state.value.startMinute
            )
            _state.update { it.copy(showAddDialog = false, newHabitName = "") }
        }
    }

    fun toggleDeleteMode() {
        _state.update { it.copy(deleteMode = !it.deleteMode) }
    }

    fun exitDeleteMode() {
        _state.update { it.copy(deleteMode = false) }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }
}

private fun MutableStateFlow<HabitsUiState>.update(block: (HabitsUiState) -> HabitsUiState) {
    value = block(value)
}

class HabitsViewModelFactory(
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitsViewModel::class.java)) {
            return HabitsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

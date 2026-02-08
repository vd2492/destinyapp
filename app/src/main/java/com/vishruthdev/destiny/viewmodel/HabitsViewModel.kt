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

data class HabitsUiState(
    val habits: List<HabitWithStats>,
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val newHabitName: String = "",
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
        _state.update { it.copy(showAddDialog = true, newHabitName = "") }
    }

    fun dismissAddDialog() {
        _state.update { it.copy(showAddDialog = false, newHabitName = "") }
    }

    fun updateNewHabitName(name: String) {
        _state.update { it.copy(newHabitName = name) }
    }

    fun addHabit() {
        val name = _state.value.newHabitName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addHabit(name)
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

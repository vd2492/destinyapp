package com.vishruthdev.destiny.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishruthdev.destiny.data.HabitRepository
import com.vishruthdev.destiny.data.RevisionTopicWithProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class RevisionStartOption {
    Today,
    Tomorrow,
    Custom
}

data class RevisionsUiState(
    val topics: List<RevisionTopicWithProgress>,
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val newTopicName: String = "",
    val startOption: RevisionStartOption = RevisionStartOption.Today,
    val customStartDateMillis: Long = 0L,
    val revisionHour: Int = 9,
    val revisionMinute: Int = 0,
    val deleteMode: Boolean = false
)

class RevisionsViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RevisionsUiState(topics = emptyList()))
    val state: StateFlow<RevisionsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRevisionTopicsWithProgress().collect { topics ->
                _state.update {
                    it.copy(
                        topics = topics.sortedWith(
                            compareByDescending<RevisionTopicWithProgress> { topic -> topic.overdueDay != null }
                                .thenByDescending { topic -> topic.activeDay != null }
                        )
                    )
                }
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
                newTopicName = "",
                startOption = RevisionStartOption.Today,
                customStartDateMillis = repository.todayStartMillis(),
                revisionHour = now.get(Calendar.HOUR_OF_DAY),
                revisionMinute = now.get(Calendar.MINUTE)
            )
        }
    }

    fun dismissAddDialog() {
        _state.update { it.copy(showAddDialog = false, newTopicName = "") }
    }

    fun updateNewTopicName(name: String) {
        _state.update { it.copy(newTopicName = name) }
    }

    fun updateStartOption(option: RevisionStartOption) {
        _state.update { it.copy(startOption = option) }
    }

    fun updateCustomStartDate(dateStartMillis: Long) {
        _state.update { it.copy(customStartDateMillis = dateStartMillis) }
    }

    fun updateRevisionTime(hour: Int, minute: Int) {
        _state.update {
            it.copy(
                revisionHour = hour.coerceIn(0, 23),
                revisionMinute = minute.coerceIn(0, 59)
            )
        }
    }

    fun addTopic() {
        val name = _state.value.newTopicName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val todayStart = repository.todayStartMillis()
            val startDateMillis = when (_state.value.startOption) {
                RevisionStartOption.Today -> todayStart
                RevisionStartOption.Tomorrow -> todayStart + (24 * 60 * 60 * 1000L)
                RevisionStartOption.Custom -> _state.value.customStartDateMillis
            }
            repository.addRevisionTopic(
                name = name,
                startDateMillis = startDateMillis,
                revisionHour = _state.value.revisionHour,
                revisionMinute = _state.value.revisionMinute
            )
            _state.update { it.copy(showAddDialog = false, newTopicName = "") }
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

    fun resetRevisionFromToday(topicId: String) {
        viewModelScope.launch {
            repository.resetRevisionTopicFromToday(topicId)
        }
    }

    fun toggleDeleteMode() {
        _state.update { it.copy(deleteMode = !it.deleteMode) }
    }

    fun exitDeleteMode() {
        _state.update { it.copy(deleteMode = false) }
    }

    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            repository.deleteRevisionTopic(topicId)
        }
    }
}

private fun MutableStateFlow<RevisionsUiState>.update(block: (RevisionsUiState) -> RevisionsUiState) {
    value = block(value)
}

class RevisionsViewModelFactory(
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RevisionsViewModel::class.java)) {
            return RevisionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

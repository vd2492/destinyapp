package com.vishruthdev.destiny.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishruthdev.destiny.data.HabitRepository
import com.vishruthdev.destiny.data.RevisionTopicWithProgress
import kotlinx.coroutines.flow.catch
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
    val deleteMode: Boolean = false,
    val flippedTopicId: String? = null,
    val completionDialog: RevisionCompletionDialogState? = null
)

data class RevisionCompletionDialogState(
    val topicId: String,
    val topicName: String
)

class RevisionsViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RevisionsUiState(topics = emptyList()))
    val state: StateFlow<RevisionsUiState> = _state.asStateFlow()
    private val handledCompletionDialogTopicIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repository.getRevisionTopicsWithProgress()
                .catch { throwable ->
                    Log.w(TAG, "Failed to load revision topics", throwable)
                    _state.update { it.copy(topics = emptyList(), completionDialog = null) }
                }
                .collect { topics ->
                    handledCompletionDialogTopicIds.retainAll(
                        topics.filter { it.isCompleted }.map { it.id }.toSet()
                    )
                    val currentDialog = _state.value.completionDialog
                        ?.takeIf { dialog -> topics.any { it.id == dialog.topicId && it.isCompleted } }
                    val nextAutoDialog = if (currentDialog == null) {
                        topics.firstOrNull { topic ->
                            topic.isCompleted &&
                                !topic.completionDialogDismissed &&
                                topic.id !in handledCompletionDialogTopicIds
                        }?.let { topic ->
                            RevisionCompletionDialogState(
                                topicId = topic.id,
                                topicName = topic.name
                            )
                        }
                    } else {
                        null
                    }
                    _state.update {
                        it.copy(
                            topics = topics.sortedWith(
                                compareByDescending<RevisionTopicWithProgress> { topic -> topic.requiresAttention }
                                    .thenByDescending { topic -> topic.inProgressDay != null }
                                    .thenByDescending { topic -> topic.activeDay != null }
                                    .thenBy { topic -> topic.actionableDay ?: Int.MAX_VALUE }
                            ),
                            completionDialog = currentDialog ?: nextAutoDialog
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

    fun toggleDeleteMode() {
        _state.update { it.copy(deleteMode = !it.deleteMode) }
    }

    fun exitDeleteMode() {
        _state.update { it.copy(deleteMode = false) }
    }

    fun deleteTopic(topicId: String) {
        handledCompletionDialogTopicIds.add(topicId)
        viewModelScope.launch {
            repository.deleteRevisionTopic(topicId)
        }
        _state.update {
            it.copy(
                completionDialog = it.completionDialog?.takeUnless { dialog -> dialog.topicId == topicId }
            )
        }
    }

    fun toggleFlip(topicId: String) {
        _state.update {
            it.copy(flippedTopicId = if (it.flippedTopicId == topicId) null else topicId)
        }
    }

    fun toggleRevisionAlarm(topicId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRevisionAlarm(topicId, enabled)
        }
    }

    fun showCompletionDialog(topicId: String) {
        val topic = _state.value.topics.firstOrNull { it.id == topicId && it.isCompleted } ?: return
        _state.update {
            it.copy(
                completionDialog = RevisionCompletionDialogState(
                    topicId = topic.id,
                    topicName = topic.name
                )
            )
        }
    }

    fun dismissCompletionDialog() {
        val topicId = _state.value.completionDialog?.topicId ?: return
        handledCompletionDialogTopicIds.add(topicId)
        _state.update { it.copy(completionDialog = null) }
        viewModelScope.launch {
            repository.acknowledgeRevisionCompletionDialog(topicId)
        }
    }

    fun restartCompletedTopic() {
        val topicId = _state.value.completionDialog?.topicId ?: return
        handledCompletionDialogTopicIds.add(topicId)
        _state.update { it.copy(completionDialog = null) }
        viewModelScope.launch {
            repository.restartRevisionTopic(topicId)
        }
    }

    fun deleteCompletedTopic() {
        val topicId = _state.value.completionDialog?.topicId ?: return
        handledCompletionDialogTopicIds.add(topicId)
        _state.update { it.copy(completionDialog = null) }
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

private const val TAG = "RevisionsViewModel"

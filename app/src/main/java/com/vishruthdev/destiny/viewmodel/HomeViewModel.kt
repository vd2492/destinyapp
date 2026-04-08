package com.vishruthdev.destiny.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishruthdev.destiny.data.HabitCompletionState
import com.vishruthdev.destiny.data.HabitRepository
import com.vishruthdev.destiny.data.RevisionTopicWithProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
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
    val latestMissedDateMillis: Long?,
    val hasThirtyDayMilestone: Boolean,
    val thirtyDayDialogDismissed: Boolean
)

data class HomeHabitMilestoneDialogState(
    val habitId: String,
    val habitName: String
)

data class HomeRevisionCompletionDialogState(
    val topicId: String,
    val topicName: String
)

data class HomeUiState(
    val habits: List<HabitUiState>,
    val dueHabitsCount: Int,
    val dueRevisionsCount: Int,
    val progressPercent: Int,
    val dueRevisions: List<RevisionTopicWithProgress> = emptyList(),
    val hasRevisionTopics: Boolean = false,
    val showAllCompletedState: Boolean = false,
    val undoCountdownSeconds: Int = -1,
    val habitMilestoneDialog: HomeHabitMilestoneDialogState? = null,
    val revisionCompletionDialog: HomeRevisionCompletionDialogState? = null
)

class HomeViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        HomeUiState(
            habits = emptyList(),
            dueHabitsCount = 0,
            dueRevisionsCount = 0,
            progressPercent = 0
        )
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var celebrationJob: Job? = null
    private var countdownJob: Job? = null
    private var isFirstEmission = true
    private val handledHabitMilestoneDialogIds = mutableSetOf<String>()
    private val handledRevisionCompletionDialogIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repository.getTodayHabitsWithCompletion()
                .catch { throwable ->
                    Log.w(TAG, "Failed to load today's habits", throwable)
                    countdownJob?.cancel()
                    celebrationJob?.cancel()
                    _state.update {
                        it.copy(
                            habits = emptyList(),
                            dueHabitsCount = 0,
                            progressPercent = 0,
                            showAllCompletedState = false,
                            undoCountdownSeconds = -1,
                            habitMilestoneDialog = null
                        )
                    }
                }
                .collect { withCompletion ->
                handledHabitMilestoneDialogIds.retainAll(
                    withCompletion.filter { it.hasThirtyDayMilestone }.map { it.id }.toSet()
                )
                val habits = withCompletion.map { h ->
                    HabitUiState(
                        id = h.id,
                        label = h.name,
                        state = h.state,
                        startHour = h.startHour,
                        startMinute = h.startMinute,
                        missedDaysCount = h.missedDaysCount,
                        latestMissedDateMillis = h.latestMissedDateMillis,
                        hasThirtyDayMilestone = h.hasThirtyDayMilestone,
                        thirtyDayDialogDismissed = h.thirtyDayDialogDismissed
                    )
                }
                val total = habits.size
                val completedHabitsCount = habits.count { it.state == HabitCompletionState.Completed }
                val dueHabitsCount = habits.count { it.state != HabitCompletionState.Completed }
                val progressPercent = if (total == 0) 0 else (completedHabitsCount.toFloat() / total * 100).toInt()
                val allCompleted = total > 0 && progressPercent == 100
                val currentDialog = _state.value.habitMilestoneDialog
                    ?.takeIf { dialog -> habits.any { it.id == dialog.habitId && it.hasThirtyDayMilestone } }
                val nextAutoDialog = if (currentDialog == null) {
                    habits.firstOrNull { habit ->
                        habit.hasThirtyDayMilestone &&
                            !habit.thirtyDayDialogDismissed &&
                            habit.id !in handledHabitMilestoneDialogIds
                    }?.let { habit ->
                        HomeHabitMilestoneDialogState(
                            habitId = habit.id,
                            habitName = habit.label
                        )
                    }
                } else {
                    null
                }

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
                        dueHabitsCount = dueHabitsCount,
                        progressPercent = progressPercent,
                        habitMilestoneDialog = currentDialog ?: nextAutoDialog
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getRevisionTopicsWithProgress()
                .catch { throwable ->
                    Log.w(TAG, "Failed to load revision topics", throwable)
                    _state.update {
                        it.copy(
                            dueRevisionsCount = 0,
                            dueRevisions = emptyList(),
                            hasRevisionTopics = false,
                            revisionCompletionDialog = null
                        )
                    }
                }
                .collect { topics ->
                handledRevisionCompletionDialogIds.retainAll(
                    topics.filter { it.isCompleted }.map { it.id }.toSet()
                )
                val dueRevisions = topics
                    .filter { it.requiresAttention }
                    .sortedWith(
                        compareByDescending<RevisionTopicWithProgress> { it.inProgressDay != null }
                            .thenByDescending { it.activeDay != null }
                            .thenBy { it.actionableDay ?: Int.MAX_VALUE }
                    )
                val currentDialog = _state.value.revisionCompletionDialog
                    ?.takeIf { dialog -> topics.any { it.id == dialog.topicId && it.isCompleted } }
                val nextAutoDialog = if (currentDialog == null) {
                    topics.firstOrNull { topic ->
                        topic.isCompleted &&
                            !topic.completionDialogDismissed &&
                            topic.id !in handledRevisionCompletionDialogIds
                    }?.let { topic ->
                        HomeRevisionCompletionDialogState(
                            topicId = topic.id,
                            topicName = topic.name
                        )
                    }
                } else {
                    null
                }
                _state.update {
                    it.copy(
                        dueRevisionsCount = dueRevisions.size,
                        dueRevisions = dueRevisions,
                        hasRevisionTopics = topics.isNotEmpty(),
                        revisionCompletionDialog = currentDialog ?: nextAutoDialog
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

    fun dismissHabitMilestoneDialog() {
        val habitId = _state.value.habitMilestoneDialog?.habitId ?: return
        handledHabitMilestoneDialogIds.add(habitId)
        _state.update { it.copy(habitMilestoneDialog = null) }
        viewModelScope.launch {
            repository.acknowledgeHabitThirtyDayDialog(habitId)
        }
    }

    fun continueHabitStreak() {
        dismissHabitMilestoneDialog()
    }

    fun restartHabitFromDialog() {
        val habitId = _state.value.habitMilestoneDialog?.habitId ?: return
        handledHabitMilestoneDialogIds.add(habitId)
        _state.update { it.copy(habitMilestoneDialog = null) }
        viewModelScope.launch {
            repository.restartHabit(habitId)
        }
    }

    fun deleteHabitFromDialog() {
        val habitId = _state.value.habitMilestoneDialog?.habitId ?: return
        handledHabitMilestoneDialogIds.add(habitId)
        _state.update { it.copy(habitMilestoneDialog = null) }
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun dismissRevisionCompletionDialog() {
        val topicId = _state.value.revisionCompletionDialog?.topicId ?: return
        handledRevisionCompletionDialogIds.add(topicId)
        _state.update { it.copy(revisionCompletionDialog = null) }
        viewModelScope.launch {
            repository.acknowledgeRevisionCompletionDialog(topicId)
        }
    }

    fun restartRevisionFromDialog() {
        val topicId = _state.value.revisionCompletionDialog?.topicId ?: return
        handledRevisionCompletionDialogIds.add(topicId)
        _state.update { it.copy(revisionCompletionDialog = null) }
        viewModelScope.launch {
            repository.restartRevisionTopic(topicId)
        }
    }

    fun deleteRevisionFromDialog() {
        val topicId = _state.value.revisionCompletionDialog?.topicId ?: return
        handledRevisionCompletionDialogIds.add(topicId)
        _state.update { it.copy(revisionCompletionDialog = null) }
        viewModelScope.launch {
            repository.deleteRevisionTopic(topicId)
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

private const val TAG = "HomeViewModel"

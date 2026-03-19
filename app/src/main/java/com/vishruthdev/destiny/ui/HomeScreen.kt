package com.vishruthdev.destiny.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishruthdev.destiny.data.HabitCompletionState
import com.vishruthdev.destiny.data.RevisionDayState
import com.vishruthdev.destiny.data.RevisionTopicWithProgress
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue
import com.vishruthdev.destiny.ui.theme.DestinyCompletedGreen
import com.vishruthdev.destiny.ui.theme.DestinyInProgressOrange
import com.vishruthdev.destiny.ui.theme.DestinyLockedGrey
import com.vishruthdev.destiny.ui.theme.DestinyMissedRed
import com.vishruthdev.destiny.viewmodel.HabitUiState
import com.vishruthdev.destiny.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    darkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    onNavigateToRevisions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var topicToReset by remember { mutableStateOf<RevisionTopicWithProgress?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GreetingSection()
            Switch(
                checked = !darkTheme,
                onCheckedChange = { onThemeToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = DestinyAccentBlue.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        StatsCardsRow(
            dueCount = state.dueCount,
            totalHabitsCount = state.totalHabitsCount,
            progressPercent = state.progressPercent,
            showAllCompletedState = state.showAllCompletedState
        )
        Spacer(modifier = Modifier.height(28.dp))

        if (!state.showAllCompletedState) {
            TodaysHabitsSection(
                habits = state.habits,
                onHabitToggle = viewModel::toggleHabit,
                onViewAllClick = onNavigateToHabits
            )
            Spacer(modifier = Modifier.height(28.dp))
        } else {
            AllCompletedAlert(
                countdownSeconds = state.undoCountdownSeconds,
                onUndoAll = viewModel::undoAllHabitsToday
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        DueRevisionsSection(
            dueRevisions = state.dueRevisions,
            hasRevisionTopics = state.hasRevisionTopics,
            onViewAllClick = onNavigateToRevisions,
            onAddTopicClick = onNavigateToRevisions,
            onStartRevision = viewModel::startRevision,
            onCompleteRevision = viewModel::completeRevision,
            onResetRevision = { topicToReset = it }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    topicToReset?.let { topic ->
        ResetRevisionDialog(
            topicName = topic.name,
            onDismiss = { topicToReset = null },
            onConfirm = {
                viewModel.resetRevisionFromToday(topic.id)
                topicToReset = null
            }
        )
    }
}

@Composable
private fun GreetingSection() {
    val calendar = java.util.Calendar.getInstance()
    val greeting = when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val dateFormat = java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault())
    val formattedDate = dateFormat.format(calendar.time)

    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsCardsRow(
    dueCount: Int,
    totalHabitsCount: Int,
    progressPercent: Int,
    showAllCompletedState: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Due",
            value = "$dueCount",
            sublabel = "Revisions",
            valueColor = DestinyAccentBlue
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Today",
            value = "$totalHabitsCount",
            sublabel = "Habits",
            valueColor = MaterialTheme.colorScheme.onSurface
        )
        ProgressStatCard(
            modifier = Modifier.weight(1f),
            percent = progressPercent,
            showGreenTick = showAllCompletedState
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sublabel: String,
    valueColor: Color
) {
    Surface(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = valueColor
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressStatCard(
    modifier: Modifier = Modifier,
    percent: Int,
    showGreenTick: Boolean = false
) {
    val isFullGreen = percent == 100 && !showGreenTick
    val progressColor = when {
        showGreenTick -> DestinyCompletedGreen
        isFullGreen -> DestinyCompletedGreen
        else -> DestinyAccentBlue
    }
    Surface(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(56.dp)
            ) {
                val strokeWidth = 4.dp.toPx()
                val sweepAngle = (percent / 100f) * 360f
                drawArc(
                    color = DestinyLockedGrey,
                    startAngle = 90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = progressColor,
                    startAngle = 90f,
                    sweepAngle = -sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            if (showGreenTick) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = DestinyCompletedGreen
                )
            } else {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = progressColor
                )
            }
        }
    }
}

@Composable
private fun AllCompletedAlert(
    countdownSeconds: Int,
    onUndoAll: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All habits for today are completed!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (countdownSeconds >= 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${countdownSeconds}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Undo all",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = DestinyAccentBlue,
                        modifier = Modifier.clickable(onClick = onUndoAll)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaysHabitsSection(
    habits: List<HabitUiState>,
    onHabitToggle: (String) -> Unit,
    onViewAllClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Habits",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (habits.isNotEmpty()) {
                Text(
                    text = "View All >",
                    style = MaterialTheme.typography.labelLarge,
                    color = DestinyAccentBlue,
                    modifier = Modifier.clickable(onClick = onViewAllClick)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (habits.isEmpty()) {
            OutlinedButton(
                onClick = onViewAllClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = DestinyAccentBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Habit",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = DestinyAccentBlue
                )
            }
        } else {
            habits.forEachIndexed { index, habit ->
                HabitRow(
                    label = habit.label,
                    state = habit.state,
                    timeLabel = formatTime(habit.startHour, habit.startMinute),
                    missedLabel = if (habit.state == HabitCompletionState.Completed) null
                        else formatMissedHabitLabel(
                            missedDaysCount = habit.missedDaysCount,
                            latestMissedDateMillis = habit.latestMissedDateMillis
                        ),
                    onClick = { onHabitToggle(habit.id) }
                )
                if (index < habits.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun HabitRow(
    label: String,
    state: HabitCompletionState,
    timeLabel: String,
    missedLabel: String?,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    when (state) {
                        HabitCompletionState.Completed -> Modifier.background(DestinyCompletedGreen)
                        HabitCompletionState.InProgress -> Modifier.background(DestinyInProgressOrange)
                        HabitCompletionState.NotStarted -> Modifier.border(2.dp, DestinyLockedGrey, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                HabitCompletionState.Completed -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                HabitCompletionState.InProgress -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                HabitCompletionState.NotStarted -> Unit
            }
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (state == HabitCompletionState.InProgress) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "In progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = DestinyInProgressOrange
                )
            }
            missedLabel?.let { status ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = DestinyMissedRed
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DueRevisionsSection(
    dueRevisions: List<RevisionTopicWithProgress>,
    hasRevisionTopics: Boolean,
    onViewAllClick: () -> Unit,
    onAddTopicClick: () -> Unit,
    onStartRevision: (String) -> Unit,
    onCompleteRevision: (String) -> Unit,
    onResetRevision: (RevisionTopicWithProgress) -> Unit
) {
    Column {
        DueRevisionsHeader(
            showViewAll = hasRevisionTopics,
            onViewAllClick = onViewAllClick
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            !hasRevisionTopics -> {
                OutlinedButton(
                    onClick = onAddTopicClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = DestinyAccentBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add revision topic",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = DestinyAccentBlue
                    )
                }
            }
            dueRevisions.isEmpty() -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "No revisions are due today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                val visibleRevisions = dueRevisions.take(3)
                visibleRevisions.forEachIndexed { index, topic ->
                    RevisionCard(
                        topic = topic,
                        onStartRevision = { onStartRevision(topic.id) },
                        onMarkComplete = { onCompleteRevision(topic.id) },
                        onResetFromToday = { onResetRevision(topic) }
                    )
                    if (index < visibleRevisions.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DueRevisionsHeader(
    showViewAll: Boolean,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Due Revisions",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (showViewAll) {
            Text(
                text = "View All >",
                style = MaterialTheme.typography.labelLarge,
                color = DestinyAccentBlue,
                modifier = Modifier.clickable(onClick = onViewAllClick)
            )
        }
    }
}

@Composable
private fun RevisionCard(
    topic: RevisionTopicWithProgress,
    onStartRevision: () -> Unit,
    onMarkComplete: () -> Unit,
    onResetFromToday: () -> Unit
) {
    val isOverdue = topic.overdueDay != null
    val isInProgress = topic.inProgressDay != null
    val statusLabel = when {
        topic.inProgressDay != null -> "In Progress Day ${topic.inProgressDay}"
        topic.overdueDay != null -> "Overdue Day ${topic.overdueDay}"
        topic.activeDay != null -> "Due Day ${topic.activeDay}"
        else -> "Planned"
    }
    val statusColor = when {
        topic.inProgressDay != null -> DestinyInProgressOrange
        topic.overdueDay != null -> DestinyMissedRed
        topic.activeDay != null -> DestinyAccentBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusContainerColor = when {
        topic.inProgressDay != null -> DestinyInProgressOrange.copy(alpha = 0.16f)
        topic.overdueDay != null -> DestinyMissedRed.copy(alpha = 0.16f)
        topic.activeDay != null -> DestinyAccentBlue.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val actionDay = topic.actionableDay
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOverdue) DestinyMissedRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusContainerColor
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                topic.dayStates.forEachIndexed { index, day ->
                    if (index > 0) {
                        RevisionConnectorLine(
                            fromState = topic.dayStates[index - 1].state,
                            toState = day.state
                        )
                    }
                    RevisionDayNode(day = day.day, state = day.state)
                }
            }
            actionDay?.let { dueDay ->
                Spacer(modifier = Modifier.height(14.dp))
                if (isOverdue && !isInProgress) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onStartRevision,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = DestinyMissedRed
                            )
                        ) {
                            Text(
                                text = "Catch up Day $dueDay",
                                color = DestinyMissedRed
                            )
                        }
                        OutlinedButton(
                            onClick = onResetFromToday,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = DestinyMissedRed
                            )
                        ) {
                            Text(
                                text = "Reset from today",
                                color = DestinyMissedRed
                            )
                        }
                    }
                } else if (isInProgress) {
                    OutlinedButton(
                        onClick = onMarkComplete,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = DestinyInProgressOrange
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = DestinyInProgressOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mark Day $dueDay done",
                            color = DestinyInProgressOrange
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onStartRevision,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(
                            text = "Start Day $dueDay",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RevisionConnectorLine(
    fromState: RevisionDayState,
    toState: RevisionDayState
) {
    val lineColor = when {
        fromState == RevisionDayState.Completed && toState == RevisionDayState.Completed -> DestinyCompletedGreen
        fromState == RevisionDayState.InProgress || toState == RevisionDayState.InProgress -> DestinyInProgressOrange
        fromState == RevisionDayState.Overdue || toState == RevisionDayState.Overdue -> DestinyMissedRed
        fromState == RevisionDayState.Active || toState == RevisionDayState.Active -> DestinyAccentBlue
        else -> DestinyLockedGrey
    }
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(2.dp)
            .background(lineColor)
    )
}

@Composable
private fun RevisionDayNode(
    day: Int,
    state: RevisionDayState
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val (backgroundColor, contentColor) = when (state) {
            RevisionDayState.Completed -> DestinyCompletedGreen to Color.White
            RevisionDayState.InProgress -> DestinyInProgressOrange to Color.White
            RevisionDayState.Active -> Color.Transparent to DestinyAccentBlue
            RevisionDayState.Overdue -> Color.Transparent to DestinyMissedRed
            RevisionDayState.Locked -> Color.Transparent to DestinyLockedGrey
        }
        val borderColor = when (state) {
            RevisionDayState.Completed -> DestinyCompletedGreen
            RevisionDayState.InProgress -> DestinyInProgressOrange
            RevisionDayState.Active -> DestinyAccentBlue
            RevisionDayState.Overdue -> DestinyMissedRed
            RevisionDayState.Locked -> DestinyLockedGrey
        }
        val isFilled = state == RevisionDayState.Completed || state == RevisionDayState.InProgress

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (isFilled) Modifier.background(backgroundColor)
                    else Modifier.border(2.dp, borderColor, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                RevisionDayState.Completed -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                RevisionDayState.InProgress -> Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                RevisionDayState.Overdue -> Text(
                    text = "$day",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                RevisionDayState.Locked -> Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                RevisionDayState.Active -> Unit
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Day $day",
            style = MaterialTheme.typography.labelSmall,
            color = when (state) {
                RevisionDayState.InProgress -> DestinyInProgressOrange
                RevisionDayState.Active -> DestinyAccentBlue
                RevisionDayState.Overdue -> DestinyMissedRed
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private fun formatMissedHabitLabel(
    missedDaysCount: Int,
    latestMissedDateMillis: Long?
): String? {
    if (missedDaysCount <= 0 || latestMissedDateMillis == null) return null

    val yesterdayStart = dayStartMillis(System.currentTimeMillis()) - 24 * 60 * 60 * 1000L
    return when {
        latestMissedDateMillis == yesterdayStart && missedDaysCount == 1 -> "Missed yesterday"
        missedDaysCount == 1 -> "Last missed ${formatShortDate(latestMissedDateMillis)}"
        else -> "$missedDaysCount missed days"
    }
}

private fun formatShortDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
    return formatter.format(millis)
}

private fun dayStartMillis(timeMillis: Long): Long {
    val calendar = java.util.Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private fun formatTime(hour: Int, minute: Int): String {
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
    }
    val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return formatter.format(calendar.time)
}

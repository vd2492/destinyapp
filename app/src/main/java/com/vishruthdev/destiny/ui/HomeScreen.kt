package com.vishruthdev.destiny.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue
import com.vishruthdev.destiny.viewmodel.HabitUiState
import com.vishruthdev.destiny.viewmodel.HomeViewModel
import com.vishruthdev.destiny.ui.theme.DestinyCompletedGreen
import com.vishruthdev.destiny.ui.theme.DestinyLockedGrey

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    darkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

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
            DueRevisionsHeader()
            Spacer(modifier = Modifier.height(12.dp))
            RevisionCard(
                title = "Binary Search Patterns",
                category = "DSA",
                dayStates = listOf(
                    DayState.Completed,
                    DayState.Completed,
                    DayState.Active,
                    DayState.Locked
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            RevisionCard(
                title = "Top 100 words set 1",
                category = "Vocabulary",
                dayStates = listOf(
                    DayState.Completed,
                    DayState.Active,
                    DayState.Locked,
                    DayState.Locked
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            RevisionCard(
                title = "Load Balancing Strategies",
                category = "System Design",
                dayStates = listOf(
                    DayState.Active,
                    DayState.Locked,
                    DayState.Locked,
                    DayState.Locked
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
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
    valueColor: androidx.compose.ui.graphics.Color
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (countdownSeconds >= 0) {
                    Text(
                        text = "${countdownSeconds}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    checked = habit.completed,
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
    checked: Boolean,
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
                    if (checked) Modifier.background(DestinyCompletedGreen)
                    else Modifier.border(2.dp, DestinyLockedGrey, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun DueRevisionsHeader() {
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
        Text(
            text = "View All >",
            style = MaterialTheme.typography.labelLarge,
            color = DestinyAccentBlue
        )
    }
}

private enum class DayState { Completed, Active, Locked }

@Composable
private fun RevisionCard(
    title: String,
    category: String,
    dayStates: List<DayState>
) {
    val days = listOf("Day 1", "Day 2", "Day 4", "Day 7")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DestinyAccentBlue.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Due",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = DestinyAccentBlue
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayStates.forEachIndexed { index, state ->
                    if (index > 0) {
                        RevisionConnectorLine(
                            fromCompleted = dayStates[index - 1] == DayState.Completed,
                            toCompleted = state == DayState.Completed
                        )
                    }
                    RevisionDayNode(
                        label = days[index],
                        state = state
                    )
                }
            }
        }
    }
}

@Composable
private fun RevisionConnectorLine(
    fromCompleted: Boolean,
    toCompleted: Boolean
) {
    val lineColor = if (fromCompleted && toCompleted) DestinyCompletedGreen else DestinyLockedGrey
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(2.dp)
            .background(lineColor)
    )
}

@Composable
private fun RevisionDayNode(
    label: String,
    state: DayState
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val (backgroundColor, contentColor) = when (state) {
            DayState.Completed -> DestinyCompletedGreen to androidx.compose.ui.graphics.Color.White
            DayState.Active -> androidx.compose.ui.graphics.Color.Transparent to DestinyAccentBlue
            DayState.Locked -> androidx.compose.ui.graphics.Color.Transparent to DestinyLockedGrey
        }
        val borderColor = when (state) {
            DayState.Completed -> DestinyCompletedGreen
            DayState.Active -> DestinyAccentBlue
            DayState.Locked -> DestinyLockedGrey
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (state == DayState.Completed) Modifier.background(backgroundColor)
                    else Modifier.border(2.dp, borderColor, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                DayState.Completed -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                DayState.Locked -> Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                DayState.Active -> { /* empty circle */ }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = when (state) {
                DayState.Active -> DestinyAccentBlue
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}


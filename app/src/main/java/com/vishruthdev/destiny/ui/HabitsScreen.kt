package com.vishruthdev.destiny.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishruthdev.destiny.data.HabitWithStats
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue
import com.vishruthdev.destiny.ui.theme.DestinyCompletedGreen
import com.vishruthdev.destiny.ui.theme.DestinyMissedRed
import com.vishruthdev.destiny.viewmodel.HabitStartOption
import com.vishruthdev.destiny.viewmodel.HabitsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Habits",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Build lasting change",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        SearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::updateSearchQuery
        )
        Spacer(modifier = Modifier.height(20.dp))

        val filteredHabits = state.habits.filter { habit ->
            state.searchQuery.isBlank() || habit.name.contains(state.searchQuery, ignoreCase = true)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredHabits) { habit ->
                HabitStatsCard(
                    habitId = habit.id,
                    name = habit.name,
                    streakDays = habit.streakDays,
                    completionRatePercent = habit.completionRatePercent,
                    startDateMillis = habit.startDateMillis,
                    startHour = habit.startHour,
                    startMinute = habit.startMinute,
                    missedDaysCount = habit.missedDaysCount,
                    latestMissedDateMillis = habit.latestMissedDateMillis,
                    showDeleteButton = state.deleteMode,
                    onDelete = { viewModel.deleteHabit(habit.id) },
                    onLongPress = { viewModel.toggleDeleteMode() }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = viewModel::showAddDialog,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Add new Habit")
            }
            if (state.deleteMode) {
                OutlinedButton(
                    onClick = viewModel::exitDeleteMode,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            } else {
                OutlinedButton(
                    onClick = viewModel::toggleDeleteMode,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Remove habit")
                }
            }
        }
    }

    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAddDialog,
            title = { Text("New habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BasicTextField(
                        value = state.newHabitName,
                        onValueChange = viewModel::updateNewHabitName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        singleLine = true,
                        cursorBrush = SolidColor(DestinyAccentBlue),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            Box {
                                if (state.newHabitName.isEmpty()) {
                                    Text(
                                        "Habit name",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        }
                    )

                    Text(
                        text = "Start",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.startOption == HabitStartOption.Today,
                            onClick = { viewModel.updateStartOption(HabitStartOption.Today) },
                            label = { Text("Today") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = state.startOption == HabitStartOption.Tomorrow,
                            onClick = { viewModel.updateStartOption(HabitStartOption.Tomorrow) },
                            label = { Text("Tomorrow") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = state.startOption == HabitStartOption.Custom,
                            onClick = { viewModel.updateStartOption(HabitStartOption.Custom) },
                            label = { Text("Pick Date") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (state.startOption == HabitStartOption.Custom) {
                        OutlinedButton(
                            onClick = {
                                val selectedDate = if (state.customStartDateMillis == 0L) {
                                    dayStartMillis(System.currentTimeMillis())
                                } else {
                                    state.customStartDateMillis
                                }
                                val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        viewModel.updateCustomStartDate(dayStartMillis(year, month, dayOfMonth))
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).apply {
                                    datePicker.minDate = dayStartMillis(System.currentTimeMillis())
                                }.show()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Start date: ${formatDate(state.customStartDateMillis)}")
                        }
                    }

                    Text(
                        text = "Habit time",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    viewModel.updateStartTime(hour, minute)
                                },
                                state.startHour,
                                state.startMinute,
                                false
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Time: ${formatTime(state.startHour, state.startMinute)}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::addHabit) {
                    Text("Add", color = DestinyAccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAddDialog) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                cursorBrush = SolidColor(DestinyAccentBlue),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search habits...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun HabitStatsCard(
    habitId: String,
    name: String,
    streakDays: Int,
    completionRatePercent: Int,
    startDateMillis: Long,
    startHour: Int,
    startMinute: Int,
    missedDaysCount: Int,
    latestMissedDateMillis: Long?,
    showDeleteButton: Boolean,
    onDelete: () -> Unit,
    onLongPress: () -> Unit
) {
    val isMissed = missedDaysCount > 0
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isMissed) DestinyMissedRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = if (isMissed) BorderStroke(1.dp, DestinyMissedRed.copy(alpha = 0.35f)) else null,
        modifier = Modifier.pointerInput(habitId) {
            detectTapGestures(onLongPress = { onLongPress() })
        }
    ) {
        Box {
            if (showDeleteButton) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DestinyAccentBlue.copy(alpha = 0.2f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete habit",
                        modifier = Modifier.size(18.dp),
                        tint = DestinyAccentBlue
                    )
                }
            }
            Column(
                modifier = Modifier.padding(
                    start = if (showDeleteButton) 44.dp else 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Starts ${formatDate(startDateMillis)} at ${formatTime(startHour, startMinute)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        formatMissedHabitLabel(
                            missedDaysCount = missedDaysCount,
                            latestMissedDateMillis = latestMissedDateMillis
                        )?.let { status ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DestinyMissedRed.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = status,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = DestinyMissedRed
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = DestinyAccentBlue
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "$streakDays days",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Completion Rate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$completionRatePercent%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                ProgressBar(progressPercent = completionRatePercent)
            }
        }
    }
}

@Composable
private fun ProgressBar(
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    val fraction = (progressPercent / 100f).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                DestinyCompletedGreen.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .background(
                    DestinyCompletedGreen,
                    RoundedCornerShape(4.dp)
                )
        )
    }
}

private fun dayStartMillis(timeMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun dayStartMillis(year: Int, month: Int, dayOfMonth: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatDate(millis: Long): String {
    val safeMillis = if (millis == 0L) dayStartMillis(System.currentTimeMillis()) else millis
    val formatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return formatter.format(safeMillis)
}

private fun formatTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
    }
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(calendar.time)
}

private fun formatMissedHabitLabel(
    missedDaysCount: Int,
    latestMissedDateMillis: Long?
): String? {
    if (missedDaysCount <= 0 || latestMissedDateMillis == null) return null

    val yesterdayStart = dayStartMillis(System.currentTimeMillis()) - 24 * 60 * 60 * 1000L
    return when {
        latestMissedDateMillis == yesterdayStart && missedDaysCount == 1 -> "Missed yesterday"
        missedDaysCount == 1 -> "Last missed ${formatDate(latestMissedDateMillis)}"
        else -> "$missedDaysCount missed days"
    }
}

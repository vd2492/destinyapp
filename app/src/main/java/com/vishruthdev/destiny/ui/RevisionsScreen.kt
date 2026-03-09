package com.vishruthdev.destiny.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishruthdev.destiny.data.RevisionDayState
import com.vishruthdev.destiny.data.RevisionTopicWithProgress
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue
import com.vishruthdev.destiny.ui.theme.DestinyCompletedGreen
import com.vishruthdev.destiny.ui.theme.DestinyLockedGrey
import com.vishruthdev.destiny.viewmodel.RevisionStartOption
import com.vishruthdev.destiny.viewmodel.RevisionsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun RevisionsScreen(
    viewModel: RevisionsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val filteredTopics = state.topics.filter { topic ->
        state.searchQuery.isBlank() || topic.name.contains(state.searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Revisions",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "1-2-4-7 spaced revision",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        RevisionSearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::updateSearchQuery
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (state.topics.isEmpty()) {
            OutlinedButton(
                onClick = viewModel::showAddDialog,
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
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredTopics, key = { it.id }) { topic ->
                    RevisionTopicCard(
                        topic = topic,
                        showDeleteButton = state.deleteMode,
                        onDelete = { viewModel.deleteTopic(topic.id) },
                        onLongPress = { viewModel.toggleDeleteMode() },
                        onMarkComplete = { viewModel.completeRevision(topic.id) }
                    )
                }
            }

            if (filteredTopics.isEmpty()) {
                Text(
                    text = "No topics match your search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
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
                Text("Add topic")
            }
            if (state.topics.isNotEmpty()) {
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
                        Text("Remove topic")
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAddDialog,
            title = { Text("New revision topic") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BasicTextField(
                        value = state.newTopicName,
                        onValueChange = viewModel::updateNewTopicName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        singleLine = true,
                        cursorBrush = SolidColor(DestinyAccentBlue),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            Box {
                                if (state.newTopicName.isEmpty()) {
                                    Text(
                                        "Topic name",
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
                            selected = state.startOption == RevisionStartOption.Today,
                            onClick = { viewModel.updateStartOption(RevisionStartOption.Today) },
                            label = { Text("Today") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = state.startOption == RevisionStartOption.Tomorrow,
                            onClick = { viewModel.updateStartOption(RevisionStartOption.Tomorrow) },
                            label = { Text("Tomorrow") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = state.startOption == RevisionStartOption.Custom,
                            onClick = { viewModel.updateStartOption(RevisionStartOption.Custom) },
                            label = { Text("Pick Date") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (state.startOption == RevisionStartOption.Custom) {
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
                        text = "Revision time",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    viewModel.updateRevisionTime(hour, minute)
                                },
                                state.revisionHour,
                                state.revisionMinute,
                                false
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Time: ${formatTime(state.revisionHour, state.revisionMinute)}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::addTopic) {
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
private fun RevisionSearchBar(
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
                                "Search topics...",
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
private fun RevisionTopicCard(
    topic: RevisionTopicWithProgress,
    showDeleteButton: Boolean,
    onDelete: () -> Unit,
    onLongPress: () -> Unit,
    onMarkComplete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.pointerInput(topic.id) {
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
                        contentDescription = "Delete topic",
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
                            text = topic.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Starts ${formatDate(topic.startDateMillis)} at ${formatTime(topic.revisionHour, topic.revisionMinute)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val activeDay = topic.activeDay
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            activeDay != null -> DestinyAccentBlue.copy(alpha = 0.2f)
                            topic.dayStates.all { it.state == RevisionDayState.Completed } -> DestinyCompletedGreen.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when {
                                activeDay != null -> "Due"
                                topic.dayStates.all { it.state == RevisionDayState.Completed } -> "Done"
                                else -> "Planned"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                activeDay != null -> DestinyAccentBlue
                                topic.dayStates.all { it.state == RevisionDayState.Completed } -> DestinyCompletedGreen
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
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
                                fromCompleted = topic.dayStates[index - 1].state == RevisionDayState.Completed,
                                toCompleted = day.state == RevisionDayState.Completed
                            )
                        }
                        RevisionDayNode(day = day.day, state = day.state)
                    }
                }
                topic.activeDay?.let { dueDay ->
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onMarkComplete,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark Day $dueDay done")
                    }
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
    day: Int,
    state: RevisionDayState
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val (backgroundColor, contentColor) = when (state) {
            RevisionDayState.Completed -> DestinyCompletedGreen to Color.White
            RevisionDayState.Active -> Color.Transparent to DestinyAccentBlue
            RevisionDayState.Locked -> Color.Transparent to DestinyLockedGrey
        }
        val borderColor = when (state) {
            RevisionDayState.Completed -> DestinyCompletedGreen
            RevisionDayState.Active -> DestinyAccentBlue
            RevisionDayState.Locked -> DestinyLockedGrey
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (state == RevisionDayState.Completed) Modifier.background(backgroundColor)
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
            color = if (state == RevisionDayState.Active) DestinyAccentBlue else MaterialTheme.colorScheme.onSurface
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

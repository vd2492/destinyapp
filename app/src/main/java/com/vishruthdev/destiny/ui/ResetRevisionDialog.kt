package com.vishruthdev.destiny.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.vishruthdev.destiny.ui.theme.DestinyMissedRed

@Composable
fun ResetRevisionDialog(
    topicName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset revision schedule?") },
        text = {
            Text(
                text = "This will clear previous revision progress for \"$topicName\" and restart its 1-2-4-7 plan from today."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Reset from today",
                    color = DestinyMissedRed
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

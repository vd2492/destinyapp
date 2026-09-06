package com.vishruthdev.destiny.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue

/**
 * One-time contextual ask for the "Alarms & reminders" access, shown the first time the
 * user actually has a reminder scheduled — not at login, where there is nothing yet to
 * be late and the notification prompt is already on screen.
 *
 * Shows at most once ever; [ExactAlarmPermissionCard] in Settings is the permanent
 * recovery path for anyone who declines.
 */
@Composable
fun ExactAlarmPromptDialog() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val context = LocalContext.current
    val promptStore = remember(context) { ExactAlarmPromptStore(context) }

    var alreadyPrompted by remember { mutableStateOf(promptStore.hasPrompted()) }
    val hasScheduledReminders = rememberHasScheduledReminders()

    // ExactAlarmAccessEffect keeps this in step with the system, including grants made
    // from the Settings card or from outside the app.
    val canScheduleExact = ExactAlarmAccessState.isGranted

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // The resume that follows refreshes access state and reschedules if granted.
    }

    // Never stack on top of the system notification prompt, which fires on the same
    // composition for a returning user whose habits sync straight after sign-in.
    if (NotificationPermissionFlow.isRequestInFlight) return
    if (alreadyPrompted || canScheduleExact || !hasScheduledReminders) return

    val dismiss = {
        promptStore.markPrompted()
        alreadyPrompted = true
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Text(
                text = "Reminders on time?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Android needs one permission to fire your habit and revision alerts at the exact time you set. Without it they still arrive, but can be a few minutes late.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "You can change this any time from Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    dismiss()
                    launchExactAlarmSettings(context, launcher)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Allow",
                    color = DestinyAccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = dismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Not now",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

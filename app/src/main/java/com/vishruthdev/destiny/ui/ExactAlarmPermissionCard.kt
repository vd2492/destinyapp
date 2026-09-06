package com.vishruthdev.destiny.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue

/**
 * Prompts for the "Alarms & reminders" special access, which Android 14+ denies by
 * default. Renders nothing at all when the access is already available (including on
 * Android 11 and below, where it is not a separate permission), so the Settings layout
 * is unchanged for anyone who has it.
 */
@Composable
fun ExactAlarmPermissionCard(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val context = LocalContext.current
    val canScheduleExact = ExactAlarmAccessState.isGranted

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // The resume that follows refreshes access state and reschedules if granted.
    }

    if (canScheduleExact) return

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Exact reminders",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Allow alarms and reminders so habit and revision alerts arrive exactly on time. Without it they still arrive, but can be a few minutes late.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { launchExactAlarmSettings(context, launcher) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Allow",
                        color = DestinyAccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

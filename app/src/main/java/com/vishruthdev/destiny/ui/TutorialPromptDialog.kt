package com.vishruthdev.destiny.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue

/**
 * One-time invitation to the tutorial, shown the first time an account signs in on this
 * device. Any way of closing it counts as seen, so it never nags; the tutorial itself stays
 * available under Settings.
 */
@Composable
fun TutorialPromptDialog(
    userId: String?,
    onOpenTutorial: () -> Unit
) {
    if (userId == null) return

    val context = LocalContext.current
    val store = remember(context) { TutorialPromptStore(context) }
    var seen by remember(userId) { mutableStateOf(store.hasSeen(userId)) }

    // NotificationPermissionEffect is composed first, so by the time this effect has run the
    // system prompt is already flagged in flight. Waiting one beat keeps the two from
    // flashing over each other.
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ready = true }

    if (seen || !ready || NotificationPermissionFlow.isRequestInFlight) return

    val dismiss = {
        store.markSeen(userId)
        seen = true
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to Destiny?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = dismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss dialog"
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Take a quick tour of habits and revisions and how to start your first one. You can find it any time under Settings > Tutorial.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        dismiss()
                        onOpenTutorial()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DestinyAccentBlue)
                ) {
                    Text(
                        text = "Open tutorial",
                        color = DestinyAccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = dismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Maybe later")
                }
            }
        },
        confirmButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/** Remembers, per account, that the prompt has been shown on this device. */
internal class TutorialPromptStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSeen(userId: String): Boolean = prefs.getBoolean(key(userId), false)

    fun markSeen(userId: String) {
        prefs.edit().putBoolean(key(userId), true).apply()
    }

    private fun key(userId: String) = "$KEY_PREFIX$userId"

    private companion object {
        const val PREFS_NAME = "destiny_tutorial_prompt"
        const val KEY_PREFIX = "seen_"
    }
}

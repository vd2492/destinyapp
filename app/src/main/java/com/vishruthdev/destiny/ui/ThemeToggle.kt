package com.vishruthdev.destiny.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue
import com.vishruthdev.destiny.ui.theme.DestinyInProgressOrange

/**
 * Light/dark switch that reads as a theme control: the thumb carries a sun in light mode
 * and a moon in dark mode, so it is obvious what the switch changes.
 */
@Composable
fun ThemeToggle(
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = !darkTheme,
        onCheckedChange = { onThemeToggle() },
        modifier = modifier.semantics {
            contentDescription = if (darkTheme) "Switch to light theme" else "Switch to dark theme"
        },
        thumbContent = {
            Icon(
                imageVector = if (darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (darkTheme) DestinyAccentBlue else DestinyInProgressOrange
            )
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = DestinyAccentBlue.copy(alpha = 0.5f),
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
            uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            uncheckedBorderColor = Color.Transparent
        )
    )
}

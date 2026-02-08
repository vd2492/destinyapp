package com.vishruthdev.destiny.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DestinyAccentBlue,
    onPrimary = Color.White,
    secondary = DestinyCompletedGreen,
    tertiary = DestinyLockedGrey,
    background = DestinyBackground,
    surface = DestinyCardBackground,
    onBackground = DestinyPrimaryText,
    onSurface = DestinyPrimaryText,
    onSurfaceVariant = DestinySecondaryText,
    outline = DestinyLockedGrey
)

private val LightColorScheme = lightColorScheme(
    primary = DestinyAccentBlue,
    onPrimary = Color.White,
    secondary = DestinyCompletedGreen,
    tertiary = DestinyLockedGrey,
    background = DestinyLightBackground,
    surface = DestinyLightCardBackground,
    onBackground = DestinyLightPrimaryText,
    onSurface = DestinyLightPrimaryText,
    onSurfaceVariant = DestinyLightSecondaryText,
    outline = DestinyLockedGrey
)

@Composable
fun DestinyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (false = use Destiny dark theme)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
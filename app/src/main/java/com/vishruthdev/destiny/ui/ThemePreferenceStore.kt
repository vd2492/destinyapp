package com.vishruthdev.destiny.ui

import android.content.Context

/**
 * Remembers the light/dark choice on this device so it survives restarts. It is a device
 * preference rather than an account one, so it also applies on the login screen.
 */
class ThemePreferenceStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Dark is the default, matching the app's behaviour before this was saved. */
    fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, true)

    fun setDarkTheme(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, dark).apply()
    }

    private companion object {
        const val PREFS_NAME = "destiny_theme"
        const val KEY_DARK_THEME = "dark_theme"
    }
}

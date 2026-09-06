package com.vishruthdev.destiny.ui

import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vishruthdev.destiny.DestinyApplication
import com.vishruthdev.destiny.reminder.ReminderScheduler

private const val TAG = "ExactAlarmAccess"

/**
 * Shared helpers for the "Alarms & reminders" special access, which Android 14+ denies
 * by default. Used by both the one-time prompt and the Settings card.
 */

internal fun Context.canScheduleExactAlarmsCompat(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = getSystemService(AlarmManager::class.java) ?: return false
    return alarmManager.canScheduleExactAlarms()
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun launchExactAlarmSettings(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val packageUri = Uri.fromParts("package", context.packageName, null)
    try {
        launcher.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
    } catch (e: ActivityNotFoundException) {
        // Some OEM builds do not expose the dedicated screen; fall back to app details.
        try {
            launcher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
        } catch (fallbackFailure: ActivityNotFoundException) {
            // Locked-down build with neither screen: fail quietly rather than crash.
            Log.w(TAG, "No settings activity available for exact-alarm access", fallbackFailure)
        }
    }
}

/**
 * Single source of truth for whether exact-alarm access is held, so a grant made from
 * one surface (the Settings card, the prompt, or the OS Settings app) is immediately
 * visible to the others. Defaults to granted so nothing flashes before the first read.
 */
internal object ExactAlarmAccessState {
    var isGranted by mutableStateOf(true)
        private set

    fun refresh(context: Context) {
        isGranted = context.canScheduleExactAlarmsCompat()
    }
}

/**
 * Keeps [ExactAlarmAccessState] in step with the system on every resume, which covers
 * returning from the Settings deep link as well as a grant made outside the app. On the
 * denied -> granted transition it re-runs scheduling, since alarms queued while the
 * access was missing are still inexact.
 */
@Composable
internal fun ExactAlarmAccessEffect() {
    val context = LocalContext.current
    val app = context.applicationContext as? DestinyApplication
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val wasGranted = ExactAlarmAccessState.isGranted
                ExactAlarmAccessState.refresh(context)
                if (!wasGranted && ExactAlarmAccessState.isGranted) {
                    app?.refreshReminders()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * True once at least one reminder is actually on the books, which is the moment the
 * permission becomes worth asking about. Reads the set ReminderScheduler already
 * maintains, so this costs no extra Firestore listener.
 */
@Composable
internal fun rememberHasScheduledReminders(): Boolean {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(ReminderScheduler.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var hasReminders by remember { mutableStateOf(prefs.hasActiveCodes()) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
            if (key == null || key == ReminderScheduler.KEY_ACTIVE_CODES) {
                hasReminders = changed.hasActiveCodes()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return hasReminders
}

private fun SharedPreferences.hasActiveCodes(): Boolean =
    getStringSet(ReminderScheduler.KEY_ACTIVE_CODES, emptySet())?.isNotEmpty() == true

/** Remembers that the one-time prompt has been shown, so it never nags. */
internal class ExactAlarmPromptStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasPrompted(): Boolean = prefs.getBoolean(KEY_PROMPTED, false)

    fun markPrompted() {
        prefs.edit().putBoolean(KEY_PROMPTED, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "exact_alarm_prompt"
        const val KEY_PROMPTED = "has_prompted"
    }
}

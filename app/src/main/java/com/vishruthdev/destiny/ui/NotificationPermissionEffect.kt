package com.vishruthdev.destiny.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Whether the system notification prompt is currently on screen, so other first-run
 * asks can wait rather than stacking behind it.
 */
internal object NotificationPermissionFlow {
    var isRequestInFlight by mutableStateOf(false)
}

@Composable
fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var hasRequestedPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        NotificationPermissionFlow.isRequestInFlight = false
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission && !hasRequestedPermission) {
            hasRequestedPermission = true
            NotificationPermissionFlow.isRequestInFlight = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

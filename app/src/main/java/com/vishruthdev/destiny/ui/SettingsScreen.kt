package com.vishruthdev.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishruthdev.destiny.BuildConfig
import android.app.Activity
import androidx.credentials.CredentialManager
import com.vishruthdev.destiny.data.AuthRepository
import com.vishruthdev.destiny.data.SettingsRepository
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    authRepository: AuthRepository?,
    settingsRepository: SettingsRepository? = null,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by (authRepository?.currentUser ?: flowOf(null)).collectAsState(initial = null)
    val strictModeEnabled by (settingsRepository?.strictModeEnabled ?: flowOf(false))
        .collectAsState(initial = settingsRepository?.isStrictModeEnabled() ?: false)

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Account and app preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))

        if (currentUser != null && authRepository != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Logged in as",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentUser!!,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Logout",
                                color = DestinyAccentBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

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
                        text = "Strict Mode",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Miss a day and your habits and revisions restart from Day 1. Turn off to keep your progress when you skip a day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = strictModeEnabled,
                    onCheckedChange = { settingsRepository?.setStrictModeEnabled(it) }
                )
            }
        }

        ExactAlarmPermissionCard()

        if (currentUser != null && authRepository != null) {
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
                            text = "Delete account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Permanently removes your account along with every habit, revision topic and setting. This cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        deleteError?.let { message ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    OutlinedButton(
                        enabled = !isDeleting,
                        onClick = {
                            deleteError = null
                            deletePassword = ""
                            showDeleteDialog = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isDeleting) "Deleting..." else "Delete",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (showDeleteDialog && authRepository != null) {
            val signInMethod = authRepository.currentSignInMethod()
            val usesPassword = signInMethod == AuthRepository.SignInMethod.Password

            // Re-authentication happens before anything is deleted, so a failure here
            // leaves the account completely intact.
            val runDeletion: (suspend () -> Result<Unit>) -> Unit = { block ->
                isDeleting = true
                deleteError = null
                scope.launch {
                    block().fold(
                        // On success the auth state clears and the app returns to the
                        // login screen on its own.
                        onSuccess = { showDeleteDialog = false },
                        onFailure = {
                            // Stay on the dialog so the reason is actually visible.
                            deleteError = it.message ?: "Could not delete the account"
                        }
                    )
                    isDeleting = false
                }
            }

            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                title = {
                    Text(
                        text = "Delete account?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Your account and all of its data are erased permanently. Your habits, revision topics, streaks and settings cannot be recovered afterwards.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (usesPassword) {
                                "Enter your password to confirm it is you."
                            } else {
                                "You will be asked to confirm with Google before anything is deleted."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (usesPassword) {
                            OutlinedTextField(
                                value = deletePassword,
                                onValueChange = { deletePassword = it },
                                label = { Text("Password") },
                                singleLine = true,
                                enabled = !isDeleting,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        deleteError?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    OutlinedButton(
                        enabled = !isDeleting,
                        onClick = {
                            if (usesPassword) {
                                val password = deletePassword
                                runDeletion { authRepository.deleteAccountWithPassword(password) }
                            } else {
                                val currentActivity = activity
                                if (currentActivity == null) {
                                    deleteError = "Unable to confirm with Google"
                                } else {
                                    isDeleting = true
                                    deleteError = null
                                    scope.launch {
                                        runCatching {
                                            requestGoogleIdToken(
                                                credentialManager = credentialManager,
                                                activity = currentActivity,
                                                webClientId = authRepository.googleWebClientId
                                            )
                                        }.fold(
                                            onSuccess = { idToken ->
                                                authRepository
                                                    .deleteAccountWithGoogleIdToken(idToken)
                                                    .fold(
                                                        onSuccess = { showDeleteDialog = false },
                                                        onFailure = {
                                                            deleteError = it.message
                                                                ?: "Could not delete the account"
                                                        }
                                                    )
                                            },
                                            onFailure = {
                                                deleteError = mapGoogleSignInError(it)
                                            }
                                        )
                                        isDeleting = false
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (isDeleting) "Deleting..." else "Delete",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        enabled = !isDeleting,
                        onClick = { showDeleteDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
    }
}

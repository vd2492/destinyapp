package com.vishruthdev.destiny.ui

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    firebaseConfigured: Boolean,
    googleSignInConfigured: Boolean,
    googleWebClientId: String,
    authRegister: suspend (username: String, email: String, password: String) -> Result<Unit>,
    authLogin: suspend (email: String, password: String) -> Result<Unit>,
    authLoginWithGoogle: suspend (idToken: String) -> Result<Unit> = {
        Result.failure(UnsupportedOperationException("Google Sign-In not configured"))
    },
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(LoginMode.Login) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var createPasswordVisible by remember { mutableStateOf(false) }
    var createConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Destiny",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Build habits that last",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (!firebaseConfigured) {
            Text(
                text = "Firebase is not configured yet. Add the Firebase values in local.properties before using login.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = mode == LoginMode.Login,
                onClick = {
                    mode = LoginMode.Login
                    errorMessage = null
                    successMessage = null
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = DestinyAccentBlue,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Login")
            }
            SegmentedButton(
                selected = mode == LoginMode.CreateAccount,
                onClick = {
                    mode = LoginMode.CreateAccount
                    errorMessage = null
                    successMessage = null
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = DestinyAccentBlue,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create account")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        successMessage?.let { msg ->
            Text(
                text = msg,
                color = DestinyAccentBlue,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        when (mode) {
            LoginMode.Login -> {
                OutlinedTextField(
                    value = loginEmail,
                    onValueChange = { loginEmail = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                            Icon(
                                imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (loginPasswordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = textFieldColors()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        enabled = firebaseConfigured && !isSubmitting,
                        onClick = {
                            errorMessage = null
                            successMessage = null
                            isSubmitting = true
                            scope.launch {
                                val result = authLogin(loginEmail.trim(), loginPassword)
                                result.fold(
                                    onSuccess = { onLoginSuccess() },
                                    onFailure = { errorMessage = it.message ?: "Login failed" }
                                )
                                isSubmitting = false
                            }
                        }
                    ) {
                        Text(
                            text = if (isSubmitting) "Logging in..." else "Login",
                            color = DestinyAccentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            LoginMode.CreateAccount -> {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (createPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { createPasswordVisible = !createPasswordVisible }) {
                            Icon(
                                imageVector = if (createPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (createPasswordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = textFieldColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = if (createConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { createConfirmPasswordVisible = !createConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (createConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (createConfirmPasswordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = textFieldColors()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        enabled = firebaseConfigured && !isSubmitting,
                        onClick = {
                            errorMessage = null
                            successMessage = null
                            when {
                                username.isBlank() -> errorMessage = "Username is required"
                                email.isBlank() -> errorMessage = "Email is required"
                                password.isBlank() -> errorMessage = "Password is required"
                                password != confirmPassword -> errorMessage = "Passwords do not match"
                                else -> {
                                    isSubmitting = true
                                    scope.launch {
                                        val result = authRegister(username.trim(), email.trim(), password)
                                        result.fold(
                                            onSuccess = {
                                                successMessage = "Account created. You're logged in."
                                                onLoginSuccess()
                                            },
                                            onFailure = { errorMessage = it.message ?: "Registration failed" }
                                        )
                                        isSubmitting = false
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            text = if (isSubmitting) "Creating..." else "Create account",
                            color = DestinyAccentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )
            Text(
                text = "Or",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            enabled = googleSignInConfigured && !isSubmitting && activity != null,
            onClick = {
                errorMessage = null
                successMessage = null
                val currentActivity = activity

                if (!firebaseConfigured) {
                    errorMessage = "Firebase is not configured yet."
                    return@OutlinedButton
                }
                if (!googleSignInConfigured) {
                    errorMessage = "Google sign-in is not configured yet. Add firebase.webClientId to local.properties."
                    return@OutlinedButton
                }
                if (currentActivity == null) {
                    errorMessage = "Unable to start sign-in"
                    return@OutlinedButton
                }

                isSubmitting = true
                scope.launch {
                    val result = runCatching {
                        requestGoogleIdToken(
                            credentialManager = credentialManager,
                            activity = currentActivity,
                            webClientId = googleWebClientId
                        )
                    }

                    result.fold(
                        onSuccess = { idToken ->
                            authLoginWithGoogle(idToken).fold(
                                onSuccess = { onLoginSuccess() },
                                onFailure = { errorMessage = mapGoogleSignInError(it) }
                            )
                        },
                        onFailure = { throwable ->
                            Log.w("LoginScreen", "Google sign-in failed", throwable)
                            errorMessage = mapGoogleSignInError(throwable)
                        }
                    )
                    isSubmitting = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mail,
                contentDescription = null,
                tint = DestinyAccentBlue
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSubmitting) "Please wait..." else "Sign in with Google",
                color = DestinyAccentBlue,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (!googleSignInConfigured) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Google sign-in will unlock after you add firebase.webClientId to local.properties.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DestinyAccentBlue,
    focusedLabelColor = DestinyAccentBlue,
    cursorColor = DestinyAccentBlue,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

private suspend fun requestGoogleIdToken(
    credentialManager: CredentialManager,
    activity: Activity,
    webClientId: String
): String {
    return extractGoogleIdToken(
        credentialManager.getCredential(
            context = activity,
            request = googleCredentialRequest(webClientId = webClientId)
        ).credential
    )
}

private fun googleCredentialRequest(webClientId: String): GetCredentialRequest {
    val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
        .build()

    return GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
}

private fun mapGoogleSignInError(throwable: Throwable): String {
    if (throwable is NoCredentialException) {
        return "No Google account was available to sign in."
    }

    val message = throwable.message.orEmpty()
    val simpleName = throwable::class.java.simpleName

    if (
        simpleName.contains("Cancellation", ignoreCase = true) ||
        message.contains("cancel", ignoreCase = true)
    ) {
        return "Google sign-in was cancelled."
    }

    if (
        message.contains("developer console", ignoreCase = true) ||
        message.contains("10:", ignoreCase = true)
    ) {
        return "Google sign-in is not configured correctly yet. Check your SHA-1 and Web client ID."
    }

    return message.ifBlank { "Google sign-in failed" }
}

private fun extractGoogleIdToken(credential: Credential): String {
    if (
        credential is CustomCredential &&
        (
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
            )
    ) {
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    throw IllegalStateException("Unexpected credential returned by Google sign-in")
}

private enum class LoginMode {
    Login,
    CreateAccount
}

package com.vishruthdev.destiny

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vishruthdev.destiny.ui.DestinyApp
import com.vishruthdev.destiny.ui.LoginScreen
import com.vishruthdev.destiny.ui.ThemePreferenceStore
import com.vishruthdev.destiny.ui.theme.DestinyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = LocalContext.current.applicationContext as DestinyApplication
            val currentUser by app.authRepository.currentUser.collectAsState()
            val themeStore = remember(app) { ThemePreferenceStore(app) }
            var darkTheme by remember { mutableStateOf(themeStore.isDarkTheme()) }

            DestinyTheme(darkTheme = darkTheme) {
                if (currentUser == null) {
                    LoginScreen(
                        onLoginSuccess = { },
                        firebaseConfigured = app.authRepository.isConfigured,
                        googleSignInConfigured = app.authRepository.isGoogleSignInConfigured,
                        googleWebClientId = app.authRepository.googleWebClientId,
                        authRegister = { email, password ->
                            app.authRepository.register(email, password)
                        },
                        authLogin = { email, password ->
                            app.authRepository.login(email, password)
                        },
                        authResetPassword = { email ->
                            app.authRepository.sendPasswordReset(email)
                        },
                        authLoginWithGoogle = { idToken ->
                            app.authRepository.loginWithGoogleIdToken(idToken)
                        }
                    )
                } else {
                    DestinyApp(
                        darkTheme = darkTheme,
                        onThemeToggle = {
                            darkTheme = !darkTheme
                            themeStore.setDarkTheme(darkTheme)
                        },
                        authRepository = app.authRepository
                    )
                }
            }
        }
    }
}

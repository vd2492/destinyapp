package com.vishruthdev.destiny

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vishruthdev.destiny.ui.DestinyApp
import com.vishruthdev.destiny.ui.LoginScreen
import com.vishruthdev.destiny.ui.theme.DestinyTheme
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = LocalContext.current.applicationContext as DestinyApplication
            val currentUser by app.authRepository.currentUser.collectAsState(initial = null)
            var previousUser by remember { mutableStateOf(currentUser) }
            var darkTheme by remember { mutableStateOf(true) }

            LaunchedEffect(currentUser) {
                if (currentUser != null && currentUser != previousUser) {
                    app.habitRepository.clearAll()
                }
                previousUser = currentUser
            }
            DestinyTheme(darkTheme = darkTheme) {
                if (currentUser == null) {
                    val context = LocalContext.current
                    LoginScreen(
                        onLoginSuccess = { /* state will update from authRepository */ },
                        authRegister = { u, e, p -> app.authRepository.register(u, e, p) },
                        authLogin = { id, p -> app.authRepository.login(id, p) },
                        authLoginWithGoogle = { email, name -> app.authRepository.loginWithGoogle(email, name) }
                    )
                } else {
                    val context = LocalContext.current
                    val googleSignInClient = remember(context) {
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        ).requestEmail().requestProfile().build().let { gso ->
                            com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                        }
                    }
                    DestinyApp(
                        darkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme },
                        authRepository = app.authRepository,
                        onGoogleSignOut = { googleSignInClient.signOut() }
                    )
                }
            }
        }
    }
}
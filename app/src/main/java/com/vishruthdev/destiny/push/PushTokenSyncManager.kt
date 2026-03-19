package com.vishruthdev.destiny.push

import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PushTokenSyncManager(
    private val firebaseAuth: FirebaseAuth?,
    private val pushTokenRepository: PushTokenRepository
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Push token sync failed", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var hasStarted = false
    private var currentUid: String? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val newUid = auth.currentUser?.uid
        val previousUid = currentUid
        currentUid = newUid

        if (previousUid != null && previousUid != newUid) {
            scope.launch {
                pushTokenRepository.removeTokenForUser(previousUid)
            }
        }

        if (newUid != null) {
            scope.launch {
                pushTokenRepository.syncCurrentToken()
            }
        }
    }

    fun start() {
        val auth = firebaseAuth ?: return
        if (hasStarted) return

        hasStarted = true
        currentUid = auth.currentUser?.uid
        auth.addAuthStateListener(authStateListener)
        authStateListener.onAuthStateChanged(auth)
    }

    private companion object {
        const val TAG = "PushTokenSyncManager"
    }
}

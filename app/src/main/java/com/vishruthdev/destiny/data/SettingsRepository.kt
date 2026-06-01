package com.vishruthdev.destiny.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.vishruthdev.destiny.FirebaseRuntimeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Per-account app preferences, stored on the signed-in user's Firestore document
 * (`users/{uid}.strictModeEnabled`) so the setting syncs across devices and can be
 * honoured by the backend reminder function as well as the client.
 *
 * Strict Mode controls the "miss a day -> restart from Day 1" auto-reset behaviour
 * for habits and revision topics. When enabled, a missed day wipes progress and
 * restarts from today. When disabled (the default), missed days no longer reset
 * anything and progress is preserved.
 */
class SettingsRepository(
    private val firebaseConfig: FirebaseRuntimeConfig,
    private val firebaseAuth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) {

    private val _strictModeEnabled = MutableStateFlow(DEFAULT_STRICT_MODE)

    /** Observable strict-mode state for the Settings UI. */
    val strictModeEnabled: StateFlow<Boolean> = _strictModeEnabled.asStateFlow()

    private var activeUid: String? = null
    private var registration: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid == activeUid && registration != null) return@AuthStateListener

        registration?.remove()
        registration = null
        activeUid = uid

        if (uid == null) {
            // Signed out: fall back to the lenient default so nothing resets.
            _strictModeEnabled.value = DEFAULT_STRICT_MODE
            return@AuthStateListener
        }

        registration = settingsDocument(uid)?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Strict mode listener failed for user=$uid", error)
                return@addSnapshotListener
            }
            _strictModeEnabled.value = snapshot?.getBoolean(KEY_STRICT_MODE) ?: DEFAULT_STRICT_MODE
        }
    }

    init {
        if (firebaseConfig.isBaseConfigured && firebaseAuth != null && firestore != null) {
            firebaseAuth.addAuthStateListener(authStateListener)
        }
    }

    /** Synchronous read for non-UI callers (e.g. auto-reset decisions in [HabitRepository]). */
    fun isStrictModeEnabled(): Boolean = _strictModeEnabled.value

    fun setStrictModeEnabled(enabled: Boolean) {
        val previous = _strictModeEnabled.value
        // Optimistic update; the snapshot listener reflects the server's value once it lands.
        _strictModeEnabled.value = enabled

        val uid = firebaseAuth?.currentUser?.uid ?: return
        val document = settingsDocument(uid) ?: return
        document.set(mapOf(KEY_STRICT_MODE to enabled))
            .addOnFailureListener { throwable ->
                Log.w(TAG, "Failed to persist strict mode for user=$uid", throwable)
                _strictModeEnabled.value = previous
            }
    }

    private fun settingsDocument(uid: String) =
        firestore?.collection(USERS_COLLECTION)?.document(uid)
            ?.collection(SETTINGS_COLLECTION)?.document(SETTINGS_DOC)

    private companion object {
        const val TAG = "SettingsRepository"
        const val USERS_COLLECTION = "users"
        const val SETTINGS_COLLECTION = "settings"
        const val SETTINGS_DOC = "app"
        const val KEY_STRICT_MODE = "strictModeEnabled"

        // Default OFF: missed days do not wipe progress unless the user opts into Strict Mode.
        const val DEFAULT_STRICT_MODE = false
    }
}

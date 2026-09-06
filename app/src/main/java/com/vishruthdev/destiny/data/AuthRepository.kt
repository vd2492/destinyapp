package com.vishruthdev.destiny.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.vishruthdev.destiny.FirebaseRuntimeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class AuthRepository(
    context: Context,
    private val firebaseConfig: FirebaseRuntimeConfig,
    private val firebaseAuth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) {

    private val appContext = context.applicationContext
    private val credentialManager by lazy { CredentialManager.create(appContext) }

    private val _currentUser = MutableStateFlow(firebaseAuth?.currentUser.toDisplayLabel())
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    val isConfigured: Boolean
        get() = firebaseConfig.isBaseConfigured

    val isGoogleSignInConfigured: Boolean
        get() = firebaseConfig.isGoogleSignInConfigured

    val googleWebClientId: String
        get() = firebaseConfig.webClientId

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUser.value = auth.currentUser.toDisplayLabel()
    }

    init {
        firebaseAuth?.addAuthStateListener(authStateListener)
    }

    suspend fun register(username: String, email: String, password: String): Result<Unit> {
        val auth = firebaseAuth ?: return configurationFailure()
        val db = firestore ?: return configurationFailure()
        val displayName = username.trim()
        val trimmedEmail = email.trim()

        if (displayName.isBlank()) {
            return Result.failure(IllegalArgumentException("Username is required"))
        }
        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Email is required"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password is required"))
        }

        return runCatching {
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, password).awaitResult()
            val user = authResult.user ?: error("Registration failed")
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdate).awaitResult()
            saveUserProfile(
                firestore = db,
                uid = user.uid,
                email = user.email ?: trimmedEmail,
                displayName = displayName
            )
            _currentUser.value = displayName
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { throwable ->
                Result.failure(mapAuthException(throwable, "Registration failed"))
            }
        )
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        val auth = firebaseAuth ?: return configurationFailure()
        val db = firestore ?: return configurationFailure()
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password are required"))
        }

        return runCatching {
            val authResult = auth.signInWithEmailAndPassword(trimmedEmail, password).awaitResult()
            val user = authResult.user ?: error("Login failed")
            saveUserProfile(
                firestore = db,
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.toDisplayLabel().orEmpty()
            )
            _currentUser.value = user.toDisplayLabel()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { throwable ->
                Result.failure(mapAuthException(throwable, "Login failed"))
            }
        )
    }

    /**
     * Sends a Firebase password-reset email. Accounts created through Google Sign-In have
     * no password, and an address with no account must not be distinguishable from one
     * that has — both cases resolve successfully and the caller shows a neutral message.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val auth = firebaseAuth ?: return configurationFailure()
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Email is required"))
        }

        return runCatching {
            auth.sendPasswordResetEmail(trimmedEmail).awaitResult()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { throwable ->
                when (throwable) {
                    // Do not leak whether an account exists for this address.
                    is FirebaseAuthInvalidUserException -> Result.success(Unit)
                    is FirebaseAuthInvalidCredentialsException -> Result.failure(
                        IllegalArgumentException("Enter a valid email address")
                    )
                    else -> Result.failure(
                        IllegalArgumentException(
                            throwable.localizedMessage ?: "Could not send the reset email"
                        )
                    )
                }
            }
        )
    }

    suspend fun loginWithGoogleIdToken(idToken: String): Result<Unit> {
        val auth = firebaseAuth ?: return configurationFailure()
        val db = firestore ?: return configurationFailure()

        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Google ID token is required"))
        }

        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).awaitResult()
            val user = authResult.user ?: error("Google sign-in failed")
            saveUserProfile(
                firestore = db,
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.toDisplayLabel().orEmpty()
            )
            _currentUser.value = user.toDisplayLabel()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { throwable ->
                Result.failure(mapAuthException(throwable, "Google sign-in failed"))
            }
        )
    }

    /**
     * Permanently deletes the signed-in account: Firestore data first, then the auth
     * user. Order matters — once the auth user is gone the security rules deny every
     * write, so anything left behind would be unreachable and undeletable. Cloud
     * Functions are not deployed, so there is no server-side cleanup to fall back on.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val auth = firebaseAuth ?: return configurationFailure()
        val db = firestore ?: return configurationFailure()
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("No signed-in account to delete"))
        val uid = user.uid

        return runCatching {
            deleteUserData(db, uid)
            user.delete().awaitResult()
            _currentUser.value = null
            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { throwable ->
                when (throwable) {
                    is FirebaseAuthRecentLoginRequiredException -> {
                        // Firebase requires a fresh sign-in before deleting a user. The
                        // stored data is already gone, so signing out lets them come
                        // straight back and finish the job.
                        auth.signOut()
                        _currentUser.value = null
                        Result.failure(
                            IllegalStateException(
                                "For your security, sign in again and then delete your account."
                            )
                        )
                    }
                    else -> Result.failure(
                        IllegalArgumentException(
                            throwable.localizedMessage ?: "Could not delete the account"
                        )
                    )
                }
            }
        )
    }

    private suspend fun deleteUserData(firestore: FirebaseFirestore, uid: String) {
        val userDocument = firestore.collection(USERS_COLLECTION).document(uid)
        for (subcollection in USER_SUBCOLLECTIONS) {
            val snapshot = userDocument.collection(subcollection).get().awaitResult()
            for (document in snapshot.documents) {
                document.reference.delete().awaitResult()
            }
        }
        userDocument.delete().awaitResult()
    }

    suspend fun logout() {
        firebaseAuth?.signOut()
        _currentUser.value = null

        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private suspend fun saveUserProfile(
        firestore: FirebaseFirestore,
        uid: String,
        email: String,
        displayName: String
    ) {
        val now = Calendar.getInstance().timeInMillis
        val userDocument = firestore.collection(USERS_COLLECTION).document(uid)
        val existingSnapshot = userDocument.get().awaitResult()
        val createdAtMillis = existingSnapshot.getLong("createdAtMillis") ?: now

        userDocument.set(
            UserProfileDocument(
                uid = uid,
                email = email,
                displayName = displayName.ifBlank { email },
                updatedAtMillis = now,
                createdAtMillis = createdAtMillis
            ),
            SetOptions.merge()
        ).awaitResult()
    }

    private fun configurationFailure(): Result<Unit> {
        return Result.failure(
            IllegalStateException(
                "Firebase is not configured. Add Firebase keys to local.properties first."
            )
        )
    }

    private fun mapAuthException(throwable: Throwable, defaultMessage: String): Throwable {
        return when (throwable) {
            is FirebaseAuthUserCollisionException -> IllegalArgumentException("An account already exists with this email")
            is FirebaseAuthWeakPasswordException -> IllegalArgumentException(throwable.localizedMessage ?: "Password is too weak")
            is FirebaseAuthInvalidCredentialsException,
            is FirebaseAuthInvalidUserException -> IllegalArgumentException("Incorrect email or password")
            else -> IllegalArgumentException(throwable.localizedMessage ?: defaultMessage)
        }
    }

    private fun com.google.firebase.auth.FirebaseUser?.toDisplayLabel(): String? {
        val user = this ?: return null
        return user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.takeIf { it.isNotBlank() }
    }

    private data class UserProfileDocument(
        val uid: String = "",
        val email: String = "",
        val displayName: String = "",
        val createdAtMillis: Long = 0L,
        val updatedAtMillis: Long = 0L
    )

    private companion object {
        const val USERS_COLLECTION = "users"

        // Every subcollection written under users/{uid}; all are removed on deletion.
        val USER_SUBCOLLECTIONS = listOf(
            "habits",
            "revisionTopics",
            "notificationTokens",
            "settings"
        )
    }
}

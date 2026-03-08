package com.vishruthdev.destiny.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple local auth: register (username + optional email + password) and login
 * (identifier = email or username + password). No backend; stored in SharedPreferences.
 * Passwords are stored in plain text for demo only — use hashing in production.
 */
class AuthRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _currentUser = MutableStateFlow<String?>(prefs.getString(KEY_CURRENT_USER, null).takeIf { !it.isNullOrEmpty() })
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    fun register(username: String, email: String?, password: String): Result<Unit> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username and password are required"))
        }
        val users = loadUsers().toMutableList()
        if (users.any { it.username.equals(username, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Username already taken"))
        }
        if (!email.isNullOrBlank() && users.any { it.email?.equals(email, ignoreCase = true) == true }) {
            return Result.failure(IllegalArgumentException("Email already registered"))
        }
        users.add(StoredUser(username = username.trim(), email = email?.trim()?.takeIf { it.isNotBlank() }, password = password, isGoogle = false))
        saveUsers(users)
        setCurrentUser(username.trim())
        return Result.success(Unit)
    }

    fun login(identifier: String, password: String): Result<Unit> {
        if (identifier.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email/username and password are required"))
        }
        val user = loadUsers().find { user ->
            user.username.equals(identifier, ignoreCase = true) ||
                user.email?.equals(identifier, ignoreCase = true) == true
        }
        return when {
            user == null -> Result.failure(IllegalArgumentException("No account found with this email or username"))
            user.password != password -> Result.failure(IllegalArgumentException("Incorrect password"))
            else -> {
                setCurrentUser(user.username)
                Result.success(Unit)
            }
        }
    }

    /**
     * Sign in with Google: use email (and optional display name) as the account.
     * Creates a local account if this Google email is new; otherwise reuses existing.
     */
    fun loginWithGoogle(email: String, displayName: String?): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email is required"))
        }
        val users = loadUsers().toMutableList()
        val existing = users.find { it.email.equals(email, ignoreCase = true) || it.username.equals(email, ignoreCase = true) }
        val username = when {
            existing != null -> existing.username
            else -> {
                val name = displayName?.trim()?.takeIf { it.isNotBlank() } ?: email
                users.add(StoredUser(username = name, email = email.trim(), password = "", isGoogle = true))
                saveUsers(users)
                name
            }
        }
        setCurrentUser(username)
        return Result.success(Unit)
    }

    fun logout() {
        prefs.edit().remove(KEY_CURRENT_USER).apply()
        _currentUser.value = null
    }

    private fun setCurrentUser(username: String) {
        prefs.edit().putString(KEY_CURRENT_USER, username).apply()
        _currentUser.value = username
    }

    private fun loadUsers(): List<StoredUser> {
        val raw = prefs.getString(KEY_USERS, null) ?: return emptyList()
        return raw.split(DELIM).mapNotNull { part ->
            val tokens = part.split(SUB_DELIM)
            if (tokens.size >= 3) StoredUser(
                username = tokens[0],
                email = tokens[1].takeIf { it.isNotBlank() },
                password = tokens[2],
                isGoogle = tokens.getOrNull(3) == "1"
            ) else null
        }
    }

    private fun saveUsers(users: List<StoredUser>) {
        val value = users.joinToString(DELIM) { "${it.username}$SUB_DELIM${it.email ?: ""}$SUB_DELIM${it.password}$SUB_DELIM${if (it.isGoogle) "1" else "0"}" }
        prefs.edit().putString(KEY_USERS, value).apply()
    }

    private data class StoredUser(val username: String, val email: String?, val password: String, val isGoogle: Boolean = false)

    companion object {
        private const val PREFS_NAME = "destiny_auth"
        private const val KEY_CURRENT_USER = "current_user"
        private const val KEY_USERS = "users"
        private const val DELIM = ";"
        private const val SUB_DELIM = "|"
    }
}

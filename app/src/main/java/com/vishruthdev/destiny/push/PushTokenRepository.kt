package com.vishruthdev.destiny.push

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.vishruthdev.destiny.FirebaseRuntimeConfig
import com.vishruthdev.destiny.data.awaitResult
import java.util.Calendar

class PushTokenRepository(
    context: Context,
    private val firebaseConfig: FirebaseRuntimeConfig,
    private val firebaseAuth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) {
    private val appContext = context.applicationContext

    suspend fun syncCurrentToken(explicitToken: String? = null) {
        val uid = firebaseAuth?.currentUser?.uid ?: return
        syncTokenForUser(userId = uid, explicitToken = explicitToken)
    }

    suspend fun syncTokenForUser(
        userId: String,
        explicitToken: String? = null
    ) {
        if (!firebaseConfig.isBaseConfigured || firestore == null) return

        val installationId = FirebaseInstallations.getInstance().id.awaitResult()
        val token = explicitToken ?: FirebaseMessaging.getInstance().token.awaitResult()
        val now = Calendar.getInstance().timeInMillis
        val tokenDocument = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(TOKENS_COLLECTION)
            .document(installationId)

        val existing = tokenDocument.get().awaitResult()
        val createdAtMillis = existing.getLong("createdAtMillis") ?: now

        tokenDocument.set(
            PushTokenDocument(
                installationId = installationId,
                token = token,
                platform = "android",
                deviceModel = buildDeviceModel(),
                appPackage = appContext.packageName,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = now
            ),
            SetOptions.merge()
        ).awaitResult()
    }

    suspend fun removeTokenForUser(userId: String) {
        if (!firebaseConfig.isBaseConfigured || firestore == null) return

        val installationId = runCatching {
            FirebaseInstallations.getInstance().id.awaitResult()
        }.getOrNull() ?: return

        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(TOKENS_COLLECTION)
            .document(installationId)
            .delete()
            .awaitResult()
    }

    private fun buildDeviceModel(): String {
        return listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android device" }
    }

    private data class PushTokenDocument(
        val installationId: String = "",
        val token: String = "",
        val platform: String = "",
        val deviceModel: String = "",
        val appPackage: String = "",
        val createdAtMillis: Long = 0L,
        val updatedAtMillis: Long = 0L
    )

    private companion object {
        const val USERS_COLLECTION = "users"
        const val TOKENS_COLLECTION = "notificationTokens"
    }
}

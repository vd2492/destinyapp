package com.vishruthdev.destiny

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

data class FirebaseRuntimeConfig(
    val apiKey: String,
    val appId: String,
    val projectId: String,
    val storageBucket: String,
    val gcmSenderId: String,
    val webClientId: String
) {
    val isBaseConfigured: Boolean
        get() = apiKey.isNotBlank() && appId.isNotBlank() && projectId.isNotBlank()

    val isGoogleSignInConfigured: Boolean
        get() = isBaseConfigured && webClientId.isNotBlank()
}

object FirebaseInitializer {
    fun initialize(context: Context): FirebaseRuntimeConfig {
        val config = FirebaseRuntimeConfig(
            apiKey = BuildConfig.FIREBASE_API_KEY,
            appId = BuildConfig.FIREBASE_APP_ID,
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
            storageBucket = BuildConfig.FIREBASE_STORAGE_BUCKET,
            gcmSenderId = BuildConfig.FIREBASE_GCM_SENDER_ID,
            webClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID
        )

        if (config.isBaseConfigured && FirebaseApp.getApps(context).isEmpty()) {
            val optionsBuilder = FirebaseOptions.Builder()
                .setApiKey(config.apiKey)
                .setApplicationId(config.appId)
                .setProjectId(config.projectId)

            if (config.storageBucket.isNotBlank()) {
                optionsBuilder.setStorageBucket(config.storageBucket)
            }
            if (config.gcmSenderId.isNotBlank()) {
                optionsBuilder.setGcmSenderId(config.gcmSenderId)
            }

            FirebaseApp.initializeApp(context, optionsBuilder.build())
        }

        return config
    }
}

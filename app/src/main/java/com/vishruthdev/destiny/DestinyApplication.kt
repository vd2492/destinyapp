package com.vishruthdev.destiny

import android.app.Application
import com.vishruthdev.destiny.data.AuthRepository
import com.vishruthdev.destiny.data.HabitRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DestinyApplication : Application() {

    lateinit var firebaseConfig: FirebaseRuntimeConfig
        private set

    override fun onCreate() {
        super.onCreate()
        firebaseConfig = FirebaseInitializer.initialize(this)
    }

    private val firebaseAuth: FirebaseAuth? by lazy {
        if (firebaseConfig.isBaseConfigured) FirebaseAuth.getInstance() else null
    }

    private val firestore: FirebaseFirestore? by lazy {
        if (firebaseConfig.isBaseConfigured) FirebaseFirestore.getInstance() else null
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            context = this,
            firebaseConfig = firebaseConfig,
            firebaseAuth = firebaseAuth,
            firestore = firestore
        )
    }

    val habitRepository: HabitRepository by lazy {
        HabitRepository(
            firebaseConfig = firebaseConfig,
            firebaseAuth = firebaseAuth,
            firestore = firestore
        )
    }
}

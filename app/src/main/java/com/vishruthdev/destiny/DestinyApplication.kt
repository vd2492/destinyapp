package com.vishruthdev.destiny

import android.app.Application
import com.vishruthdev.destiny.data.AuthRepository
import com.vishruthdev.destiny.data.HabitRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vishruthdev.destiny.push.PushTokenRepository
import com.vishruthdev.destiny.push.PushTokenSyncManager
import com.vishruthdev.destiny.reminder.ReminderNotificationManager
import com.vishruthdev.destiny.reminder.ReminderScheduleManager
import com.vishruthdev.destiny.reminder.ReminderScheduler

class DestinyApplication : Application() {

    lateinit var firebaseConfig: FirebaseRuntimeConfig
        private set

    override fun onCreate() {
        super.onCreate()
        firebaseConfig = FirebaseInitializer.initialize(this)
        ReminderNotificationManager.createChannels(this)
        pushTokenSyncManager.start()
        reminderScheduleManager.start()
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

    val pushTokenRepository: PushTokenRepository by lazy {
        PushTokenRepository(
            context = this,
            firebaseConfig = firebaseConfig,
            firebaseAuth = firebaseAuth,
            firestore = firestore
        )
    }

    private val pushTokenSyncManager: PushTokenSyncManager by lazy {
        PushTokenSyncManager(
            firebaseAuth = firebaseAuth,
            pushTokenRepository = pushTokenRepository
        )
    }

    private val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(this)
    }

    private val reminderScheduleManager: ReminderScheduleManager by lazy {
        ReminderScheduleManager(
            habitRepository = habitRepository,
            reminderScheduler = reminderScheduler
        )
    }
}

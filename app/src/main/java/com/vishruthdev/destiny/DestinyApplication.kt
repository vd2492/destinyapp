package com.vishruthdev.destiny

import android.app.Application
import com.vishruthdev.destiny.data.AuthRepository
import com.vishruthdev.destiny.data.HabitRepository
import com.vishruthdev.destiny.data.local.DatabaseProvider

class DestinyApplication : Application() {

    val authRepository: AuthRepository by lazy { AuthRepository(this) }

    val habitRepository: HabitRepository by lazy {
        val db = DatabaseProvider.get(this)
        HabitRepository(db.habitDao())
    }
}

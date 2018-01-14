package com.overswell.firebasetoy

import android.app.Application
import android.arch.persistence.room.Room

class MainApplication : Application() {
    val database by lazy {
        val db = Room.databaseBuilder(applicationContext,
                AppDatabase::class.java, "database").build()
    }
}

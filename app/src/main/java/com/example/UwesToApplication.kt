package com.example

import android.app.Application
import com.example.data.ReelRepository
import com.example.data.UserPreferencesDataStore
import com.example.data.UwesToDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UwesToApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: UwesToDatabase
        private set

    lateinit var preferences: UserPreferencesDataStore
        private set

    lateinit var repository: ReelRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = UwesToDatabase.getInstance(this)
        preferences = UserPreferencesDataStore(this)
        repository = ReelRepository(database, preferences)

        appScope.launch {
            repository.ensureSeededDefaults()
        }
    }
}

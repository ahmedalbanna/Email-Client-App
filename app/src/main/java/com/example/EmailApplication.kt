package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.data.AppContainer
import com.example.work.SyncWorker

class EmailApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncWorker.schedulePeriodic(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}

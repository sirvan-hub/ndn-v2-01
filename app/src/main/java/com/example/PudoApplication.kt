package com.example

import android.app.Application
import com.example.data.sync.PudoSyncScheduler

/**
 * Main Android Application class for PUDO-NDN.
 * Automatically schedules background periodic and startup synchronization via WorkManager.
 */
class PudoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Schedule durable periodic background sync (every 15 min with network constraint)
        PudoSyncScheduler.schedulePeriodicSync(this, intervalMinutes = 15)

        // Schedule initial startup sync without blocking UI or DB initialization
        PudoSyncScheduler.scheduleOneTimeSync(this, replaceExisting = false)
    }
}

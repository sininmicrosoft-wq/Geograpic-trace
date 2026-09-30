package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.database.GeoTraceDatabase
import com.example.data.repository.TripRepository

class GeoTraceApplication : Application() {

    lateinit var database: GeoTraceDatabase
        private set

    lateinit var tripRepository: TripRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = GeoTraceDatabase.getDatabase(this)
        tripRepository = TripRepository(database.tripDao())

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracing Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active GPS location tracking telemetry and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "geotrace_tracking_channel"
        lateinit var instance: GeoTraceApplication
            private set
    }
}

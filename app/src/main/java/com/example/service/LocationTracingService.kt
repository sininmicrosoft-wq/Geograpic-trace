package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.GeoTraceApplication
import com.example.MainActivity
import com.example.location.LocationTracingManager
import com.example.location.TrackingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class LocationTracingService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var stateCollectJob: Job? = null
    private lateinit var trackingManager: LocationTracingManager
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        trackingManager = LocationTracingManager.getInstance(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundTracing()
            ACTION_STOP -> stopForegroundTracing()
            ACTION_PAUSE -> trackingManager.pauseTracing()
            ACTION_RESUME -> trackingManager.resumeTracing()
        }
        return START_STICKY
    }

    private fun startForegroundTracing() {
        val initialNotification = buildNotification(
            status = TrackingStatus.RECORDING,
            distanceMeters = 0.0,
            durationSeconds = 0,
            speedKmh = 0.0,
            activityType = "Activity"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        stateCollectJob?.cancel()
        stateCollectJob = scope.launch {
            trackingManager.state.collectLatest { state ->
                if (state.status == TrackingStatus.IDLE) {
                    stopForegroundTracing()
                } else {
                    val notif = buildNotification(
                        status = state.status,
                        distanceMeters = state.totalDistanceMeters,
                        durationSeconds = state.durationSeconds,
                        speedKmh = state.currentSpeedKmh,
                        activityType = state.activityType
                    )
                    notificationManager.notify(NOTIFICATION_ID, notif)
                }
            }
        }
    }

    private fun stopForegroundTracing() {
        stateCollectJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(
        status: TrackingStatus,
        distanceMeters: Double,
        durationSeconds: Long,
        speedKmh: Double,
        activityType: String
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distText = if (distanceMeters >= 1000) {
            String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)
        } else {
            String.format(Locale.US, "%.0f m", distanceMeters)
        }

        val hours = durationSeconds / 3600
        val mins = (durationSeconds % 3600) / 60
        val secs = durationSeconds % 60
        val timeText = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }

        val speedText = String.format(Locale.US, "%.1f km/h", speedKmh)
        val statusLabel = if (status == TrackingStatus.PAUSED) "PAUSED" else "LIVE"

        val title = "[$statusLabel] GeoTrace • $activityType"
        val content = "Dist: $distText  |  Time: $timeText  |  Speed: $speedText"

        val builder = NotificationCompat.Builder(this, GeoTraceApplication.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(contentIntent)
            .setOngoing(status != TrackingStatus.IDLE)
            .setOnlyAlertOnce(true)

        // Action Buttons: Pause/Resume
        if (status == TrackingStatus.RECORDING) {
            val pauseIntent = PendingIntent.getService(
                this,
                1,
                Intent(this, LocationTracingService::class.java).apply { action = ACTION_PAUSE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
        } else if (status == TrackingStatus.PAUSED) {
            val resumeIntent = PendingIntent.getService(
                this,
                2,
                Intent(this, LocationTracingService::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateCollectJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.geotrace.action.START"
        const val ACTION_STOP = "com.example.geotrace.action.STOP"
        const val ACTION_PAUSE = "com.example.geotrace.action.PAUSE"
        const val ACTION_RESUME = "com.example.geotrace.action.RESUME"
    }
}

package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.GeoTraceApplication
import com.example.MainActivity
import com.example.location.LocationTracingManager
import com.example.location.TrackingStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Foreground Service using FusedLocationProviderClient to provide persistent,
 * non-throttled GPS tracking while the app is in the background or device screen is off.
 */
class LocationTracingService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var stateCollectJob: Job? = null
    private var simulationJob: Job? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var trackingManager: LocationTracingManager
    private lateinit var notificationManager: NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var isLocationTrackingActive = false
    private var isSimulatedTracking = false
    private var currentActivityType = "Walking"

    inner class LocalBinder : Binder() {
        fun getService(): LocationTracingService = this@LocationTracingService
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            Log.d(TAG, "FusedLocation received: lat=${location.latitude}, lon=${location.longitude}, acc=${location.accuracy}")
            trackingManager.handleNewLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationTracingService created")
        trackingManager = LocationTracingManager.getInstance(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize partial wake lock to prevent CPU sleep during persistent outdoor tracking
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GeoTrace:PersistentTracingWakeLock").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        currentActivityType = intent?.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: currentActivityType
        isSimulatedTracking = intent?.getBooleanExtra(EXTRA_SIMULATION, isSimulatedTracking) ?: isSimulatedTracking

        Log.d(TAG, "onStartCommand action=$action, activity=$currentActivityType, sim=$isSimulatedTracking")

        when (action) {
            ACTION_START -> startPersistentTracking()
            ACTION_PAUSE -> pausePersistentTracking()
            ACTION_RESUME -> resumePersistentTracking()
            ACTION_STOP -> stopPersistentTracking()
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startPersistentTracking() {
        // 1. Immediately promote to Foreground Service with location type
        val initialNotif = buildNotification(
            status = TrackingStatus.RECORDING,
            distanceMeters = trackingManager.state.value.totalDistanceMeters,
            durationSeconds = trackingManager.state.value.durationSeconds,
            speedKmh = trackingManager.state.value.currentSpeedKmh,
            activityType = currentActivityType
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotif)
        }

        // 2. Acquire wake lock (safe timeout 6 hours)
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(6 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }

        // 3. Register FusedLocationProviderClient updates if not simulating
        if (!isSimulatedTracking) {
            startFusedLocationUpdates()
        } else {
            startSimulationTrail()
        }

        isLocationTrackingActive = true

        // 4. Continuously observe state and update sticky notification
        stateCollectJob?.cancel()
        stateCollectJob = scope.launch {
            trackingManager.state.collectLatest { state ->
                if (state.status == TrackingStatus.IDLE) {
                    stopPersistentTracking()
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

    @SuppressLint("MissingPermission")
    private fun startFusedLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(2.0f)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnSuccessListener {
                Log.d(TAG, "Successfully subscribed to FusedLocationProviderClient in foreground service")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to subscribe to FusedLocationProviderClient", e)
            }

            // Immediately query last location to seed coordinates
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    trackingManager.handleNewLocation(loc)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Location permission missing in service", e)
        }
    }

    private fun stopFusedLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d(TAG, "FusedLocationProviderClient updates removed")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates", e)
        }
    }

    private fun pausePersistentTracking() {
        Log.d(TAG, "Pausing persistent location tracking")
        stopFusedLocationUpdates()
        simulationJob?.cancel()
        isLocationTrackingActive = false
        trackingManager.pauseTrackingInternal()

        val notif = buildNotification(
            status = TrackingStatus.PAUSED,
            distanceMeters = trackingManager.state.value.totalDistanceMeters,
            durationSeconds = trackingManager.state.value.durationSeconds,
            speedKmh = 0.0,
            activityType = currentActivityType
        )
        notificationManager.notify(NOTIFICATION_ID, notif)
    }

    @SuppressLint("MissingPermission")
    private fun resumePersistentTracking() {
        Log.d(TAG, "Resuming persistent location tracking")
        trackingManager.resumeTrackingInternal()
        if (!isSimulatedTracking) {
            startFusedLocationUpdates()
        } else {
            startSimulationTrail()
        }
        isLocationTrackingActive = true
    }

    private fun stopPersistentTracking() {
        Log.d(TAG, "Stopping persistent location tracking service")
        isLocationTrackingActive = false
        stopFusedLocationUpdates()
        simulationJob?.cancel()
        stateCollectJob?.cancel()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startSimulationTrail() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val baseLat = 37.7749
            val baseLon = -122.4194
            var step = trackingManager.state.value.points.size
            val radius = 0.008

            while (isActive && isLocationTrackingActive) {
                delay(1500L)
                step++

                val angle = (step * 0.12) % (2 * Math.PI)
                val r = radius * (1.0 + 0.25 * sin(angle * 3))
                val lat = baseLat + r * cos(angle)
                val lon = baseLon + r * sin(angle)
                val alt = 85.0 + 35.0 * sin(angle * 2) + (step % 5)
                val simSpeedKmh = when (currentActivityType) {
                    "Running" -> 10.5 + 2.0 * sin(step * 0.2)
                    "Cycling" -> 22.0 + 4.0 * cos(step * 0.2)
                    "Driving" -> 50.0 + 10.0 * sin(step * 0.1)
                    else -> 4.8 + 1.2 * sin(step * 0.3)
                }

                val loc = Location("simulated").apply {
                    latitude = lat
                    longitude = lon
                    altitude = alt
                    speed = (simSpeedKmh / 3.6).toFloat()
                    accuracy = 3.2f
                    bearing = ((Math.toDegrees(angle + Math.PI / 2) + 360) % 360).toFloat()
                    time = System.currentTimeMillis()
                }

                trackingManager.handleNewLocation(loc)
            }
        }
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
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
        val statusPrefix = if (status == TrackingStatus.PAUSED) "⏸️ PAUSED" else "📍 LIVE TRACE"

        val title = "$statusPrefix • $activityType"
        val content = "Dist: $distText  |  Time: $timeText  |  Speed: $speedText"

        val builder = NotificationCompat.Builder(this, GeoTraceApplication.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(contentIntent)
            .setOngoing(status != TrackingStatus.IDLE)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        // Notification Action Buttons
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

        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, LocationTracingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop & Save", stopIntent)

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationTracingService onDestroy")
        stopPersistentTracking()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        private const val TAG = "LocationTracingService"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.geotrace.action.START"
        const val ACTION_STOP = "com.example.geotrace.action.STOP"
        const val ACTION_PAUSE = "com.example.geotrace.action.PAUSE"
        const val ACTION_RESUME = "com.example.geotrace.action.RESUME"

        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"
        const val EXTRA_SIMULATION = "extra_simulation"
        const val EXTRA_GEOFENCE_RADIUS = "extra_geofence_radius"
    }
}

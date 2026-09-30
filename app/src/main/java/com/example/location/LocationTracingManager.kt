package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import android.os.Build
import com.example.GeoTraceApplication
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.RouteElevationPoint
import com.example.data.model.TripSession
import com.example.data.model.WaypointMarker
import com.example.service.LocationTracingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TrackingStatus {
    IDLE,
    RECORDING,
    PAUSED
}

data class TracingState(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val activityType: String = "Walking",
    val activeSessionId: Long? = null,
    val currentLat: Double? = null,
    val currentLon: Double? = null,
    val currentAltitude: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val elevationGainMeters: Double = 0.0,
    val minAltitudeMeters: Double = 0.0,
    val maxAltitudeMeters: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val bearingDegrees: Float = 0f,
    val points: List<LocationBreadcrumb> = emptyList(),
    val waypoints: List<WaypointMarker> = emptyList(),
    val isSimulated: Boolean = false,
    val geofenceRadiusMeters: Double? = null,
    val isOutsideGeofence: Boolean = false,
    val distanceToStartMeters: Double? = null,
    val bearingToStartDegrees: Float? = null,
    val estimatedPoints: Int = 0
)

class LocationTracingManager private constructor(private val context: Context) {

    private val repository = GeoTraceApplication.instance.tripRepository
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _state = MutableStateFlow(TracingState())
    val state: StateFlow<TracingState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var lastLocation: Location? = null
    private var lastRecordedAltitude: Double? = null
    private var lastDbSaveTime: Long = 0

    @SuppressLint("MissingPermission")
    fun startTracing(
        activityType: String = "Walking",
        useSimulation: Boolean = false,
        geofenceRadiusMeters: Double? = null
    ) {
        if (_state.value.status == TrackingStatus.RECORDING) return

        scope.launch {
            val title = "$activityType Trace"
            val newSession = TripSession(
                title = title,
                activityType = activityType,
                startTimeMillis = System.currentTimeMillis()
            )
            val sessionId = repository.createSession(newSession)

            _state.value = TracingState(
                status = TrackingStatus.RECORDING,
                activityType = activityType,
                activeSessionId = sessionId,
                isSimulated = useSimulation,
                geofenceRadiusMeters = geofenceRadiusMeters
            )

            startTimer()

            // Start LocationTracingService as Foreground Service with location type
            try {
                val serviceIntent = Intent(context, LocationTracingService::class.java).apply {
                    action = LocationTracingService.ACTION_START
                    putExtra(LocationTracingService.EXTRA_ACTIVITY_TYPE, activityType)
                    putExtra(LocationTracingService.EXTRA_SIMULATION, useSimulation)
                    geofenceRadiusMeters?.let {
                        putExtra(LocationTracingService.EXTRA_GEOFENCE_RADIUS, it)
                    }
                }
                ContextCompat.startForegroundService(context, serviceIntent)
                Log.d("LocationTracingManager", "Started foreground tracing service successfully")
            } catch (e: Exception) {
                Log.e("LocationTracingManager", "Failed to start foreground service", e)
            }
        }
    }

    fun pauseTracing() {
        if (_state.value.status != TrackingStatus.RECORDING) return
        pauseTrackingInternal()

        try {
            val serviceIntent = Intent(context, LocationTracingService::class.java).apply {
                action = LocationTracingService.ACTION_PAUSE
            }
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("LocationTracingManager", "Failed to pause service", e)
        }
    }

    fun pauseTrackingInternal() {
        timerJob?.cancel()
        _state.value = _state.value.copy(status = TrackingStatus.PAUSED)
    }

    @SuppressLint("MissingPermission")
    fun resumeTracing() {
        if (_state.value.status != TrackingStatus.PAUSED) return
        resumeTrackingInternal()

        try {
            val serviceIntent = Intent(context, LocationTracingService::class.java).apply {
                action = LocationTracingService.ACTION_RESUME
            }
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("LocationTracingManager", "Failed to resume service", e)
        }
    }

    fun resumeTrackingInternal() {
        _state.value = _state.value.copy(status = TrackingStatus.RECORDING)
        startTimer()
    }

    fun stopTracing(onFinished: ((Long, com.example.data.repository.TraceRewardSummary?) -> Unit)? = null) {
        val currentSessionId = _state.value.activeSessionId ?: return
        timerJob?.cancel()

        // Stop foreground service
        try {
            val serviceIntent = Intent(context, LocationTracingService::class.java).apply {
                action = LocationTracingService.ACTION_STOP
            }
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("LocationTracingManager", "Failed to stop service", e)
        }

        scope.launch {
            val currentState = _state.value
            val rewardSummary = repository.awardTraceRewards(
                sessionId = currentSessionId,
                distanceMeters = currentState.totalDistanceMeters,
                elevationMeters = currentState.elevationGainMeters,
                waypointCount = currentState.waypoints.size,
                activityType = currentState.activityType
            )

            val session = TripSession(
                id = currentSessionId,
                title = "${currentState.activityType} Trace",
                activityType = currentState.activityType,
                startTimeMillis = System.currentTimeMillis() - (currentState.durationSeconds * 1000),
                endTimeMillis = System.currentTimeMillis(),
                durationSeconds = currentState.durationSeconds,
                totalDistanceMeters = currentState.totalDistanceMeters,
                avgSpeedKmh = currentState.avgSpeedKmh,
                maxSpeedKmh = currentState.maxSpeedKmh,
                elevationGainMeters = currentState.elevationGainMeters,
                minAltitudeMeters = currentState.minAltitudeMeters,
                maxAltitudeMeters = currentState.maxAltitudeMeters,
                pointCount = currentState.points.size,
                pointsEarned = rewardSummary.totalPointsEarned,
                isCompleted = true
            )
            repository.updateSession(session)

            _state.value = TracingState()
            lastLocation = null
            lastRecordedAltitude = null

            onFinished?.invoke(currentSessionId, rewardSummary)
        }
    }

    fun dropWaypoint(title: String, description: String, category: String) {
        val sessionId = _state.value.activeSessionId ?: return
        val lat = _state.value.currentLat ?: return
        val lon = _state.value.currentLon ?: return
        val alt = _state.value.currentAltitude

        scope.launch {
            val waypoint = WaypointMarker(
                sessionId = sessionId,
                title = title,
                description = description,
                category = category,
                latitude = lat,
                longitude = lon,
                altitude = alt
            )
            val id = repository.addWaypoint(waypoint)
            val updated = _state.value.waypoints + waypoint.copy(id = id)
            _state.value = _state.value.copy(waypoints = updated)
        }
    }

    fun setGeofenceRadius(radiusMeters: Double?) {
        _state.value = _state.value.copy(geofenceRadiusMeters = radiusMeters)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val current = _state.value
                val newDuration = current.durationSeconds + 1
                val avgSpeed = if (newDuration > 0) {
                    (current.totalDistanceMeters / newDuration) * 3.6
                } else 0.0

                _state.value = current.copy(
                    durationSeconds = newDuration,
                    avgSpeedKmh = avgSpeed
                )

                // Periodic DB sync every 15s to keep Room session record up to date
                val now = System.currentTimeMillis()
                if (now - lastDbSaveTime > 15000L && current.activeSessionId != null) {
                    lastDbSaveTime = now
                    repository.updateSession(
                        TripSession(
                            id = current.activeSessionId,
                            title = "${current.activityType} Trace",
                            activityType = current.activityType,
                            durationSeconds = newDuration,
                            totalDistanceMeters = current.totalDistanceMeters,
                            avgSpeedKmh = avgSpeed,
                            maxSpeedKmh = current.maxSpeedKmh,
                            elevationGainMeters = current.elevationGainMeters,
                            minAltitudeMeters = current.minAltitudeMeters,
                            maxAltitudeMeters = current.maxAltitudeMeters,
                            pointCount = current.points.size,
                            isCompleted = false
                        )
                    )
                }
            }
        }
    }

    fun handleNewLocation(location: Location) {
        val current = _state.value
        if (current.status != TrackingStatus.RECORDING) return
        val sessionId = current.activeSessionId ?: return

        // Outlier reject: accuracy > 40m
        if (location.hasAccuracy() && location.accuracy > 40f) {
            return
        }

        var distanceDelta = 0.0
        var bearing = location.bearing

        val prevLoc = lastLocation
        if (prevLoc != null) {
            val distArray = FloatArray(2)
            Location.distanceBetween(
                prevLoc.latitude, prevLoc.longitude,
                location.latitude, location.longitude,
                distArray
            )
            distanceDelta = distArray[0].toDouble()

            // If stationary jitter (< 1.5m), ignore distance
            if (distanceDelta < 1.5) {
                distanceDelta = 0.0
            } else if (bearing == 0f && distArray.size > 1) {
                bearing = distArray[1]
            }
        }

        val totalDist = current.totalDistanceMeters + distanceDelta
        val speedKmh = if (location.hasSpeed() && location.speed > 0f) {
            location.speed * 3.6
        } else if (prevLoc != null && distanceDelta > 0) {
            val timeDiffSec = (location.time - prevLoc.time).coerceAtLeast(1000L) / 1000.0
            (distanceDelta / timeDiffSec) * 3.6
        } else {
            0.0
        }

        val maxSpeed = maxOf(current.maxSpeedKmh, speedKmh)
        val altitude = if (location.hasAltitude()) location.altitude else current.currentAltitude

        var elevationGain = current.elevationGainMeters
        val prevAlt = lastRecordedAltitude
        if (prevAlt != null && altitude > prevAlt && (altitude - prevAlt) >= 1.0) {
            elevationGain += (altitude - prevAlt)
        }
        lastRecordedAltitude = altitude

        val minAlt = if (current.points.isEmpty()) altitude else minOf(current.minAltitudeMeters, altitude)
        val maxAlt = if (current.points.isEmpty()) altitude else maxOf(current.maxAltitudeMeters, altitude)

        // Compute slope gradient grade percentage
        val deltaDist = if (lastLocation != null) location.distanceTo(lastLocation!!).toDouble() else 0.0
        val deltaAlt = if (prevAlt != null) altitude - prevAlt else 0.0
        val gradePct = if (deltaDist >= 2.0) {
            ((deltaAlt / deltaDist) * 100.0).toFloat().coerceIn(-45f, 45f)
        } else 0f

        val verticalAcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
            location.verticalAccuracyMeters
        } else 0f

        val breadcrumb = LocationBreadcrumb(
            sessionId = sessionId,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = altitude,
            altitudeAccuracyMeters = verticalAcc,
            gradePercentage = gradePct,
            speed = (speedKmh / 3.6).toFloat(),
            accuracy = if (location.hasAccuracy()) location.accuracy else 5f,
            bearing = bearing,
            timestampMillis = location.time.coerceAtLeast(System.currentTimeMillis()),
            cumulativeDistanceMeters = totalDist
        )

        val newPoints = current.points + breadcrumb

        val elevationPoint = RouteElevationPoint(
            sessionId = sessionId,
            pointIndex = newPoints.size - 1,
            latitude = location.latitude,
            longitude = location.longitude,
            elevationMeters = altitude,
            distanceFromStartMeters = totalDist,
            gradePercentage = gradePct,
            cumulativeAscentMeters = elevationGain,
            cumulativeDescentMeters = 0.0,
            verticalAccuracyMeters = verticalAcc,
            timestampMillis = breadcrumb.timestampMillis
        )

        // Calculate backtrack metrics to start
        var distToStart: Double? = null
        var bearingToStart: Float? = null
        var isOutsideGeofence = false

        if (newPoints.isNotEmpty()) {
            val startPoint = newPoints.first()
            val result = FloatArray(2)
            Location.distanceBetween(
                location.latitude, location.longitude,
                startPoint.latitude, startPoint.longitude,
                result
            )
            distToStart = result[0].toDouble()
            bearingToStart = (result[1] + 360f) % 360f

            if (current.geofenceRadiusMeters != null && distToStart > current.geofenceRadiusMeters) {
                isOutsideGeofence = true
            }
        }

        val distPts = (totalDist / 10.0).toInt()
        val activityMult = if (current.activityType in listOf("Running", "Hiking")) 1.2 else 1.0
        val elevPts = (elevationGain / 2.0).toInt()
        val wptPts = current.waypoints.size * 20
        val estPts = ((distPts * activityMult) + elevPts + wptPts).toInt().coerceAtLeast(0)

        _state.value = current.copy(
            currentLat = location.latitude,
            currentLon = location.longitude,
            currentAltitude = altitude,
            currentSpeedKmh = speedKmh,
            maxSpeedKmh = maxSpeed,
            totalDistanceMeters = totalDist,
            elevationGainMeters = elevationGain,
            minAltitudeMeters = minAlt,
            maxAltitudeMeters = maxAlt,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 5f,
            bearingDegrees = bearing,
            points = newPoints,
            isOutsideGeofence = isOutsideGeofence,
            distanceToStartMeters = distToStart,
            bearingToStartDegrees = bearingToStart,
            estimatedPoints = estPts
        )

        lastLocation = location

        scope.launch {
            repository.addPoint(breadcrumb)
            repository.addElevationPoint(elevationPoint)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LocationTracingManager? = null

        fun getInstance(context: Context): LocationTracingManager {
            return INSTANCE ?: synchronized(this) {
                val instance = LocationTracingManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

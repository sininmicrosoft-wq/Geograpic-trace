package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GeoTraceApplication
import com.example.location.LocationTracingManager
import com.example.location.TrackingStatus
import com.example.location.TracingState
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.util.Locale

/**
 * Live Telemetry HUD composable that directly connects to the active location tracing service
 * (via LocationTracingManager) and displays real-time speed, distance traveled, and current duration.
 */
@Composable
fun LiveTelemetryHUD(
    modifier: Modifier = Modifier,
    useImperialUnits: Boolean = false,
    manager: LocationTracingManager? = null
) {
    val context = LocalContext.current
    val tracingManager = manager ?: remember { LocationTracingManager.getInstance(context) }
    val state by tracingManager.state.collectAsState()

    TelemetryHUD(
        state = state,
        useImperialUnits = useImperialUnits,
        modifier = modifier.testTag("live_telemetry_hud")
    )
}

/**
 * Streamlined floating telemetry HUD pill composable displaying real-time speed,
 * distance traveled, and duration, ideal for full map overlays.
 */
@Composable
fun CompactLiveTelemetryHUD(
    state: TracingState,
    useImperialUnits: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displaySpeed = if (useImperialUnits) state.currentSpeedKmh * 0.621371 else state.currentSpeedKmh
    val speedUnit = if (useImperialUnits) "mph" else "km/h"
    val displayDistance = if (useImperialUnits) state.totalDistanceMeters * 0.000621371 else state.totalDistanceMeters / 1000.0
    val distUnit = if (useImperialUnits) "mi" else "km"

    val hours = state.durationSeconds / 3600
    val mins = (state.durationSeconds % 3600) / 60
    val secs = state.durationSeconds % 60
    val timeStr = if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Slate900.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Slate800),
        shadowElevation = 8.dp,
        modifier = modifier.testTag("compact_telemetry_hud")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real-time Speed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.US, "%.1f %s", displaySpeed, speedUnit),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = CyanNeon,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("compact_hud_speed")
                )
            }

            // Real-time Distance
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Route, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.US, "%.2f %s", displayDistance, distUnit),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("compact_hud_distance")
                )
            }

            // Current Duration
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeStr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = EmeraldGreen,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("compact_hud_duration")
                )
            }
        }
    }
}

/**
 * Full Telemetry HUD Composable displaying comprehensive real-time metrics:
 * speed, distance traveled, duration, pace, elevation, accuracy, rewards, and backtrack vector.
 */
@Composable
fun TelemetryHUD(
    state: TracingState,
    useImperialUnits: Boolean = false,
    modifier: Modifier = Modifier
) {
    val repository = remember { GeoTraceApplication.instance.tripRepository }
    val activeStakes by repository.activeStakes.collectAsState(initial = emptyList())
    val maxStakingBoost = remember(activeStakes) { activeStakes.maxOfOrNull { it.traceBoostPercent } ?: 0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("telemetry_hud"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Geofence alert banner if outside safe zone
        if (state.isOutsideGeofence) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hud_geofence_alert")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Geofence Alert",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GEOFENCE ALERT: Outside designated boundary (${state.geofenceRadiusMeters?.toInt()}m)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Backtrack info pill (distance and bearing to start)
        if (state.distanceToStartMeters != null && state.points.size > 2) {
            val distStartText = if (useImperialUnits) {
                val miles = (state.distanceToStartMeters * 0.000621371)
                String.format(Locale.US, "%.2f mi to Start", miles)
            } else {
                if (state.distanceToStartMeters >= 1000) {
                    String.format(Locale.US, "%.2f km to Start", state.distanceToStartMeters / 1000.0)
                } else {
                    String.format(Locale.US, "%.0f m to Start", state.distanceToStartMeters)
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val bearingDiff = (state.bearingToStartDegrees ?: 0f) - state.bearingDegrees
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Direction to Start",
                            tint = CyanNeon,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(bearingDiff)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Backtrack Vector",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate400
                        )
                    }
                    Text(
                        text = distStartText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanLight,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Live Reward GeoPoints Pill
        if (state.status != TrackingStatus.IDLE) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AmberAccent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hud_reward_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (maxStakingBoost > 0) "Live Trace (+${maxStakingBoost}% Boost)" else "Live Trace Rewards",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "+${state.estimatedPoints} GeoPoints",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = AmberAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Main Telemetry Matrix Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Service Status Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("hud_status_badge")
                    ) {
                        val (dotColor, statusText) = when (state.status) {
                            TrackingStatus.RECORDING -> Pair(EmeraldGreen, "RECORDING • ${state.activityType.uppercase()}")
                            TrackingStatus.PAUSED -> Pair(AmberAccent, "PAUSED")
                            TrackingStatus.IDLE -> Pair(Slate400, "STANDBY")
                        }
                        Surface(
                            shape = CircleShape,
                            color = dotColor,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = dotColor,
                            letterSpacing = 1.sp
                        )
                    }

                    if (state.currentLat != null && state.currentLon != null) {
                        Text(
                            text = String.format(Locale.US, "%.5f°, %.5f°", state.currentLat, state.currentLon),
                            fontSize = 10.sp,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Primary Speed & Distance Readouts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Real-time Speed Display
                    Column {
                        val displaySpeed = if (useImperialUnits) {
                            state.currentSpeedKmh * 0.621371
                        } else {
                            state.currentSpeedKmh
                        }
                        val speedUnit = if (useImperialUnits) "MPH" else "KM/H"

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f", displaySpeed),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanNeon,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("hud_speed_value")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = speedUnit,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = "CURRENT SPEED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                    }

                    // Real-time Distance Display
                    Column(horizontalAlignment = Alignment.End) {
                        val displayDistance = if (useImperialUnits) {
                            state.totalDistanceMeters * 0.000621371
                        } else {
                            state.totalDistanceMeters / 1000.0
                        }
                        val distUnit = if (useImperialUnits) "MILES" else "KM"

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.2f", displayDistance),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("hud_distance_value")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = distUnit,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = "TOTAL DISTANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Dynamic Speed Gauge Bar
                Spacer(modifier = Modifier.height(8.dp))
                val maxRefSpeed = if (state.maxSpeedKmh > 0) state.maxSpeedKmh.coerceAtLeast(20.0) else 30.0
                val speedFraction = (state.currentSpeedKmh / maxRefSpeed).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Slate800, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(speedFraction)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(CyanNeon, EmeraldGreen)),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Slate800)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Metrics Grid: Current Duration, Pace, Elevation Gain, GPS Accuracy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Current Duration
                    val hours = state.durationSeconds / 3600
                    val mins = (state.durationSeconds % 3600) / 60
                    val secs = state.durationSeconds % 60
                    val timeStr = if (hours > 0) {
                        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
                    } else {
                        String.format(Locale.US, "%02d:%02d", mins, secs)
                    }
                    MetricItem(
                        icon = Icons.Default.Timer,
                        label = "DURATION",
                        value = timeStr,
                        valueColor = EmeraldGreen,
                        testTag = "hud_duration_value"
                    )

                    // Pace (min/km or min/mi)
                    val paceStr = calculatePace(state.totalDistanceMeters, state.durationSeconds, useImperialUnits)
                    MetricItem(
                        icon = Icons.Default.Speed,
                        label = if (useImperialUnits) "PACE (/MI)" else "PACE (/KM)",
                        value = paceStr,
                        valueColor = AmberAccent,
                        testTag = "hud_pace_value"
                    )

                    // Elevation Gain
                    val elevGainStr = if (useImperialUnits) {
                        String.format(Locale.US, "+%.0f ft", state.elevationGainMeters * 3.28084)
                    } else {
                        String.format(Locale.US, "+%.0f m", state.elevationGainMeters)
                    }
                    MetricItem(
                        icon = Icons.Default.Terrain,
                        label = "ELEV GAIN",
                        value = elevGainStr,
                        valueColor = CyanLight,
                        testTag = "hud_elev_value"
                    )

                    // GPS Accuracy
                    val accColor = when {
                        state.accuracyMeters <= 5f -> EmeraldGreen
                        state.accuracyMeters <= 15f -> AmberAccent
                        else -> RedAlert
                    }
                    MetricItem(
                        icon = Icons.Default.GpsFixed,
                        label = "GPS FIX",
                        value = "±${state.accuracyMeters.toInt()}m",
                        valueColor = accColor,
                        testTag = "hud_gps_fix_value"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    testTag: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

private fun calculatePace(distanceMeters: Double, durationSeconds: Long, imperial: Boolean): String {
    if (distanceMeters < 10 || durationSeconds <= 0) return "--:--"
    val dist = if (imperial) distanceMeters * 0.000621371 else distanceMeters / 1000.0
    if (dist <= 0) return "--:--"
    val paceSecPerUnit = durationSeconds / dist
    if (paceSecPerUnit > 3600) return "--:--"
    val mins = (paceSecPerUnit / 60).toInt()
    val secs = (paceSecPerUnit % 60).toInt()
    return String.format(Locale.US, "%d'%02d\"", mins, secs)
}

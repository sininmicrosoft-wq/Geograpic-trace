package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.TracingState
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Locale

@Composable
fun TelemetryHUD(
    state: TracingState,
    useImperialUnits: Boolean = false,
    modifier: Modifier = Modifier
) {
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
                modifier = Modifier.fillMaxWidth()
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
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
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

        // Main Telemetry Matrix Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Row: Primary Speed & Distance Readouts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Display
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
                                fontFamily = FontFamily.Monospace
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

                    // Distance Display
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
                                fontFamily = FontFamily.Monospace
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

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Slate800)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Metrics Grid: Time, Pace, Elevation Gain, GPS Accuracy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Time
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
                        valueColor = EmeraldGreen
                    )

                    // Pace (min/km or min/mi)
                    val paceStr = calculatePace(state.totalDistanceMeters, state.durationSeconds, useImperialUnits)
                    MetricItem(
                        icon = Icons.Default.Speed,
                        label = if (useImperialUnits) "PACE (/MI)" else "PACE (/KM)",
                        value = paceStr,
                        valueColor = AmberAccent
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
                        valueColor = CyanLight
                    )

                    // GPS Accuracy
                    val accRating = when {
                        state.accuracyMeters <= 5f -> "HIGH"
                        state.accuracyMeters <= 15f -> "GOOD"
                        else -> "FAIR"
                    }
                    val accColor = when {
                        state.accuracyMeters <= 5f -> EmeraldGreen
                        state.accuracyMeters <= 15f -> AmberAccent
                        else -> RedAlert
                    }
                    MetricItem(
                        icon = Icons.Default.GpsFixed,
                        label = "GPS FIX",
                        value = "±${state.accuracyMeters.toInt()}m",
                        valueColor = accColor
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
    valueColor: Color
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
            fontFamily = FontFamily.Monospace
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

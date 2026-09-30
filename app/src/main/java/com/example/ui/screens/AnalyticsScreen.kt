package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GeoTraceApplication
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.util.Locale

@Composable
fun AnalyticsScreen(
    useImperialUnits: Boolean = false
) {
    val repository = remember { GeoTraceApplication.instance.tripRepository }
    val totalDistMeters by repository.totalDistanceMeters.collectAsState(initial = 0.0)
    val totalDurationSec by repository.totalDurationSeconds.collectAsState(initial = 0L)
    val totalElevGain by repository.totalElevationGain.collectAsState(initial = 0.0)
    val topSpeed by repository.topSpeedKmh.collectAsState(initial = 0.0)
    val sessions by repository.allSessions.collectAsState(initial = emptyList())

    val dist = totalDistMeters ?: 0.0
    val duration = totalDurationSec ?: 0L
    val elevGain = totalElevGain ?: 0.0
    val maxSpeed = topSpeed ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("analytics_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Surface(
                color = Slate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lifetime Analytics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // All-Time Summary Grid
                Text(
                    text = "ALL-TIME TOTALS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val distVal = if (useImperialUnits) dist * 0.000621371 else dist / 1000.0
                    val distUnit = if (useImperialUnits) "mi" else "km"
                    MetricCard(
                        icon = Icons.Default.AutoAwesome,
                        label = "TOTAL DISTANCE",
                        value = String.format(Locale.US, "%.1f", distVal),
                        unit = distUnit,
                        color = CyanNeon,
                        modifier = Modifier.weight(1f)
                    )

                    val hours = duration / 3600
                    val mins = (duration % 3600) / 60
                    MetricCard(
                        icon = Icons.Default.Timer,
                        label = "TOTAL TIME",
                        value = "${hours}h ${mins}m",
                        unit = "",
                        color = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val elevVal = if (useImperialUnits) elevGain * 3.28084 else elevGain
                    val elevUnit = if (useImperialUnits) "ft" else "m"
                    MetricCard(
                        icon = Icons.Default.Terrain,
                        label = "ELEVATION CLIMBED",
                        value = String.format(Locale.US, "%.0f", elevVal),
                        unit = elevUnit,
                        color = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )

                    val topSpeedVal = if (useImperialUnits) maxSpeed * 0.621371 else maxSpeed
                    val topSpeedUnit = if (useImperialUnits) "mph" else "km/h"
                    MetricCard(
                        icon = Icons.Default.Speed,
                        label = "TOP SPEED RECORD",
                        value = String.format(Locale.US, "%.1f", topSpeedVal),
                        unit = topSpeedUnit,
                        color = RedAlert,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Activity Breakdown
                Text(
                    text = "ACTIVITY BREAKDOWN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                val activityTypes = listOf("Walking", "Running", "Cycling", "Hiking", "Driving")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        activityTypes.forEach { act ->
                            val actSessions = sessions.filter { it.activityType.equals(act, ignoreCase = true) }
                            val actDist = actSessions.sumOf { it.totalDistanceMeters }
                            val actRatio = if (dist > 0) (actDist / dist).toFloat() else 0f

                            val actDistVal = if (useImperialUnits) actDist * 0.000621371 else actDist / 1000.0
                            val actUnit = if (useImperialUnits) "mi" else "km"

                            val icon = when (act) {
                                "Running" -> Icons.Default.DirectionsRun
                                "Cycling" -> Icons.Default.DirectionsBike
                                "Hiking" -> Icons.Default.Hiking
                                "Driving" -> Icons.Default.DirectionsCar
                                else -> Icons.Default.DirectionsWalk
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(icon, contentDescription = null, tint = CyanLight, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = act, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Text(
                                        text = "${actSessions.size} trips • ${String.format(Locale.US, "%.1f %s", actDistVal, actUnit)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate400,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { actRatio.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = CyanNeon,
                                    trackColor = Slate800,
                                )
                            }
                        }
                    }
                }

                // Milestones & Badges
                Text(
                    text = "PATHFINDER ACHIEVEMENTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                AchievementCard(
                    title = "First Steps",
                    description = "Record your first location trace session",
                    achieved = sessions.isNotEmpty(),
                    icon = Icons.Default.EmojiEvents
                )

                AchievementCard(
                    title = "5K Trekker",
                    description = "Log at least 5 km (3.1 miles) of total route tracking",
                    achieved = dist >= 5000,
                    icon = Icons.Default.MilitaryTech
                )

                AchievementCard(
                    title = "Mountain Goat",
                    description = "Gain over 100 meters of cumulative elevation",
                    achieved = elevGain >= 100,
                    icon = Icons.Default.Terrain
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    title: String,
    description: String,
    achieved: Boolean,
    icon: ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (achieved) Slate900 else Slate900.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (achieved) androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (achieved) EmeraldGreen.copy(alpha = 0.2f) else Slate800,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (achieved) EmeraldGreen else Slate400,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (achieved) Color.White else Slate400
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Slate400
                )
            }
        }
    }
}

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GeoTraceApplication
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.WaypointMarker
import com.example.ui.components.ElevationSpeedChart
import com.example.ui.components.InteractiveRouteMap
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripDetailScreen(
    sessionId: Long,
    useImperialUnits: Boolean = false,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val repository = remember { GeoTraceApplication.instance.tripRepository }
    val session by repository.getSession(sessionId).collectAsState(initial = null)
    val points by repository.getSessionPoints(sessionId).collectAsState(initial = emptyList())
    val waypoints by repository.getSessionWaypoints(sessionId).collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var gpxContent by remember { mutableStateOf("") }

    // Route Replay / Playback animation state
    var isPlaying by remember { mutableStateOf(false) }
    var playbackIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying, points) {
        if (isPlaying && points.isNotEmpty()) {
            while (isActive && isPlaying) {
                delay(120L)
                if (playbackIndex < points.size - 1) {
                    playbackIndex++
                } else {
                    isPlaying = false
                }
            }
        }
    }

    val currentPoint = if (points.isNotEmpty()) {
        points[playbackIndex.coerceIn(0, points.size - 1)]
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("trip_detail_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                color = Slate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("detail_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = session?.title ?: "Trip Details",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            session?.let {
                                val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                                Text(
                                    text = sdf.format(Date(it.startTimeMillis)),
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }

                    // Export / Share GPX button
                    Button(
                        onClick = {
                            scope.launch {
                                gpxContent = repository.exportGpxXml(sessionId)
                                showExportDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanNeon),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("export_gpx_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GPX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Interactive Route Map
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    // Points up to playbackIndex if replaying, or all points
                    val displayedPoints = if (isPlaying || playbackIndex > 0) {
                        points.take(playbackIndex + 1)
                    } else {
                        points
                    }

                    InteractiveRouteMap(
                        points = displayedPoints,
                        currentLat = currentPoint?.latitude,
                        currentLon = currentPoint?.longitude,
                        bearingDegrees = currentPoint?.bearing ?: 0f,
                        accuracyMeters = currentPoint?.accuracy ?: 5f,
                        waypoints = waypoints,
                        enableControls = true,
                        autoCenter = false
                    )
                }

                // Playback Scrubber Bar
                if (points.size >= 2) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (playbackIndex >= points.size - 1) playbackIndex = 0
                                            isPlaying = !isPlaying
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause Replay",
                                            tint = CyanNeon
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            isPlaying = false
                                            playbackIndex = 0
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Replay,
                                            contentDescription = "Restart",
                                            tint = Slate400
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Route Replay",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                currentPoint?.let { pt ->
                                    val spd = if (useImperialUnits) (pt.speed * 3.6) * 0.621371 else pt.speed * 3.6
                                    val unit = if (useImperialUnits) "mph" else "km/h"
                                    Text(
                                        text = String.format(Locale.US, "%.1f %s | Alt: %.0fm", spd, unit, pt.altitude),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanLight,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Slider(
                                value = playbackIndex.toFloat(),
                                onValueChange = {
                                    isPlaying = false
                                    playbackIndex = it.toInt()
                                },
                                valueRange = 0f..(points.size - 1).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanNeon,
                                    activeTrackColor = CyanLight,
                                    inactiveTrackColor = Slate800
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playback_slider")
                            )
                        }
                    }
                }

                // Full Analytics Grid
                session?.let { sess ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "METRICS BREAKDOWN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )

                        // 4 Big Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val distText = if (useImperialUnits) {
                                String.format(Locale.US, "%.2f mi", sess.totalDistanceMeters * 0.000621371)
                            } else {
                                String.format(Locale.US, "%.2f km", sess.totalDistanceMeters / 1000.0)
                            }
                            DetailStatBox(
                                label = "TOTAL DISTANCE",
                                value = distText,
                                valueColor = CyanNeon,
                                modifier = Modifier.weight(1f)
                            )

                            val hours = sess.durationSeconds / 3600
                            val mins = (sess.durationSeconds % 3600) / 60
                            val secs = sess.durationSeconds % 60
                            val durationText = if (hours > 0) {
                                String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
                            } else {
                                String.format(Locale.US, "%02d:%02d", mins, secs)
                            }
                            DetailStatBox(
                                label = "TOTAL DURATION",
                                value = durationText,
                                valueColor = EmeraldGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val avgSpeedText = if (useImperialUnits) {
                                String.format(Locale.US, "%.1f mph", sess.avgSpeedKmh * 0.621371)
                            } else {
                                String.format(Locale.US, "%.1f km/h", sess.avgSpeedKmh)
                            }
                            DetailStatBox(
                                label = "AVG SPEED",
                                value = avgSpeedText,
                                valueColor = AmberAccent,
                                modifier = Modifier.weight(1f)
                            )

                            val maxSpeedText = if (useImperialUnits) {
                                String.format(Locale.US, "%.1f mph", sess.maxSpeedKmh * 0.621371)
                            } else {
                                String.format(Locale.US, "%.1f km/h", sess.maxSpeedKmh)
                            }
                            DetailStatBox(
                                label = "MAX SPEED",
                                value = maxSpeedText,
                                valueColor = RedAlert,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Elevation Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val elevGainText = if (useImperialUnits) {
                                String.format(Locale.US, "+%.0f ft", sess.elevationGainMeters * 3.28084)
                            } else {
                                String.format(Locale.US, "+%.0f m", sess.elevationGainMeters)
                            }
                            DetailStatBox(
                                label = "ELEVATION GAIN",
                                value = elevGainText,
                                valueColor = CyanLight,
                                modifier = Modifier.weight(1f)
                            )

                            DetailStatBox(
                                label = "GPS BREADCRUMBS",
                                value = "${points.size} pts",
                                valueColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Rewards Earned from this Trace
                        if (sess.pointsEarned > 0) {
                            DetailStatBox(
                                label = "REWARDS EARNED",
                                value = "+${sess.pointsEarned} GeoPoints",
                                valueColor = AmberAccent,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Elevation Profile Chart
                        ElevationSpeedChart(
                            points = points,
                            useImperialUnits = useImperialUnits
                        )

                        // Waypoints list if any
                        if (waypoints.isNotEmpty()) {
                            Text(
                                text = "SAVED WAYPOINTS (${waypoints.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 1.sp
                            )

                            waypoints.forEach { wpt ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Slate900),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Slate800,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    when (wpt.category) {
                                                        "PHOTO" -> Icons.Default.CameraAlt
                                                        "WATER" -> Icons.Default.LocalDrink
                                                        "PEAK" -> Icons.Default.Landscape
                                                        "HAZARD" -> Icons.Default.Warning
                                                        else -> Icons.Default.Flag
                                                    },
                                                    contentDescription = null,
                                                    tint = CyanNeon,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = wpt.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            if (wpt.description.isNotBlank()) {
                                                Text(
                                                    text = wpt.description,
                                                    fontSize = 12.sp,
                                                    color = Slate400
                                                )
                                            }
                                            Text(
                                                text = String.format(Locale.US, "%.5f, %.5f", wpt.latitude, wpt.longitude),
                                                fontSize = 10.sp,
                                                color = Slate400,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                containerColor = Slate900,
                title = {
                    Text("Export GPX Trace", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Standard GPX format compatible with Garmin, Strava, Google Earth, and GIS tools.",
                            fontSize = 13.sp,
                            color = Slate400
                        )

                        Text(
                            text = "Points: ${points.size} | Waypoints: ${waypoints.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Copy to clipboard
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("GPX Trace", gpxContent)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "GPX XML copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    showExportDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy XML", fontSize = 12.sp)
                            }

                            // Share Intent
                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, gpxContent)
                                        putExtra(Intent.EXTRA_TITLE, "${session?.title ?: "Trip"}.gpx")
                                        type = "application/gpx+xml"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share GPX Trace")
                                    context.startActivity(shareIntent)
                                    showExportDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close", color = Slate400)
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailStatBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

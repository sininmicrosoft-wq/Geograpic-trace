package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.location.LocationTracingManager
import com.example.location.TrackingStatus
import com.example.ui.components.AddWaypointDialog
import com.example.ui.components.ElevationSpeedChart
import com.example.ui.components.InteractiveRouteMap
import com.example.ui.components.TelemetryHUD
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

data class ActivityOption(val title: String, val icon: ImageVector)

val activityOptions = listOf(
    ActivityOption("Walking", Icons.Default.DirectionsWalk),
    ActivityOption("Running", Icons.Default.DirectionsRun),
    ActivityOption("Cycling", Icons.Default.DirectionsBike),
    ActivityOption("Hiking", Icons.Default.Hiking),
    ActivityOption("Driving", Icons.Default.DirectionsCar)
)

val geofenceOptions = listOf(
    Pair("Off", null),
    Pair("250m", 250.0),
    Pair("500m", 500.0),
    Pair("1 km", 1000.0),
    Pair("3 km", 3000.0)
)

@Composable
fun LiveTraceScreen(
    useImperialUnits: Boolean = false,
    onTripCompleted: (Long) -> Unit
) {
    val context = LocalContext.current
    val manager = remember { LocationTracingManager.getInstance(context) }
    val state by manager.state.collectAsState()

    var selectedActivity by remember { mutableStateOf("Walking") }
    var selectedGeofence by remember { mutableStateOf<Double?>(null) }
    var simulationMode by remember { mutableStateOf(false) }
    var showWaypointDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(0) } // 0 = Split, 1 = Full Map, 2 = Telemetry

    // Permission launcher
    val hasLocationPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    var permissionGranted by remember { mutableStateOf(hasLocationPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        permissionGranted = fineGranted || coarseGranted
        if (permissionGranted) {
            manager.startTracing(
                activityType = selectedActivity,
                useSimulation = simulationMode,
                geofenceRadiusMeters = selectedGeofence
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // View Mode selector top bar
            Surface(
                color = Slate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = when (state.status) {
                                TrackingStatus.RECORDING -> EmeraldGreen
                                TrackingStatus.PAUSED -> AmberAccent
                                TrackingStatus.IDLE -> Slate700
                            },
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (state.status) {
                                TrackingStatus.RECORDING -> "TRACING LIVE"
                                TrackingStatus.PAUSED -> "TRACING PAUSED"
                                TrackingStatus.IDLE -> "READY TO TRACE"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Map View Modes
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = viewMode == 0,
                            onClick = { viewMode = 0 },
                            label = { Text("Split", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Slate900,
                                containerColor = Slate800,
                                labelColor = Slate400
                            )
                        )
                        FilterChip(
                            selected = viewMode == 1,
                            onClick = { viewMode = 1 },
                            label = { Text("Map", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Slate900,
                                containerColor = Slate800,
                                labelColor = Slate400
                            )
                        )
                        FilterChip(
                            selected = viewMode == 2,
                            onClick = { viewMode = 2 },
                            label = { Text("HUD", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Slate900,
                                containerColor = Slate800,
                                labelColor = Slate400
                            )
                        )
                    }
                }
            }

            // Body
            when (viewMode) {
                0 -> {
                    // Split view: Top Map, Bottom HUD & Charts
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        InteractiveRouteMap(
                            points = state.points,
                            currentLat = state.currentLat,
                            currentLon = state.currentLon,
                            bearingDegrees = state.bearingDegrees,
                            accuracyMeters = state.accuracyMeters,
                            waypoints = state.waypoints,
                            geofenceRadiusMeters = state.geofenceRadiusMeters
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.15f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        TelemetryHUD(
                            state = state,
                            useImperialUnits = useImperialUnits
                        )

                        if (state.points.size >= 2) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                ElevationSpeedChart(
                                    points = state.points,
                                    useImperialUnits = useImperialUnits
                                )
                            }
                        }

                        // Config controls when IDLE
                        if (state.status == TrackingStatus.IDLE) {
                            IdleConfigCard(
                                selectedActivity = selectedActivity,
                                onSelectActivity = { selectedActivity = it },
                                selectedGeofence = selectedGeofence,
                                onSelectGeofence = { selectedGeofence = it },
                                simulationMode = simulationMode,
                                onToggleSimulation = { simulationMode = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(80.dp)) // Space for bottom controls
                    }
                }
                1 -> {
                    // Full Map
                    Box(modifier = Modifier.fillMaxSize()) {
                        InteractiveRouteMap(
                            points = state.points,
                            currentLat = state.currentLat,
                            currentLon = state.currentLon,
                            bearingDegrees = state.bearingDegrees,
                            accuracyMeters = state.accuracyMeters,
                            waypoints = state.waypoints,
                            geofenceRadiusMeters = state.geofenceRadiusMeters
                        )

                        // Compact HUD overlay on top of full map
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Slate900.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f km/h", state.currentSpeedKmh),
                                    fontWeight = FontWeight.Black,
                                    color = CyanNeon,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f km", state.totalDistanceMeters / 1000.0),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Full Telemetry HUD & Elevation
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        TelemetryHUD(
                            state = state,
                            useImperialUnits = useImperialUnits
                        )
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            ElevationSpeedChart(
                                points = state.points,
                                useImperialUnits = useImperialUnits
                            )
                        }

                        if (state.status == TrackingStatus.IDLE) {
                            IdleConfigCard(
                                selectedActivity = selectedActivity,
                                onSelectActivity = { selectedActivity = it },
                                selectedGeofence = selectedGeofence,
                                onSelectGeofence = { selectedGeofence = it },
                                simulationMode = simulationMode,
                                onToggleSimulation = { simulationMode = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating Action Bar at the Bottom
        Surface(
            color = Slate900.copy(alpha = 0.95f),
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state.status) {
                    TrackingStatus.IDLE -> {
                        Button(
                            onClick = {
                                if (permissionGranted || simulationMode) {
                                    manager.startTracing(
                                        activityType = selectedActivity,
                                        useSimulation = simulationMode,
                                        geofenceRadiusMeters = selectedGeofence
                                    )
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = Slate950
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_tracing_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (simulationMode) "START DEMO ROUTE TRACE" else "START GPS TRACING",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TrackingStatus.RECORDING -> {
                        // Drop Waypoint button
                        OutlinedButton(
                            onClick = { showWaypointDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Slate700),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("drop_waypoint_button")
                        ) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = CyanLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Marker", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Pause button
                        Button(
                            onClick = { manager.pauseTracing() },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Slate950),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pause_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Stop & Finish button
                        Button(
                            onClick = {
                                manager.stopTracing { sessionId ->
                                    onTripCompleted(sessionId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("stop_tracing_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Finish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TrackingStatus.PAUSED -> {
                        // Resume
                        Button(
                            onClick = { manager.resumeTracing() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Slate950),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("resume_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Stop & Save
                        Button(
                            onClick = {
                                manager.stopTracing { sessionId ->
                                    onTripCompleted(sessionId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("stop_tracing_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Finish & Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Add Waypoint Dialog
        if (showWaypointDialog) {
            AddWaypointDialog(
                onDismiss = { showWaypointDialog = false },
                onConfirm = { title, desc, cat ->
                    manager.dropWaypoint(title, desc, cat)
                }
            )
        }
    }
}

@Composable
private fun IdleConfigCard(
    selectedActivity: String,
    onSelectActivity: (String) -> Unit,
    selectedGeofence: Double?,
    onSelectGeofence: (Double?) -> Unit,
    simulationMode: Boolean,
    onToggleSimulation: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "TRIP CONFIGURATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 1.sp
            )

            // Activity Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                activityOptions.forEach { act ->
                    val isSelected = act.title == selectedActivity
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanNeon else Slate800,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clickable { onSelectActivity(act.title) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                act.icon,
                                contentDescription = act.title,
                                tint = if (isSelected) Slate950 else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = act.title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Slate950 else Slate400
                            )
                        }
                    }
                }
            }

            // Safe Zone / Geofence Perimeter
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = CyanLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Geofence Safe Boundary Alert",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    geofenceOptions.forEach { (label, radius) ->
                        val isSelected = selectedGeofence == radius
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectGeofence(radius) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Slate950,
                                containerColor = Slate800,
                                labelColor = Slate400
                            )
                        )
                    }
                }
            }

            // Demo Simulation Mode Toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate800,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Sensors,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Simulate Scenic Trek Route",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Test full tracing loop indoors or in emulator",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    Switch(
                        checked = simulationMode,
                        onCheckedChange = onToggleSimulation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmberAccent,
                            checkedTrackColor = Slate900
                        ),
                        modifier = Modifier.testTag("simulation_toggle")
                    )
                }
            }
        }
    }
}

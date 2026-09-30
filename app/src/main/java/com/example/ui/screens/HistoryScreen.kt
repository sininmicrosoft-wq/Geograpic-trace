package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.TripSession
import com.example.ui.components.MiniRouteThumbnail
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    useImperialUnits: Boolean = false,
    onSelectTrip: (Long) -> Unit,
    onStartTracingClick: () -> Unit
) {
    val repository = remember { GeoTraceApplication.instance.tripRepository }
    val sessions by repository.allSessions.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var sessionToDelete by remember { mutableStateOf<TripSession?>(null) }

    val filterActivities = listOf("All", "Walking", "Running", "Cycling", "Hiking", "Driving")

    val filteredSessions = sessions.filter { session ->
        val matchesActivity = selectedFilter == "All" || session.activityType.equals(selectedFilter, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() || session.title.contains(searchQuery, ignoreCase = true)
        matchesActivity && matchesSearch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("history_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header & Search Bar
            Surface(
                color = Slate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trip History",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "${sessions.size} recorded",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate400
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search recorded traces...", fontSize = 13.sp, color = Slate400) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate800,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("history_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Activity filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filterActivities.forEach { act ->
                            val isSelected = selectedFilter == act
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = act },
                                label = { Text(act, fontSize = 11.sp) },
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
            }

            // List of Sessions or Empty State
            if (filteredSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            tint = Slate800,
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = if (sessions.isEmpty()) "No GPS Traces Yet" else "No matching trips found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (sessions.isEmpty()) "Start tracking your walk, run, ride or hike to record breadcrumb routes, telemetry HUD, and elevation." else "Try adjusting your search query or filter chips.",
                            fontSize = 13.sp,
                            color = Slate400,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (sessions.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onStartTracingClick,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("empty_start_tracing_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start First Trace", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSessions, key = { it.id }) { session ->
                        TripCard(
                            session = session,
                            useImperialUnits = useImperialUnits,
                            onClick = { onSelectTrip(session.id) },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        sessionToDelete?.let { session ->
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                containerColor = Slate900,
                title = { Text("Delete Trip?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to permanently delete \"${session.title}\" and all its recorded GPS points?",
                        color = Slate400
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.deleteTrip(session.id)
                                sessionToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToDelete = null }) {
                        Text("Cancel", color = Slate400)
                    }
                }
            )
        }
    }
}

@Composable
private fun TripCard(
    session: TripSession,
    useImperialUnits: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val repository = remember { GeoTraceApplication.instance.tripRepository }
    val points by repository.getSessionPoints(session.id).collectAsState(initial = emptyList())

    val dateStr = remember(session.startTimeMillis) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(session.startTimeMillis))
    }

    val distStr = if (useImperialUnits) {
        String.format(Locale.US, "%.2f mi", session.totalDistanceMeters * 0.000621371)
    } else {
        if (session.totalDistanceMeters >= 1000) {
            String.format(Locale.US, "%.2f km", session.totalDistanceMeters / 1000.0)
        } else {
            String.format(Locale.US, "%.0f m", session.totalDistanceMeters)
        }
    }

    val hours = session.durationSeconds / 3600
    val mins = (session.durationSeconds % 3600) / 60
    val secs = session.durationSeconds % 60
    val durationStr = if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    val avgSpeedStr = if (useImperialUnits) {
        String.format(Locale.US, "%.1f mph", session.avgSpeedKmh * 0.621371)
    } else {
        String.format(Locale.US, "%.1f km/h", session.avgSpeedKmh)
    }

    val activityIcon = when (session.activityType.lowercase()) {
        "running" -> Icons.Default.DirectionsRun
        "cycling" -> Icons.Default.DirectionsBike
        "hiking" -> Icons.Default.Hiking
        "driving" -> Icons.Default.DirectionsCar
        else -> Icons.Default.DirectionsWalk
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("trip_card_${session.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Route thumbnail
            MiniRouteThumbnail(
                points = points,
                modifier = Modifier.size(70.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Trip",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = distStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = durationStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = avgSpeedStr,
                        fontSize = 12.sp,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.WaypointMarker
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun InteractiveRouteMap(
    points: List<LocationBreadcrumb>,
    currentLat: Double?,
    currentLon: Double?,
    bearingDegrees: Float = 0f,
    accuracyMeters: Float = 5f,
    waypoints: List<WaypointMarker> = emptyList(),
    geofenceRadiusMeters: Double? = null,
    modifier: Modifier = Modifier,
    enableControls: Boolean = true,
    autoCenter: Boolean = true
) {
    // Zoom and pan transform states
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var userInteracted by remember { mutableStateOf(false) }

    // Reset pan/zoom to center on route or current location
    fun resetView() {
        zoomScale = 1.0f
        panOffset = Offset.Zero
        userInteracted = false
    }

    LaunchedEffect(currentLat, currentLon) {
        if (!userInteracted && autoCenter) {
            // Keep centered
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    userInteracted = true
                    zoomScale = (zoomScale * zoom).coerceIn(0.2f, 15.0f)
                    panOffset += pan
                }
            }
            .testTag("interactive_route_map")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw subtle tactical grid lines & coordinates
            drawTacticalGrid(canvasWidth, canvasHeight)

            // Determine bounds
            val allLats = mutableListOf<Double>()
            val allLons = mutableListOf<Double>()

            points.forEach {
                allLats.add(it.latitude)
                allLons.add(it.longitude)
            }
            if (currentLat != null && currentLon != null) {
                allLats.add(currentLat)
                allLons.add(currentLon)
            }

            if (allLats.isEmpty()) {
                // Empty state crosshair
                drawEmptyCrosshairs(canvasWidth, canvasHeight)
                return@Canvas
            }

            val minLat = allLats.minOrNull() ?: 0.0
            val maxLat = allLats.maxOrNull() ?: 0.0
            val minLon = allLons.minOrNull() ?: 0.0
            val maxLon = allLons.maxOrNull() ?: 0.0

            val latSpan = max(maxLat - minLat, 0.0008)
            val lonSpan = max(maxLon - minLon, 0.0008)

            // Padding inside canvas
            val margin = 56.dp.toPx()
            val effectiveWidth = (canvasWidth - 2 * margin).coerceAtLeast(10f)
            val effectiveHeight = (canvasHeight - 2 * margin).coerceAtLeast(10f)

            // Aspect ratio calculation with longitude cosine factor
            val midLatRad = Math.toRadians((minLat + maxLat) / 2.0)
            val lonScaleFactor = cos(midLatRad)

            val scaleX = (effectiveWidth / (lonSpan * lonScaleFactor)).toFloat()
            val scaleY = (effectiveHeight / latSpan).toFloat()
            val baseScale = kotlin.math.min(scaleX, scaleY) * zoomScale

            val centerLat = (minLat + maxLat) / 2.0
            val centerLon = (minLon + maxLon) / 2.0

            fun latLonToScreen(lat: Double, lon: Double): Offset {
                val dx = ((lon - centerLon) * lonScaleFactor * baseScale).toFloat()
                val dy = -((lat - centerLat) * baseScale).toFloat()
                return Offset(
                    x = (canvasWidth / 2f) + dx + panOffset.x,
                    y = (canvasHeight / 2f) + dy + panOffset.y
                )
            }

            // 2. Draw Geofence safe zone radius if active
            if (geofenceRadiusMeters != null && points.isNotEmpty()) {
                val startScreen = latLonToScreen(points.first().latitude, points.first().longitude)
                // convert meters to pixels at baseScale
                val metersPerDegree = 111320.0
                val radiusPx = (geofenceRadiusMeters / metersPerDegree * baseScale).toFloat()

                drawCircle(
                    color = RedAlert.copy(alpha = 0.08f),
                    radius = radiusPx,
                    center = startScreen
                )
                drawCircle(
                    color = RedAlert.copy(alpha = 0.45f),
                    radius = radiusPx,
                    center = startScreen,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
                )
            }

            // 3. Draw Breadcrumb Polyline Route
            if (points.size >= 2) {
                // Route glow background shadow
                val glowPath = Path()
                val firstScreen = latLonToScreen(points[0].latitude, points[0].longitude)
                glowPath.moveTo(firstScreen.x, firstScreen.y)

                for (i in 1 until points.size) {
                    val pt = latLonToScreen(points[i].latitude, points[i].longitude)
                    glowPath.lineTo(pt.x, pt.y)
                }

                drawPath(
                    path = glowPath,
                    color = CyanNeon.copy(alpha = 0.22f),
                    style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Segment drawing with speed-based color gradients
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val pt1 = latLonToScreen(p1.latitude, p1.longitude)
                    val pt2 = latLonToScreen(p2.latitude, p2.longitude)

                    val speedKmh = p2.speed * 3.6
                    val segmentColor = when {
                        speedKmh > 20 -> CyanNeon
                        speedKmh > 8 -> EmeraldGreen
                        speedKmh > 2 -> Color(0xFF38BDF8)
                        else -> Color(0xFFF59E0B)
                    }

                    drawLine(
                        color = segmentColor,
                        start = pt1,
                        end = pt2,
                        strokeWidth = 4.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Draw Waypoint Markers
            waypoints.forEach { wpt ->
                val wptScreen = latLonToScreen(wpt.latitude, wpt.longitude)
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = 8.dp.toPx(),
                    center = wptScreen
                )
                drawCircle(
                    color = Slate950,
                    radius = 4.dp.toPx(),
                    center = wptScreen
                )
            }

            // 5. Draw Start Pin
            if (points.isNotEmpty()) {
                val startScreen = latLonToScreen(points.first().latitude, points.first().longitude)
                drawCircle(
                    color = EmeraldGreen.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = startScreen
                )
                drawCircle(
                    color = EmeraldGreen,
                    radius = 6.dp.toPx(),
                    center = startScreen
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = startScreen
                )
            }

            // 6. Draw Current Position Marker with Heading & Accuracy Halo
            if (currentLat != null && currentLon != null) {
                val curScreen = latLonToScreen(currentLat, currentLon)

                // Accuracy circle
                val metersPerDegree = 111320.0
                val accPx = (accuracyMeters / metersPerDegree * baseScale).toFloat().coerceIn(16f, 150f)
                drawCircle(
                    color = CyanLight.copy(alpha = 0.15f),
                    radius = accPx,
                    center = curScreen
                )
                drawCircle(
                    color = CyanLight.copy(alpha = 0.45f),
                    radius = accPx,
                    center = curScreen,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Current heading directional arrow
                rotate(bearingDegrees, pivot = curScreen) {
                    val arrowPath = Path().apply {
                        moveTo(curScreen.x, curScreen.y - 16.dp.toPx())
                        lineTo(curScreen.x - 7.dp.toPx(), curScreen.y + 7.dp.toPx())
                        lineTo(curScreen.x, curScreen.y + 3.dp.toPx())
                        lineTo(curScreen.x + 7.dp.toPx(), curScreen.y + 7.dp.toPx())
                        close()
                    }
                    drawPath(arrowPath, color = CyanNeon)
                }

                // Core dot
                drawCircle(
                    color = Slate900,
                    radius = 7.dp.toPx(),
                    center = curScreen
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = curScreen
                )
            }
        }

        // Overlay Map Controls
        if (enableControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900.copy(alpha = 0.85f),
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        IconButton(
                            onClick = { zoomScale = (zoomScale * 1.35f).coerceAtMost(15.0f) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = CyanLight)
                        }
                        IconButton(
                            onClick = { zoomScale = (zoomScale / 1.35f).coerceAtLeast(0.2f) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = CyanLight)
                        }
                        IconButton(
                            onClick = { resetView() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.CropFree, contentDescription = "Fit Route", tint = EmeraldGreen)
                        }
                    }
                }
            }

            // Recenter on current location FAB
            FloatingActionButton(
                onClick = { resetView() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("recenter_button"),
                containerColor = Slate800,
                contentColor = CyanNeon,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter Map")
            }

            // North Compass Indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                shape = CircleShape,
                color = Slate900.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "Compass North",
                        tint = RedAlert,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "N",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawTacticalGrid(width: Float, height: Float) {
    val step = 48.dp.toPx()
    val gridColor = Color(0xFF1E293B).copy(alpha = 0.4f)

    var x = step
    while (x < width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 0.75f
        )
        x += step
    }

    var y = step
    while (y < height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.75f
        )
        y += step
    }
}

private fun DrawScope.drawEmptyCrosshairs(width: Float, height: Float) {
    val cx = width / 2f
    val cy = height / 2f

    drawCircle(
        color = CyanLight.copy(alpha = 0.1f),
        radius = 80.dp.toPx(),
        center = Offset(cx, cy)
    )
    drawCircle(
        color = CyanLight.copy(alpha = 0.25f),
        radius = 50.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawCircle(
        color = CyanLight.copy(alpha = 0.5f),
        radius = 24.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawLine(
        color = CyanLight.copy(alpha = 0.4f),
        start = Offset(cx - 30.dp.toPx(), cy),
        end = Offset(cx + 30.dp.toPx(), cy),
        strokeWidth = 1.5.dp.toPx()
    )
    drawLine(
        color = CyanLight.copy(alpha = 0.4f),
        start = Offset(cx, cy - 30.dp.toPx()),
        end = Offset(cx, cy + 30.dp.toPx()),
        strokeWidth = 1.5.dp.toPx()
    )
}

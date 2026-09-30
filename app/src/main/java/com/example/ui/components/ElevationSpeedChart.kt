package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationBreadcrumb
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Locale

@Composable
fun ElevationSpeedChart(
    points: List<LocationBreadcrumb>,
    useImperialUnits: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Altitude profile will generate as you move",
                    fontSize = 12.sp,
                    color = Slate400
                )
            }
        }
        return
    }

    var scrubRatio by remember { mutableStateOf<Float?>(null) }

    val altitudes = points.map { it.altitude }
    val minAlt = altitudes.minOrNull() ?: 0.0
    val maxAlt = altitudes.maxOrNull() ?: 0.0
    val altRange = (maxAlt - minAlt).coerceAtLeast(5.0)

    val totalDistance = points.last().cumulativeDistanceMeters

    val scrubPoint = scrubRatio?.let { ratio ->
        val index = (ratio * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
        points[index]
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Min & Max Elevation & Scrub details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ELEVATION PROFILE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                if (scrubPoint != null) {
                    val sAlt = if (useImperialUnits) scrubPoint.altitude * 3.28084 else scrubPoint.altitude
                    val sDist = if (useImperialUnits) scrubPoint.cumulativeDistanceMeters * 0.000621371 else scrubPoint.cumulativeDistanceMeters / 1000.0
                    val sSpeed = if (useImperialUnits) (scrubPoint.speed * 3.6) * 0.621371 else (scrubPoint.speed * 3.6).toDouble()
                    val altUnit = if (useImperialUnits) "ft" else "m"
                    val distUnit = if (useImperialUnits) "mi" else "km"
                    val speedUnit = if (useImperialUnits) "mph" else "km/h"

                    Text(
                        text = String.format(Locale.US, "%.0f %s | %.2f %s | %.1f %s", sAlt, altUnit, sDist, distUnit, sSpeed, speedUnit),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    val minDisp = if (useImperialUnits) minAlt * 3.28084 else minAlt
                    val maxDisp = if (useImperialUnits) maxAlt * 3.28084 else maxAlt
                    val unit = if (useImperialUnits) "ft" else "m"

                    Text(
                        text = String.format(Locale.US, "Min: %.0f%s  |  Max: %.0f%s", minDisp, unit, maxDisp, unit),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .pointerInput(points) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                scrubRatio = (offset.x / size.width).coerceIn(0f, 1f)
                            },
                            onDrag = { change, _ ->
                                scrubRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = { scrubRatio = null },
                            onDragCancel = { scrubRatio = null }
                        )
                    }
                    .pointerInput(points) {
                        detectTapGestures(
                            onPress = { offset ->
                                scrubRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                tryAwaitRelease()
                                scrubRatio = null
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val paddingBottom = 16.dp.toPx()
                    val chartHeight = h - paddingBottom

                    // Grid line
                    drawLine(
                        color = Slate800,
                        start = Offset(0f, chartHeight),
                        end = Offset(w, chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )

                    val path = Path()
                    val fillPath = Path()

                    points.forEachIndexed { i, pt ->
                        val x = if (totalDistance > 0) {
                            (pt.cumulativeDistanceMeters / totalDistance * w).toFloat()
                        } else {
                            (i.toFloat() / (points.size - 1) * w)
                        }

                        val normalizedAlt = ((pt.altitude - minAlt) / altRange).toFloat().coerceIn(0f, 1f)
                        val y = chartHeight - (normalizedAlt * (chartHeight * 0.82f)) - 6.dp.toPx()

                        if (i == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, chartHeight)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }

                    fillPath.lineTo(w, chartHeight)
                    fillPath.close()

                    // Draw elevation fill gradient
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyanLight.copy(alpha = 0.35f),
                                CyanLight.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Draw elevation stroke line
                    drawPath(
                        path = path,
                        color = CyanLight,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw scrub line if touching
                    scrubRatio?.let { ratio ->
                        val sx = ratio * w
                        drawLine(
                            color = Color.White,
                            start = Offset(sx, 0f),
                            end = Offset(sx, chartHeight),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(
                            color = CyanNeon,
                            radius = 4.dp.toPx(),
                            center = Offset(sx, chartHeight * 0.5f)
                        )
                    }
                }
            }
        }
    }
}

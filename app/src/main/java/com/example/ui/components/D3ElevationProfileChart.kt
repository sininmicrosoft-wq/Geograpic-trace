package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.RouteElevationPoint
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
import kotlin.math.abs

/**
 * Normalized data structure for D3-style elevation chart rendering.
 */
data class ChartElevationSample(
    val index: Int,
    val distanceMeters: Double,
    val elevationMeters: Double,
    val gradePercentage: Float,
    val speedKmh: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

/**
 * A D3-inspired charting composable that renders a route's elevation profile.
 * Features:
 * - Mathematical domain & range scaling (similar to d3.scaleLinear)
 * - Monotone cubic spline interpolation (similar to d3.curveMonotoneX)
 * - Gradient area fill and horizon stroke (similar to d3.area and d3.line)
 * - Subtle axis tick lines & gridlines (similar to d3.axisBottom and d3.axisLeft)
 * - Interactive scrubber crosshair with D3-style bisector and floating tooltip inspector
 * - Peak/Summit and Valley callout tags
 * - Key elevation metrics breakdown (Gain, Loss, Max, Min, Steepest Grade)
 */
@Composable
fun D3ElevationProfileChart(
    samples: List<ChartElevationSample>,
    useImperialUnits: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (samples.size < 2) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("d3_elevation_chart_empty")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Terrain,
                        contentDescription = null,
                        tint = Slate700,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Elevation profile will generate as you move",
                        fontSize = 12.sp,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()

    // Domain bounds
    val totalDistance = samples.last().distanceMeters.coerceAtLeast(1.0)
    val rawMinAlt = samples.minOf { it.elevationMeters }
    val rawMaxAlt = samples.maxOf { it.elevationMeters }
    val altSpan = (rawMaxAlt - rawMinAlt).coerceAtLeast(10.0)

    // Add 10% headroom to domain bounds for aesthetic breathing room (like d3.nice())
    val domainMinAlt = (rawMinAlt - altSpan * 0.08).coerceAtLeast(0.0)
    val domainMaxAlt = rawMaxAlt + altSpan * 0.12
    val domainAltRange = (domainMaxAlt - domainMinAlt).coerceAtLeast(5.0)

    // Find peak (summit) and valley points
    val peakSample = remember(samples) { samples.maxByOrNull { it.elevationMeters } ?: samples.first() }
    val valleySample = remember(samples) { samples.minByOrNull { it.elevationMeters } ?: samples.first() }
    val steepestClimb = remember(samples) { samples.maxOfOrNull { it.gradePercentage } ?: 0f }

    // Total ascent calculation
    val totalAscent = remember(samples) {
        var gain = 0.0
        for (i in 1 until samples.size) {
            val delta = samples[i].elevationMeters - samples[i - 1].elevationMeters
            if (delta > 0) gain += delta
        }
        gain
    }

    // Interactive scrubber position ratio [0f, 1f]
    var scrubFraction by remember { mutableStateOf<Float?>(null) }

    // D3 bisector lookup: find closest sample to the scrubber's x position
    val activeSample = scrubFraction?.let { fraction ->
        val targetDist = fraction * totalDistance
        samples.minByOrNull { abs(it.distanceMeters - targetDist) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate800),
        modifier = modifier
            .fillMaxWidth()
            .testTag("d3_elevation_profile_chart")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title & Summit/Valley Quick Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Terrain,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ELEVATION PROFILE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                // Elevation Gain Summary Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        val gainText = if (useImperialUnits) {
                            String.format(Locale.US, "+%.0f ft Gain", totalAscent * 3.28084)
                        } else {
                            String.format(Locale.US, "+%.0f m Gain", totalAscent)
                        }
                        Text(
                            text = gainText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // D3 Scrubber Tooltip Inspector
            if (activeSample != null) {
                val sAlt = if (useImperialUnits) activeSample.elevationMeters * 3.28084 else activeSample.elevationMeters
                val sDist = if (useImperialUnits) activeSample.distanceMeters * 0.000621371 else activeSample.distanceMeters / 1000.0
                val altUnit = if (useImperialUnits) "ft" else "m"
                val distUnit = if (useImperialUnits) "mi" else "km"

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("d3_scrubber_tooltip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Elevation
                        Column {
                            Text(text = "ALTITUDE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                            Text(
                                text = String.format(Locale.US, "%.1f %s", sAlt, altUnit),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Distance along route
                        Column {
                            Text(text = "DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                            Text(
                                text = String.format(Locale.US, "%.2f %s", sDist, distUnit),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Incline Slope Grade
                        Column {
                            Text(text = "INCLINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                            val gradeColor = when {
                                activeSample.gradePercentage > 5f -> AmberAccent
                                activeSample.gradePercentage > 0f -> EmeraldGreen
                                activeSample.gradePercentage < -5f -> RedAlert
                                else -> CyanLight
                            }
                            val prefix = if (activeSample.gradePercentage > 0f) "+" else ""
                            Text(
                                text = String.format(Locale.US, "%s%.1f%%", prefix, activeSample.gradePercentage),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = gradeColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Speed if present
                        if (activeSample.speedKmh > 0) {
                            Column {
                                Text(text = "SPEED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                                val sSpd = if (useImperialUnits) activeSample.speedKmh * 0.621371 else activeSample.speedKmh
                                val spdUnit = if (useImperialUnits) "mph" else "km/h"
                                Text(
                                    text = String.format(Locale.US, "%.1f %s", sSpd, spdUnit),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else {
                // Default stats pill bar before scrubbing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val minDisp = if (useImperialUnits) rawMinAlt * 3.28084 else rawMinAlt
                    val maxDisp = if (useImperialUnits) rawMaxAlt * 3.28084 else rawMaxAlt
                    val unit = if (useImperialUnits) "ft" else "m"

                    Text(
                        text = String.format(Locale.US, "Valley: %.0f%s", minDisp, unit),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanLight,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "Summit: %.0f%s", maxDisp, unit),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "Max Grade: +%.1f%%", steepestClimb),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // D3 Canvas Area & Curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(samples) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                scrubFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = { scrubFraction = null },
                            onDragCancel = { scrubFraction = null },
                            onDrag = { change, _ ->
                                scrubFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        )
                    }
                    .pointerInput(samples) {
                        detectTapGestures(
                            onPress = { offset ->
                                scrubFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                tryAwaitRelease()
                                scrubFraction = null
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val padLeft = 4f
                    val padRight = 4f
                    val padTop = 14f
                    val padBottom = 22f
                    val chartW = w - padLeft - padRight
                    val chartH = h - padTop - padBottom

                    // Scales (equivalent to d3.scaleLinear)
                    fun xScale(distance: Double): Float {
                        return (padLeft + (distance / totalDistance).toFloat() * chartW).coerceIn(padLeft, w - padRight)
                    }

                    fun yScale(elevation: Double): Float {
                        val fraction = ((elevation - domainMinAlt) / domainAltRange).toFloat()
                        return (h - padBottom - fraction * chartH).coerceIn(padTop, h - padBottom)
                    }

                    // 1. Draw D3 Gridlines & Axes
                    val gridLinesCount = 3
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    for (i in 0..gridLinesCount) {
                        val gridAlt = domainMinAlt + (domainAltRange / gridLinesCount) * i
                        val y = yScale(gridAlt)
                        drawLine(
                            color = Slate800,
                            start = Offset(padLeft, y),
                            end = Offset(w - padRight, y),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )

                        // Y-axis label
                        val labelAlt = if (useImperialUnits) gridAlt * 3.28084 else gridAlt
                        val labelUnit = if (useImperialUnits) "ft" else "m"
                        val labelStr = String.format(Locale.US, "%.0f%s", labelAlt, labelUnit)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = labelStr,
                            topLeft = Offset(padLeft + 4f, y - 14f),
                            style = TextStyle(
                                color = Slate400.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    // Baseline (ground)
                    drawLine(
                        color = Slate700,
                        start = Offset(padLeft, h - padBottom),
                        end = Offset(w - padRight, h - padBottom),
                        strokeWidth = 1.5f
                    )

                    // 2. Compute smooth monotone cubic bezier curve points
                    val screenPoints = samples.map { sample ->
                        Offset(xScale(sample.distanceMeters), yScale(sample.elevationMeters))
                    }

                    val linePath = Path()
                    val areaPath = Path()

                    if (screenPoints.isNotEmpty()) {
                        linePath.moveTo(screenPoints.first().x, screenPoints.first().y)
                        areaPath.moveTo(screenPoints.first().x, h - padBottom)
                        areaPath.lineTo(screenPoints.first().x, screenPoints.first().y)

                        // Smooth Monotone Cubic Bezier interpolation
                        for (i in 0 until screenPoints.size - 1) {
                            val p0 = screenPoints[(i - 1).coerceAtLeast(0)]
                            val p1 = screenPoints[i]
                            val p2 = screenPoints[i + 1]
                            val p3 = screenPoints[(i + 2).coerceAtMost(screenPoints.size - 1)]

                            val cp1x = p1.x + (p2.x - p0.x) / 6f
                            val cp1y = p1.y + (p2.y - p0.y) / 6f
                            val cp2x = p2.x - (p3.x - p1.x) / 6f
                            val cp2y = p2.y - (p3.y - p1.y) / 6f

                            linePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                            areaPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                        }

                        areaPath.lineTo(screenPoints.last().x, h - padBottom)
                        areaPath.close()

                        // 3. Draw Topographic Gradient Area Fill (d3.area)
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    CyanNeon.copy(alpha = 0.45f),
                                    EmeraldGreen.copy(alpha = 0.20f),
                                    Slate950.copy(alpha = 0.05f)
                                ),
                                startY = padTop,
                                endY = h - padBottom
                            )
                        )

                        // 4. Draw Horizon Contour Stroke (d3.line)
                        drawPath(
                            path = linePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(CyanNeon, EmeraldGreen, CyanLight)
                            ),
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                        )
                    }

                    // 5. Summit / Peak marker
                    val peakX = xScale(peakSample.distanceMeters)
                    val peakY = yScale(peakSample.elevationMeters)
                    drawCircle(
                        color = AmberAccent,
                        radius = 4.5f,
                        center = Offset(peakX, peakY)
                    )
                    drawCircle(
                        color = Slate950,
                        radius = 2f,
                        center = Offset(peakX, peakY)
                    )

                    // 6. X-axis tick labels (Distance markers: Start, Halfway, Finish)
                    val distUnit = if (useImperialUnits) "mi" else "km"
                    val startDistStr = String.format(Locale.US, "0 %s", distUnit)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = startDistStr,
                        topLeft = Offset(padLeft, h - 14f),
                        style = TextStyle(color = Slate400, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    )

                    val midDist = if (useImperialUnits) (totalDistance * 0.000621371) / 2.0 else (totalDistance / 1000.0) / 2.0
                    val midDistStr = String.format(Locale.US, "%.1f %s", midDist, distUnit)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = midDistStr,
                        topLeft = Offset(w / 2f - 18f, h - 14f),
                        style = TextStyle(color = Slate400, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    )

                    val endDist = if (useImperialUnits) totalDistance * 0.000621371 else totalDistance / 1000.0
                    val endDistStr = String.format(Locale.US, "%.1f %s", endDist, distUnit)
                    val endTextLayout = textMeasurer.measure(endDistStr)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = endDistStr,
                        topLeft = Offset(w - padRight - endTextLayout.size.width, h - 14f),
                        style = TextStyle(color = Slate400, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    )

                    // 7. Interactive Scrubber Crosshair & Inspection Point
                    activeSample?.let { sample ->
                        val scrubX = xScale(sample.distanceMeters)
                        val scrubY = yScale(sample.elevationMeters)

                        // Vertical guideline crosshair
                        drawLine(
                            color = CyanNeon.copy(alpha = 0.8f),
                            start = Offset(scrubX, padTop),
                            end = Offset(scrubX, h - padBottom),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        // Outer pulsing radar beacon
                        drawCircle(
                            color = CyanNeon.copy(alpha = 0.3f),
                            radius = 9f,
                            center = Offset(scrubX, scrubY)
                        )
                        // Inner active beacon dot
                        drawCircle(
                            color = CyanNeon,
                            radius = 5f,
                            center = Offset(scrubX, scrubY)
                        )
                        drawCircle(
                            color = Slate950,
                            radius = 2.5f,
                            center = Offset(scrubX, scrubY)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convenient overload accepting raw List<LocationBreadcrumb>
 * and converting it into normalized D3 chart samples.
 */
@Composable
@JvmName("D3ElevationProfileChartFromBreadcrumbs")
fun D3ElevationProfileFromBreadcrumbs(
    breadcrumbs: List<LocationBreadcrumb>,
    useImperialUnits: Boolean = false,
    modifier: Modifier = Modifier
) {
    val samples = remember(breadcrumbs) {
        breadcrumbs.mapIndexed { index, point ->
            ChartElevationSample(
                index = index,
                distanceMeters = point.cumulativeDistanceMeters,
                elevationMeters = point.altitude,
                gradePercentage = point.gradePercentage,
                speedKmh = point.speed * 3.6,
                latitude = point.latitude,
                longitude = point.longitude
            )
        }
    }

    D3ElevationProfileChart(
        samples = samples,
        useImperialUnits = useImperialUnits,
        modifier = modifier
    )
}

/**
 * Convenient overload accepting List<RouteElevationPoint> directly from Room database
 * and converting it into normalized D3 chart samples.
 */
@Composable
fun D3ElevationProfileFromDatabase(
    elevationPoints: List<RouteElevationPoint>,
    useImperialUnits: Boolean = false,
    modifier: Modifier = Modifier
) {
    val samples = remember(elevationPoints) {
        elevationPoints.map { point ->
            ChartElevationSample(
                index = point.pointIndex,
                distanceMeters = point.distanceFromStartMeters,
                elevationMeters = point.elevationMeters,
                gradePercentage = point.gradePercentage,
                latitude = point.latitude,
                longitude = point.longitude
            )
        }
    }

    D3ElevationProfileChart(
        samples = samples,
        useImperialUnits = useImperialUnits,
        modifier = modifier
    )
}

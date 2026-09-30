package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.LocationBreadcrumb
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate950
import kotlin.math.cos
import kotlin.math.max

@Composable
fun MiniRouteThumbnail(
    points: List<LocationBreadcrumb>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Slate950)
    ) {
        if (points.size < 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Slate800,
                    radius = size.minDimension / 4,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val margin = 10.dp.toPx()
            val drawW = w - 2 * margin
            val drawH = h - 2 * margin

            val lats = points.map { it.latitude }
            val lons = points.map { it.longitude }

            val minLat = lats.min()
            val maxLat = lats.max()
            val minLon = lons.min()
            val maxLon = lons.max()

            val latSpan = max(maxLat - minLat, 0.0001)
            val lonSpan = max(maxLon - minLon, 0.0001)
            val midLatRad = Math.toRadians((minLat + maxLat) / 2.0)
            val lonScaleFactor = cos(midLatRad)

            val scaleX = (drawW / (lonSpan * lonScaleFactor)).toFloat()
            val scaleY = (drawH / latSpan).toFloat()
            val scale = kotlin.math.min(scaleX, scaleY)

            val centerLat = (minLat + maxLat) / 2.0
            val centerLon = (minLon + maxLon) / 2.0

            fun toPoint(lat: Double, lon: Double): Offset {
                val dx = ((lon - centerLon) * lonScaleFactor * scale).toFloat()
                val dy = -((lat - centerLat) * scale).toFloat()
                return Offset((w / 2f) + dx, (h / 2f) + dy)
            }

            val path = Path()
            val p0 = toPoint(points[0].latitude, points[0].longitude)
            path.moveTo(p0.x, p0.y)

            for (i in 1 until points.size) {
                val pt = toPoint(points[i].latitude, points[i].longitude)
                path.lineTo(pt.x, pt.y)
            }

            // Route path
            drawPath(
                path = path,
                color = CyanNeon,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Start dot
            drawCircle(
                color = EmeraldGreen,
                radius = 3.5.dp.toPx(),
                center = p0
            )

            // Finish dot
            val pEnd = toPoint(points.last().latitude, points.last().longitude)
            drawCircle(
                color = RedAlert,
                radius = 3.5.dp.toPx(),
                center = pEnd
            )
        }
    }
}

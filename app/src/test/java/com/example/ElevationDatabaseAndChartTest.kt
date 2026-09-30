package com.example

import com.example.data.model.LocationBreadcrumb
import com.example.data.model.RouteElevationPoint
import com.example.ui.components.ChartElevationSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ElevationDatabaseAndChartTest {

    @Test
    fun `route elevation point entity holds valid schema values`() {
        val elevationPoint = RouteElevationPoint(
            id = 1L,
            sessionId = 42L,
            pointIndex = 0,
            latitude = 37.7749,
            longitude = -122.4194,
            elevationMeters = 150.0,
            distanceFromStartMeters = 0.0,
            gradePercentage = 0f,
            cumulativeAscentMeters = 0.0,
            cumulativeDescentMeters = 0.0,
            verticalAccuracyMeters = 2.5f
        )

        assertEquals(42L, elevationPoint.sessionId)
        assertEquals(150.0, elevationPoint.elevationMeters, 0.01)
        assertEquals(0f, elevationPoint.gradePercentage)
        assertEquals(2.5f, elevationPoint.verticalAccuracyMeters)
    }

    @Test
    fun `grade percentage slope calculates rise over run accurately`() {
        val prevElevation = 100.0
        val currElevation = 115.0 // +15 meters rise
        val distanceRun = 200.0 // 200 meters run

        val deltaAlt = currElevation - prevElevation
        val gradePct = ((deltaAlt / distanceRun) * 100.0).toFloat()

        // 15 / 200 = 0.075 -> 7.5% incline
        assertEquals(7.5f, gradePct, 0.001f)
    }

    @Test
    fun `d3 elevation samples identify summit and valley correctly`() {
        val samples = listOf(
            ChartElevationSample(0, 0.0, 120.0, 0f),
            ChartElevationSample(1, 500.0, 240.0, 6.0f), // Summit
            ChartElevationSample(2, 1000.0, 95.0, -14.5f), // Valley
            ChartElevationSample(3, 1500.0, 180.0, 8.5f)
        )

        val summit = samples.maxByOrNull { it.elevationMeters }
        val valley = samples.minByOrNull { it.elevationMeters }

        assertEquals(240.0, summit?.elevationMeters ?: 0.0, 0.01)
        assertEquals(1, summit?.index)
        assertEquals(95.0, valley?.elevationMeters ?: 0.0, 0.01)
        assertEquals(2, valley?.index)
    }

    @Test
    fun `total ascent accumulation sums only positive elevation deltas`() {
        val elevations = listOf(100.0, 120.0, 115.0, 140.0, 130.0)
        var totalAscent = 0.0
        for (i in 1 until elevations.size) {
            val delta = elevations[i] - elevations[i - 1]
            if (delta > 0) totalAscent += delta
        }

        // 100 -> 120 (+20), 120 -> 115 (-5), 115 -> 140 (+25), 140 -> 130 (-10)
        // Total ascent = 20 + 25 = 45 meters
        assertEquals(45.0, totalAscent, 0.01)
    }

    @Test
    fun `imperial conversion of elevation and distance is accurate`() {
        val elevationMeters = 304.8 // exactly 1000 feet
        val elevationFeet = elevationMeters * 3.28084
        assertEquals("1000", String.format(Locale.US, "%.0f", elevationFeet))

        val distanceMeters = 1609.34 // 1.0 mile
        val distanceMiles = distanceMeters * 0.000621371
        assertEquals("1.00", String.format(Locale.US, "%.2f", distanceMiles))
    }
}

package com.example

import com.example.location.TrackingStatus
import com.example.location.TracingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TelemetryHUDTest {

    @Test
    fun `telemetry state formats metric speed and distance correctly`() {
        val state = TracingState(
            status = TrackingStatus.RECORDING,
            currentSpeedKmh = 14.4,
            maxSpeedKmh = 22.0,
            totalDistanceMeters = 3450.0,
            durationSeconds = 1245L, // 20 mins 45 secs
            elevationGainMeters = 85.0
        )

        val speedStr = String.format(Locale.US, "%.1f", state.currentSpeedKmh)
        assertEquals("14.4", speedStr)

        val distKm = state.totalDistanceMeters / 1000.0
        val distStr = String.format(Locale.US, "%.2f", distKm)
        assertEquals("3.45", distStr)

        val mins = (state.durationSeconds % 3600) / 60
        val secs = state.durationSeconds % 60
        val durationStr = String.format(Locale.US, "%02d:%02d", mins, secs)
        assertEquals("20:45", durationStr)
    }

    @Test
    fun `telemetry state converts imperial units accurately`() {
        val state = TracingState(
            status = TrackingStatus.RECORDING,
            currentSpeedKmh = 16.0934, // ~10.0 mph
            totalDistanceMeters = 1609.34, // 1.0 mile
            durationSeconds = 600L
        )

        val speedMph = state.currentSpeedKmh * 0.621371
        assertEquals("10.0", String.format(Locale.US, "%.1f", speedMph))

        val distMiles = state.totalDistanceMeters * 0.000621371
        assertEquals("1.00", String.format(Locale.US, "%.2f", distMiles))
    }

    @Test
    fun `duration format handles hours correctly`() {
        val durationSeconds = 3665L // 1 hour, 1 min, 5 secs
        val hours = durationSeconds / 3600
        val mins = (durationSeconds % 3600) / 60
        val secs = durationSeconds % 60

        val formatted = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }

        assertEquals("01:01:05", formatted)
    }

    @Test
    fun `active tracking status indicates live recording`() {
        val state = TracingState(
            status = TrackingStatus.RECORDING,
            activityType = "Running"
        )
        assertTrue(state.status == TrackingStatus.RECORDING)
        assertEquals("Running", state.activityType)
    }
}

package com.example

import com.example.data.model.LocationBreadcrumb
import com.example.data.model.TripSession
import com.example.data.model.WaypointMarker
import com.example.util.GpxFileUtility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class GpxFileUtilityTest {

    @Test
    fun `convertToGpx generates valid GPX 1_1 XML with trackpoints and waypoints`() {
        val session = TripSession(
            id = 101L,
            title = "Morning Trail Run",
            activityType = "Running",
            startTimeMillis = 1714500000000L,
            endTimeMillis = 1714503600000L,
            durationSeconds = 3600L,
            totalDistanceMeters = 5400.0,
            avgSpeedKmh = 5.4,
            maxSpeedKmh = 11.2,
            minAltitudeMeters = 120.0,
            maxAltitudeMeters = 240.0,
            elevationGainMeters = 120.0,
            pointCount = 2,
            isCompleted = true
        )

        val points = listOf(
            LocationBreadcrumb(
                id = 1L,
                sessionId = 101L,
                latitude = 37.7749,
                longitude = -122.4194,
                altitude = 125.0,
                speed = 2.8f,
                bearing = 90.0f,
                accuracy = 4.2f,
                timestampMillis = 1714500000000L
            ),
            LocationBreadcrumb(
                id = 2L,
                sessionId = 101L,
                latitude = 37.7755,
                longitude = -122.4180,
                altitude = 135.0,
                speed = 3.1f,
                bearing = 85.0f,
                accuracy = 3.5f,
                timestampMillis = 1714501000000L
            )
        )

        val waypoints = listOf(
            WaypointMarker(
                id = 1L,
                sessionId = 101L,
                latitude = 37.7752,
                longitude = -122.4187,
                altitude = 130.0,
                title = "Scenic Overlook",
                description = "Great view of the bay",
                category = "Scenic",
                timestampMillis = 1714500500000L
            )
        )

        val gpxXml = GpxFileUtility.convertToGpx(session, points, waypoints)

        // Verify XML structure and headers
        assertTrue(gpxXml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(gpxXml.contains("<gpx version=\"1.1\""))
        assertTrue(gpxXml.contains("creator=\"GeoTrace Android App\""))

        // Verify metadata
        assertTrue(gpxXml.contains("<name>Morning Trail Run</name>"))
        assertTrue(gpxXml.contains("<desc>Activity: Running | Distance: 5.40 km"))
        assertTrue(gpxXml.contains("<bounds minlat=\"37.774900\""))

        // Verify waypoints
        assertTrue(gpxXml.contains("<wpt lat=\"37.775200\" lon=\"-122.418700\">"))
        assertTrue(gpxXml.contains("<name>Scenic Overlook</name>"))
        assertTrue(gpxXml.contains("<desc>Great view of the bay</desc>"))
        assertTrue(gpxXml.contains("<sym>scenic</sym>"))
        assertTrue(gpxXml.contains("<ele>130.0</ele>"))

        // Verify track and trackpoints
        assertTrue(gpxXml.contains("<trk>"))
        assertTrue(gpxXml.contains("<trkseg>"))
        assertTrue(gpxXml.contains("<trkpt lat=\"37.774900\" lon=\"-122.419400\">"))
        assertTrue(gpxXml.contains("<ele>125.0</ele>"))
        assertTrue(gpxXml.contains("<speed>2.80</speed>"))
        assertTrue(gpxXml.contains("<course>90.0</course>"))
        assertTrue(gpxXml.contains("<trkpt lat=\"37.775500\" lon=\"-122.418000\">"))

        // Verify it parses as valid XML
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(gpxXml.toByteArray(Charsets.UTF_8)))
        assertNotNull(doc)
        assertEquals("gpx", doc.documentElement.nodeName)
    }

    @Test
    fun `convertMultipleSessionsToGpx outputs multiple tracks`() {
        val s1 = TripSession(id = 1L, title = "Walk 1", activityType = "Walking", startTimeMillis = 1714500000000L)
        val s2 = TripSession(id = 2L, title = "Cycle 2", activityType = "Cycling", startTimeMillis = 1714510000000L)
        val pts1 = listOf(LocationBreadcrumb(id = 1, sessionId = 1, latitude = 10.0, longitude = 20.0, timestampMillis = 1714500000000L))
        val pts2 = listOf(LocationBreadcrumb(id = 2, sessionId = 2, latitude = 10.5, longitude = 20.5, timestampMillis = 1714510000000L))

        val sessionsData = listOf(
            Triple(s1, pts1, emptyList<WaypointMarker>()),
            Triple(s2, pts2, emptyList<WaypointMarker>())
        )

        val gpxXml = GpxFileUtility.convertMultipleSessionsToGpx(sessionsData)

        assertTrue(gpxXml.contains("<name>GeoTrace Consolidated History</name>"))
        assertTrue(gpxXml.contains("<name>Walk 1</name>"))
        assertTrue(gpxXml.contains("<name>Cycle 2</name>"))
        assertTrue(gpxXml.contains("<trkpt lat=\"10.000000\" lon=\"20.000000\">"))
        assertTrue(gpxXml.contains("<trkpt lat=\"10.500000\" lon=\"20.500000\">"))
    }

    @Test
    fun `generateDefaultFilename creates clean sanitized filename`() {
        val session = TripSession(
            id = 5L,
            title = "Mountain & Trail (Day 1)",
            activityType = "Hiking",
            startTimeMillis = 1714500000000L
        )

        val filename = GpxFileUtility.generateDefaultFilename(session)
        assertTrue(filename.endsWith(".gpx"))
        assertTrue(!filename.contains(" "))
        assertTrue(!filename.contains("&"))
        assertTrue(!filename.contains("("))
        assertTrue(!filename.contains(")"))
    }
}

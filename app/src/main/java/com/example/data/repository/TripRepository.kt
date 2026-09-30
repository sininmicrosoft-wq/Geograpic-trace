package com.example.data.repository

import com.example.data.dao.TripDao
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.TripSession
import com.example.data.model.WaypointMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TripRepository(private val tripDao: TripDao) {

    val allSessions: Flow<List<TripSession>> = tripDao.getAllSessions()
    val totalDistanceMeters: Flow<Double?> = tripDao.getTotalDistanceMeters()
    val totalDurationSeconds: Flow<Long?> = tripDao.getTotalDurationSeconds()
    val totalElevationGain: Flow<Double?> = tripDao.getTotalElevationGain()
    val topSpeedKmh: Flow<Double?> = tripDao.getTopSpeedKmh()
    val sessionCount: Flow<Int> = tripDao.getSessionCount()

    fun getSession(sessionId: Long): Flow<TripSession?> = tripDao.getSessionById(sessionId)

    fun getSessionPoints(sessionId: Long): Flow<List<LocationBreadcrumb>> =
        tripDao.getSessionPoints(sessionId)

    fun getSessionWaypoints(sessionId: Long): Flow<List<WaypointMarker>> =
        tripDao.getSessionWaypoints(sessionId)

    suspend fun createSession(session: TripSession): Long = withContext(Dispatchers.IO) {
        tripDao.insertSession(session)
    }

    suspend fun updateSession(session: TripSession) = withContext(Dispatchers.IO) {
        tripDao.updateSession(session)
    }

    suspend fun addPoint(point: LocationBreadcrumb): Long = withContext(Dispatchers.IO) {
        tripDao.insertPoint(point)
    }

    suspend fun addPoints(points: List<LocationBreadcrumb>) = withContext(Dispatchers.IO) {
        tripDao.insertPoints(points)
    }

    suspend fun addWaypoint(waypoint: WaypointMarker): Long = withContext(Dispatchers.IO) {
        tripDao.insertWaypoint(waypoint)
    }

    suspend fun deleteTrip(sessionId: Long) = withContext(Dispatchers.IO) {
        tripDao.deletePointsForSession(sessionId)
        tripDao.deleteWaypointsForSession(sessionId)
        tripDao.deleteSession(sessionId)
    }

    suspend fun exportGpxXml(sessionId: Long): String = withContext(Dispatchers.IO) {
        val session = tripDao.getSessionByIdSync(sessionId) ?: return@withContext ""
        val points = tripDao.getSessionPointsSync(sessionId)
        val waypoints = tripDao.getSessionWaypointsSync(sessionId)

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"GeoTrace Android App\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapeXml(session.title)).append("</name>\n")
        sb.append("    <time>").append(isoFormat.format(Date(session.startTimeMillis))).append("</time>\n")
        sb.append("  </metadata>\n")

        // Waypoints
        for (wpt in waypoints) {
            sb.append("  <wpt lat=\"").append(wpt.latitude).append("\" lon=\"").append(wpt.longitude).append("\">\n")
            if (wpt.altitude != 0.0) {
                sb.append("    <ele>").append(String.format(Locale.US, "%.1f", wpt.altitude)).append("</ele>\n")
            }
            sb.append("    <name>").append(escapeXml(wpt.title)).append("</name>\n")
            if (wpt.description.isNotBlank()) {
                sb.append("    <desc>").append(escapeXml(wpt.description)).append("</desc>\n")
            }
            sb.append("    <sym>").append(wpt.category.lowercase()).append("</sym>\n")
            sb.append("  </wpt>\n")
        }

        // Track
        sb.append("  <trk>\n")
        sb.append("    <name>").append(escapeXml(session.title)).append("</name>\n")
        sb.append("    <type>").append(escapeXml(session.activityType)).append("</type>\n")
        sb.append("    <trkseg>\n")
        for (pt in points) {
            sb.append("      <trkpt lat=\"").append(pt.latitude).append("\" lon=\"").append(pt.longitude).append("\">\n")
            sb.append("        <ele>").append(String.format(Locale.US, "%.1f", pt.altitude)).append("</ele>\n")
            sb.append("        <time>").append(isoFormat.format(Date(pt.timestampMillis))).append("</time>\n")
            if (pt.speed > 0) {
                sb.append("        <speed>").append(String.format(Locale.US, "%.2f", pt.speed)).append("</speed>\n")
            }
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>")

        sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

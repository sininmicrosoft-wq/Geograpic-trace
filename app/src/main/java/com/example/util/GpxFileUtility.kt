package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.TripSession
import com.example.data.model.WaypointMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * File utility to convert recorded GPS location history into standard GPX 1.1 XML files
 * and handle saving to user storage or sharing via FileProvider.
 */
object GpxFileUtility {

    private const val TAG = "GpxFileUtility"
    private const val GPX_FOLDER = "gpx_exports"

    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    /**
     * Converts a single recorded trip session into standard GPX 1.1 XML.
     */
    fun convertToGpx(
        session: TripSession,
        points: List<LocationBreadcrumb>,
        waypoints: List<WaypointMarker>
    ): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"GeoTrace Android App\" ")
        sb.append("xmlns=\"http://www.topografix.com/GPX/1/1\" ")
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
        sb.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")

        // Metadata block
        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapeXml(session.title)).append("</name>\n")
        val descStr = "Activity: ${session.activityType} | Distance: ${String.format(Locale.US, "%.2f", session.totalDistanceMeters / 1000.0)} km | Duration: ${session.durationSeconds}s | Points: ${points.size}"
        sb.append("    <desc>").append(escapeXml(descStr)).append("</desc>\n")
        sb.append("    <time>").append(isoFormat.format(Date(session.startTimeMillis))).append("</time>\n")

        // Bounds calculation
        if (points.isNotEmpty()) {
            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLon = points.minOf { it.longitude }
            val maxLon = points.maxOf { it.longitude }
            sb.append("    <bounds minlat=\"").append(String.format(Locale.US, "%.6f", minLat))
                .append("\" minlon=\"").append(String.format(Locale.US, "%.6f", minLon))
                .append("\" maxlat=\"").append(String.format(Locale.US, "%.6f", maxLat))
                .append("\" maxlon=\"").append(String.format(Locale.US, "%.6f", maxLon))
                .append("\" />\n")
        }
        sb.append("  </metadata>\n")

        // Waypoints block
        for (wpt in waypoints) {
            sb.append("  <wpt lat=\"").append(String.format(Locale.US, "%.6f", wpt.latitude))
                .append("\" lon=\"").append(String.format(Locale.US, "%.6f", wpt.longitude)).append("\">\n")
            if (wpt.altitude != 0.0) {
                sb.append("    <ele>").append(String.format(Locale.US, "%.1f", wpt.altitude)).append("</ele>\n")
            }
            sb.append("    <time>").append(isoFormat.format(Date(wpt.timestampMillis))).append("</time>\n")
            sb.append("    <name>").append(escapeXml(wpt.title)).append("</name>\n")
            if (wpt.description.isNotBlank()) {
                sb.append("    <desc>").append(escapeXml(wpt.description)).append("</desc>\n")
            }
            sb.append("    <sym>").append(escapeXml(wpt.category.lowercase())).append("</sym>\n")
            sb.append("    <type>").append(escapeXml(wpt.category)).append("</type>\n")
            sb.append("  </wpt>\n")
        }

        // Track block
        sb.append("  <trk>\n")
        sb.append("    <name>").append(escapeXml(session.title)).append("</name>\n")
        sb.append("    <type>").append(escapeXml(session.activityType)).append("</type>\n")
        sb.append("    <trkseg>\n")
        for (pt in points) {
            sb.append("      <trkpt lat=\"").append(String.format(Locale.US, "%.6f", pt.latitude))
                .append("\" lon=\"").append(String.format(Locale.US, "%.6f", pt.longitude)).append("\">\n")
            if (pt.altitude != 0.0) {
                sb.append("        <ele>").append(String.format(Locale.US, "%.1f", pt.altitude)).append("</ele>\n")
            }
            sb.append("        <time>").append(isoFormat.format(Date(pt.timestampMillis))).append("</time>\n")
            if (pt.speed > 0f) {
                sb.append("        <speed>").append(String.format(Locale.US, "%.2f", pt.speed)).append("</speed>\n")
            }
            if (pt.bearing > 0f) {
                sb.append("        <course>").append(String.format(Locale.US, "%.1f", pt.bearing)).append("</course>\n")
            }
            if (pt.accuracy > 0f) {
                sb.append("        <hdop>").append(String.format(Locale.US, "%.1f", pt.accuracy)).append("</hdop>\n")
            }
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>")

        return sb.toString()
    }

    /**
     * Converts multiple recorded sessions into a consolidated GPX file containing multiple tracks.
     */
    fun convertMultipleSessionsToGpx(
        sessionsWithData: List<Triple<TripSession, List<LocationBreadcrumb>, List<WaypointMarker>>>
    ): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"GeoTrace Android App\" ")
        sb.append("xmlns=\"http://www.topografix.com/GPX/1/1\" ")
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
        sb.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")

        sb.append("  <metadata>\n")
        sb.append("    <name>GeoTrace Consolidated History</name>\n")
        sb.append("    <desc>Consolidated export of ${sessionsWithData.size} recorded trips</desc>\n")
        sb.append("    <time>").append(isoFormat.format(Date())).append("</time>\n")
        sb.append("  </metadata>\n")

        // Waypoints across all sessions
        for ((_, _, waypoints) in sessionsWithData) {
            for (wpt in waypoints) {
                sb.append("  <wpt lat=\"").append(String.format(Locale.US, "%.6f", wpt.latitude))
                    .append("\" lon=\"").append(String.format(Locale.US, "%.6f", wpt.longitude)).append("\">\n")
                if (wpt.altitude != 0.0) {
                    sb.append("    <ele>").append(String.format(Locale.US, "%.1f", wpt.altitude)).append("</ele>\n")
                }
                sb.append("    <time>").append(isoFormat.format(Date(wpt.timestampMillis))).append("</time>\n")
                sb.append("    <name>").append(escapeXml(wpt.title)).append("</name>\n")
                sb.append("    <sym>").append(escapeXml(wpt.category.lowercase())).append("</sym>\n")
                sb.append("  </wpt>\n")
            }
        }

        // Each session as a separate track
        for ((session, points, _) in sessionsWithData) {
            sb.append("  <trk>\n")
            sb.append("    <name>").append(escapeXml(session.title)).append("</name>\n")
            sb.append("    <type>").append(escapeXml(session.activityType)).append("</type>\n")
            sb.append("    <trkseg>\n")
            for (pt in points) {
                sb.append("      <trkpt lat=\"").append(String.format(Locale.US, "%.6f", pt.latitude))
                    .append("\" lon=\"").append(String.format(Locale.US, "%.6f", pt.longitude)).append("\">\n")
                if (pt.altitude != 0.0) {
                    sb.append("        <ele>").append(String.format(Locale.US, "%.1f", pt.altitude)).append("</ele>\n")
                }
                sb.append("        <time>").append(isoFormat.format(Date(pt.timestampMillis))).append("</time>\n")
                if (pt.speed > 0f) {
                    sb.append("        <speed>").append(String.format(Locale.US, "%.2f", pt.speed)).append("</speed>\n")
                }
                sb.append("      </trkpt>\n")
            }
            sb.append("    </trkseg>\n")
            sb.append("  </trk>\n")
        }

        sb.append("</gpx>")
        return sb.toString()
    }

    /**
     * Writes GPX XML content to a user-selected destination URI (via Storage Access Framework).
     */
    suspend fun writeGpxToUri(context: Context, destinationUri: Uri, gpxContent: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(destinationUri, "wt")?.use { os ->
                    OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
                        writer.write(gpxContent)
                        writer.flush()
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error writing GPX to URI: $destinationUri", e)
                false
            }
        }

    /**
     * Creates a temporary shareable .gpx file in the cache directory and returns a content:// URI
     * granted via FileProvider.
     */
    suspend fun createShareableGpxFile(context: Context, filename: String, gpxContent: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val folder = File(context.cacheDir, GPX_FOLDER).apply { if (!exists()) mkdirs() }
                val file = File(folder, filename)
                FileOutputStream(file).use { fos ->
                    OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                        writer.write(gpxContent)
                        writer.flush()
                    }
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating shareable GPX file", e)
                null
            }
        }

    /**
     * Generates a sanitized default filename for a session, e.g. "GeoTrace_Running_20260930_1510.gpx".
     */
    fun generateDefaultFilename(session: TripSession?): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(
            Date(session?.startTimeMillis ?: System.currentTimeMillis())
        )
        val act = (session?.activityType ?: "Trip").replace(Regex("[^a-zA-Z0-9]"), "")
        val rawTitle = (session?.title ?: "").replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val prefix = if (rawTitle.isNotBlank() && rawTitle != "Trip" && !rawTitle.startsWith("Trace")) {
            rawTitle.take(20)
        } else {
            "GeoTrace_$act"
        }
        return "${prefix}_$timestamp.gpx"
    }

    /**
     * Generates a filename for the consolidated all-trips export.
     */
    fun generateAllTripsFilename(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return "GeoTrace_All_History_$dateStr.gpx"
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

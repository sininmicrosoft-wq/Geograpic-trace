package com.example.data.repository

import com.example.data.dao.TripDao
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.RewardTransaction
import com.example.data.model.TripSession
import com.example.data.model.UserRewardWallet
import com.example.data.model.WaypointMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class TraceRewardSummary(
    val distancePoints: Int,
    val elevationPoints: Int,
    val waypointPoints: Int,
    val totalPointsEarned: Int,
    val previousBalance: Int,
    val newBalance: Int,
    val currentLevel: Int,
    val didLevelUp: Boolean,
    val levelTitle: String
)

class TripRepository(private val tripDao: TripDao) {

    val allSessions: Flow<List<TripSession>> = tripDao.getAllSessions()
    val totalDistanceMeters: Flow<Double?> = tripDao.getTotalDistanceMeters()
    val totalDurationSeconds: Flow<Long?> = tripDao.getTotalDurationSeconds()
    val totalElevationGain: Flow<Double?> = tripDao.getTotalElevationGain()
    val topSpeedKmh: Flow<Double?> = tripDao.getTopSpeedKmh()
    val sessionCount: Flow<Int> = tripDao.getSessionCount()

    val rewardWallet: Flow<UserRewardWallet?> = tripDao.getWallet()
    val rewardTransactions: Flow<List<RewardTransaction>> = tripDao.getAllRewardTransactions()

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

    suspend fun getOrCreateWallet(): UserRewardWallet = withContext(Dispatchers.IO) {
        var wallet = tripDao.getWalletSync()
        if (wallet == null) {
            wallet = UserRewardWallet()
            tripDao.insertOrUpdateWallet(wallet)
        }
        wallet
    }

    /**
     * Calculates and awards GeoPoints for a completed GPS location trace.
     */
    suspend fun awardTraceRewards(
        sessionId: Long,
        distanceMeters: Double,
        elevationMeters: Double,
        waypointCount: Int,
        activityType: String
    ): TraceRewardSummary = withContext(Dispatchers.IO) {
        val currentWallet = getOrCreateWallet()
        val prevBalance = currentWallet.balancePoints
        val prevLevel = currentWallet.currentLevel

        // Base reward formula: 10 pts per 100 meters (100 pts per km)
        var distPts = if (distanceMeters >= 20) {
            (distanceMeters / 10.0).toInt().coerceAtLeast(10)
        } else {
            5
        }

        // Activity multiplier: Running & Hiking receive a 20% stamina bonus
        if (activityType.equals("Running", ignoreCase = true) || activityType.equals("Hiking", ignoreCase = true)) {
            distPts = (distPts * 1.20).toInt()
        }

        // Elevation bonus: 1 pt per 2 meters of elevation gained
        val elevPts = if (elevationMeters > 0) (elevationMeters / 2.0).toInt() else 0

        // Waypoints bonus: 20 pts per dropped POI marker
        val wptPts = waypointCount * 20

        val totalEarned = distPts + elevPts + wptPts
        val newBalance = prevBalance + totalEarned
        val newLifetime = currentWallet.lifetimePointsEarned + totalEarned

        // Level thresholds
        val (newLevel, levelTitle) = calculateLevelAndTitle(newLifetime)
        val didLevelUp = newLevel > prevLevel

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val streak = if (currentWallet.lastTraceDate != todayDate) {
            currentWallet.streakDays + 1
        } else {
            currentWallet.streakDays
        }

        val updatedWallet = currentWallet.copy(
            balancePoints = newBalance,
            lifetimePointsEarned = newLifetime,
            currentLevel = newLevel,
            levelTitle = levelTitle,
            streakDays = streak,
            lastTraceDate = todayDate
        )
        tripDao.insertOrUpdateWallet(updatedWallet)

        // Record transaction
        tripDao.insertRewardTransaction(
            RewardTransaction(
                sessionId = sessionId,
                title = "$activityType GPS Trace Reward",
                points = totalEarned,
                category = "TRACE_DISTANCE"
            )
        )

        TraceRewardSummary(
            distancePoints = distPts,
            elevationPoints = elevPts,
            waypointPoints = wptPts,
            totalPointsEarned = totalEarned,
            previousBalance = prevBalance,
            newBalance = newBalance,
            currentLevel = newLevel,
            didLevelUp = didLevelUp,
            levelTitle = levelTitle
        )
    }

    suspend fun claimQuestReward(questTitle: String, points: Int): Boolean = withContext(Dispatchers.IO) {
        val wallet = getOrCreateWallet()
        val newBalance = wallet.balancePoints + points
        val newLifetime = wallet.lifetimePointsEarned + points
        val (newLevel, levelTitle) = calculateLevelAndTitle(newLifetime)

        val updated = wallet.copy(
            balancePoints = newBalance,
            lifetimePointsEarned = newLifetime,
            currentLevel = newLevel,
            levelTitle = levelTitle
        )
        tripDao.insertOrUpdateWallet(updated)

        tripDao.insertRewardTransaction(
            RewardTransaction(
                title = "Quest Completed: $questTitle",
                points = points,
                category = "DAILY_QUEST"
            )
        )
        true
    }

    suspend fun redeemStoreTheme(themeId: String, costPoints: Int): Boolean = withContext(Dispatchers.IO) {
        val wallet = getOrCreateWallet()
        if (wallet.balancePoints < costPoints) return@withContext false

        val currentUnlocked = wallet.unlockedThemes.split(",").map { it.trim() }.toMutableSet()
        currentUnlocked.add(themeId)

        val updated = wallet.copy(
            balancePoints = wallet.balancePoints - costPoints,
            unlockedThemes = currentUnlocked.joinToString(","),
            activeTheme = themeId
        )
        tripDao.insertOrUpdateWallet(updated)

        tripDao.insertRewardTransaction(
            RewardTransaction(
                title = "Unlocked Theme: $themeId",
                points = -costPoints,
                category = "PURCHASE"
            )
        )
        true
    }

    suspend fun setActiveTheme(themeId: String) = withContext(Dispatchers.IO) {
        val wallet = getOrCreateWallet()
        val updated = wallet.copy(activeTheme = themeId)
        tripDao.insertOrUpdateWallet(updated)
    }

    private fun calculateLevelAndTitle(lifetimePoints: Int): Pair<Int, String> {
        return when {
            lifetimePoints >= 3500 -> Pair(5, "Master Voyager")
            lifetimePoints >= 2000 -> Pair(4, "Summit Pathfinder")
            lifetimePoints >= 1000 -> Pair(3, "Trail Pioneer")
            lifetimePoints >= 400 -> Pair(2, "Field Tracker")
            else -> Pair(1, "Novice Scout")
        }
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

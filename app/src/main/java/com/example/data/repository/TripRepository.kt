package com.example.data.repository

import com.example.blockchain.OpChainConfig
import com.example.blockchain.OpClaimReceipt
import com.example.blockchain.OpNetwork
import com.example.blockchain.StakingPoolDef
import com.example.data.dao.TripDao
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.RewardTransaction
import com.example.data.model.RouteElevationPoint
import com.example.data.model.StakedPosition
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
    val stakingBonusPoints: Int = 0,
    val activeStakingBoostPercent: Int = 0,
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
    val activeStakes: Flow<List<StakedPosition>> = tripDao.getActiveStakedPositions()
    val allStakes: Flow<List<StakedPosition>> = tripDao.getAllStakedPositions()

    val opChainService = com.example.blockchain.OpChainService()

    fun getSession(sessionId: Long): Flow<TripSession?> = tripDao.getSessionById(sessionId)

    fun getSessionPoints(sessionId: Long): Flow<List<LocationBreadcrumb>> =
        tripDao.getSessionPoints(sessionId)

    fun getSessionWaypoints(sessionId: Long): Flow<List<WaypointMarker>> =
        tripDao.getSessionWaypoints(sessionId)

    suspend fun getSessionPointsSync(sessionId: Long): List<LocationBreadcrumb> = withContext(Dispatchers.IO) {
        tripDao.getSessionPointsSync(sessionId)
    }

    suspend fun getSessionWaypointsSync(sessionId: Long): List<WaypointMarker> = withContext(Dispatchers.IO) {
        tripDao.getSessionWaypointsSync(sessionId)
    }

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

    fun getSessionElevationPoints(sessionId: Long): Flow<List<RouteElevationPoint>> =
        tripDao.getElevationPoints(sessionId)

    suspend fun getSessionElevationPointsSync(sessionId: Long): List<RouteElevationPoint> = withContext(Dispatchers.IO) {
        tripDao.getElevationPointsSync(sessionId)
    }

    suspend fun addElevationPoint(point: RouteElevationPoint): Long = withContext(Dispatchers.IO) {
        tripDao.insertElevationPoint(point)
    }

    suspend fun addElevationPoints(points: List<RouteElevationPoint>) = withContext(Dispatchers.IO) {
        tripDao.insertElevationPoints(points)
    }

    suspend fun deleteTrip(sessionId: Long) = withContext(Dispatchers.IO) {
        tripDao.deletePointsForSession(sessionId)
        tripDao.deleteElevationPointsForSession(sessionId)
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
     * Calculates and awards GeoPoints for a completed GPS location trace,
     * including active staking APY multipliers.
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

        // Active Staking Boost Check
        val activePositions = tripDao.getActiveStakedPositionsSync()
        val maxStakingBoostPercent = activePositions.maxOfOrNull { it.traceBoostPercent } ?: 0
        val stakingBonusPts = if (maxStakingBoostPercent > 0) {
            (distPts * (maxStakingBoostPercent / 100.0)).toInt().coerceAtLeast(1)
        } else 0

        // Elevation bonus: 1 pt per 2 meters of elevation gained
        val elevPts = if (elevationMeters > 0) (elevationMeters / 2.0).toInt() else 0

        // Waypoints bonus: 20 pts per dropped POI marker
        val wptPts = waypointCount * 20

        val totalEarned = distPts + elevPts + wptPts + stakingBonusPts
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
                title = if (stakingBonusPts > 0) {
                    "$activityType Trace (+${maxStakingBoostPercent}% Staking Boost)"
                } else {
                    "$activityType GPS Trace Reward"
                },
                points = totalEarned,
                category = "TRACE_DISTANCE"
            )
        )

        TraceRewardSummary(
            distancePoints = distPts,
            elevationPoints = elevPts,
            waypointPoints = wptPts,
            stakingBonusPoints = stakingBonusPts,
            activeStakingBoostPercent = maxStakingBoostPercent,
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

    suspend fun updateOpWalletConfig(address: String, networkId: String, customContract: String) = withContext(Dispatchers.IO) {
        val wallet = getOrCreateWallet()
        val updated = wallet.copy(
            opWalletAddress = address.trim(),
            opNetworkId = networkId,
            opTokenContractAddress = customContract.trim()
        )
        tripDao.insertOrUpdateWallet(updated)
    }

    suspend fun claimGeotTokensOnOp(
        pointsToConvert: Int,
        sessionId: Long? = null
    ): OpClaimReceipt? = withContext(Dispatchers.IO) {
        val wallet = getOrCreateWallet()
        if (wallet.balancePoints < pointsToConvert || pointsToConvert <= 0) return@withContext null
        if (wallet.opWalletAddress.isBlank()) return@withContext null

        val geotAmount = pointsToConvert / OpChainConfig.GEOPOINTS_PER_GEOT
        val network = OpNetwork.fromId(wallet.opNetworkId)

        val receipt = opChainService.claimTokensToOp(
            network = network,
            recipientAddress = wallet.opWalletAddress,
            amountGeot = geotAmount,
            referenceTraceId = sessionId
        )

        val updated = wallet.copy(
            balancePoints = wallet.balancePoints - pointsToConvert,
            claimedGeotTokens = wallet.claimedGeotTokens + geotAmount
        )
        tripDao.insertOrUpdateWallet(updated)

        tripDao.insertRewardTransaction(
            RewardTransaction(
                sessionId = sessionId,
                title = "Claimed ${String.format(Locale.US, "%.2f", geotAmount)} \$GEOT on ${network.displayName}",
                points = -pointsToConvert,
                category = "CRYPTO_CLAIM"
            )
        )

        receipt
    }

    // ==========================================
    // STAKING REWARDS & APY ENGINE
    // ==========================================

    suspend fun stakeTokens(poolDef: StakingPoolDef, geotAmount: Double): Boolean = withContext(Dispatchers.IO) {
        val pointsEquivalent = (geotAmount * OpChainConfig.GEOPOINTS_PER_GEOT).toInt()
        val wallet = getOrCreateWallet()
        if (wallet.balancePoints < pointsEquivalent || geotAmount < poolDef.minStakeGeot) {
            return@withContext false
        }

        // Deduct points from wallet
        val updatedWallet = wallet.copy(balancePoints = wallet.balancePoints - pointsEquivalent)
        tripDao.insertOrUpdateWallet(updatedWallet)

        val position = StakedPosition(
            poolId = poolDef.id,
            poolName = poolDef.name,
            stakedAmountGeot = geotAmount,
            stakedPointsEquivalent = pointsEquivalent,
            apyPercent = poolDef.apyPercent,
            traceBoostPercent = poolDef.traceBoostPercent,
            startTimeMillis = System.currentTimeMillis(),
            lockDurationDays = poolDef.lockDays,
            lastHarvestMillis = System.currentTimeMillis(),
            isActive = true
        )
        tripDao.insertStakedPosition(position)

        tripDao.insertRewardTransaction(
            RewardTransaction(
                title = "Staked ${String.format(Locale.US, "%.2f", geotAmount)} \$GEOT in ${poolDef.name}",
                points = -pointsEquivalent,
                category = "STAKING_LOCK"
            )
        )
        true
    }

    suspend fun harvestYield(position: StakedPosition): Double = withContext(Dispatchers.IO) {
        val pendingGeot = position.calculatePendingYieldGeot()
        if (pendingGeot <= 0.0001) return@withContext 0.0

        val yieldPoints = (pendingGeot * OpChainConfig.GEOPOINTS_PER_GEOT).toInt().coerceAtLeast(1)
        val wallet = getOrCreateWallet()
        val updatedWallet = wallet.copy(
            balancePoints = wallet.balancePoints + yieldPoints,
            lifetimePointsEarned = wallet.lifetimePointsEarned + yieldPoints
        )
        tripDao.insertOrUpdateWallet(updatedWallet)

        val updatedPosition = position.copy(
            lastHarvestMillis = System.currentTimeMillis(),
            totalClaimedYieldGeot = position.totalClaimedYieldGeot + pendingGeot
        )
        tripDao.updateStakedPosition(updatedPosition)

        tripDao.insertRewardTransaction(
            RewardTransaction(
                title = "Harvested Yield from ${position.poolName} (+${yieldPoints} pts)",
                points = yieldPoints,
                category = "STAKING_YIELD"
            )
        )
        pendingGeot
    }

    suspend fun harvestAllActiveYield(): Double = withContext(Dispatchers.IO) {
        val active = tripDao.getActiveStakedPositionsSync()
        var totalYield = 0.0
        for (pos in active) {
            totalYield += harvestYield(pos)
        }
        totalYield
    }

    suspend fun unstakeTokens(position: StakedPosition): Boolean = withContext(Dispatchers.IO) {
        if (!position.isLockMatured && position.lockDurationDays > 0) return@withContext false

        // Harvest any pending yield first
        harvestYield(position)

        // Return principal
        val wallet = getOrCreateWallet()
        val updatedWallet = wallet.copy(
            balancePoints = wallet.balancePoints + position.stakedPointsEquivalent
        )
        tripDao.insertOrUpdateWallet(updatedWallet)

        val deactivated = position.copy(isActive = false)
        tripDao.updateStakedPosition(deactivated)

        tripDao.insertRewardTransaction(
            RewardTransaction(
                title = "Unstaked ${String.format(Locale.US, "%.2f", position.stakedAmountGeot)} \$GEOT from ${position.poolName}",
                points = position.stakedPointsEquivalent,
                category = "STAKING_UNSTAKE"
            )
        )
        true
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
        com.example.util.GpxFileUtility.convertToGpx(session, points, waypoints)
    }

    suspend fun exportAllHistoryGpxXml(): String = withContext(Dispatchers.IO) {
        val sessions = tripDao.getAllSessionsSync()
        val dataList = mutableListOf<Triple<TripSession, List<LocationBreadcrumb>, List<WaypointMarker>>>()
        for (sess in sessions) {
            val pts = tripDao.getSessionPointsSync(sess.id)
            val wpts = tripDao.getSessionWaypointsSync(sess.id)
            dataList.add(Triple(sess, pts, wpts))
        }
        com.example.util.GpxFileUtility.convertMultipleSessionsToGpx(dataList)
    }
}

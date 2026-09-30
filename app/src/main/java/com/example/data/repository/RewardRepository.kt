package com.example.data.repository

import com.example.data.dao.RewardDao
import com.example.data.model.RedeemedReward
import com.example.data.model.RewardQuest
import com.example.data.model.UserRewardProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RewardRepository(private val rewardDao: RewardDao) {

    val profileFlow: Flow<UserRewardProfile?> = rewardDao.getProfileFlow()
    val questsFlow: Flow<List<RewardQuest>> = rewardDao.getAllQuestsFlow()
    val redeemedRewardsFlow: Flow<List<RedeemedReward>> = rewardDao.getAllRedeemedRewardsFlow()

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        val existingProfile = rewardDao.getProfileSync()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (existingProfile == null) {
            val initial = UserRewardProfile(
                id = 1,
                totalCoins = 150, // 150 Welcome GeoCoins
                lifetimeCoinsEarned = 150,
                totalDistanceMeters = 0.0,
                totalElevationGainMeters = 0.0,
                streakDays = 1,
                lastActiveDate = today,
                level = 1,
                levelTitle = "Bronze Pathfinder"
            )
            rewardDao.insertOrUpdateProfile(initial)
        } else if (existingProfile.lastActiveDate != today) {
            // Check daily streak
            val nextStreak = existingProfile.streakDays + 1
            rewardDao.insertOrUpdateProfile(
                existingProfile.copy(
                    lastActiveDate = today,
                    streakDays = nextStreak
                )
            )
        }

        val existingQuests = rewardDao.getAllQuestsSync()
        if (existingQuests.isEmpty()) {
            val defaultQuests = listOf(
                RewardQuest(
                    id = "quest_first_trace",
                    title = "First Steps",
                    description = "Trace your first 500 meters outdoors or on demo trail",
                    category = "DISTANCE",
                    targetValue = 500.0,
                    currentValue = 0.0,
                    unit = "m",
                    rewardCoins = 50,
                    iconType = "WALK"
                ),
                RewardQuest(
                    id = "quest_1k_distance",
                    title = "1K Trail Explorer",
                    description = "Accumulate 1.0 km (1,000m) of recorded route traces",
                    category = "DISTANCE",
                    targetValue = 1000.0,
                    currentValue = 0.0,
                    unit = "m",
                    rewardCoins = 100,
                    iconType = "RUN"
                ),
                RewardQuest(
                    id = "quest_3k_trekker",
                    title = "3K Odyssey",
                    description = "Reach 3.0 km (3,000m) of total location traces",
                    category = "DISTANCE",
                    targetValue = 3000.0,
                    currentValue = 0.0,
                    unit = "m",
                    rewardCoins = 250,
                    iconType = "BIKE"
                ),
                RewardQuest(
                    id = "quest_elevation_climber",
                    title = "Summit Seeker",
                    description = "Climb at least 25 meters of cumulative elevation",
                    category = "ELEVATION",
                    targetValue = 25.0,
                    currentValue = 0.0,
                    unit = "m",
                    rewardCoins = 150,
                    iconType = "MOUNTAIN"
                ),
                RewardQuest(
                    id = "quest_waypoint_marker",
                    title = "Cartographer",
                    description = "Drop 2 GPS Waypoints along your active route",
                    category = "WAYPOINT",
                    targetValue = 2.0,
                    currentValue = 0.0,
                    unit = "pins",
                    rewardCoins = 100,
                    iconType = "FLAG"
                ),
                RewardQuest(
                    id = "quest_duration_endurance",
                    title = "Endurance Tracker",
                    description = "Log at least 15 minutes (900s) of active location tracing",
                    category = "TIME",
                    targetValue = 900.0,
                    currentValue = 0.0,
                    unit = "sec",
                    rewardCoins = 200,
                    iconType = "TIMER"
                )
            )
            rewardDao.insertQuests(defaultQuests)
        }
    }

    suspend fun addTraceTelemetryReward(
        distanceDeltaMeters: Double,
        elevationDeltaMeters: Double,
        activeDurationDeltaSec: Long = 0
    ) = withContext(Dispatchers.IO) {
        if (distanceDeltaMeters <= 0 && elevationDeltaMeters <= 0) return@withContext

        // Reward Formula: 1 GeoCoin per 10 meters + 1 GeoCoin per 2 meters of elevation climb
        val distanceCoins = (distanceDeltaMeters / 10.0).toInt()
        val elevationCoins = (elevationDeltaMeters / 2.0).toInt().coerceAtLeast(0)
        val earnedCoins = distanceCoins + elevationCoins

        val profile = rewardDao.getProfileSync() ?: return@withContext
        val newTotalDist = profile.totalDistanceMeters + distanceDeltaMeters
        val newTotalElev = profile.totalElevationGainMeters + elevationDeltaMeters
        val newTotalCoins = profile.totalCoins + earnedCoins
        val newLifetimeCoins = profile.lifetimeCoinsEarned + earnedCoins

        // Calculate Level Tier based on lifetime coins
        val newLevel = when {
            newLifetimeCoins >= 3000 -> 4
            newLifetimeCoins >= 1500 -> 3
            newLifetimeCoins >= 500 -> 2
            else -> 1
        }
        val newTitle = when (newLevel) {
            4 -> "Diamond Ranger"
            3 -> "Gold Trailblazer"
            2 -> "Silver Pathfinder"
            else -> "Bronze Explorer"
        }

        rewardDao.insertOrUpdateProfile(
            profile.copy(
                totalCoins = newTotalCoins,
                lifetimeCoinsEarned = newLifetimeCoins,
                totalDistanceMeters = newTotalDist,
                totalElevationGainMeters = newTotalElev,
                level = newLevel,
                levelTitle = newTitle
            )
        )

        // Update Quest Progress
        val quests = rewardDao.getAllQuestsSync()
        for (q in quests) {
            if (!q.isCompleted) {
                val updatedVal = when (q.category) {
                    "DISTANCE" -> (q.currentValue + distanceDeltaMeters).coerceAtMost(q.targetValue)
                    "ELEVATION" -> (q.currentValue + elevationDeltaMeters).coerceAtMost(q.targetValue)
                    "TIME" -> (q.currentValue + activeDurationDeltaSec).coerceAtMost(q.targetValue)
                    else -> q.currentValue
                }
                val completed = updatedVal >= q.targetValue
                if (updatedVal != q.currentValue || completed) {
                    rewardDao.updateQuest(q.copy(currentValue = updatedVal, isCompleted = completed))
                }
            }
        }
    }

    suspend fun incrementWaypointQuest() = withContext(Dispatchers.IO) {
        val quests = rewardDao.getAllQuestsSync()
        for (q in quests) {
            if (q.category == "WAYPOINT" && !q.isCompleted) {
                val newVal = (q.currentValue + 1.0).coerceAtMost(q.targetValue)
                rewardDao.updateQuest(q.copy(currentValue = newVal, isCompleted = newVal >= q.targetValue))
            }
        }
    }

    suspend fun claimQuest(questId: String): Boolean = withContext(Dispatchers.IO) {
        val quests = rewardDao.getAllQuestsSync()
        val quest = quests.find { it.id == questId } ?: return@withContext false
        if (quest.isCompleted && !quest.isClaimed) {
            rewardDao.markQuestClaimed(questId)
            rewardDao.addCoins(quest.rewardCoins)
            return@withContext true
        }
        false
    }

    suspend fun claimDailyBonus(): Int = withContext(Dispatchers.IO) {
        val bonus = 75
        rewardDao.addCoins(bonus)
        bonus
    }

    suspend fun redeemReward(
        rewardId: String,
        title: String,
        description: String,
        category: String,
        costCoins: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val profile = rewardDao.getProfileSync() ?: return@withContext false
        if (profile.totalCoins >= costCoins) {
            val rows = rewardDao.deductCoins(costCoins)
            if (rows > 0) {
                val code = "GEO-" + UUID.randomUUID().toString().take(8).uppercase()
                rewardDao.insertRedeemedReward(
                    RedeemedReward(
                        rewardId = rewardId,
                        title = title,
                        description = description,
                        category = category,
                        costCoins = costCoins,
                        rewardCode = code
                    )
                )
                return@withContext true
            }
        }
        false
    }
}

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_quests")
data class RewardQuest(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String, // DISTANCE, ELEVATION, TIME, WAYPOINT, DISCOVERY
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val unit: String,
    val rewardCoins: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val iconType: String = "RUN"
)

@Entity(tableName = "redeemed_rewards")
data class RedeemedReward(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rewardId: String,
    val title: String,
    val description: String,
    val category: String,
    val costCoins: Int,
    val redeemedTimestamp: Long = System.currentTimeMillis(),
    val rewardCode: String = ""
)

data class GeoDropCheckpoint(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val rewardCoins: Int = 50,
    val isCollected: Boolean = false
)

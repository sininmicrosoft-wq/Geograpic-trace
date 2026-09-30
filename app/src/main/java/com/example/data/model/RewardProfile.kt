package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_reward_profile")
data class UserRewardProfile(
    @PrimaryKey
    val id: Int = 1,
    val totalCoins: Int = 100, // Welcome bonus 100 coins
    val lifetimeCoinsEarned: Int = 100,
    val totalDistanceMeters: Double = 0.0,
    val totalElevationGainMeters: Double = 0.0,
    val streakDays: Int = 1,
    val lastActiveDate: String = "",
    val level: Int = 1,
    val levelTitle: String = "Bronze Explorer"
)

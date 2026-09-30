package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_wallet")
data class UserRewardWallet(
    @PrimaryKey
    val id: Int = 1,
    val balancePoints: Int = 150, // Starter bonus for new explorers
    val lifetimePointsEarned: Int = 150,
    val currentLevel: Int = 1,
    val levelTitle: String = "Novice Scout",
    val streakDays: Int = 1,
    val lastTraceDate: String = "",
    val unlockedThemes: String = "CYBERPUNK", // Comma-separated theme identifiers
    val activeTheme: String = "CYBERPUNK"
)

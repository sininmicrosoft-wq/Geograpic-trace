package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_transactions")
data class RewardTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long? = null,
    val title: String,
    val points: Int,
    val category: String, // "TRACE_DISTANCE", "ELEVATION", "WAYPOINT", "DAILY_QUEST", "PURCHASE"
    val timestampMillis: Long = System.currentTimeMillis()
)

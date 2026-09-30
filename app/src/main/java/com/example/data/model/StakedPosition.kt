package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staked_positions")
data class StakedPosition(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val poolId: String, // "FLEXIBLE", "SILVER_30D", "GOLD_90D", "DIAMOND_180D"
    val poolName: String,
    val stakedAmountGeot: Double,
    val stakedPointsEquivalent: Int,
    val apyPercent: Double,
    val traceBoostPercent: Int, // e.g., 5, 15, 30, 50
    val startTimeMillis: Long = System.currentTimeMillis(),
    val lockDurationDays: Int, // 0 for flexible
    val lastHarvestMillis: Long = System.currentTimeMillis(),
    val totalClaimedYieldGeot: Double = 0.0,
    val isActive: Boolean = true
) {
    val lockEndTimeMillis: Long
        get() = startTimeMillis + (lockDurationDays * 24L * 60L * 60L * 1000L)

    val isLockMatured: Boolean
        get() = lockDurationDays == 0 || System.currentTimeMillis() >= lockEndTimeMillis

    /**
     * Calculates pending yield accrued since last harvest.
     * Yield = Staked * (APY / 100) * (ElapsedSeconds / 31536000)
     */
    fun calculatePendingYieldGeot(): Double {
        if (!isActive || stakedAmountGeot <= 0) return 0.0
        val now = System.currentTimeMillis()
        val effectiveEnd = if (lockDurationDays > 0) minOf(now, lockEndTimeMillis) else now
        val elapsedMillis = (effectiveEnd - lastHarvestMillis).coerceAtLeast(0L)
        val elapsedYears = elapsedMillis.toDouble() / (365.0 * 24.0 * 3600.0 * 1000.0)
        return stakedAmountGeot * (apyPercent / 100.0) * elapsedYears
    }
}

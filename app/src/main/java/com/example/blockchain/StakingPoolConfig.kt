package com.example.blockchain

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen

data class StakingPoolDef(
    val id: String,
    val name: String,
    val lockDays: Int,
    val apyPercent: Double,
    val traceBoostPercent: Int,
    val minStakeGeot: Double,
    val description: String,
    val accentColor: Color
)

object StakingPoolConfig {

    val POOLS = listOf(
        StakingPoolDef(
            id = "FLEXIBLE",
            name = "Flexible Trail Pool",
            lockDays = 0,
            apyPercent = 8.0,
            traceBoostPercent = 5,
            minStakeGeot = 1.0,
            description = "No lockup. Unstake anytime. Steady 8% APY with +5% GPS trace boost.",
            accentColor = CyanNeon
        ),
        StakingPoolDef(
            id = "SILVER_30D",
            name = "Silver Explorer Vault",
            lockDays = 30,
            apyPercent = 18.0,
            traceBoostPercent = 15,
            minStakeGeot = 5.0,
            description = "30-day lock. 18% APY yield + 15% bonus GeoPoints on all recorded routes.",
            accentColor = Color(0xFF94A3B8)
        ),
        StakingPoolDef(
            id = "GOLD_90D",
            name = "Gold Pathfinder Vault",
            lockDays = 90,
            apyPercent = 35.0,
            traceBoostPercent = 30,
            minStakeGeot = 20.0,
            description = "90-day lock. 35% APY yield + powerful +30% GPS stamina reward multiplier.",
            accentColor = AmberAccent
        ),
        StakingPoolDef(
            id = "DIAMOND_180D",
            name = "Diamond Summit Vault",
            lockDays = 180,
            apyPercent = 65.0,
            traceBoostPercent = 50,
            minStakeGeot = 50.0,
            description = "180-day lock. Maximum 65% APY + massive +50% outdoor move-to-earn boost.",
            accentColor = Color(0xFFFF0420)
        )
    )

    fun getPoolById(id: String): StakingPoolDef {
        return POOLS.find { it.id.equals(id, ignoreCase = true) } ?: POOLS.first()
    }
}

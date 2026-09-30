package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GeoTraceApplication
import com.example.data.model.RewardTransaction
import com.example.data.model.UserRewardWallet
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MapThemeItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val previewColor: Color
)

val availableMapThemes = listOf(
    MapThemeItem("CYBERPUNK", "Cyberpunk Neon", "High-contrast electric cyan & laser trail line", 0, CyanNeon),
    MapThemeItem("TACTICAL", "Tactical Stealth", "Night ops dark slate with amber trace accents", 250, AmberAccent),
    MapThemeItem("EMERALD", "Emerald Forest", "Lush topographic greens & nature path glow", 400, EmeraldGreen),
    MapThemeItem("SOLAR", "Solar Flare", "Radiant gold & blazing dusk trail gradient", 600, Color(0xFFF97316))
)

data class QuestItem(
    val id: String,
    val title: String,
    val requirement: String,
    val rewardPoints: Int,
    val icon: ImageVector,
    val progressFraction: Float,
    val isCompleted: Boolean
)

@Composable
fun RewardsScreen() {
    val context = LocalContext.current
    val repository = remember { GeoTraceApplication.instance.tripRepository }
    val walletState by repository.rewardWallet.collectAsState(initial = null)
    val transactions by repository.rewardTransactions.collectAsState(initial = emptyList())
    val allSessions by repository.allSessions.collectAsState(initial = emptyList())
    val totalDistance by repository.totalDistanceMeters.collectAsState(initial = 0.0)
    val totalElevation by repository.totalElevationGain.collectAsState(initial = 0.0)

    val wallet = walletState ?: UserRewardWallet()
    val scope = rememberCoroutineScope()

    val totalDistKm = (totalDistance ?: 0.0) / 1000.0
    val totalElevM = totalElevation ?: 0.0

    // Dynamic Quest evaluations
    val quests = remember(allSessions, totalDistance, totalElevation) {
        listOf(
            QuestItem(
                id = "QUEST_FIRST_TRACE",
                title = "Pathfinder Scout",
                requirement = "Complete your first location trace",
                rewardPoints = 100,
                icon = Icons.Default.Route,
                progressFraction = if (allSessions.isNotEmpty()) 1.0f else 0.0f,
                isCompleted = allSessions.isNotEmpty()
            ),
            QuestItem(
                id = "QUEST_DISTANCE_3K",
                title = "Distance Explorer",
                requirement = "Trace at least 3.0 km of cumulative routes",
                rewardPoints = 200,
                icon = Icons.Default.MilitaryTech,
                progressFraction = (totalDistKm / 3.0).toFloat().coerceIn(0f, 1f),
                isCompleted = totalDistKm >= 3.0
            ),
            QuestItem(
                id = "QUEST_ELEVATION_30M",
                title = "Summit Seeker",
                requirement = "Gain 30+ meters of elevation",
                rewardPoints = 150,
                icon = Icons.Default.Terrain,
                progressFraction = (totalElevM / 30.0).toFloat().coerceIn(0f, 1f),
                isCompleted = totalElevM >= 30.0
            )
        )
    }

    // Points required for next level
    val nextLevelTarget = when (wallet.currentLevel) {
        1 -> 400
        2 -> 1000
        3 -> 2000
        4 -> 3500
        else -> 5000
    }
    val levelProgress = (wallet.lifetimePointsEarned.toFloat() / nextLevelTarget).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("rewards_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Surface(
                color = Slate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trace & Earn Rewards",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Wallet Hero Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AVAILABLE BALANCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400,
                                    letterSpacing = 1.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = AmberAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${wallet.balancePoints}",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AmberAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GeoPoints",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate400,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }

                            // Streak badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate800,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${wallet.streakDays}d Streak",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Level & Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Level ${wallet.currentLevel} • ${wallet.levelTitle}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "${wallet.lifetimePointsEarned} / $nextLevelTarget XP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate400,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { levelProgress },
                            color = EmeraldGreen,
                            trackColor = Slate800,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    }
                }

                // Daily & Weekly Quests
                Text(
                    text = "GPS LOCATION QUESTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                quests.forEach { quest ->
                    QuestCard(
                        quest = quest,
                        onClaim = {
                            scope.launch {
                                repository.claimQuestReward(quest.title, quest.rewardPoints)
                                Toast.makeText(context, "+${quest.rewardPoints} GeoPoints Claimed!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Rewards Vault: Custom Map Themes
                Text(
                    text = "MAP THEMES VAULT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                val unlockedSet = remember(wallet.unlockedThemes) {
                    wallet.unlockedThemes.split(",").map { it.trim() }.toSet()
                }

                availableMapThemes.forEach { theme ->
                    val isUnlocked = unlockedSet.contains(theme.id)
                    val isActive = wallet.activeTheme == theme.id

                    ThemeStoreCard(
                        theme = theme,
                        isUnlocked = isUnlocked,
                        isActive = isActive,
                        canAfford = wallet.balancePoints >= theme.cost,
                        onUnlock = {
                            scope.launch {
                                val success = repository.redeemStoreTheme(theme.id, theme.cost)
                                if (success) {
                                    Toast.makeText(context, "Unlocked ${theme.name}!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Not enough GeoPoints", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onEquip = {
                            scope.launch {
                                repository.setActiveTheme(theme.id)
                                Toast.makeText(context, "Equipped ${theme.name}!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Recent Reward History Transactions
                if (transactions.isNotEmpty()) {
                    Text(
                        text = "RECENT REWARD TRANSACTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp
                    )

                    transactions.take(8).forEach { tx ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = tx.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(tx.timestampMillis))
                                    Text(
                                        text = dateStr,
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                }

                                Text(
                                    text = if (tx.points >= 0) "+${tx.points}" else "${tx.points}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (tx.points >= 0) AmberAccent else Color(0xFFEF4444),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun QuestCard(
    quest: QuestItem,
    onClaim: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (quest.isCompleted) AmberAccent.copy(alpha = 0.2f) else Slate800,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        quest.icon,
                        contentDescription = null,
                        tint = if (quest.isCompleted) AmberAccent else Slate400,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = quest.requirement,
                    fontSize = 11.sp,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { quest.progressFraction },
                    color = AmberAccent,
                    trackColor = Slate800,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (quest.isCompleted) {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Slate950),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("claim_quest_${quest.id}")
                ) {
                    Text("+${quest.rewardPoints}", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800
                ) {
                    Text(
                        text = "+${quest.rewardPoints} pts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeStoreCard(
    theme: MapThemeItem,
    isUnlocked: Boolean,
    isActive: Boolean,
    canAfford: Boolean,
    onUnlock: () -> Unit,
    onEquip: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, theme.previewColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = theme.previewColor.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(2.dp, theme.previewColor),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = theme.previewColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = theme.description,
                    fontSize = 11.sp,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isActive) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = theme.previewColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.previewColor)
                ) {
                    Text(
                        text = "EQUIPPED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.previewColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            } else if (isUnlocked) {
                OutlinedButton(
                    onClick = onEquip,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Equip", fontSize = 11.sp)
                }
            } else {
                Button(
                    onClick = onUnlock,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = Slate950,
                        disabledContainerColor = Slate800,
                        disabledContentColor = Slate400
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("unlock_theme_${theme.id}")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${theme.cost} pts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

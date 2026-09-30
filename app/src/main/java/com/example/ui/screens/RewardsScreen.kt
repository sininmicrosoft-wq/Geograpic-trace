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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GeoTraceApplication
import com.example.blockchain.OpChainConfig
import com.example.blockchain.OpClaimReceipt
import com.example.blockchain.OpNetwork
import com.example.blockchain.StakingPoolConfig
import com.example.blockchain.StakingPoolDef
import com.example.data.model.RewardTransaction
import com.example.data.model.StakedPosition
import com.example.data.model.UserRewardWallet
import com.example.ui.components.OpClaimSuccessDialog
import com.example.ui.components.OpContractViewerDialog
import com.example.ui.components.OpWalletConfigDialog
import com.example.ui.components.StakeTokensDialog
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
    val clipboardManager = LocalClipboardManager.current
    val repository = remember { GeoTraceApplication.instance.tripRepository }
    val walletState by repository.rewardWallet.collectAsState(initial = null)
    val transactions by repository.rewardTransactions.collectAsState(initial = emptyList())
    val allSessions by repository.allSessions.collectAsState(initial = emptyList())
    val totalDistance by repository.totalDistanceMeters.collectAsState(initial = 0.0)
    val totalElevation by repository.totalElevationGain.collectAsState(initial = 0.0)

    val wallet = walletState ?: UserRewardWallet()
    val scope = rememberCoroutineScope()

    // OP Chain dialog states
    var showContractDialog by remember { mutableStateOf(false) }
    var showWalletConfigDialog by remember { mutableStateOf(false) }
    var claimReceiptToShow by remember { mutableStateOf<OpClaimReceipt?>(null) }
    var poolToStake by remember { mutableStateOf<StakingPoolDef?>(null) }

    val activeStakes by repository.activeStakes.collectAsState(initial = emptyList())
    val allStakes by repository.allStakes.collectAsState(initial = emptyList())
    val totalStakedGeot = remember(activeStakes) { activeStakes.sumOf { it.stakedAmountGeot } }
    val maxStakingBoost = remember(activeStakes) { activeStakes.maxOfOrNull { it.traceBoostPercent } ?: 0 }
    val totalPendingYieldGeot = remember(activeStakes) { activeStakes.sumOf { it.calculatePendingYieldGeot() } }

    // Live On-Chain status
    var onChainBalance by remember { mutableDoubleStateOf(0.0) }
    var isRpcConnected by remember { mutableStateOf(true) }
    var isLoadingBalance by remember { mutableStateOf(false) }

    val activeNetwork = remember(wallet.opNetworkId) {
        OpNetwork.fromId(wallet.opNetworkId)
    }

    LaunchedEffect(wallet.opWalletAddress, wallet.opNetworkId, wallet.opTokenContractAddress) {
        if (wallet.opWalletAddress.isNotBlank()) {
            isLoadingBalance = true
            isRpcConnected = repository.opChainService.checkRpcConnection(activeNetwork)
            onChainBalance = repository.opChainService.getGeotTokenBalance(
                network = activeNetwork,
                contractAddress = wallet.opTokenContractAddress,
                walletAddress = wallet.opWalletAddress
            )
            isLoadingBalance = false
        }
    }

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
                // In-App Points Wallet Card
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

                        Spacer(modifier = Modifier.height(14.dp))

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

                // ==========================================
                // OPTIMISM (OP CHAIN) $GEOT TOKEN REWARDS HUB
                // ==========================================
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF0420).copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("op_chain_rewards_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // OP Chain Header Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF0420),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("OP", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "OP CHAIN REWARDS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "${activeNetwork.displayName} • ${OpChainConfig.TOKEN_SYMBOL}",
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                }
                            }

                            // Wallet / Network Settings button
                            OutlinedButton(
                                onClick = { showWalletConfigDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF0420)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF0420).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("open_op_wallet_config_button")
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (wallet.opWalletAddress.isBlank()) "Connect" else "Manage",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // On-Chain Token Balances Readout
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Slate950,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "ON-CHAIN ${OpChainConfig.TOKEN_SYMBOL} BALANCE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate400,
                                            letterSpacing = 1.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isLoadingBalance) {
                                                CircularProgressIndicator(
                                                    color = Color(0xFFFF0420),
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = if (wallet.opWalletAddress.isBlank()) "0.00" else String.format(Locale.US, "%.2f", onChainBalance),
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFFF0420),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = OpChainConfig.TOKEN_SYMBOL,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "TOTAL CLAIMED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", wallet.claimedGeotTokens)} ${OpChainConfig.TOKEN_SYMBOL}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = if (isRpcConnected) "● RPC Online" else "○ RPC Offline",
                                            fontSize = 10.sp,
                                            color = if (isRpcConnected) EmeraldGreen else Color(0xFFEF4444)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Connected Address Pill
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate900, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (wallet.opWalletAddress.isBlank()) "No wallet connected" else OpChainConfig.formatAddress(wallet.opWalletAddress),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (wallet.opWalletAddress.isBlank()) Slate400 else CyanLight
                                    )

                                    if (wallet.opWalletAddress.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(wallet.opWalletAddress))
                                                Toast.makeText(context, "Address copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanNeon, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Copy", fontSize = 10.sp, color = CyanNeon, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Convert GeoPoints to $GEOT on OP Chain
                        Text(
                            text = "MINT / CONVERT TO ${OpChainConfig.TOKEN_SYMBOL} ON OP CHAIN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Rate: 100 GeoPoints = 1.0 ${OpChainConfig.TOKEN_SYMBOL} • Gas optimized on Optimism",
                            fontSize = 11.sp,
                            color = Slate400
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Convert 100 pts -> 1 $GEOT
                            Button(
                                onClick = {
                                    if (wallet.opWalletAddress.isBlank()) {
                                        showWalletConfigDialog = true
                                        Toast.makeText(context, "Please configure your OP wallet address first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        scope.launch {
                                            val receipt = repository.claimGeotTokensOnOp(100)
                                            if (receipt != null) {
                                                claimReceiptToShow = receipt
                                            } else {
                                                Toast.makeText(context, "Insufficient GeoPoints", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = wallet.balancePoints >= 100,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF0420),
                                    contentColor = Color.White,
                                    disabledContainerColor = Slate800,
                                    disabledContentColor = Slate400
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("claim_1_geot_button")
                            ) {
                                Text("1 ${OpChainConfig.TOKEN_SYMBOL}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Convert 500 pts -> 5 $GEOT
                            Button(
                                onClick = {
                                    if (wallet.opWalletAddress.isBlank()) {
                                        showWalletConfigDialog = true
                                        Toast.makeText(context, "Please configure your OP wallet address first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        scope.launch {
                                            val receipt = repository.claimGeotTokensOnOp(500)
                                            if (receipt != null) {
                                                claimReceiptToShow = receipt
                                            } else {
                                                Toast.makeText(context, "Insufficient GeoPoints", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = wallet.balancePoints >= 500,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF0420),
                                    contentColor = Color.White,
                                    disabledContainerColor = Slate800,
                                    disabledContentColor = Slate400
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("claim_5_geot_button")
                            ) {
                                Text("5 ${OpChainConfig.TOKEN_SYMBOL}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Convert Max
                            val maxConvertiblePoints = (wallet.balancePoints / 100) * 100
                            Button(
                                onClick = {
                                    if (wallet.opWalletAddress.isBlank()) {
                                        showWalletConfigDialog = true
                                        Toast.makeText(context, "Please configure your OP wallet address first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        scope.launch {
                                            val receipt = repository.claimGeotTokensOnOp(maxConvertiblePoints)
                                            if (receipt != null) {
                                                claimReceiptToShow = receipt
                                            } else {
                                                Toast.makeText(context, "Insufficient GeoPoints", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = maxConvertiblePoints >= 100,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Slate800,
                                    contentColor = Color.White,
                                    disabledContainerColor = Slate950,
                                    disabledContentColor = Slate400
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF0420).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("claim_max_geot_button")
                            ) {
                                Text("Convert Max", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Smart Contract Viewer action
                        OutlinedButton(
                            onClick = { showContractDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("view_solidity_contract_button")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFFF0420), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View & Deploy OP Smart Contracts (.sol)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ==========================================
                // STAKING REWARDS VAULTS (8% - 65% APY)
                // ==========================================
                Text(
                    text = "STAKING REWARDS VAULTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                // Staking Summary Hero Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staking_summary_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL STAKED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", totalStakedGeot)} ${OpChainConfig.TOKEN_SYMBOL}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (maxStakingBoost > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AmberAccent.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "+${maxStakingBoost}% GPS Boost", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Harvestable yield row
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate950,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "HARVESTABLE YIELD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                                    Text(
                                        text = "+${String.format(Locale.US, "%.4f", totalPendingYieldGeot)} ${OpChainConfig.TOKEN_SYMBOL}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val harvested = repository.harvestAllActiveYield()
                                            if (harvested > 0) {
                                                Toast.makeText(context, "Harvested +${String.format(Locale.US, "%.4f", harvested)} \$GEOT yield!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No yield available yet to harvest", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = totalPendingYieldGeot > 0.0001,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        contentColor = Slate950,
                                        disabledContainerColor = Slate800,
                                        disabledContentColor = Slate400
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("harvest_all_yield_button")
                                ) {
                                    Text("Harvest All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Active Staking Positions (if any)
                if (activeStakes.isNotEmpty()) {
                    Text(
                        text = "ACTIVE STAKING POSITIONS (${activeStakes.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp
                    )

                    activeStakes.forEach { pos ->
                        ActiveStakeCard(
                            position = pos,
                            onHarvest = {
                                scope.launch {
                                    val harvested = repository.harvestYield(pos)
                                    Toast.makeText(context, "Harvested +${String.format(Locale.US, "%.4f", harvested)} \$GEOT!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onUnstake = {
                                scope.launch {
                                    val ok = repository.unstakeTokens(pos)
                                    if (ok) {
                                        Toast.makeText(context, "Unstaked ${String.format(Locale.US, "%.2f", pos.stakedAmountGeot)} \$GEOT successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Lock period not matured yet", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }

                // Available Staking Pools List
                Text(
                    text = "EXPLORE STAKING VAULTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                StakingPoolConfig.POOLS.forEach { pool ->
                    StakingPoolCard(
                        pool = pool,
                        onStakeClick = { poolToStake = pool }
                    )
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
                                    color = if (tx.points >= 0) AmberAccent else Color(0xFFFF0420),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Dialogs
        if (showContractDialog) {
            OpContractViewerDialog(onDismiss = { showContractDialog = false })
        }

        if (showWalletConfigDialog) {
            OpWalletConfigDialog(
                initialAddress = wallet.opWalletAddress,
                initialNetworkId = wallet.opNetworkId,
                initialContractAddress = wallet.opTokenContractAddress,
                onDismiss = { showWalletConfigDialog = false },
                onSave = { address, networkId, contractAddr ->
                    scope.launch {
                        repository.updateOpWalletConfig(address, networkId, contractAddr)
                        showWalletConfigDialog = false
                    }
                }
            )
        }

        claimReceiptToShow?.let { receipt ->
            OpClaimSuccessDialog(
                receipt = receipt,
                onDismiss = { claimReceiptToShow = null }
            )
        }

        poolToStake?.let { pool ->
            StakeTokensDialog(
                pool = pool,
                availableBalancePoints = wallet.balancePoints,
                onDismiss = { poolToStake = null },
                onConfirmStake = { amountGeot ->
                    scope.launch {
                        val success = repository.stakeTokens(pool, amountGeot)
                        if (success) {
                            Toast.makeText(context, "Successfully staked ${amountGeot} \$GEOT!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Staking failed. Insufficient balance.", Toast.LENGTH_SHORT).show()
                        }
                        poolToStake = null
                    }
                }
            )
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

@Composable
private fun ActiveStakeCard(
    position: StakedPosition,
    onHarvest: () -> Unit,
    onUnstake: () -> Unit
) {
    val pendingYield = remember(position) { position.calculatePendingYieldGeot() }
    val daysRemaining = remember(position) {
        val remMillis = position.lockEndTimeMillis - System.currentTimeMillis()
        if (remMillis <= 0) 0 else (remMillis / (24L * 3600L * 1000L)).toInt() + 1
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_stake_card_${position.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = position.poolName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${position.apyPercent}% APY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+${position.traceBoostPercent}% GPS Boost",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberAccent
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", position.stakedAmountGeot)} ${OpChainConfig.TOKEN_SYMBOL}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (position.lockDurationDays == 0) "Flexible" else if (position.isLockMatured) "Lock Matured" else "$daysRemaining days left",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (position.isLockMatured) EmeraldGreen else Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate950,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "ACCRUED YIELD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text(
                            text = "+${String.format(Locale.US, "%.4f", pendingYield)} ${OpChainConfig.TOKEN_SYMBOL}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onHarvest,
                            enabled = pendingYield > 0.0001,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Slate950,
                                disabledContainerColor = Slate800,
                                disabledContentColor = Slate400
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("harvest_button_${position.id}")
                        ) {
                            Text("Harvest", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onUnstake,
                            enabled = position.isLockMatured,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                                disabledContentColor = Slate800
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("unstake_button_${position.id}")
                        ) {
                            Text("Unstake", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StakingPoolCard(
    pool: StakingPoolDef,
    onStakeClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, pool.accentColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staking_pool_${pool.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = pool.accentColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                tint = pool.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = pool.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (pool.lockDays == 0) "Flexible • Unstake anytime" else "${pool.lockDays}-Day Lockup",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldGreen.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "${pool.apyPercent}% APY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pool.description,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${pool.traceBoostPercent}% GPS Trace Boost",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }

                Button(
                    onClick = onStakeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pool.accentColor,
                        contentColor = if (pool.accentColor == AmberAccent || pool.accentColor == CyanNeon) Slate950 else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("stake_pool_button_${pool.id}")
                ) {
                    Text("Stake Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

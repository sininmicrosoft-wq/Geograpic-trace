package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blockchain.OpChainConfig
import com.example.blockchain.StakingPoolDef
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.util.Locale

@Composable
fun StakeTokensDialog(
    pool: StakingPoolDef,
    availableBalancePoints: Int,
    onDismiss: () -> Unit,
    onConfirmStake: (amountGeot: Double) -> Unit
) {
    val context = LocalContext.current
    val maxAvailableGeot = (availableBalancePoints / OpChainConfig.GEOPOINTS_PER_GEOT)
    var amountInput by remember { mutableStateOf(pool.minStakeGeot.toString()) }

    val amountDouble = amountInput.toDoubleOrNull() ?: 0.0
    val pointsRequired = (amountDouble * OpChainConfig.GEOPOINTS_PER_GEOT).toInt()
    val canStake = amountDouble >= pool.minStakeGeot && pointsRequired <= availableBalancePoints && amountDouble > 0

    // APY calculations
    val estimatedAnnualGeot = amountDouble * (pool.apyPercent / 100.0)
    val estimatedDailyGeot = estimatedAnnualGeot / 365.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = pool.accentColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            tint = pool.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Stake in ${pool.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${pool.apyPercent}% APY • ${if (pool.lockDays == 0) "Flexible (No lock)" else "${pool.lockDays}-day lock"}",
                        fontSize = 11.sp,
                        color = pool.accentColor
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Staking Yield & Boost Highlights Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate950),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, pool.accentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "ANNUAL YIELD (APY):", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Bold)
                            Text(text = "${pool.apyPercent}% APY", fontSize = 13.sp, fontWeight = FontWeight.Black, color = EmeraldGreen, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "GPS TRACE BOOST:", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "+${pool.traceBoostPercent}% Stamina", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "LOCK DURATION:", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (pool.lockDays == 0) "Flexible / Any time" else "${pool.lockDays} Days",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Amount Input
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STAKE AMOUNT (${OpChainConfig.TOKEN_SYMBOL})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Avail: ${String.format(Locale.US, "%.1f", maxAvailableGeot)} \$GEOT",
                            fontSize = 11.sp,
                            color = CyanLight,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stake_amount_input"),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pool.accentColor,
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        trailingIcon = {
                            Text(
                                text = OpChainConfig.TOKEN_SYMBOL,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = pool.accentColor,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )
                }

                // Quick Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Min" to pool.minStakeGeot,
                        "25%" to (maxAvailableGeot * 0.25),
                        "50%" to (maxAvailableGeot * 0.50),
                        "Max" to maxAvailableGeot
                    ).forEach { (label, value) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate800,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val safeVal = String.format(Locale.US, "%.1f", maxOf(pool.minStakeGeot, value))
                                    amountInput = safeVal
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Projected Returns
                if (amountDouble > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate950, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Projected Daily Yield:", fontSize = 11.sp, color = Slate400)
                            Text(
                                text = "+${String.format(Locale.US, "%.4f", estimatedDailyGeot)} \$GEOT/day",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Projected Annual Yield:", fontSize = 11.sp, color = Slate400)
                            Text(
                                text = "+${String.format(Locale.US, "%.2f", estimatedAnnualGeot)} \$GEOT/yr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountDouble < pool.minStakeGeot) {
                        Toast.makeText(context, "Minimum stake is ${pool.minStakeGeot} \$GEOT", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (pointsRequired > availableBalancePoints) {
                        Toast.makeText(context, "Insufficient GeoPoints", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onConfirmStake(amountDouble)
                },
                enabled = canStake,
                colors = ButtonDefaults.buttonColors(
                    containerColor = pool.accentColor,
                    contentColor = if (pool.accentColor == AmberAccent || pool.accentColor == CyanNeon) Slate950 else Color.White,
                    disabledContainerColor = Slate800,
                    disabledContentColor = Slate400
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_stake_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stake & Lock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

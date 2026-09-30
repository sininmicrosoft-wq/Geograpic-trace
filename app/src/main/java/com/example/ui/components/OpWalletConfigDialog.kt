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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blockchain.OpChainConfig
import com.example.blockchain.OpNetwork
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun OpWalletConfigDialog(
    initialAddress: String,
    initialNetworkId: String,
    initialContractAddress: String,
    onDismiss: () -> Unit,
    onSave: (address: String, networkId: String, contractAddress: String) -> Unit
) {
    val context = LocalContext.current
    var walletAddress by remember { mutableStateOf(initialAddress) }
    var selectedNetwork by remember { mutableStateOf(OpNetwork.fromId(initialNetworkId)) }
    var contractAddress by remember {
        mutableStateOf(
            if (initialContractAddress.isNotBlank()) initialContractAddress else OpChainConfig.DEFAULT_OP_SEPOLIA_CONTRACT
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF0420).copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFFFF0420),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "OP Chain Reward Wallet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Configure Optimism address & network",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wallet Address Field
                Column {
                    Text(
                        text = "YOUR OP WALLET ADDRESS (EVM)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = walletAddress,
                        onValueChange = { walletAddress = it },
                        placeholder = { Text("0x...", color = Slate400, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("op_wallet_address_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF0420),
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                }

                // Network Selector
                Column {
                    Text(
                        text = "SELECT OPTIMISM NETWORK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OpNetwork.entries.forEach { network ->
                            val isSelected = selectedNetwork == network
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFF0420).copy(alpha = 0.2f) else Slate950
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF0420) else Slate800
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedNetwork = network }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = network.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Slate400
                                    )
                                    Text(
                                        text = "ID: ${network.chainId}",
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color(0xFFFF0420) else Slate400,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Token Contract Address
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${OpChainConfig.TOKEN_SYMBOL} TOKEN CONTRACT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Reset Default",
                            fontSize = 10.sp,
                            color = CyanNeon,
                            modifier = Modifier.clickable {
                                contractAddress = if (selectedNetwork == OpNetwork.OP_MAINNET) {
                                    OpChainConfig.DEFAULT_OP_MAINNET_CONTRACT
                                } else {
                                    OpChainConfig.DEFAULT_OP_SEPOLIA_CONTRACT
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = contractAddress,
                        onValueChange = { contractAddress = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("op_contract_address_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF0420),
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val addr = walletAddress.trim()
                    if (addr.isNotEmpty() && (!addr.startsWith("0x") || addr.length != 42)) {
                        Toast.makeText(context, "Please enter a valid 0x EVM address (42 chars)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSave(addr, selectedNetwork.id, contractAddress.trim())
                    Toast.makeText(context, "OP Wallet configuration saved!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0420),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_op_wallet_button")
            ) {
                Text("Save Configuration", fontWeight = FontWeight.Bold)
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

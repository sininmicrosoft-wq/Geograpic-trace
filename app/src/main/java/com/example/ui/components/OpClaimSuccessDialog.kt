package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blockchain.OpChainConfig
import com.example.blockchain.OpClaimReceipt
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun OpClaimSuccessDialog(
    receipt: OpClaimReceipt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF0420).copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Token,
                            contentDescription = null,
                            tint = Color(0xFFFF0420),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "CLAIMED TO OP CHAIN!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "${receipt.network.displayName} (Chain ID ${receipt.network.chainId})",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Token Amount Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate950,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF0420).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ON-CHAIN TOKENS MINTED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+${String.format(java.util.Locale.US, "%.2f", receipt.amountGeot)} ${OpChainConfig.TOKEN_SYMBOL}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF0420),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Transaction details box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate800.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recipient Address
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Recipient:", fontSize = 11.sp, color = Slate400)
                        Text(
                            text = OpChainConfig.formatAddress(receipt.recipientAddress),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Block Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "OP Block:", fontSize = 11.sp, color = Slate400)
                        Text(
                            text = "#${receipt.blockNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Tx Hash
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Transaction Hash:", fontSize = 11.sp, color = Slate400)
                            Row(
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(receipt.txHash))
                                    Toast.makeText(context, "Tx Hash copied!", Toast.LENGTH_SHORT).show()
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = OpChainConfig.formatAddress(receipt.txHash),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Hash",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(receipt.explorerUrl))
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        clipboardManager.setText(AnnotatedString(receipt.explorerUrl))
                        Toast.makeText(context, "Explorer URL copied!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0420),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("view_op_explorer_button")
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("View on Explorer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done")
            }
        }
    )
}

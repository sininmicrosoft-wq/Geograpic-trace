package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.blockchain.OpContractTemplate
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun OpContractViewerDialog(
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    tint = Color(0xFFFF0420), // Optimism Red Brand Color
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Optimism ${OpChainConfig.TOKEN_SYMBOL} Smart Contract",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ERC-20 Token on OP Chain • 100M Cap",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                // Tab Row: Contract Code vs Deploy Guide
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Slate950,
                    contentColor = CyanNeon,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFFF0420)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Token (.sol)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Staking Vault (.sol)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Deploy Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Slate950, RoundedCornerShape(10.dp))
                        .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> {
                            Text(
                                text = OpContractTemplate.TOKEN_SOLIDITY_CODE,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = CyanLight
                            )
                        }
                        1 -> {
                            Text(
                                text = OpContractTemplate.STAKING_SOLIDITY_CODE,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = AmberAccent
                            )
                        }
                        else -> {
                            Text(
                                text = OpContractTemplate.DEPLOYMENT_GUIDE.trimIndent(),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val textToCopy = when (selectedTab) {
                        0 -> OpContractTemplate.TOKEN_SOLIDITY_CODE
                        1 -> OpContractTemplate.STAKING_SOLIDITY_CODE
                        else -> OpContractTemplate.DEPLOYMENT_GUIDE
                    }
                    clipboardManager.setText(AnnotatedString(textToCopy))
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0420),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("copy_contract_code_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy to Clipboard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close", fontSize = 12.sp)
            }
        }
    )
}

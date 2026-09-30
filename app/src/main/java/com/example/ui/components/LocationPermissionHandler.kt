package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Manages runtime location and notification permissions using Accompanist Permissions.
 * Ensures the app accesses GPS data securely and provides clear privacy rationale,
 * permission request triggers, and system settings navigation if permanently denied.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    modifier: Modifier = Modifier,
    showRationaleBanner: Boolean = true,
    content: @Composable (permissionsState: MultiplePermissionsState, launchRequest: () -> Unit) -> Unit
) {
    val context = LocalContext.current

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasRequestedOnce by remember { mutableStateOf(false) }

    val fineLocationGranted = permissionsState.permissions
        .find { it.permission == Manifest.permission.ACCESS_FINE_LOCATION }
        ?.status?.isGranted == true

    val coarseLocationGranted = permissionsState.permissions
        .find { it.permission == Manifest.permission.ACCESS_COARSE_LOCATION }
        ?.status?.isGranted == true

    val hasAnyLocationPermission = fineLocationGranted || coarseLocationGranted

    fun triggerPermissionRequest() {
        hasRequestedOnce = true
        if (permissionsState.shouldShowRationale) {
            permissionsState.launchMultiplePermissionRequest()
        } else if (!hasAnyLocationPermission && hasRequestedOnce) {
            // Likely permanently denied or first time
            permissionsState.launchMultiplePermissionRequest()
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Rationale / Status banner if permission is not fully granted
        if (showRationaleBanner && !fineLocationGranted) {
            LocationPermissionBanner(
                hasCoarseOnly = coarseLocationGranted && !fineLocationGranted,
                shouldShowRationale = permissionsState.shouldShowRationale,
                onRequestPermission = { triggerPermissionRequest() },
                onOpenSettings = {
                    openAppSettings(context)
                }
            )
        }

        // Child composable receives the permissions state and request launcher
        content(permissionsState) {
            if (!hasAnyLocationPermission && hasRequestedOnce && !permissionsState.shouldShowRationale) {
                showSettingsDialog = true
            } else {
                triggerPermissionRequest()
            }
        }
    }

    // Permanent Denial Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = Slate900,
            icon = {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Location Permission Required",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Precise location access is disabled in Android system settings.",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "To trace routes, record speed, elevation, and generate GPX exports, please enable \"Precise Location\" in App Info > Permissions.",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Privacy guarantee: Your GPS telemetry is saved only on this device.",
                            color = EmeraldGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        openAppSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeon,
                        contentColor = Slate900
                    ),
                    modifier = Modifier.testTag("open_settings_button")
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Not Now", color = Slate400)
                }
            }
        )
    }
}

@Composable
private fun LocationPermissionBanner(
    hasCoarseOnly: Boolean,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (hasCoarseOnly) Slate900 else Slate900
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (hasCoarseOnly) AmberAccent.copy(alpha = 0.5f) else CyanLight.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("permission_rationale_banner")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (hasCoarseOnly) AmberAccent.copy(alpha = 0.2f) else CyanLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (hasCoarseOnly) Icons.Default.WarningAmber else Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = if (hasCoarseOnly) AmberAccent else CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasCoarseOnly) "Approximate Location Only" else "Precise GPS Access Recommended",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (hasCoarseOnly)
                            "Upgrade to 'Precise' location for sub-meter breadcrumb accuracy and exact speed telemetry."
                        else
                            "GeoTrace needs GPS to record breadcrumb routes, speed gauges, and altitude profiles.",
                        fontSize = 11.sp,
                        color = Slate400,
                        lineHeight = 15.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("banner_settings_button")
                ) {
                    Text("App Info", fontSize = 11.sp, color = Slate400)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasCoarseOnly) AmberAccent else CyanNeon,
                        contentColor = Slate900
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("banner_grant_permission_button")
                ) {
                    Text(
                        text = if (hasCoarseOnly) "Enable Precise" else "Grant Access",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to general settings
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

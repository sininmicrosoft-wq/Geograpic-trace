package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.LocationTracingManager
import com.example.location.TrackingStatus
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LiveTraceScreen
import com.example.ui.screens.RewardsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripDetailScreen
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

enum class AppTab(val title: String, val icon: ImageVector) {
    LIVE_TRACE("Live Trace", Icons.Default.MyLocation),
    HISTORY("History", Icons.Default.History),
    REWARDS("Rewards", Icons.Default.EmojiEvents),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val manager = remember { LocationTracingManager.getInstance(context) }
    val trackingState by manager.state.collectAsState()

    var currentTab by remember { mutableStateOf(AppTab.LIVE_TRACE) }
    var selectedTripId by remember { mutableStateOf<Long?>(null) }
    var useImperialUnits by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selectedTripId == null) {
                NavigationBar(
                    containerColor = Slate900,
                    contentColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    AppTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedTripId = null
                                currentTab = tab
                            },
                            icon = {
                                if (tab == AppTab.LIVE_TRACE && trackingState.status != TrackingStatus.IDLE) {
                                    BadgedBox(
                                        badge = {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (trackingState.status == TrackingStatus.RECORDING) EmeraldGreen else Color(0xFFF59E0B),
                                                modifier = Modifier.size(8.dp)
                                            ) {}
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.title)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanNeon,
                                selectedTextColor = CyanNeon,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400,
                                indicatorColor = Slate950
                            ),
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate950)
        ) {
            if (selectedTripId != null) {
                TripDetailScreen(
                    sessionId = selectedTripId!!,
                    useImperialUnits = useImperialUnits,
                    onBack = { selectedTripId = null }
                )
            } else {
                when (currentTab) {
                    AppTab.LIVE_TRACE -> {
                        LiveTraceScreen(
                            useImperialUnits = useImperialUnits,
                            onTripCompleted = { newSessionId ->
                                selectedTripId = newSessionId
                            },
                            onOpenRewards = {
                                currentTab = AppTab.REWARDS
                            }
                        )
                    }
                    AppTab.HISTORY -> {
                        HistoryScreen(
                            useImperialUnits = useImperialUnits,
                            onSelectTrip = { tripId ->
                                selectedTripId = tripId
                            },
                            onStartTracingClick = {
                                currentTab = AppTab.LIVE_TRACE
                            }
                        )
                    }
                    AppTab.REWARDS -> {
                        RewardsScreen()
                    }
                    AppTab.ANALYTICS -> {
                        AnalyticsScreen(useImperialUnits = useImperialUnits)
                    }
                    AppTab.SETTINGS -> {
                        SettingsScreen(
                            useImperialUnits = useImperialUnits,
                            onToggleUnits = { useImperialUnits = it }
                        )
                    }
                }
            }
        }
    }
}

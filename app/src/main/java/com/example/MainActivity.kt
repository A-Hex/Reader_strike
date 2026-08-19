package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    LIBRARY("Library", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    STREAK("Streak", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
    HIGHLIGHTS("Highlights", Icons.Filled.Highlight, Icons.Outlined.Highlight),
    DISCOVER("Discover", Icons.Filled.Public, Icons.Outlined.Public),
    SYNC("Cloud", Icons.Filled.Sync, Icons.Outlined.Sync),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentBook by viewModel.currentBook.collectAsState()
    var currentTab by remember { mutableStateOf(NavigationTab.LIBRARY) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentBook != null,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                } else {
                    (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                }
            },
            label = "ReaderTransition"
        ) { isReading ->
            if (isReading) {
                ReaderScreen(
                    viewModel = viewModel,
                    onClose = { viewModel.closeReader() }
                )
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 4.dp
                        ) {
                            NavigationTab.entries.forEach { tab ->
                                val isSelected = currentTab == tab
                                val streakData by viewModel.streakData.collectAsState()

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (tab == NavigationTab.STREAK && streakData.currentStreakDays > 0) {
                                                    Badge(
                                                        containerColor = Color(0xFFB4CCB9),
                                                        contentColor = Color(0xFF1E281F)
                                                    ) {
                                                        Text("${streakData.currentStreakDays}")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title,
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 11.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            NavigationTab.LIBRARY -> LibraryScreen(
                                viewModel = viewModel,
                                onNavigateToDiscover = { currentTab = NavigationTab.DISCOVER },
                                onNavigateToStreak = { currentTab = NavigationTab.STREAK }
                            )
                            NavigationTab.STREAK -> StreakStatsScreen(viewModel = viewModel)
                            NavigationTab.HIGHLIGHTS -> HighlightsScreen(viewModel = viewModel)
                            NavigationTab.DISCOVER -> DiscoverStoreScreen(
                                viewModel = viewModel,
                                onBack = { currentTab = NavigationTab.LIBRARY }
                            )
                            NavigationTab.SYNC -> CloudSyncScreen(viewModel = viewModel)
                            NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

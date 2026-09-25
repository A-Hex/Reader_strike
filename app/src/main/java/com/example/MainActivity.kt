package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.InteractiveTutorialOverlay
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel

enum class NavigationTab(
    val stringKey: String,
    val defaultTitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    LIBRARY("tab_library", "Library", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    STREAK("tab_streak", "Streak", Icons.Filled.Insights, Icons.Outlined.Insights),
    HIGHLIGHTS("tab_highlights", "Highlights", Icons.Filled.Bookmark, Icons.Outlined.Bookmark),
    DISCOVER("tab_discover", "Discover", Icons.Filled.TravelExplore, Icons.Outlined.TravelExplore),
    SETTINGS("tab_settings", "Settings", Icons.Filled.Tune, Icons.Outlined.Tune)
}

/**
 * Which surface owns the screen right now.
 *
 * SecureMind launches through a short sequence: the brand visual, then a sign-in gate (until the
 * reader signs in or chooses to continue without an account), then first-run onboarding, and only
 * then the library.
 */
private enum class StartupStage { VISUAL, LOGIN, ONBOARDING, MAIN }

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val requestedTabFlow = kotlinx.coroutines.flow.MutableStateFlow<NavigationTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.notification.ReadingNotificationManager.initChannels(this)
        enableEdgeToEdge()

        val initialTab = parseInitialTab(intent)
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppContent(viewModel = viewModel, initialTab = initialTab, requestedTabFlow = requestedTabFlow)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTabFlow.value = parseInitialTab(intent)
    }

    private fun parseInitialTab(intent: android.content.Intent?): NavigationTab {
        val target = intent?.getStringExtra("target_tab") ?: return NavigationTab.LIBRARY
        return when (target.uppercase()) {
            "STREAK" -> NavigationTab.STREAK
            "HIGHLIGHTS" -> NavigationTab.HIGHLIGHTS
            "DISCOVER" -> NavigationTab.DISCOVER
            "SETTINGS" -> NavigationTab.SETTINGS
            else -> NavigationTab.LIBRARY
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    initialTab: NavigationTab = NavigationTab.LIBRARY,
    requestedTabFlow: kotlinx.coroutines.flow.StateFlow<NavigationTab?> = remember { kotlinx.coroutines.flow.MutableStateFlow(null) }
) {
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val isTutorialVisible by viewModel.isTutorialVisible.collectAsState()
    val currentBook by viewModel.currentBook.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val authGateResolved by viewModel.authGateResolved.collectAsState()
    val accountState by viewModel.accountState.collectAsState()

    var currentTab by remember { mutableStateOf(initialTab) }
    // Saved so a configuration change (rotation) doesn't replay the intro visual.
    var startupVisualDone by rememberSaveable { mutableStateOf(false) }

    val stage = when {
        !startupVisualDone -> StartupStage.VISUAL
        // Wait out the Firebase session restore so a signed-in reader never sees a login flash.
        !accountState.isRestoring && !accountState.isSignedIn && !authGateResolved -> StartupStage.LOGIN
        !isOnboardingCompleted -> StartupStage.ONBOARDING
        else -> StartupStage.MAIN
    }

    LaunchedEffect(requestedTabFlow) {
        requestedTabFlow.collect { newTab ->
            if (newTab != null) {
                currentTab = newTab
            }
        }
    }

    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = stage,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(420)) + scaleIn(initialScale = 0.96f))
                            .togetherWith(fadeOut(animationSpec = tween(320)))
                    },
                    label = "StartupStage"
                ) { current ->
                    when (current) {
                        StartupStage.VISUAL -> SecureMindStartupScreen(
                            onFinished = { startupVisualDone = true }
                        )

                        StartupStage.LOGIN -> SecureMindLoginScreen(
                            viewModel = viewModel,
                            onDone = { /* stage is derived from authGateResolved / accountState */ }
                        )

                        StartupStage.ONBOARDING -> OnboardingScreen(viewModel = viewModel)

                        StartupStage.MAIN -> AnimatedContent(
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
                                                val tabTitle = AppStrings.get(tab.stringKey, currentLanguage)

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
                                                                contentDescription = tabTitle,
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
                                                            text = tabTitle,
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
                                    BackHandler(enabled = currentTab != NavigationTab.LIBRARY) {
                                        currentTab = NavigationTab.LIBRARY
                                    }
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
                                            NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Walkthrough Tutorial Overlay
                if (isTutorialVisible) {
                    InteractiveTutorialOverlay(
                        viewModel = viewModel,
                        onDismiss = { viewModel.dismissTutorial() }
                    )
                }
            }
        }
    }
}

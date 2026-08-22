package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val selectedGenres by viewModel.selectedGenres.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar with Language Selector and Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Dropdown / Toggle
                var showLangMenu by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        onClick = { showLangMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, NaturalDarkBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(currentLanguage.flag, fontSize = 16.sp)
                            Text(
                                text = currentLanguage.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = NaturalDarkTextMuted
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showLangMenu,
                        onDismissRequest = { showLangMenu = false }
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text("${lang.flag} ${lang.displayName} (${lang.nativeName})") },
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    showLangMenu = false
                                }
                            )
                        }
                    }
                }

                // Skip Button
                if (pagerState.currentPage < 3) {
                    TextButton(
                        onClick = { viewModel.completeOnboarding(launchTutorial = false) }
                    ) {
                        Text(
                            text = AppStrings.get("onboarding_skip", currentLanguage),
                            style = MaterialTheme.typography.labelLarge,
                            color = NaturalDarkTextMuted
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomePage(
                        currentLanguage = currentLanguage
                    )
                    1 -> OnboardingHabitBuilderPage(
                        dailyGoalMinutes = dailyGoalMinutes,
                        currentLanguage = currentLanguage,
                        onGoalChange = { viewModel.updateDailyGoal(it) }
                    )
                    2 -> OnboardingFeaturePlaygroundPage(
                        currentLanguage = currentLanguage
                    )
                    3 -> OnboardingGenreCurationPage(
                        selectedGenres = selectedGenres,
                        currentLanguage = currentLanguage,
                        onToggleGenre = { viewModel.toggleGenreInterest(it) },
                        onGetStarted = { launchTutorial ->
                            viewModel.completeOnboarding(launchTutorial = launchTutorial)
                        }
                    )
                }
            }

            // Bottom Navigation Indicators & Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Page Indicator Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) NaturalPrimary else NaturalDarkSurfaceVariant
                                )
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NaturalDarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.get("onboarding_back", currentLanguage))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    if (pagerState.currentPage < 3) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                        ) {
                            Text(
                                text = AppStrings.get("onboarding_next", currentLanguage),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// PAGE 1: Welcome & Mission
// ----------------------------------------------------
@Composable
private fun OnboardingWelcomePage(
    currentLanguage: AppLanguage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, NaturalDarkBorder)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_onboarding_welcome),
                contentDescription = "Welcome to A-Hex streak",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Title & Description
        Text(
            text = AppStrings.get("onboarding_welcome_title", currentLanguage),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = AppStrings.get("onboarding_welcome_subtitle", currentLanguage),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = NaturalDarkTextMuted,
            lineHeight = 22.sp
        )

        // Key Value Chips
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WelcomeFeatureBadge(
                icon = Icons.Default.CloudOff,
                title = "100% Offline & Private",
                subtitle = "Zero server sync or tracking. Your books and notes stay entirely on device."
            )
            WelcomeFeatureBadge(
                icon = Icons.Default.MenuBook,
                title = "Universal Format Support",
                subtitle = "Seamlessly render EPUB, PDF (scanned & searchable), and TXT documents."
            )
            WelcomeFeatureBadge(
                icon = Icons.Default.LocalFireDepartment,
                title = "Habit & Streak Mastery",
                subtitle = "Precision reading timer, streak freeze shields, and reading quests."
            )
        }
    }
}

@Composable
private fun WelcomeFeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NaturalPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ----------------------------------------------------
// PAGE 2: Habit Architecture & Projections
// ----------------------------------------------------
@Composable
private fun OnboardingHabitBuilderPage(
    dailyGoalMinutes: Int,
    currentLanguage: AppLanguage,
    onGoalChange: (Int) -> Unit
) {
    val estimatedBooksPerYear = ((dailyGoalMinutes * 365 * 220) / (70000)).coerceAtLeast(4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Streak Banner Image
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, NaturalDarkBorder)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_onboarding_streak),
                contentDescription = "Streak Habit",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = AppStrings.get("onboarding_habit_title", currentLanguage),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = AppStrings.get("onboarding_habit_subtitle", currentLanguage),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = NaturalDarkTextMuted
        )

        // Habit Projection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalForestAccent.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, NaturalForestAccent.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Yearly Projection",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NaturalSageAccent
                    )
                    Text(
                        text = "~$estimatedBooksPerYear Books / Year",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = NaturalPrimary
                    )
                    Text(
                        text = "Based on $dailyGoalMinutes min/day @ average reading speed",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(NaturalPrimary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = NaturalPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Quick Preset Buttons
        Text(
            text = "Select Target Daily Reading Time:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                10 to "Casual",
                20 to "Balanced",
                30 to "Deep",
                45 to "Scholar"
            )

            presets.forEach { (mins, label) ->
                val isSelected = dailyGoalMinutes == mins
                Surface(
                    onClick = { onGoalChange(mins) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) NaturalPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) NaturalPrimary else NaturalDarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${mins}m",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) NaturalOnPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) NaturalOnPrimary.copy(alpha = 0.8f) else NaturalDarkTextMuted
                        )
                    }
                }
            }
        }

        // Fine tuning slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Custom Duration:", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                Text("$dailyGoalMinutes minutes", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
            }
            Slider(
                value = dailyGoalMinutes.toFloat(),
                onValueChange = { onGoalChange(it.toInt()) },
                valueRange = 5f..120f,
                steps = 22,
                colors = SliderDefaults.colors(
                    thumbColor = NaturalPrimary,
                    activeTrackColor = NaturalPrimary
                )
            )
        }
    }
}

// ----------------------------------------------------
// PAGE 3: Interactive Feature Playground
// ----------------------------------------------------
@Composable
private fun OnboardingFeaturePlaygroundPage(
    currentLanguage: AppLanguage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = AppStrings.get("onboarding_features_title", currentLanguage),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = AppStrings.get("onboarding_features_subtitle", currentLanguage),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = NaturalDarkTextMuted
        )

        // 1. Live Interactive RSVP Speed Reader Preview Widget
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, NaturalDarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = NaturalPrimary)
                        Text(
                            text = "Interactive RSVP Speed Reader",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        color = NaturalPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Live Demo",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Interactive Stream Box
                val sampleWords = listOf("Master", "reading", "at", "hyper", "speed", "with", "flawless", "retention", "and", "laser", "focus.")
                var wordIndex by remember { mutableStateOf(0) }
                var isRsvpRunning by remember { mutableStateOf(false) }
                var rsvpWpm by remember { mutableStateOf(300) }

                LaunchedEffect(isRsvpRunning, rsvpWpm) {
                    if (isRsvpRunning) {
                        while (true) {
                            delay((60000L / rsvpWpm).coerceAtLeast(100L))
                            wordIndex = (wordIndex + 1) % sampleWords.size
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141915))
                        .border(1.dp, NaturalDarkBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val currentWord = sampleWords[wordIndex]
                    Text(
                        text = currentWord,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = NaturalPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speed: $rsvpWpm WPM",
                        style = MaterialTheme.typography.labelMedium,
                        color = NaturalDarkTextMuted
                    )

                    Button(
                        onClick = { isRsvpRunning = !isRsvpRunning },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRsvpRunning) Color(0xFFC75D5D) else NaturalPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            if (isRsvpRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRsvpRunning) "Pause" else "Test RSVP", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 2. Interactive Theme Switcher Demo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, NaturalDarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = NaturalPrimary)
                    Text(
                        text = "Eye-Care Reading Themes",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                var selectedDemoTheme by remember { mutableStateOf(0) }
                val themes = listOf(
                    Triple("Forest Sage", Color(0xFF1E281F), Color(0xFFE4EDE5)),
                    Triple("Warm Sepia", Color(0xFFFBF0D9), Color(0xFF433422)),
                    Triple("AMOLED Noir", Color(0xFF000000), Color(0xFFE0E0E0)),
                    Triple("Parchment", Color(0xFFF6EADB), Color(0xFF2C2218))
                )

                // Theme Preview Box
                val activeTheme = themes[selectedDemoTheme]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(activeTheme.second)
                        .border(1.dp, NaturalDarkBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "“The impediment to action advances action. What stands in the way becomes the way.” — Marcus Aurelius",
                        color = activeTheme.third,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    themes.forEachIndexed { idx, item ->
                        val isSel = selectedDemoTheme == idx
                        Surface(
                            onClick = { selectedDemoTheme = idx },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = item.second,
                            border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) NaturalPrimary else NaturalDarkBorder)
                        ) {
                            Text(
                                text = item.first,
                                color = item.third,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// PAGE 4: Genre Curation & Finish
// ----------------------------------------------------
@Composable
private fun OnboardingGenreCurationPage(
    selectedGenres: Set<String>,
    currentLanguage: AppLanguage,
    onToggleGenre: (String) -> Unit,
    onGetStarted: (launchTutorial: Boolean) -> Unit
) {
    val allGenres = listOf(
        "Philosophy" to Icons.Default.Psychology,
        "Classics" to Icons.Default.AutoStories,
        "Sci-Fi" to Icons.Default.RocketLaunch,
        "Personal Growth" to Icons.Default.TrendingUp,
        "History" to Icons.Default.AccountBalance,
        "Mystery" to Icons.Default.Search,
        "Science" to Icons.Default.Biotech,
        "Literature" to Icons.Default.Create
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(NaturalPrimary.copy(alpha = 0.2f))
                .border(2.dp, NaturalPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NaturalPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = AppStrings.get("onboarding_genres_title", currentLanguage),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = AppStrings.get("onboarding_genres_subtitle", currentLanguage),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = NaturalDarkTextMuted
        )

        // Genre Chips Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allGenres.chunked(2).forEach { rowPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPair.forEach { (genre, icon) ->
                        val isSelected = selectedGenres.contains(genre)
                        Surface(
                            onClick = { onToggleGenre(genre) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) NaturalPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) NaturalPrimary else NaturalDarkBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) NaturalPrimary else NaturalDarkTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) NaturalPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Big Main CTA Buttons
        Button(
            onClick = { onGetStarted(false) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AppStrings.get("onboarding_get_started", currentLanguage),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        OutlinedButton(
            onClick = { onGetStarted(true) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, NaturalPrimary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalPrimary)
        ) {
            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AppStrings.get("onboarding_start_tutorial", currentLanguage),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

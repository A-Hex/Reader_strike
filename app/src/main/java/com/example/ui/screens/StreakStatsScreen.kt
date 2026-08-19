package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DayReadingStat
import com.example.model.StreakBadge
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun StreakStatsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val streakData by viewModel.streakData.collectAsState()
    val activeSessionState by viewModel.activeSessionState.collectAsState()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()

    var activeShareContent by remember { mutableStateOf<ShareContentType?>(null) }
    var showGoalPickerDialog by remember { mutableStateOf(false) }
    var selectedGraphMetric by remember { mutableStateOf("Minutes") } // "Minutes" or "Pages"
    var selectedDayStat by remember { mutableStateOf<DayReadingStat?>(null) }

    if (activeShareContent != null) {
        SocialShareModal(
            contentType = activeShareContent!!,
            onDismiss = { activeShareContent = null }
        )
    }

    if (showGoalPickerDialog) {
        DailyGoalPickerDialog(
            currentGoalMinutes = dailyGoalMinutes,
            onGoalSelected = { newGoal ->
                viewModel.updateDailyGoal(newGoal)
                showGoalPickerDialog = false
            },
            onDismiss = { showGoalPickerDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp)
    ) {
        // Active Reading Session Live Banner (if a session is ongoing)
        if (activeSessionState.isSessionRunning) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalDarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(NaturalPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = NaturalPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Active Reading Session",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NaturalPrimary
                                    )
                                )
                                Text(
                                    text = activeSessionState.currentBookTitle.ifBlank { "Current Book" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NaturalDarkText
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${activeSessionState.formattedDuration} elapsed • ${activeSessionState.currentTotalTodayMinutes}/${activeSessionState.dailyGoalMinutes}m toward streak goal",
                                    style = MaterialTheme.typography.bodySmall.copy(color = NaturalDarkTextMuted)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.toggleActiveSessionPause() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NaturalPrimary)
                        ) {
                            Icon(
                                imageVector = if (activeSessionState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Toggle Session Pause",
                                tint = NaturalOnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hero Streak Banner in Natural Sage Tone
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalSageBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "A-HEX READING STREAK",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = NaturalSageAccent
                            )
                        )

                        Surface(
                            color = NaturalSageAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Active Habit",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalSageAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hexagon Flame Badge
                    HexagonFlameBadge(
                        streakDays = streakData.currentStreakDays,
                        size = 110.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "${streakData.currentStreakDays}-Day Active Reading Streak!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NaturalSageAccent
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Longest streak: ${streakData.longestStreakDays} consecutive days • Daily target: ${dailyGoalMinutes} min/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalSageMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Share Progress Action Button
                    Button(
                        onClick = { activeShareContent = ShareContentType.Stats(streakData) },
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalSageAccent, contentColor = NaturalDarkBackground),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Streak & Progress", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // DAILY READING GOAL FEATURE: Dedicated interactive progress bar and customizable target
        item {
            DailyReadingGoalCard(
                todayMinutes = streakData.todayMinutesRead,
                goalMinutes = dailyGoalMinutes,
                streakDays = streakData.currentStreakDays,
                onAdjustGoal = { showGoalPickerDialog = true },
                onShareGoal = {
                    activeShareContent = ShareContentType.DailyGoal(
                        goalMinutes = dailyGoalMinutes,
                        todayMinutes = streakData.todayMinutesRead,
                        streakDays = streakData.currentStreakDays
                    )
                }
            )
        }

        // Section Title: Dashboard Key Metrics
        item {
            Text(
                text = "Reading Statistics Overview",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        // 4 Core Dashboard Metric Cards:
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Books Read",
                    value = "${streakData.totalBooksRead}",
                    subtitle = "Finished in library (${streakData.totalBooksInLibrary} total)",
                    icon = Icons.Default.AutoStories,
                    accentColor = NaturalPrimary,
                    containerBg = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Pages Read",
                    value = "${streakData.totalPagesRead}",
                    subtitle = "Across all e-books",
                    icon = Icons.Default.MenuBook,
                    accentColor = NaturalSecondary,
                    containerBg = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Avg Session Time",
                    value = "${streakData.avgSessionMinutes.toInt()} min",
                    subtitle = "${streakData.totalSessionsCount} sessions recorded",
                    icon = Icons.Default.AccessTime,
                    accentColor = NaturalOchreAccent,
                    containerBg = NaturalOchreBg,
                    borderStrokeColor = NaturalOchreBorder,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Current Streak",
                    value = "${streakData.currentStreakDays} Days",
                    subtitle = "Best: ${streakData.longestStreakDays} days",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = NaturalSageAccent,
                    containerBg = NaturalSageBg,
                    borderStrokeColor = NaturalSageBorder,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Past Month Reading Activity Graph (30-Day Activity)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with metric toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Activity Graph",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Past 30 days reading distribution",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                        }

                        // Toggle Buttons
                        Row(
                            modifier = Modifier
                                .background(NaturalDarkBackground, RoundedCornerShape(10.dp))
                                .border(1.dp, NaturalDarkBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(2.dp)
                        ) {
                            listOf("Minutes", "Pages").forEach { metric ->
                                val isSelected = selectedGraphMetric == metric
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NaturalPrimary else Color.Transparent)
                                        .clickable { selectedGraphMetric = metric }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = metric,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) NaturalOnPrimary else NaturalDarkTextMuted
                                    )
                                }
                            }
                        }
                    }

                    // Selected Day Detail Box (if clicked)
                    AnimatedVisibility(
                        visible = selectedDayStat != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        selectedDayStat?.let { day ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = NaturalDarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${day.dayOfWeek}, ${day.date}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = NaturalDarkText
                                        )
                                        Text(
                                            text = "${day.minutesRead} minutes read • ${day.pagesRead} pages turned",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NaturalDarkTextMuted
                                        )
                                    }
                                    Surface(
                                        color = if (day.isGoalReached) NaturalSageAccent.copy(alpha = 0.2f) else NaturalPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (day.isGoalReached) "Goal Met 🔥" else "Logged 📖",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (day.isGoalReached) NaturalSageAccent else NaturalPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 30-Day Bar Visualizer
                    MonthlyActivityBarChart(
                        monthlyStats = streakData.monthlyStats,
                        metric = selectedGraphMetric,
                        dailyGoalMinutes = dailyGoalMinutes,
                        selectedStat = selectedDayStat,
                        onSelectStat = { stat -> selectedDayStat = if (selectedDayStat == stat) null else stat }
                    )
                }
            }
        }

        // Hex Badges Showcase Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "A-Hex Achievements & Badges",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = NaturalDarkTextMuted
                )
            )
        }

        items(streakData.badges) { badge ->
            StreakBadgeCard(
                badge = badge,
                onShare = {
                    activeShareContent = ShareContentType.Achievement(
                        title = badge.title,
                        description = badge.description,
                        streakDays = streakData.currentStreakDays
                    )
                }
            )
        }
    }
}

@Composable
fun DailyReadingGoalCard(
    todayMinutes: Int,
    goalMinutes: Int,
    streakDays: Int,
    onAdjustGoal: () -> Unit,
    onShareGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (todayMinutes.toFloat() / goalMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()
    val isGoalAchieved = todayMinutes >= goalMinutes
    val remainingMinutes = (goalMinutes - todayMinutes).coerceAtLeast(0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isGoalAchieved) NaturalSageAccent.copy(alpha = 0.8f) else NaturalPrimary.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Goal Title, Target Badge, and Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isGoalAchieved) NaturalSageAccent.copy(alpha = 0.2f) else NaturalPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGoalAchieved) Icons.Default.CheckCircle else Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = if (isGoalAchieved) NaturalSageAccent else NaturalPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Daily Reading Goal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalDarkText
                        )
                        Text(
                            text = "Target: $goalMinutes minutes / day",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }

                // Adjust Target Goal Button
                OutlinedButton(
                    onClick = onAdjustGoal,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Stats & Percentage Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$todayMinutes / $goalMinutes min",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isGoalAchieved) NaturalSageAccent else NaturalPrimary
                        )
                    )
                    Text(
                        text = if (isGoalAchieved) "Goal Reached Today! 🎉" else "$remainingMinutes min remaining to complete goal",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isGoalAchieved) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isGoalAchieved) NaturalSageAccent else NaturalDarkTextMuted
                        )
                    )
                }

                Surface(
                    color = if (isGoalAchieved) NaturalSageAccent.copy(alpha = 0.2f) else NaturalPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$percentage%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isGoalAchieved) NaturalSageAccent else NaturalPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Visual Progress Bar
            Box(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (isGoalAchieved) NaturalSageAccent else NaturalPrimary,
                    trackColor = NaturalDarkBackground
                )
            }

            HorizontalDivider(color = NaturalDarkBorder.copy(alpha = 0.4f))

            // Action Row: Share Daily Goal Achievement & Quick Motivational Callout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isGoalAchieved) "🔥 Streak protected for today!" else "Keep reading to extend your streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaturalDarkTextMuted,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onShareGoal,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share Goal Progress",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun DailyGoalPickerDialog(
    currentGoalMinutes: Int,
    onGoalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableFloatStateOf(currentGoalMinutes.toFloat()) }
    val presetOptions = listOf(10, 15, 20, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NaturalDarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.TrackChanges, contentDescription = null, tint = NaturalPrimary)
                Text(
                    text = "Set Daily Reading Goal",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = NaturalDarkText
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose your target reading time per day to maintain your streak and build lasting habits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaturalDarkTextMuted
                )

                // Large Visual Display of Selected Time
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NaturalDarkSurfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${selectedMinutes.toInt()} Minutes",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary
                        )
                        Text(
                            text = "~${(selectedMinutes.toInt() * 1.25f).toInt()} estimated pages per day",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }

                // Interactive Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5 min", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                        Text("120 min", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                    }
                    Slider(
                        value = selectedMinutes,
                        onValueChange = { selectedMinutes = (it / 5).toInt() * 5f },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = NaturalPrimary,
                            activeTrackColor = NaturalPrimary,
                            inactiveTrackColor = NaturalDarkBorder
                        )
                    )
                }

                // Quick Preset Chips
                Text(
                    text = "Popular Presets:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NaturalDarkText
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetOptions) { minutes ->
                        val isSelected = selectedMinutes.toInt() == minutes
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMinutes = minutes.toFloat() },
                            label = { Text("${minutes}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaturalPrimary,
                                selectedLabelColor = NaturalOnPrimary,
                                containerColor = NaturalDarkSurfaceVariant,
                                labelColor = NaturalDarkText
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) NaturalPrimary else NaturalDarkBorder
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGoalSelected(selectedMinutes.toInt()) },
                colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
            ) {
                Text("Save Goal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NaturalDarkTextMuted)
            }
        }
    )
}

@Composable
fun MonthlyActivityBarChart(
    monthlyStats: List<DayReadingStat>,
    metric: String,
    dailyGoalMinutes: Int,
    selectedStat: DayReadingStat?,
    onSelectStat: (DayReadingStat) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxValue = remember(monthlyStats, metric) {
        val maxFromData = if (metric == "Minutes") {
            monthlyStats.maxOfOrNull { it.minutesRead } ?: 40
        } else {
            monthlyStats.maxOfOrNull { it.pagesRead } ?: 30
        }
        maxOf(maxFromData, if (metric == "Minutes") dailyGoalMinutes else 25).toFloat().coerceAtLeast(10f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            monthlyStats.takeLast(28).forEach { stat ->
                val value = if (metric == "Minutes") stat.minutesRead else stat.pagesRead
                val heightFraction = (value / maxValue).coerceIn(0.06f, 1f)
                val isSelected = selectedStat == stat
                val isGoalMet = stat.minutesRead >= dailyGoalMinutes

                val barColor = when {
                    isSelected -> NaturalOchreAccent
                    isGoalMet -> NaturalSageAccent
                    value > 0 -> NaturalPrimary
                    else -> NaturalDarkBorder.copy(alpha = 0.4f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightFraction)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(barColor)
                        .clickable { onSelectStat(stat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("28 Days Ago", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NaturalSageAccent))
                    Text("Goal Met", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NaturalPrimary))
                    Text("Reading", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                }
            }
            Text("Today", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
        }
    }
}

@Composable
fun StreakBadgeCard(
    badge: StreakBadge,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) NaturalDarkSurfaceVariant else NaturalDarkSurface.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (badge.isUnlocked) NaturalOchreAccent.copy(alpha = 0.6f) else NaturalDarkBorder.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (badge.isUnlocked)
                            Brush.linearGradient(listOf(NaturalOchreAccent, NaturalPrimary))
                        else
                            Brush.linearGradient(listOf(NaturalDarkBorder, NaturalDarkBorder))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.iconName.ifBlank { "🏆" },
                    fontSize = 22.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (badge.isUnlocked) NaturalDarkText else NaturalDarkTextMuted
                        )
                    )
                    if (badge.isUnlocked) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = NaturalSageAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted
                )

                if (!badge.isUnlocked && badge.progress > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { badge.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NaturalPrimary,
                        trackColor = NaturalDarkBackground
                    )
                }
            }

            if (badge.isUnlocked && onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Badge",
                        tint = NaturalPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    containerBg: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderStrokeColor: Color = NaturalDarkBorder.copy(alpha = 0.5f),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = NaturalDarkTextMuted
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = NaturalDarkTextMuted
            )
        }
    }
}

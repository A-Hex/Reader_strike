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
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun StreakStatsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val streakData by viewModel.streakData.collectAsState()
    var showShareModal by remember { mutableStateOf(false) }
    var selectedGraphMetric by remember { mutableStateOf("Minutes") } // "Minutes" or "Pages"
    var selectedDayStat by remember { mutableStateOf<DayReadingStat?>(null) }

    if (showShareModal) {
        SocialShareModal(
            contentType = ShareContentType.Stats(streakData),
            onDismiss = { showShareModal = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp)
    ) {
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
                        text = "Longest streak: ${streakData.longestStreakDays} consecutive days • Goal: ${streakData.dailyGoalMinutes}m/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalSageMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Share Stats Button
                    Button(
                        onClick = { showShareModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalSageAccent, contentColor = NaturalDarkBackground),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Stats to Instagram (@ahex0_01)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
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
        // 1. Total Books Read  2. Total Pages Read
        // 3. Avg Reading Time Per Session  4. Current Reading Streak
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
                        selectedDayStat?.let { stat ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = NaturalDarkBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Date: ${stat.date} (${stat.dayOfWeek})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = NaturalPrimary
                                        )
                                        Text(
                                            text = "Minutes: ${stat.minutesRead}m • Pages: ${stat.pagesRead} pages",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NaturalDarkText
                                        )
                                    }
                                    if (stat.isGoalReached) {
                                        Surface(color = NaturalSageAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "Goal Reached ✓",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NaturalSageAccent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 30-Day Activity Bar Chart
                    val monthlyStats = streakData.monthlyStats
                    val maxVal = if (selectedGraphMetric == "Minutes") {
                        monthlyStats.maxOfOrNull { it.minutesRead }?.coerceAtLeast(45) ?: 45
                    } else {
                        monthlyStats.maxOfOrNull { it.pagesRead }?.coerceAtLeast(35) ?: 35
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        // Graph Bars Scrollable Row
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            items(monthlyStats) { stat ->
                                val currentVal = if (selectedGraphMetric == "Minutes") stat.minutesRead else stat.pagesRead
                                val heightRatio = (currentVal.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 1.0f)
                                val isSelected = selectedDayStat?.date == stat.date

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight()
                                        .clickable { selectedDayStat = stat }
                                ) {
                                    if (isSelected) {
                                        Text(
                                            text = "$currentVal",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = NaturalOchreAccent
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(if (isSelected) 18.dp else 14.dp)
                                            .fillMaxHeight(heightRatio)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                when {
                                                    isSelected -> NaturalOchreAccent
                                                    stat.isGoalReached -> NaturalSageAccent
                                                    currentVal > 0 -> NaturalPrimary
                                                    else -> NaturalDarkBorder.copy(alpha = 0.5f)
                                                }
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Display day number (e.g. 15)
                                    val dayNumber = stat.date.takeLast(2)
                                    Text(
                                        text = dayNumber,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) NaturalPrimary else NaturalDarkTextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Timeline markers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "30 Days Ago", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                            Text(text = "15 Days Ago", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                            Text(text = "Today", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                        }
                    }

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NaturalSageAccent))
                            Text("Goal Reached (≥20m)", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NaturalPrimary))
                            Text("Reading Session", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                        }
                    }
                }
            }
        }

        // Daily Goal Progress & Reading Speed
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = NaturalSageAccent)
                            Text(
                                text = "Today's Reading Progress",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            text = "${streakData.todayMinutesRead} / ${streakData.dailyGoalMinutes} min",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = NaturalSageAccent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val progress = (streakData.todayMinutesRead.toFloat() / streakData.dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NaturalSageAccent,
                        trackColor = NaturalDarkBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (streakData.todayMinutesRead >= streakData.dailyGoalMinutes)
                            "🎉 Today's goal achieved! Keep the momentum alive tomorrow."
                        else
                            "Read ${streakData.dailyGoalMinutes - streakData.todayMinutesRead} more minutes to complete today's streak target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
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
            StreakBadgeCard(badge = badge)
        }

        // Distinctive Dashboard Footer with App Name 'A-Hex streak' and Instagram handle '@ahex0_01'
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NaturalPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = "A-Hex streak logo",
                                    tint = NaturalOnPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "A-Hex streak",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Creator Account: @ahex0_01",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = NaturalOchreAccent
                                )
                            }
                        }

                        Surface(
                            color = NaturalPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "v1.2 Pro",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Build deep comprehension, maintain daily reading habits, and share your intellectual journey.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    HorizontalDivider(color = NaturalDarkBorder.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openInstagramProfile() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Visit @ahex0_01", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showShareModal = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Stats", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
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


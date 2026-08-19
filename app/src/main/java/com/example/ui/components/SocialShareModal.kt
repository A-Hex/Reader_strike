package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.model.Highlight
import com.example.model.ReadingStreakData
import com.example.ui.theme.*
import com.example.util.SocialShareHelper

sealed class ShareContentType {
    data class Stats(val data: ReadingStreakData) : ShareContentType()
    data class DailyGoal(val goalMinutes: Int, val todayMinutes: Int, val streakDays: Int) : ShareContentType()
    data class HighlightQuote(val highlight: Highlight) : ShareContentType()
    data class BookProgress(val book: Book, val streakData: ReadingStreakData) : ShareContentType()
    data class Achievement(val title: String, val description: String, val streakDays: Int) : ShareContentType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialShareModal(
    contentType: ShareContentType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val shareText = remember(contentType) {
        when (contentType) {
            is ShareContentType.Stats -> SocialShareHelper.formatStatsShareText(contentType.data)
            is ShareContentType.DailyGoal -> SocialShareHelper.formatDailyGoalShareText(
                goalMinutes = contentType.goalMinutes,
                todayMinutes = contentType.todayMinutes,
                streakDays = contentType.streakDays
            )
            is ShareContentType.HighlightQuote -> SocialShareHelper.formatHighlightShareText(contentType.highlight)
            is ShareContentType.BookProgress -> SocialShareHelper.formatBookProgressShareText(contentType.book, contentType.streakData)
            is ShareContentType.Achievement -> SocialShareHelper.formatAchievementShareText(contentType.title, contentType.description, contentType.streakDays)
        }
    }

    val headerTitle = when (contentType) {
        is ShareContentType.DailyGoal -> "Share Daily Goal Progress"
        is ShareContentType.Stats -> "Share Reading Streak & Stats"
        is ShareContentType.HighlightQuote -> "Share Favorite Quote"
        is ShareContentType.BookProgress -> "Share Book Progress"
        is ShareContentType.Achievement -> "Share Achievement"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NaturalDarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NaturalDarkBorder) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = NaturalDarkText
                    )
                    Text(
                        text = "Share your reading milestones with any app or friends",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                }
            }

            // Visual Share Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalDarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(NaturalPrimary.copy(alpha = 0.8f), NaturalOchreAccent.copy(alpha = 0.8f)))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card Top branding
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NaturalPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = NaturalOnPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "A-Hex streak",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalDarkText
                                )
                                Text(
                                    text = "Reading Habit & Streak Tracker",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                        }

                        Surface(
                            color = NaturalSageAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Milestone",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalSageAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = NaturalDarkBorder.copy(alpha = 0.5f))

                    // Content preview based on type
                    when (contentType) {
                        is ShareContentType.DailyGoal -> {
                            val isReached = contentType.todayMinutes >= contentType.goalMinutes
                            val progressFraction = (contentType.todayMinutes.toFloat() / contentType.goalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = if (isReached) "🎯 Daily Goal Achieved! 🎉" else "🎯 Daily Goal In Progress",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isReached) NaturalSageAccent else NaturalPrimary
                                        )
                                        Text(
                                            text = "${contentType.todayMinutes} of ${contentType.goalMinutes} minutes read today",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NaturalDarkTextMuted
                                        )
                                    }

                                    Surface(
                                        color = if (isReached) NaturalSageAccent.copy(alpha = 0.2f) else NaturalPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${(progressFraction * 100).toInt()}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isReached) NaturalSageAccent else NaturalPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isReached) NaturalSageAccent else NaturalPrimary,
                                    trackColor = NaturalDarkBorder
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = NaturalDarkSurface
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("${contentType.streakDays} Days 🔥", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFF9800))
                                            Text("Active Streak", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = NaturalDarkSurface
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("${contentType.todayMinutes}m", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                                            Text("Read Today", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                                        }
                                    }
                                }
                            }
                        }

                        is ShareContentType.Stats -> {
                            val stats = contentType.data
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "🔥 ${stats.currentStreakDays}-Day Reading Streak",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = NaturalDarkSurface
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("${stats.totalPagesRead}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NaturalOchreAccent)
                                            Text("Pages Read", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = NaturalDarkSurface
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("${stats.totalBooksRead}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                                            Text("Books Finished", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = NaturalDarkSurface
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("${stats.avgSessionMinutes.toInt()}m", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NaturalSecondary)
                                            Text("Avg Session", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                                        }
                                    }
                                }
                            }
                        }

                        is ShareContentType.HighlightQuote -> {
                            val hl = contentType.highlight
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "\"${hl.text}\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = NaturalDarkText
                                )
                                Text(
                                    text = "— ${hl.bookTitle} (${hl.chapterTitle})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalPrimary
                                )
                                if (!hl.note.isNullOrBlank()) {
                                    Text(
                                        text = "Note: ${hl.note}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NaturalDarkTextMuted
                                    )
                                }
                            }
                        }

                        is ShareContentType.BookProgress -> {
                            val book = contentType.book
                            val streak = contentType.streakData
                            val percent = (book.readingProgress * 100).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BookCoverImage(
                                    book = book,
                                    modifier = Modifier.size(width = 64.dp, height = 88.dp),
                                    cornerRadius = 10.dp,
                                    showFormatBadge = true,
                                    showFavoriteBadge = false,
                                    elevation = 3.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "By ${book.author} • $percent% completed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NaturalDarkTextMuted
                                    )
                                    LinearProgressIndicator(
                                        progress = { book.readingProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = NaturalPrimary,
                                        trackColor = NaturalDarkBorder
                                    )
                                    Text(
                                        text = "🔥 ${streak.currentStreakDays}-day streak • p. ${book.currentPage}/${book.totalPages}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalOchreAccent
                                    )
                                }
                            }
                        }

                        is ShareContentType.Achievement -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "🏆 ${contentType.title}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalPrimary
                                )
                                Text(
                                    text = contentType.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NaturalDarkText
                                )
                                Text(
                                    text = "🔥 Current Active Streak: ${contentType.streakDays} Days",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = NaturalOchreAccent
                                )
                            }
                        }
                    }

                    // Card Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "#ReadingStreak #DailyReading",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                        Text(
                            text = "Tracked with A-Hex",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = NaturalPrimary
                        )
                    }
                }
            }

            // Share Action Buttons (Let user decide where and how to share)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary: System Share Chooser (lets user pick ANY app on their phone)
                Button(
                    onClick = {
                        SocialShareHelper.shareContent(
                            context = context,
                            content = shareText,
                            subject = "My Reading Progress - A-Hex Streak"
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = NaturalOnPrimary),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share to Any App...",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Secondary Action: Direct Copy to Clipboard
                OutlinedButton(
                    onClick = {
                        SocialShareHelper.copyToClipboard(context, shareText, label = "Reading Progress")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalDarkText),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Progress Text to Clipboard", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

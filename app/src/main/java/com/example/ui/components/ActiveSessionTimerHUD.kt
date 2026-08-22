package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ActiveSessionState
import com.example.model.ReadingStreakData
import com.example.reader.FacePresenceState
import com.example.ui.theme.*

@Composable
fun ActiveSessionTimerHUD(
    sessionState: ActiveSessionState,
    streakData: ReadingStreakData,
    facePresenceState: FacePresenceState = FacePresenceState.Disabled,
    isFaceAssistedEnabled: Boolean = false,
    onTogglePause: () -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBreakdownDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                onUserInteraction()
                showBreakdownDialog = true
            },
        color = NaturalDarkSurface.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (sessionState.isDailyGoalReached) NaturalPrimary.copy(alpha = 0.8f) else NaturalDarkBorder
        ),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Pulse / Status Dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isFaceAssistedEnabled && facePresenceState == FacePresenceState.NoFace -> Color(0xFFEF4444)
                            sessionState.isPaused -> Color(0xFFEAB308)
                            sessionState.isIdle -> Color(0xFF94A3B8)
                            sessionState.isDailyGoalReached -> NaturalPrimary
                            else -> NaturalPrimary
                        }
                    )
            )

            // Timer display
            Text(
                text = if (isFaceAssistedEnabled && facePresenceState == FacePresenceState.NoFace) "Face Away (${sessionState.formattedDuration})"
                       else if (sessionState.isPaused) "Paused (${sessionState.formattedDuration})"
                       else if (sessionState.isIdle) "Idle (${sessionState.formattedDuration})"
                       else sessionState.formattedDuration,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isFaceAssistedEnabled && facePresenceState == FacePresenceState.NoFace) Color(0xFFEF4444)
                            else if (sessionState.isPaused) Color(0xFFEAB308) else NaturalDarkText
                )
            )

            // Face Presence indicator badge if enabled
            if (isFaceAssistedEnabled) {
                Icon(
                    imageVector = when (facePresenceState) {
                        is FacePresenceState.Attentive -> Icons.Default.Visibility
                        is FacePresenceState.NoFace -> Icons.Default.VisibilityOff
                        else -> Icons.Default.Face
                    },
                    contentDescription = "Face Reading Presence",
                    tint = if (facePresenceState == FacePresenceState.Attentive) NaturalPrimary else Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Vertical Divider
            Box(
                modifier = Modifier
                    .height(14.dp)
                    .width(1.dp)
                    .background(NaturalDarkBorder)
            )

            // Streak Goal Progress Mini Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak Fire",
                    tint = if (sessionState.isDailyGoalReached) Color(0xFFFF9800) else NaturalPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${sessionState.currentTotalTodayMinutes}/${sessionState.dailyGoalMinutes}m",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (sessionState.isDailyGoalReached) Color(0xFFFFB74D) else NaturalDarkTextMuted
                )
            }

            // Quick Play/Pause Button
            IconButton(
                onClick = {
                    onUserInteraction()
                    onTogglePause()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (sessionState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (sessionState.isPaused) "Resume Session Timer" else "Pause Session Timer",
                    tint = NaturalDarkText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    if (showBreakdownDialog) {
        ActiveSessionBreakdownDialog(
            sessionState = sessionState,
            streakData = streakData,
            facePresenceState = facePresenceState,
            isFaceAssistedEnabled = isFaceAssistedEnabled,
            onTogglePause = onTogglePause,
            onDismiss = { showBreakdownDialog = false }
        )
    }
}

@Composable
fun ActiveSessionBreakdownDialog(
    sessionState: ActiveSessionState,
    streakData: ReadingStreakData,
    facePresenceState: FacePresenceState = FacePresenceState.Disabled,
    isFaceAssistedEnabled: Boolean = false,
    onTogglePause: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = NaturalPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Active Session Tracker",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NaturalDarkText
                            )
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                // Current Book Info
                if (sessionState.currentBookTitle.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NaturalDarkSurface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = sessionState.currentBookTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = NaturalDarkText
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Big Session Time Clock
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = sessionState.formattedDuration,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NaturalPrimary
                        )
                    )
                    Text(
                        text = when {
                            isFaceAssistedEnabled && facePresenceState == FacePresenceState.NoFace -> "Face attention absent — timer paused"
                            sessionState.isPaused -> "Timer is paused"
                            sessionState.isIdle -> "Idle detected — auto paused"
                            else -> "Actively tracking reading session"
                        },
                        fontSize = 12.sp,
                        color = if (isFaceAssistedEnabled && facePresenceState == FacePresenceState.NoFace) Color(0xFFEF4444)
                                else if (sessionState.isPaused || sessionState.isIdle) Color(0xFFEAB308) else NaturalDarkTextMuted
                    )
                }

                // Metrics Grid (Pages read, Today Total, Current Streak)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metric 1: Session Pages
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = NaturalDarkSurfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${sessionState.sessionPagesRead}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NaturalDarkText)
                            Text("Pages Read", fontSize = 10.sp, color = NaturalDarkTextMuted, textAlign = TextAlign.Center)
                        }
                    }

                    // Metric 2: Today's Minutes
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = NaturalDarkSurfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${sessionState.currentTotalTodayMinutes}m", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NaturalPrimary)
                            Text("Today Total", fontSize = 10.sp, color = NaturalDarkTextMuted, textAlign = TextAlign.Center)
                        }
                    }

                    // Metric 3: Active Streak
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = NaturalDarkSurfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${streakData.currentStreakDays} 🔥", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFFF9800))
                            Text("Day Streak", fontSize = 10.sp, color = NaturalDarkTextMuted, textAlign = TextAlign.Center)
                        }
                    }
                }

                // Streak Goal Progress Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Goal (${sessionState.dailyGoalMinutes} min)",
                            style = MaterialTheme.typography.labelSmall.copy(color = NaturalDarkTextMuted)
                        )
                        Text(
                            text = if (sessionState.isDailyGoalReached) "Goal Reached! 🔥" else "${(sessionState.goalProgressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (sessionState.isDailyGoalReached) NaturalPrimary else NaturalDarkText
                            )
                        )
                    }

                    LinearProgressIndicator(
                        progress = { sessionState.goalProgressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (sessionState.isDailyGoalReached) NaturalPrimary else NaturalPrimary.copy(alpha = 0.8f),
                        trackColor = NaturalDarkSurfaceVariant
                    )
                }

                // Controls & Info Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onTogglePause,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalDarkText)
                    ) {
                        Icon(
                            if (sessionState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (sessionState.isPaused) "Resume" else "Pause Timer", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = NaturalOnPrimary)
                    ) {
                        Text("Keep Reading", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.QuestsAndShieldsManager
import com.example.model.ReaderRank
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingQuestsSheet(
    questsManager: QuestsAndShieldsManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by questsManager.state.collectAsState()
    val nextRank = remember(state.currentRank) {
        ReaderRank.getNextRank(state.currentRank)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NaturalDarkBorder) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(NaturalOchreAccent, Color(0xFFFF7043)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(state.currentRank.badgeIcon, fontSize = 22.sp)
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.currentRank.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${state.totalXp} Total Reading XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalOchreAccent
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                }
            }

            // Rank Progress Card & Streak Shields
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NaturalDarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Next rank progress
                    if (nextRank != null) {
                        val progress = ((state.totalXp - state.currentRank.minXp).toFloat() / (nextRank.minXp - state.currentRank.minXp)).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Next Rank: ${nextRank.title} ${nextRank.badgeIcon}", style = MaterialTheme.typography.labelSmall, color = NaturalDarkText)
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                        }
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = NaturalPrimary,
                            trackColor = NaturalDarkBorder
                        )
                    }

                    Divider(color = NaturalDarkBorder.copy(alpha = 0.6f))

                    // Streak Shields / Streak Freeze
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(22.dp))
                            Column {
                                Text("Streak Shields", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("Auto-protects streak if you miss a day", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = NaturalDarkTextMuted)
                            }
                        }

                        // Shield slots (up to 3)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 0 until state.maxShields) {
                                val isArmed = i < state.streakShields
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isArmed) Color(0xFF1E88E5) else NaturalDarkBorder.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isArmed) Color.White else NaturalDarkTextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active Quests Section
            Text(
                text = "DAILY & SEASONAL QUESTS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                color = NaturalDarkTextMuted
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.activeQuests, key = { it.id }) { quest ->
                    val isComplete = quest.currentProgress >= quest.targetProgress || quest.isCompleted
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, if (quest.isClaimed) NaturalDarkBorder else if (isComplete) NaturalSageSuccess else NaturalDarkBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(quest.iconEmoji, fontSize = 24.sp)

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(quest.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text(quest.description, style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)

                                // Progress bar
                                val prog = (quest.currentProgress.toFloat() / quest.targetProgress.coerceAtLeast(1)).coerceIn(0f, 1f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = prog,
                                        modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = if (isComplete) NaturalSageSuccess else NaturalPrimary,
                                        trackColor = NaturalDarkBorder
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${quest.currentProgress}/${quest.targetProgress}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = NaturalDarkTextMuted)
                                }
                            }

                            // Reward / Claim button
                            if (quest.isClaimed) {
                                Surface(
                                    color = NaturalDarkBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Claimed", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = NaturalDarkTextMuted, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            } else if (isComplete) {
                                Button(
                                    onClick = { questsManager.claimQuestReward(quest.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NaturalSageSuccess, contentColor = NaturalOnPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Claim +${quest.xpReward} XP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(
                                    color = NaturalPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("+${quest.xpReward} XP", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = NaturalPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

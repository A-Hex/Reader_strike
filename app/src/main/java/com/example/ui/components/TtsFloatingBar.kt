package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reader.TtsManager
import com.example.ui.theme.*

@Composable
fun TtsFloatingBar(
    ttsManager: TtsManager,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by ttsManager.isPlaying.collectAsState()
    val speechRate by ttsManager.speechRate.collectAsState()
    var showSpeedMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f)),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed Selector
                Box {
                    TextButton(
                        onClick = { showSpeedMenu = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${speechRate}x",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { rate ->
                            DropdownMenuItem(
                                text = { Text("${rate}x Speed") },
                                onClick = {
                                    ttsManager.setSpeed(rate)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                // Previous sentence
                IconButton(
                    onClick = { ttsManager.previousSentence() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = NaturalDarkText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause
                FilledIconButton(
                    onClick = {
                        if (isPlaying) ttsManager.pause() else ttsManager.resume()
                    },
                    modifier = Modifier.size(42.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = NaturalPrimary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = NaturalOnPrimary
                    )
                }

                // Next sentence
                IconButton(
                    onClick = { ttsManager.nextSentence() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = NaturalDarkText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = if (isPlaying) "Reading aloud..." else "Audio paused",
                style = MaterialTheme.typography.labelSmall,
                color = NaturalDarkTextMuted
            )

            // Close
            IconButton(
                onClick = {
                    ttsManager.stop()
                    onClose()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Audio Reader",
                    tint = NaturalDarkTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


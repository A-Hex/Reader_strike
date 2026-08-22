package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reader.TtsEngineState
import com.example.reader.TtsManager
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings

@Composable
fun TtsFloatingBar(
    ttsManager: TtsManager,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engineState by ttsManager.engineState.collectAsState()
    val speechRate by ttsManager.speechRate.collectAsState()
    var showSpeedMenu by remember { mutableStateOf(false) }

    val isPlaying = engineState is TtsEngineState.Playing

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Speed Selector
                    Box {
                        TextButton(
                            onClick = { showSpeedMenu = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
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

                    // Previous segment
                    IconButton(
                        onClick = { ttsManager.previousSegment() },
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
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = NaturalPrimary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NaturalOnPrimary
                        )
                    }

                    // Next segment
                    IconButton(
                        onClick = { ttsManager.nextSegment() },
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

                // Status message
                Text(
                    text = when (engineState) {
                        is TtsEngineState.Playing -> AppStrings.get("tts_playing", currentLanguage)
                        is TtsEngineState.Paused -> AppStrings.get("tts_paused", currentLanguage)
                        is TtsEngineState.Initializing -> AppStrings.get("tts_initializing", currentLanguage)
                        is TtsEngineState.Ready -> AppStrings.get("tts_ready", currentLanguage)
                        is TtsEngineState.Completed -> "Completed"
                        is TtsEngineState.MissingVoiceData -> "Voice Data Missing"
                        is TtsEngineState.Unsupported -> AppStrings.get("tts_unsupported", currentLanguage)
                        is TtsEngineState.Error -> AppStrings.get("tts_error", currentLanguage)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (engineState) {
                        is TtsEngineState.MissingVoiceData, is TtsEngineState.Unsupported, is TtsEngineState.Error -> Color(0xFFEF4444)
                        else -> NaturalDarkTextMuted
                    },
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
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

            // Action banner if voice data is missing
            if (engineState is TtsEngineState.MissingVoiceData) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get("tts_missing_data", currentLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            try {
                                context.startActivity(ttsManager.getVoiceDataInstallIntent())
                            } catch (_: Exception) {}
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                    ) {
                        Text(AppStrings.get("tts_open_settings", currentLanguage), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

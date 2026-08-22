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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceMode
import com.example.reader.TtsEngineState
import com.example.reader.TtsManager
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel

@Composable
fun TtsFloatingBar(
    viewModel: MainViewModel,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onOpenVoiceStudio: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ttsManager = viewModel.ttsManager
    val context = LocalContext.current
    val engineState by ttsManager.engineState.collectAsState()
    val speechRate by ttsManager.speechRate.collectAsState()
    val activeVoiceMode by viewModel.voiceProfileRepository.voiceMode.collectAsState()
    val customProfile by viewModel.voiceProfileRepository.voiceProfile.collectAsState()

    var showSpeedMenu by remember { mutableStateOf(false) }

    val isPlaying = engineState is TtsEngineState.Playing
    val currentSegmentIdx = when (val s = engineState) {
        is TtsEngineState.Playing -> s.segmentIndex
        is TtsEngineState.Paused -> s.segmentIndex
        else -> 0
    }
    val totalSegments = when (val s = engineState) {
        is TtsEngineState.Playing -> s.totalSegments
        is TtsEngineState.Paused -> s.totalSegments
        else -> 1
    }
    val currentTextSnippet = when (val s = engineState) {
        is TtsEngineState.Playing -> s.segment?.text
        is TtsEngineState.Paused -> s.segment?.text
        else -> null
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalDarkBorder.copy(alpha = 0.7f)),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Voice Mode Switcher & Quick Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Voice Mode Toggle Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) NaturalPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) NaturalPrimary else NaturalDarkBorder
                    ),
                    modifier = Modifier.clickable {
                        if (customProfile != null) {
                            val newMode = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) {
                                VoiceMode.SYSTEM_DEFAULT
                            } else {
                                VoiceMode.USER_CLONED_VOICE
                            }
                            viewModel.setVoiceMode(newMode)
                        } else {
                            onOpenVoiceStudio()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) Icons.Default.RecordVoiceOver else Icons.Default.SmartToy,
                            contentDescription = "Voice Mode",
                            tint = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) NaturalPrimary else NaturalDarkTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) "Cloned Voice (${customProfile?.name ?: "User"})" else "System Voice",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) NaturalPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Open Voice Studio Button
                    IconButton(
                        onClick = onOpenVoiceStudio,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Voice Studio Settings",
                            tint = NaturalPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Speed Dropdown
                    Box {
                        FilledTonalButton(
                            onClick = { showSpeedMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.background)
                        ) {
                            Text(
                                text = "${speechRate}x",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { rate ->
                                DropdownMenuItem(
                                    text = { Text("${rate}x Speed") },
                                    onClick = {
                                        viewModel.setTtsSpeed(rate)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = {
                            ttsManager.stop()
                            onClose()
                        },
                        modifier = Modifier.size(32.dp)
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

            // Middle: Active Reading Text Snippet
            if (currentTextSnippet != null) {
                Text(
                    text = currentTextSnippet,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Seek Bar & Segment Progress
            if (totalSegments > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = currentSegmentIdx.toFloat(),
                        onValueChange = { targetIdx ->
                            ttsManager.seekToSegment(targetIdx.toInt())
                        },
                        valueRange = 0f..(totalSegments - 1).toFloat(),
                        steps = if (totalSegments > 2) totalSegments - 2 else 0,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = NaturalPrimary,
                            activeTrackColor = NaturalPrimary,
                            inactiveTrackColor = NaturalDarkSurfaceVariant
                        )
                    )

                    Text(
                        text = "${currentSegmentIdx + 1}/$totalSegments",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }
            }

            // Bottom Controls: Playback Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Previous
                IconButton(
                    onClick = { ttsManager.previousSegment() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Segment",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Play / Pause
                FilledIconButton(
                    onClick = {
                        if (isPlaying) ttsManager.pause() else ttsManager.resume()
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = NaturalPrimary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = NaturalOnPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Next
                IconButton(
                    onClick = { ttsManager.nextSegment() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Segment",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
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

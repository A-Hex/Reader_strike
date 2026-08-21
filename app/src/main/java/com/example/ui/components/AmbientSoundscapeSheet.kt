package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reader.AmbientAudioEngine
import com.example.reader.SoundscapeType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientSoundscapeSheet(
    ambientEngine: AmbientAudioEngine,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSoundscape by ambientEngine.currentSoundscape.collectAsState()
    val volume by ambientEngine.volume.collectAsState()
    val timerRemaining by ambientEngine.timerMinutesRemaining.collectAsState()

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
            // Title Header
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NaturalPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = NaturalPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ambient Soundscapes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activeSoundscape == SoundscapeType.OFF) "Select relaxing soundscape for reading focus" else "Playing: ${activeSoundscape.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                }
            }

            // Soundscape Grid Options
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SoundscapeType.entries) { type ->
                    val isSelected = activeSoundscape == type
                    val iconVector: ImageVector = when (type) {
                        SoundscapeType.OFF -> Icons.Default.VolumeOff
                        SoundscapeType.RAIN -> Icons.Default.WaterDrop
                        SoundscapeType.FIREPLACE -> Icons.Default.LocalFireDepartment
                        SoundscapeType.CAFE -> Icons.Default.Coffee
                        SoundscapeType.FOREST -> Icons.Default.Forest
                        SoundscapeType.BINAURAL_ALPHA -> Icons.Default.Psychology
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ambientEngine.setSoundscape(if (isSelected && type != SoundscapeType.OFF) SoundscapeType.OFF else type)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NaturalPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) NaturalPrimary else NaturalDarkBorder.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) NaturalPrimary else NaturalDarkBorder.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = if (isSelected) NaturalOnPrimary else NaturalDarkText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) NaturalPrimary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = type.description,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = NaturalDarkTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Volume Slider (only when soundscape is active)
            if (activeSoundscape != SoundscapeType.OFF) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(16.dp))
                                Text("Ambience Volume", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                        }

                        Slider(
                            value = volume,
                            onValueChange = { ambientEngine.setVolume(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = NaturalPrimary,
                                activeTrackColor = NaturalPrimary,
                                inactiveTrackColor = NaturalDarkBorder
                            )
                        )
                    }
                }
            }

            // Sleep Timer
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = NaturalOchreAccent, modifier = Modifier.size(16.dp))
                            Text("Sleep Timer", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (timerRemaining > 0) {
                            Text(
                                text = "Stops in $timerRemaining min",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalOchreAccent
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m").forEach { (mins, label) ->
                            val isTimerSelected = (mins == 0 && timerRemaining == 0) || (mins > 0 && timerRemaining in (mins - 1)..mins)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { ambientEngine.setSleepTimer(mins) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isTimerSelected) NaturalOchreAccent else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isTimerSelected) NaturalOchreAccent else NaturalDarkBorder)
                            ) {
                                Text(
                                    text = label,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTimerSelected) NaturalOnPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SpeedReaderModal(
    chapterTitle: String,
    content: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val words = remember(content) {
        content.split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var wpm by remember { mutableIntStateOf(350) } // Words Per Minute

    // Timer loop for RSVP Speed Reading
    LaunchedEffect(isPlaying, wpm, currentIndex) {
        if (isPlaying && words.isNotEmpty() && currentIndex < words.size) {
            val currentWord = words[currentIndex]
            // Calculate delay: longer for punctuation
            var delayMs = 60_000L / wpm
            if (currentWord.endsWith(".") || currentWord.endsWith("!") || currentWord.endsWith("?")) {
                delayMs = (delayMs * 1.6).toLong()
            } else if (currentWord.endsWith(",") || currentWord.endsWith(";") || currentWord.endsWith(":")) {
                delayMs = (delayMs * 1.3).toLong()
            }

            delay(delayMs)
            if (currentIndex < words.size - 1) {
                currentIndex++
            } else {
                isPlaying = false
            }
        }
    }

    val currentWord = if (words.isNotEmpty() && currentIndex in words.indices) words[currentIndex] else "Ready"

    // Format current word with Optimal Recognition Point (ORP) highlight
    val formattedWord = remember(currentWord) {
        buildAnnotatedString {
            if (currentWord.length <= 1) {
                withStyle(SpanStyle(color = NaturalPrimary, fontWeight = FontWeight.Bold)) {
                    append(currentWord)
                }
            } else {
                val orpIndex = when (currentWord.length) {
                    2, 3 -> 1
                    4, 5 -> 1
                    6, 7 -> 2
                    8, 9 -> 3
                    else -> 4
                }
                val prefix = currentWord.substring(0, orpIndex)
                val focalChar = currentWord.substring(orpIndex, orpIndex + 1)
                val suffix = currentWord.substring(orpIndex + 1)

                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
                    append(prefix)
                }
                withStyle(SpanStyle(color = NaturalOchreAccent, fontWeight = FontWeight.Black)) {
                    append(focalChar)
                }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
                    append(suffix)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = NaturalPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = NaturalPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "RSVP Speed Reader",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalPrimary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted,
                    maxLines = 1
                )

                // RSVP Focus Display Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F1410))
                        .border(1.dp, NaturalDarkBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Vertical guide lines for eye centering
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.size(width = 2.dp, height = 12.dp).background(NaturalOchreAccent))
                        Box(modifier = Modifier.size(width = 2.dp, height = 12.dp).background(NaturalOchreAccent))
                    }

                    Text(
                        text = formattedWord,
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                }

                // Progress Info & Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Word ${currentIndex + 1} of ${words.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                        val progressPercent = if (words.isNotEmpty()) ((currentIndex.toFloat() / words.size) * 100).toInt() else 0
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary
                        )
                    }
                    Slider(
                        value = if (words.isNotEmpty()) currentIndex.toFloat() else 0f,
                        onValueChange = {
                            currentIndex = it.toInt()
                            isPlaying = false
                        },
                        valueRange = 0f..(words.size - 1).coerceAtLeast(0).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = NaturalPrimary,
                            activeTrackColor = NaturalPrimary,
                            inactiveTrackColor = NaturalDarkBorder
                        )
                    )
                }

                // Speed (WPM) Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Speed: $wpm WPM",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NaturalDarkText
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { wpm = (wpm - 50).coerceAtLeast(150) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-50", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        listOf(250, 400, 600).forEach { presetWpm ->
                            Surface(
                                color = if (wpm == presetWpm) NaturalPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { wpm = presetWpm }
                            ) {
                                Text(
                                    text = "$presetWpm",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (wpm == presetWpm) NaturalOnPrimary else NaturalDarkText
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { wpm = (wpm + 50).coerceAtMost(900) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+50", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Playback Navigation Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentIndex = (currentIndex - 25).coerceAtLeast(0)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = "Back 25 words", tint = NaturalDarkText)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    FloatingActionButton(
                        onClick = { isPlaying = !isPlaying },
                        containerColor = NaturalPrimary,
                        contentColor = NaturalOnPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            currentIndex = (currentIndex + 25).coerceAtMost(words.size - 1)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 25 words", tint = NaturalDarkText)
                    }
                }
            }
        }
    }
}

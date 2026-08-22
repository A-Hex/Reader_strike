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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.reader.RsvpSpeedReaderEngine
import com.example.reader.RsvpToken
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import kotlinx.coroutines.delay

@Composable
fun SpeedReaderModal(
    chapterTitle: String,
    content: String,
    onDismiss: () -> Unit,
    currentPage: Int = 1,
    totalPages: Int = 1,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onPageChange: ((Int) -> Unit)? = null,
    onTokensRead: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = remember(content) {
        val list = RsvpSpeedReaderEngine.tokenize(content)
        if (list.isEmpty()) {
            listOf(
                RsvpToken("1", "Ready", false, 1.0f, 1),
                RsvpToken("2", "to", false, 1.0f, 0),
                RsvpToken("3", "Read", false, 1.0f, 1)
            )
        } else list
    }

    var currentIndex by remember(content) { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var wpm by remember { mutableIntStateOf(350) } // Words Per Minute
    var readTokensCountSincePlay by remember { mutableIntStateOf(0) }

    // Accurate ticker loop with punctuation delays and token progression
    LaunchedEffect(isPlaying, wpm, currentIndex, tokens) {
        if (isPlaying && tokens.isNotEmpty() && currentIndex < tokens.size) {
            val currentToken = tokens[currentIndex]
            val baseDelay = 60_000L / wpm
            val totalDelay = (baseDelay * currentToken.delayMultiplier).toLong()

            delay(totalDelay)
            readTokensCountSincePlay++
            if (readTokensCountSincePlay % 25 == 0) {
                onTokensRead?.invoke(25)
            }

            if (currentIndex < tokens.size - 1) {
                currentIndex++
            } else {
                if (onPageChange != null && currentPage < totalPages) {
                    onTokensRead?.invoke(readTokensCountSincePlay % 25)
                    readTokensCountSincePlay = 0
                    delay(350)
                    onPageChange(currentPage + 1)
                } else {
                    isPlaying = false
                    onTokensRead?.invoke(readTokensCountSincePlay % 25)
                    readTokensCountSincePlay = 0
                }
            }
        }
    }

    val currentToken = if (tokens.isNotEmpty() && currentIndex in tokens.indices) tokens[currentIndex] else null
    val currentWordText = currentToken?.text ?: "Ready"
    val isRtlWord = currentToken?.isRtl ?: false

    // Direction-safe ORP formatting
    val formattedWord = remember(currentToken, currentWordText) {
        buildAnnotatedString {
            if (isRtlWord) {
                // Arabic: Uniform elegant styling preserving ligatures without destructive splitting
                withStyle(SpanStyle(color = Color(0xFFF0FDF4), fontWeight = FontWeight.Bold)) {
                    append(currentWordText)
                }
            } else {
                if (currentWordText.length <= 1) {
                    withStyle(SpanStyle(color = NaturalOchreAccent, fontWeight = FontWeight.Bold)) {
                        append(currentWordText)
                    }
                } else {
                    val focalIdx = currentToken?.focalCharIndex ?: 1
                    val prefix = currentWordText.substring(0, focalIdx)
                    val focalChar = currentWordText.substring(focalIdx, focalIdx + 1)
                    val suffix = currentWordText.substring(focalIdx + 1)

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
    }

    Dialog(
        onDismissRequest = {
            onTokensRead?.invoke(readTokensCountSincePlay % 25)
            onDismiss()
        },
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
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                                text = AppStrings.get("rsvp_title", currentLanguage),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onTokensRead?.invoke(readTokensCountSincePlay % 25)
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                // Title & Page Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = NaturalDarkText,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (onPageChange != null && totalPages > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                                enabled = currentPage > 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page", tint = if (currentPage > 1) NaturalPrimary else NaturalDarkBorder)
                            }
                            Text(
                                text = "Pg $currentPage/$totalPages",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                            IconButton(
                                onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                                enabled = currentPage < totalPages,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = if (currentPage < totalPages) NaturalPrimary else NaturalDarkBorder)
                            }
                        }
                    }
                }

                // RSVP Focus Display Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0D120E))
                        .border(1.5.dp, NaturalDarkBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Vertical guide lines for eye centering
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.size(width = 3.dp, height = 14.dp).background(NaturalOchreAccent))
                        Box(modifier = Modifier.size(width = 3.dp, height = 14.dp).background(NaturalOchreAccent))
                    }

                    CompositionLocalProvider(
                        LocalLayoutDirection provides (if (isRtlWord) LayoutDirection.Rtl else LayoutDirection.Ltr)
                    ) {
                        Text(
                            text = formattedWord,
                            fontSize = if (isRtlWord) 34.sp else 32.sp,
                            fontFamily = if (isRtlWord) FontFamily.Default else FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            letterSpacing = if (isRtlWord) 0.sp else 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Progress Info & Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Word ${currentIndex + 1} / ${tokens.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                        val remainingSeconds = RsvpSpeedReaderEngine.calculateRemainingSeconds(tokens, currentIndex + 1, wpm)
                        val timeStr = if (remainingSeconds > 60) "${remainingSeconds / 60}m ${remainingSeconds % 60}s" else "${remainingSeconds}s"
                        Text(
                            text = "~$timeStr left",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                        val progressPercent = RsvpSpeedReaderEngine.calculateProgressPercent(currentIndex, tokens.size)
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary
                        )
                    }
                    Slider(
                        value = if (tokens.isNotEmpty()) currentIndex.toFloat() else 0f,
                        onValueChange = {
                            currentIndex = it.toInt().coerceIn(0, tokens.size - 1)
                            isPlaying = false
                        },
                        valueRange = 0f..(tokens.size - 1).coerceAtLeast(0).toFloat(),
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
                        text = "$wpm WPM",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NaturalDarkText
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { wpm = (wpm - 50).coerceAtLeast(150) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Text("-50", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        listOf(250, 350, 500, 700).forEach { presetWpm ->
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
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { wpm = (wpm + 50).coerceAtMost(900) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Text("+50", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            currentIndex = (currentIndex - 20).coerceAtLeast(0)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = "Back 20 words", tint = NaturalDarkText)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    FloatingActionButton(
                        onClick = { isPlaying = !isPlaying },
                        containerColor = NaturalPrimary,
                        contentColor = NaturalOnPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            currentIndex = (currentIndex + 20).coerceAtMost(tokens.size - 1)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 20 words", tint = NaturalDarkText)
                    }
                }
            }
        }
    }
}

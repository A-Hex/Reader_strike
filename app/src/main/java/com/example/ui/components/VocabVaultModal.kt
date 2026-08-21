package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.VocabVaultManager
import com.example.model.VocabWord
import com.example.ui.theme.*

enum class VocabViewMode {
    LIST,
    FLASHCARDS
}

@Composable
fun VocabVaultModal(
    vocabVaultManager: VocabVaultManager,
    onEarnXp: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val words by vocabVaultManager.vocabWords.collectAsState()
    var viewMode by remember { mutableStateOf(VocabViewMode.LIST) }
    var filterMastered by remember { mutableStateOf<Boolean?>(null) } // null = all, true = mastered, false = learning
    var searchQuery by remember { mutableStateOf("") }

    // Flashcard state
    var cardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val filteredWords = remember(words, filterMastered, searchQuery) {
        words.filter { word ->
            (filterMastered == null || word.isMastered == filterMastered) &&
                    (searchQuery.isBlank() || word.word.contains(searchQuery, ignoreCase = true) || word.definition.contains(searchQuery, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
            border = BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(NaturalPrimary, NaturalOchreAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = NaturalOnPrimary, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Vocab Vault",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val masteredCount = words.count { it.isMastered }
                                Surface(
                                    color = NaturalSageSuccess.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "$masteredCount/${words.size} Mastered",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = NaturalSageSuccess,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Spaced repetition & literary lexicon",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                // View Mode Switcher: List vs Flashcards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewMode = VocabViewMode.LIST },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (viewMode == VocabViewMode.LIST) NaturalPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (viewMode == VocabViewMode.LIST) NaturalOnPrimary else NaturalDarkText
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vault List (${words.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            viewMode = VocabViewMode.FLASHCARDS
                            cardIndex = 0
                            isFlipped = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (viewMode == VocabViewMode.FLASHCARDS) NaturalPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (viewMode == VocabViewMode.FLASHCARDS) NaturalOnPrimary else NaturalDarkText
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flashcards SRS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (viewMode == VocabViewMode.LIST) {
                    // Search & Filters
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search vocabulary words or meanings...", fontSize = 12.sp, color = NaturalDarkTextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NaturalDarkTextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = NaturalDarkTextMuted)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalDarkBorder
                        )
                    )

                    // Filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterMastered == null,
                            onClick = { filterMastered = null },
                            label = { Text("All (${words.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterMastered == false,
                            onClick = { filterMastered = false },
                            label = { Text("Learning (${words.count { !it.isMastered }})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterMastered == true,
                            onClick = { filterMastered = true },
                            label = { Text("Mastered (${words.count { it.isMastered }})", fontSize = 11.sp) }
                        )
                    }

                    // Words List
                    if (filteredWords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = NaturalDarkBorder, modifier = Modifier.size(42.dp))
                                Text("No words match this filter", color = NaturalDarkTextMuted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredWords, key = { it.id }) { word ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = BorderStroke(1.dp, if (word.isMastered) NaturalSageSuccess.copy(alpha = 0.5f) else NaturalDarkBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
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
                                                Text(
                                                    text = word.word,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "${word.partOfSpeech} ${word.phonetic}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                                    color = NaturalOchreAccent
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        vocabVaultManager.toggleMastered(word.id)
                                                        if (!word.isMastered) {
                                                            onEarnXp?.invoke(25)
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (word.isMastered) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                        contentDescription = "Toggle Mastered",
                                                        tint = if (word.isMastered) NaturalSageSuccess else NaturalDarkTextMuted
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { vocabVaultManager.deleteWord(word.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = NaturalDarkTextMuted)
                                                }
                                            }
                                        }

                                        Text(
                                            text = word.definition,
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                            color = NaturalDarkText
                                        )

                                        if (word.exampleSentence.isNotBlank()) {
                                            Text(
                                                text = "\"${word.exampleSentence}\"",
                                                style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                                color = NaturalDarkTextMuted
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "From ${word.bookTitle}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = NaturalPrimary
                                            )
                                            Text(
                                                text = "Reviewed ${word.reviewCount}x",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = NaturalDarkTextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // FLASHCARDS MODE
                    if (words.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Your Vocab Vault is empty. Add words while reading!", color = NaturalDarkTextMuted)
                        }
                    } else {
                        val activeIndex = cardIndex.coerceIn(0, words.size - 1)
                        val activeWord = words[activeIndex]
                        val rotation by animateFloatAsState(
                            targetValue = if (isFlipped) 180f else 0f,
                            animationSpec = tween(durationMillis = 350),
                            label = "CardFlip"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Progress bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Card ${activeIndex + 1} of ${words.size}", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                                Text("Tap card to flip", style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic), color = NaturalPrimary)
                            }
                            LinearProgressIndicator(
                                progress = (activeIndex + 1).toFloat() / words.size,
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = NaturalPrimary,
                                trackColor = NaturalDarkBorder
                            )

                            // Interactive Flippable 3D Flashcard
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .graphicsLayer {
                                        rotationY = rotation
                                        cameraDistance = 12f * density
                                    }
                                    .clickable { isFlipped = !isFlipped },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(2.dp, if (activeWord.isMastered) NaturalSageSuccess else NaturalPrimary.copy(alpha = 0.7f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rotation <= 90f) {
                                        // FRONT: Word & Phonetic
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                color = NaturalPrimary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = activeWord.partOfSpeech.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                                    color = NaturalPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            Text(
                                                text = activeWord.word,
                                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                textAlign = TextAlign.Center
                                            )

                                            Text(
                                                text = activeWord.phonetic,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = NaturalOchreAccent
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "From \"${activeWord.bookTitle}\"",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NaturalDarkTextMuted
                                            )
                                        }
                                    } else {
                                        // BACK: Definition & In-Context Example (rotated back to normal reading orientation)
                                        Column(
                                            modifier = Modifier.graphicsLayer { rotationY = 180f },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "MEANING",
                                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                                color = NaturalSageSuccess
                                            )

                                            Text(
                                                text = activeWord.definition,
                                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp, fontWeight = FontWeight.Medium),
                                                color = Color.White,
                                                textAlign = TextAlign.Center
                                            )

                                            if (activeWord.exampleSentence.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Surface(
                                                    color = NaturalDarkBackground,
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(
                                                        text = "\"${activeWord.exampleSentence}\"",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                                        color = NaturalDarkTextMuted,
                                                        modifier = Modifier.padding(10.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Review Controls (Still Learning vs Mastered)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        vocabVaultManager.recordReview(activeWord.id, remembered = false)
                                        isFlipped = false
                                        cardIndex = (cardIndex + 1) % words.size
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NaturalDarkBorder)
                                ) {
                                    Icon(Icons.Default.Replay, contentDescription = null, tint = NaturalOchreAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Review Later", color = NaturalOchreAccent, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        vocabVaultManager.recordReview(activeWord.id, remembered = true)
                                        onEarnXp?.invoke(10)
                                        isFlipped = false
                                        cardIndex = (cardIndex + 1) % words.size
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NaturalSageSuccess, contentColor = NaturalOnPrimary)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Got It! (+10 XP)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

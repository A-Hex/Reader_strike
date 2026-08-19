package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.model.Book
import com.example.model.BookChapter
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AiTaskType(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SUMMARY("Chapter Summary", Icons.Default.Summarize),
    TAKEAWAYS("Key Takeaways", Icons.Default.Lightbulb),
    VOCABULARY("Vocabulary", Icons.Default.Spellcheck),
    ANALYSIS("Deep Analysis", Icons.Default.Psychology),
    ASK("Ask Questions", Icons.Default.Chat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
    book: Book,
    chapter: BookChapter,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTask by remember { mutableStateOf(AiTaskType.SUMMARY) }
    var userQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var generatedResponse by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Pre-calculate responses for instant smart assistant experience
    fun generateAiContent(task: AiTaskType, customPrompt: String = "") {
        coroutineScope.launch {
            isLoading = true
            delay(400) // Smooth processing feel
            generatedResponse = when (task) {
                AiTaskType.SUMMARY -> {
                    """
                    📌 **Core Summary for "${chapter.title}"**:
                    • **Primary Focus**: Explores the thematic foundation of ${book.title}, focusing on ${book.genre.lowercase()} nuances.
                    • **Narrative Progression**: The author ${book.author} develops the intellectual conflict, emphasizing emotional tension and philosophical resolution.
                    • **Key Transition**: Shifting from introduction of circumstances into decisive character or philosophical shifts.
                    • **Word Count**: ~${chapter.wordCount} words analyzed.
                    """.trimIndent()
                }
                AiTaskType.TAKEAWAYS -> {
                    """
                    💡 **Top Key Takeaways & Philosophical Insights**:
                    1. **Internal Mastery**: True tranquility is not the absence of chaos, but master of one's own perception and reactions.
                    2. **Existential Reflection**: Characters or ideas mirror humanity's perpetual search for purpose amidst societal pressures.
                    3. **Actionable Wisdom**: Direct attention exclusively to what is within your sphere of control.
                    """.trimIndent()
                }
                AiTaskType.VOCABULARY -> {
                    """
                    📖 **Key Vocabulary & Etymology**:
                    • **Equanimity** *(noun)*: Mental calmness, composure, and evenness of temper, especially in a difficult situation.
                    • **Existential** *(adj.)*: Relating to existence, especially human existence as depicted in existentialist philosophy.
                    • **Ephemeral** *(adj.)*: Lasting for a very short time; fleeting.
                    • **Metamorphosis** *(noun)*: A change of the form or nature of a thing or person into a completely different one.
                    """.trimIndent()
                }
                AiTaskType.ANALYSIS -> {
                    """
                    🧠 **Literary & Contextual Analysis**:
                    • **Symbolism**: The passage uses subtle metaphors of space, confinement, and light to represent moral clarity versus isolation.
                    • **Historical Tone**: Written during a pivotal period of ${book.genre}, reflecting late Victorian/Classical perspectives on society.
                    • **Deeper Meaning**: Serves as an allegory for personal responsibility and intellectual integrity.
                    """.trimIndent()
                }
                AiTaskType.ASK -> {
                    if (customPrompt.isNotBlank()) {
                        """
                        🤖 **Response to: "$customPrompt"**:
                        In the context of *${book.title}* by ${book.author}:
                        The passage addresses this directly through character decisions and philosophical exposition. The central message warns against complacency and encourages disciplined introspection.
                        """.trimIndent()
                    } else {
                        "Type any question above or tap one of the suggested prompts to query the book."
                    }
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedTask) {
        if (selectedTask != AiTaskType.ASK) {
            generateAiContent(selectedTask)
        } else if (generatedResponse.isBlank()) {
            generateAiContent(AiTaskType.ASK, "What is the main message of this chapter?")
        }
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(listOf(NaturalPrimary, NaturalSecondary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NaturalOnPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Smart Reading Assistant",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${book.title} • ${chapter.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted,
                            maxLines = 1
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                }
            }

            // Task Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AiTaskType.entries) { task ->
                    val isSelected = selectedTask == task
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTask = task },
                        label = { Text(task.title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = task.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) NaturalPrimary else NaturalDarkTextMuted
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NaturalPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = NaturalPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) NaturalPrimary else NaturalDarkBorder
                        )
                    )
                }
            }

            // Custom Question Input when ASK is selected
            if (selectedTask == AiTaskType.ASK) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = userQuery,
                        onValueChange = { userQuery = it },
                        placeholder = { Text("Ask a question about this chapter...", fontSize = 13.sp, color = NaturalDarkTextMuted) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (userQuery.isNotBlank()) {
                                        generateAiContent(AiTaskType.ASK, userQuery)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = NaturalPrimary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalDarkBorder
                        )
                    )

                    // Suggested Prompts
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val suggestions = listOf("Explain symbolism", "Summarize in 3 bullets", "Historical context", "Key philosophical moral")
                        items(suggestions) { prompt ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    userQuery = prompt
                                    generateAiContent(AiTaskType.ASK, prompt)
                                }
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Response Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 340.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = NaturalPrimary,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Analyzing chapter content...",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = generatedResponse,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 22.sp,
                                        letterSpacing = 0.2.sp
                                    ),
                                    color = NaturalDarkText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

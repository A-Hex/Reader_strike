package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.model.BookChapter
import com.example.ui.theme.*
import com.example.util.AiReadingAssistantEngine
import com.example.util.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AiTaskType(
    val titleEn: String,
    val titleAr: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ANALYSIS("Deep Analysis", "التحليل والعمق", Icons.Default.Psychology),
    CHARACTER_MAP("Character Map", "خريطة الشخصيات", Icons.Default.AccountTree),
    PLOT_BREAKDOWN("Plot Breakdown", "الحبكة والأحداث", Icons.Default.Timeline),
    RSVP_VOICE("RSVP & Voice Prep", "القراءة السريعة والنطق", Icons.Default.Bolt),
    SUMMARY("Summary & Key Points", "الملخص والفوائد", Icons.Default.Summarize),
    VOCABULARY("Vocabulary", "المفردات والسياق", Icons.Default.Spellcheck),
    ASK("Ask & Quiz", "حوار واختبار", Icons.Default.Chat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
    book: Book,
    chapter: BookChapter,
    onDismiss: () -> Unit,
    onSaveToNotes: ((String) -> Unit)? = null,
    onStartSpeedReading: ((String) -> Unit)? = null,
    currentLanguage: AppLanguage = AppLanguage.ARABIC,
    modifier: Modifier = Modifier
) {
    var selectedTask by remember { mutableStateOf(AiTaskType.ANALYSIS) }
    var userQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var generatedResponse by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isArabicBook = book.languageCode == "ar" || book.title.any { it in '\u0600'..'\u06FF' }
    val isRtl = isArabicBook || currentLanguage.isRtl

    // Dynamically generate deep, realistic reading assistant responses from actual content
    fun generateAiContent(task: AiTaskType, customPrompt: String = "") {
        coroutineScope.launch {
            isLoading = true
            delay(300) // Smooth conversational processing feel
            generatedResponse = when (task) {
                AiTaskType.ANALYSIS -> {
                    AiReadingAssistantEngine.generateDeepAnalysis(book, chapter.title, chapter.content)
                }
                AiTaskType.CHARACTER_MAP -> {
                    AiReadingAssistantEngine.generateCharacterMapText(book, chapter.title, chapter.content)
                }
                AiTaskType.PLOT_BREAKDOWN -> {
                    AiReadingAssistantEngine.generatePlotBreakdown(book, chapter.title, chapter.content)
                }
                AiTaskType.RSVP_VOICE -> {
                    AiReadingAssistantEngine.generateRsvpAndVoicePrep(book, chapter.title, chapter.content)
                }
                AiTaskType.SUMMARY -> {
                    AiReadingAssistantEngine.generateSummary(book, chapter.title, chapter.content)
                }
                AiTaskType.VOCABULARY -> {
                    AiReadingAssistantEngine.extractVocabulary(chapter.content)
                }
                AiTaskType.ASK -> {
                    val promptToUse = customPrompt.ifBlank { if (isArabicBook) "ما هي الفكرة الجوهرية لهذا المقطع؟" else "What is the main premise of this passage?" }
                    AiReadingAssistantEngine.answerQuery(book, chapter.title, chapter.content, promptToUse)
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedTask, chapter.title, chapter.content) {
        if (selectedTask != AiTaskType.ASK) {
            generateAiContent(selectedTask)
        } else if (generatedResponse.isBlank()) {
            generateAiContent(AiTaskType.ASK, if (isArabicBook) "ما هي الفكرة الجوهرية لهذا المقطع؟" else "What is the main premise of this passage?")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NaturalDarkBorder) },
        modifier = modifier
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                .size(38.dp)
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
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isRtl) "المساعد والرفيق الذكي للقراءة" else "Reading Companion & Guide",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = NaturalPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isRtl) "تحليل ذكي" else "Text Guide",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                        color = NaturalPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
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
                    val chipTitle = if (isArabicBook) task.titleAr else task.titleEn
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTask = task },
                        label = { Text(chipTitle, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
                        placeholder = {
                            Text(
                                if (isArabicBook) "اطرح أي سؤال حول المقطع أو اختر من المقترحات..." else "Ask any question or tap Quiz below...",
                                fontSize = 13.sp,
                                color = NaturalDarkTextMuted
                            )
                        },
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
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalDarkBorder
                        )
                    )

                    // Suggested Prompts
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val suggestions = if (isArabicBook) {
                            listOf(
                                "📝 اختبار الفهم السريع (3 أسئلة)",
                                "🎯 ما هو الصراع المحوري في هذا المشهد؟",
                                "🏛️ السياق الأدبي والرمزي",
                                "💡 كيف نطبق هذه الحكمة في واقعنا؟",
                                "🔍 اشرح الجملة الختامية ومغزاها"
                            )
                        } else {
                            listOf(
                                "📝 3-Question Quiz",
                                "🎯 What is the core dilemma?",
                                "🏛️ Historical & Literary Context",
                                "💡 Practical Life Application",
                                "🔍 Explain the concluding thought"
                            )
                        }
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
                                    color = NaturalDarkText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Response Box with Copy & Note Saving actions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 360.dp),
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
                                text = "Analyzing passage structure and vocabulary...",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
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

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = NaturalDarkBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom actions: Copy and Save Note
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Chapter Study Notes", generatedResponse))
                                        Toast.makeText(context, if (isRtl) "تم نسخ التحليل إلى الحافظة!" else "Copied insight to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = NaturalPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isRtl) "نسخ" else "Copy", fontSize = 12.sp, color = NaturalPrimary)
                                }

                                if (onSaveToNotes != null) {
                                    TextButton(
                                        onClick = {
                                            onSaveToNotes(generatedResponse)
                                            Toast.makeText(context, if (isRtl) "تم الحفظ في ملاحظات الكتاب!" else "Saved to library notes!", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = NaturalOchreAccent)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isRtl) "حفظ في الملاحظات" else "Save Note", fontSize = 12.sp, color = NaturalOchreAccent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

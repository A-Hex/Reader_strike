package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiCredentials
import com.example.ai.LocalLlmModel
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.AssistantEngine
import com.example.data.repository.AssistantProgress
import com.example.data.repository.AssistantResult
import com.example.data.repository.AssistantTask
import com.example.model.Book
import com.example.model.BookChapter
import com.example.ui.theme.*
import com.example.util.AppLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class AiTaskType(
    val titleEn: String,
    val titleAr: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val task: AssistantTask
) {
    ANALYSIS("Deep Analysis", "التحليل والعمق", Icons.Default.Psychology, AssistantTask.DEEP_ANALYSIS),
    CHARACTER_MAP("Character Map", "خريطة الشخصيات", Icons.Default.AccountTree, AssistantTask.CHARACTER_MAP),
    PLOT_BREAKDOWN("Plot Breakdown", "الحبكة والأحداث", Icons.Default.Timeline, AssistantTask.PLOT_BREAKDOWN),
    RSVP_VOICE("RSVP & Voice Prep", "القراءة السريعة والنطق", Icons.Default.Bolt, AssistantTask.RSVP_VOICE_PREP),
    SUMMARY("Summary & Key Points", "الملخص والفوائد", Icons.Default.Summarize, AssistantTask.SUMMARY_AND_KEY_POINTS),
    VOCABULARY("Vocabulary", "المفردات والسياق", Icons.Default.Spellcheck, AssistantTask.VOCABULARY),
    ASK("Ask & Quiz", "حوار واختبار", Icons.Default.Chat, AssistantTask.ASK)
}

/**
 * The answer text. While an on-device model is still writing, the newest words are kept in view so
 * the reader follows the answer instead of hunting for it.
 */
@Composable
private fun AnswerBody(
    markdown: String,
    followWhileGrowing: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    if (followWhileGrowing) {
        LaunchedEffect(markdown.length) {
            // Let the new text be measured before following it, or the scroll lands one frame short.
            withFrameNanos { }
            scrollState.scrollTo(scrollState.maxValue)
        }
    }
    Column(modifier = modifier.verticalScroll(scrollState)) {
        Text(
            text = markdown,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, letterSpacing = 0.2.sp),
            color = NaturalDarkText
        )
    }
}

private sealed interface AssistantUiState {
    object Idle : AssistantUiState
    data class Loading(val message: String) : AssistantUiState

    /** Text arriving from the on-device model while it is still writing. */
    data class Streaming(val markdown: String, val engine: AssistantEngine) : AssistantUiState

    data class Ready(
        val markdown: String,
        val engine: AssistantEngine,
        /** True when the reader stopped the model part way through. */
        val stoppedEarly: Boolean = false
    ) : AssistantUiState

    data class Failed(val message: String, val offline: String?) : AssistantUiState
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
    var uiState by remember { mutableStateOf<AssistantUiState>(AssistantUiState.Idle) }
    var engineInUse by remember { mutableStateOf<AssistantEngine?>(null) }
    var streamJob by remember { mutableStateOf<Job?>(null) }
    // Bumped for every run and every stop, so a cancelled or superseded generation can never write
    // into the sheet after the reader has moved on.
    var runToken by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = remember(context) { AiAssistantRepository(context) }
    // Re-read per composition (SharedPreferences is memory-cached) so a key or model installed in
    // Settings is picked up the next time the sheet opens instead of being frozen at first compose.
    val cloudConfigured = AiCredentials.hasApiKey(context)
    val localModelReady = LocalLlmModel.isInstalled(context)
    val localModelPreferred = localModelReady && repository.prefersLocalModel()

    val isArabicBook = book.languageCode == "ar" || book.title.any { it in '\u0600'..'\u06FF' }
    val isRtl = isArabicBook || currentLanguage.isRtl

    fun applyResult(result: AssistantResult) {
        when (result) {
            is AssistantResult.Success -> {
                engineInUse = result.engine
                uiState = AssistantUiState.Ready(result.markdown, result.engine)
            }

            is AssistantResult.Failure -> {
                engineInUse = null
                uiState = AssistantUiState.Failed(result.message, result.offlineMarkdown)
            }
        }
    }

    fun runTask(task: AiTaskType, prompt: String = "") {
        val token = ++runToken
        streamJob?.cancel()
        streamJob = coroutineScope.launch {
            uiState = AssistantUiState.Loading(
                when {
                    localModelPreferred && isArabicBook -> "يعمل النموذج على جهازك..."
                    localModelPreferred -> "Writing on your device..."
                    isArabicBook -> "جارٍ تحليل المقطع..."
                    else -> "Analysing the passage..."
                }
            )
            repository.runStreaming(
                task = task.task,
                book = book,
                chapter = chapter,
                query = prompt
            ).collect { progress ->
                if (token != runToken) return@collect
                when (progress) {
                    // Repaint with the answer so far while the on-device model keeps writing.
                    is AssistantProgress.Partial -> {
                        engineInUse = progress.engine
                        uiState = AssistantUiState.Streaming(progress.markdown, progress.engine)
                    }

                    is AssistantProgress.Done -> applyResult(progress.result)
                }
            }
        }
    }

    /** Stops the on-device model and keeps whatever it had already written. */
    fun stopStreaming() {
        val streaming = uiState as? AssistantUiState.Streaming
        val partial = streaming?.markdown?.trim().orEmpty()
        val engine = streaming?.engine ?: AssistantEngine.LOCAL_LLM

        runToken++
        streamJob?.cancel()
        streamJob = null

        uiState = if (partial.isEmpty()) {
            AssistantUiState.Idle
        } else {
            AssistantUiState.Ready(partial, engine, stoppedEarly = true)
        }
    }

    LaunchedEffect(selectedTask, chapter) {
        if (selectedTask != AiTaskType.ASK) {
            runTask(selectedTask)
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
                                .background(Brush.linearGradient(listOf(NaturalPrimary, NaturalSecondary))),
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
                                    text = if (isRtl) "المساعد والرفيق الذكي للقراءة" else "Reading Companion",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val localLabel = if (isRtl) "نموذج محلي" else "Local model"
                                val extractionLabel = if (isRtl) "على الجهاز" else "On-device"
                                val badgeLabel = when {
                                    engineInUse == AssistantEngine.LOCAL_LLM -> localLabel
                                    engineInUse == AssistantEngine.GEMINI -> "Gemini"
                                    engineInUse == AssistantEngine.ON_DEVICE -> extractionLabel
                                    localModelPreferred -> localLabel
                                    cloudConfigured -> "Gemini"
                                    localModelReady -> localLabel
                                    else -> extractionLabel
                                }
                                Surface(
                                    color = NaturalPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        ),
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

                // Exactly one honest status line: a real local model, a cloud key, or neither.
                when {
                    localModelReady -> Surface(
                        color = NaturalSageBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NaturalSageAccent.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isRtl) "نموذج محلي على الجهاز" else "On-device model",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalSageAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isRtl) {
                                    "التحليل يجري على جهازك عبر ${repository.localModelName()}. لا يُرسَل النص إلى أي خدمة. أوقف «تفضيل النموذج المحلي» من الإعدادات لاستخدام مفتاح Gemini."
                                } else {
                                    "Analysis runs on this device with ${repository.localModelName()}. Your text is never uploaded. Turn off \"Prefer the on-device model\" in Settings to use a Gemini key instead."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalSageAccent
                            )
                        }
                    }

                    !cloudConfigured -> Surface(
                        color = NaturalOchreBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NaturalOchreBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isRtl) "وضع التحليل المحلي مُفعّل" else "On-device extraction mode",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalOchreAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isRtl) {
                                    "لا يوجد نموذج محلي ولا مفتاح Gemini. النتائج الحالية مستخرجة من النص على جهازك (اقتباسات وإحصاءات حقيقية). لتشغيل نموذج لغوي، أضِفه من الإعدادات ← نموذج على الجهاز، أو أضف مفتاح Gemini."
                                } else {
                                    "No on-device model and no Gemini API key. Results come from real on-device extraction (verbatim quotes and statistics). To run a real LLM offline, add a model bundle in Settings → On-device model, or add a Gemini key."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalOchreMuted
                            )
                        }
                    }
                }

                // Task selector chips
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
                            label = {
                                Text(
                                    chipTitle,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
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

                // Question input for ASK
                if (selectedTask == AiTaskType.ASK) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = userQuery,
                            onValueChange = { userQuery = it },
                            placeholder = {
                                Text(
                                    if (isArabicBook) "اطرح سؤالاً حول هذا المقطع..." else "Ask a question about this passage...",
                                    fontSize = 13.sp,
                                    color = NaturalDarkTextMuted
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    enabled = userQuery.isNotBlank(),
                                    onClick = { if (userQuery.isNotBlank()) runTask(AiTaskType.ASK, userQuery) }
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

                        val suggestions = if (isArabicBook) {
                            listOf(
                                "ما الفكرة المركزية في هذا المقطع؟",
                                "اشرح الجملة الختامية ومغزاها",
                                "ما الصراع الظاهر في هذا المشهد؟",
                                "اختبار فهم من النص"
                            )
                        } else {
                            listOf(
                                "What is the central idea of this passage?",
                                "Explain the concluding sentence",
                                "What conflict is visible here?",
                                "Quiz me on this passage"
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(suggestions) { prompt ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable {
                                        userQuery = prompt
                                        runTask(AiTaskType.ASK, prompt)
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

                // Result area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 360.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
                    border = BorderStroke(1.dp, NaturalDarkBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        when (val state = uiState) {
                            is AssistantUiState.Loading -> {
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
                                        text = state.message,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalDarkTextMuted
                                    )
                                }
                            }

                            is AssistantUiState.Idle -> {
                                Text(
                                    text = if (isArabicBook) {
                                        "اختر نوع التحليل أو اطرح سؤالاً."
                                    } else {
                                        "Pick an analysis type or ask a question."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalDarkTextMuted,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            is AssistantUiState.Failed -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (isRtl) "تعذّر إكمال الطلب" else "Request could not be completed",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NaturalDarkText
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(onClick = { runTask(selectedTask, userQuery) }) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isRtl) "إعادة المحاولة" else "Retry", fontSize = 12.sp)
                                        }
                                        if (!state.offline.isNullOrBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    engineInUse = AssistantEngine.ON_DEVICE
                                                    uiState = AssistantUiState.Ready(state.offline, AssistantEngine.ON_DEVICE)
                                                }
                                            ) {
                                                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(if (isRtl) "نتيجة محلية" else "Use on-device", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            is AssistantUiState.Streaming -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                color = NaturalPrimary,
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = if (isRtl) "النموذج يكتب على جهازك..." else "Writing on your device...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NaturalPrimary
                                            )
                                        }
                                        TextButton(
                                            onClick = { stopStreaming() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp), tint = NaturalPrimary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isRtl) "إيقاف" else "Stop", fontSize = 12.sp, color = NaturalPrimary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    AnswerBody(
                                        markdown = state.markdown,
                                        followWhileGrowing = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    )
                                }
                            }

                            is AssistantUiState.Ready -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (state.stoppedEarly) {
                                                if (isRtl) {
                                                    "تم الإيقاف — إجابة جزئية، راجع الاقتباسات"
                                                } else {
                                                    "Stopped early — partial answer, verify quotations"
                                                }
                                            } else when (state.engine) {
                                                AssistantEngine.LOCAL_LLM ->
                                                    if (isRtl) {
                                                        "مُولّد على جهازك بواسطة نموذج محلي — راجع الاقتباسات"
                                                    } else {
                                                        "Generated on this device by a local model — verify quotations"
                                                    }

                                                AssistantEngine.GEMINI ->
                                                    if (isRtl) {
                                                        "مُولّد بواسطة Gemini — راجع الاقتباسات"
                                                    } else {
                                                        "Generated with Gemini — verify quotations"
                                                    }

                                                AssistantEngine.ON_DEVICE ->
                                                    if (isRtl) {
                                                        "استخلاص محلي — اقتباسات حرفية وإحصاءات"
                                                    } else {
                                                        "On-device extraction — verbatim quotes and statistics"
                                                    }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NaturalDarkTextMuted,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    AnswerBody(
                                        markdown = state.markdown,
                                        followWhileGrowing = false,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = NaturalDarkBorder.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Chapter Study Notes", state.markdown))
                                                Toast.makeText(
                                                    context,
                                                    if (isRtl) "تم نسخ التحليل!" else "Copied insight to clipboard!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
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
                                                    onSaveToNotes(state.markdown)
                                                    Toast.makeText(
                                                        context,
                                                        if (isRtl) "تم الحفظ في ملاحظات الكتاب!" else "Saved to library notes!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = NaturalOchreAccent)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isRtl) "حفظ" else "Save Note", fontSize = 12.sp, color = NaturalOchreAccent)
                                            }
                                        }

                                        if (onStartSpeedReading != null) {
                                            TextButton(
                                                onClick = { onStartSpeedReading(state.markdown) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp), tint = NaturalSageAccent)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isRtl) "قراءة سريعة" else "Speed Read", fontSize = 12.sp, color = NaturalSageAccent)
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
}

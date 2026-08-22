package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.*
import com.example.reader.FacePresenceState
import com.example.reader.PdfLoadResult
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val book by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val currentChapterIndex by viewModel.currentChapterIndex.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val readerPreferences by viewModel.readerPreferences.collectAsState()
    val highlights by viewModel.currentBookHighlights.collectAsState()
    val bookmarks by viewModel.currentBookBookmarks.collectAsState()
    val inBookSearchQuery by viewModel.inBookSearchQuery.collectAsState()
    val inBookSearchResults by viewModel.inBookSearchResults.collectAsState()
    val allReviews by viewModel.allReviews.collectAsState()
    val streakData by viewModel.streakData.collectAsState()
    val activeSessionState by viewModel.activeSessionState.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val facePresenceState by viewModel.facePresenceEngine.presenceState.collectAsState()
    val isFaceAssistedEnabled by viewModel.isFaceAssistedEnabled.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showHighlightSheet by remember { mutableStateOf(false) }
    var showTtsBar by remember { mutableStateOf(false) }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }
    var showSpeedReader by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }
    var showSoundscapes by remember { mutableStateOf(false) }
    var showWordInspector by remember { mutableStateOf("") }
    var inspectingWord by remember { mutableStateOf("") }
    var inspectingSentence by remember { mutableStateOf("") }
    var showVocabVault by remember { mutableStateOf(false) }
    var showMindMap by remember { mutableStateOf(false) }
    var showEpubExport by remember { mutableStateOf(false) }
    var showVoiceAuthDialog by remember { mutableStateOf(false) }
    var selectedTextForHighlight by remember { mutableStateOf("") }

    val ttsEngineState by viewModel.ttsManager.engineState.collectAsState()
    val ttsPlaying = ttsEngineState is com.example.reader.TtsEngineState.Playing
    val ttsSentenceIndex = when (val s = ttsEngineState) {
        is com.example.reader.TtsEngineState.Playing -> s.segmentIndex
        is com.example.reader.TtsEngineState.Paused -> s.segmentIndex
        else -> -1
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val currentBookObj = book ?: return

    // Face Presence Lifecycle binding
    LaunchedEffect(isFaceAssistedEnabled, lifecycleOwner) {
        if (isFaceAssistedEnabled) {
            viewModel.facePresenceEngine.startAnalyzing(lifecycleOwner)
        } else {
            viewModel.facePresenceEngine.stopAnalyzing()
        }
    }

    // Resolve Theme via ThemeManager
    val activeTheme = com.example.util.ThemeManager.getThemeById(readerPreferences.themeId)

    // Auto-scroll loop with safe cancellation and bounds check
    val listState = rememberLazyListState()
    LaunchedEffect(readerPreferences.isAutoScrollActive) {
        if (readerPreferences.isAutoScrollActive) {
            while (readerPreferences.isAutoScrollActive) {
                delay(300)
                if (!listState.isScrollInProgress) {
                    if (listState.canScrollForward) {
                        listState.scrollBy(10f)
                    } else {
                        break
                    }
                }
            }
        }
    }

    // Active reading interaction tracking on scroll
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            viewModel.recordUserInteraction()
        }
    }

    // PDF Page Rendering for PDF books
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPdfLoading by remember { mutableStateOf(false) }
    var pdfErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentBookObj.id, currentPage) {
        if (currentBookObj.format == BookFormat.PDF) {
            isPdfLoading = true
            pdfErrorMessage = null
            if (currentBookObj.localFilePath != null) {
                val file = File(currentBookObj.localFilePath)
                if (file.exists()) {
                    when (val openResult = viewModel.pdfManager.openPdfFile(file)) {
                        is PdfLoadResult.Success -> {
                            val rendered = viewModel.pdfManager.renderPage(currentPage - 1, 1080)
                            if (rendered != null) {
                                pdfBitmap = rendered
                            } else {
                                pdfErrorMessage = "Could not render PDF page $currentPage"
                            }
                        }
                        is PdfLoadResult.Error -> {
                            pdfErrorMessage = openResult.message
                        }
                    }
                } else {
                    pdfErrorMessage = "PDF file not found."
                }
            } else {
                pdfErrorMessage = "No local file for PDF sample rendering."
            }
            isPdfLoading = false
        }
    }

    val currentFontFamily = when (readerPreferences.fontFamily) {
        FontFamilyPreference.SERIF -> FontFamily.Serif
        FontFamilyPreference.SANS_SERIF -> FontFamily.SansSerif
        FontFamilyPreference.MONOSPACE -> FontFamily.Monospace
        FontFamilyPreference.CURSIVE -> FontFamily.Cursive
    }

    // Determine layout direction based on language or book language
    val isArabic = currentBookObj.languageCode == "ar" || currentLanguage == AppLanguage.ARABIC
    val textDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides textDirection) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(activeTheme.backgroundColor)
        ) {
            // Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                viewModel.recordUserInteraction()
                                showControls = !showControls
                            }
                        )
                    }
            ) {
                // Spacer for Top Bar
                Spacer(modifier = Modifier.height(if (showControls) 70.dp else 24.dp))

                if (currentBookObj.format == BookFormat.PDF) {
                    // PDF Viewer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = readerPreferences.horizontalMarginDp.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPdfLoading) {
                            CircularProgressIndicator(color = activeTheme.accentColor)
                        } else if (pdfBitmap != null) {
                            Image(
                                bitmap = pdfBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page $currentPage",
                                colorFilter = com.example.util.ThemeManager.getPdfColorFilter(activeTheme),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else if (pdfErrorMessage != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = activeTheme.accentColor, modifier = Modifier.size(36.dp))
                                Text(
                                    text = "PDF Viewer Notice",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = activeTheme.textColor)
                                )
                                Text(
                                    text = pdfErrorMessage ?: "Failed to display PDF",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = activeTheme.textColor.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // EPUB & TXT Fluid Typography Viewer
                    val activeChapter = chapters.getOrNull(currentChapterIndex) ?: BookChapter(0, "Chapter", "No text content found.")
                    val paragraphs = remember(activeChapter.content) {
                        activeChapter.content.split("\n\n").filter { it.isNotBlank() }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = readerPreferences.horizontalMarginDp.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = activeChapter.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = currentFontFamily,
                                    color = activeTheme.accentColor
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        itemsIndexed(paragraphs) { pIdx, paragraph ->
                            val matchingHighlight = highlights.find { hl ->
                                hl.chapterIndex == currentChapterIndex && paragraph.contains(hl.text)
                            }
                            val isTtsFocus = ttsPlaying && ttsSentenceIndex == pIdx

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            matchingHighlight != null -> matchingHighlight.color.toComposeColor().copy(alpha = 0.25f)
                                            isTtsFocus -> activeTheme.accentColor.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isTtsFocus) 1.dp else 0.dp,
                                        color = if (isTtsFocus) activeTheme.accentColor.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                    .combinedClickable(
                                        onClick = {
                                            selectedTextForHighlight = paragraph.take(180)
                                            showHighlightSheet = true
                                        },
                                        onLongClick = {
                                            val candidateWords = paragraph.split(" ", ",", ".", ";", "\"", "—", "-")
                                                .map { it.trim().trim('“', '”', '‘', '’', '"', '\'') }
                                                .filter { it.length >= 3 }
                                            val chosen = candidateWords.firstOrNull { it.length > 5 } ?: candidateWords.firstOrNull() ?: "literature"
                                            inspectingWord = chosen
                                            inspectingSentence = paragraph
                                            showWordInspector = chosen
                                        }
                                    )
                            ) {
                                Text(
                                    text = paragraph,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = readerPreferences.fontSizeSp.sp,
                                        lineHeight = (readerPreferences.fontSizeSp * readerPreferences.lineSpacingMultiplier).sp,
                                        letterSpacing = readerPreferences.letterSpacingSp.sp,
                                        fontFamily = currentFontFamily,
                                        color = activeTheme.textColor
                                    ),
                                    textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                            if (currentChapterIndex < chapters.size - 1) {
                                Button(
                                    onClick = {
                                        viewModel.selectChapter(currentChapterIndex + 1)
                                        coroutineScope.launch { listState.scrollToItem(0) }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.surfaceColor),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${chapters.getOrNull(currentChapterIndex + 1)?.title ?: "Next Chapter"} →",
                                        color = activeTheme.accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Night Light warm overlay
            if (readerPreferences.isNightLightFilter) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                )
            }

            // Top Controls Bar
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = activeTheme.surfaceColor.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = {
                                viewModel.closeReader()
                                onClose()
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = activeTheme.textColor)
                            }
                            Column {
                                Text(
                                    text = currentBookObj.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = activeTheme.textColor,
                                    maxLines = 1
                                )
                                Text(
                                    text = chapters.getOrNull(currentChapterIndex)?.title ?: "Page $currentPage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeTheme.accentColor,
                                    maxLines = 1
                                )
                            }
                        }

                        // Action Icons
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            IconButton(onClick = { showSoundscapes = true }) {
                                Icon(Icons.Default.Headphones, contentDescription = "Ambient Soundscapes", tint = NaturalForestAccent)
                            }

                            IconButton(onClick = { showAiAssistant = true }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Reading Assistant", tint = NaturalOchreAccent)
                            }

                            IconButton(onClick = { showMindMap = true }) {
                                Icon(Icons.Default.Hub, contentDescription = "Visual Mind Map", tint = activeTheme.accentColor)
                            }

                            IconButton(onClick = { showVocabVault = true }) {
                                Icon(Icons.Default.School, contentDescription = "Vocabulary Vault", tint = activeTheme.textColor)
                            }

                            IconButton(onClick = { showSpeedReader = true }) {
                                Icon(Icons.Default.Bolt, contentDescription = "Speed Reader (RSVP)", tint = activeTheme.accentColor)
                            }

                            IconButton(onClick = {
                                if (viewModel.voiceAuthRepository.isVoiceAuthEnabled.value && !viewModel.voiceAuthRepository.isVerified) {
                                    showVoiceAuthDialog = true
                                } else {
                                    showTtsBar = !showTtsBar
                                    if (showTtsBar) viewModel.playTtsForCurrentChapter()
                                }
                            }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Audio Reader", tint = if (showTtsBar) activeTheme.accentColor else activeTheme.textColor)
                            }

                            IconButton(onClick = { showSearchSheet = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = activeTheme.textColor)
                            }

                            IconButton(onClick = { showEpubExport = true }) {
                                Icon(Icons.Default.Transform, contentDescription = "Export to EPUB", tint = activeTheme.accentColor)
                            }

                            IconButton(onClick = { showReviewsSheet = true }) {
                                Icon(Icons.Default.RateReview, contentDescription = "Book Reviews", tint = activeTheme.textColor)
                            }

                            IconButton(onClick = { showShareModal = true }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Reading Progress", tint = activeTheme.textColor)
                            }

                            IconButton(onClick = { showTocSheet = true }) {
                                Icon(Icons.Default.List, contentDescription = "Table of Contents", tint = activeTheme.textColor)
                            }

                            IconButton(onClick = { showThemeSheet = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "Customizer", tint = activeTheme.textColor)
                            }
                        }
                    }
                }
            }

            // Floating TTS Player Bar
            if (showTtsBar) {
                TtsFloatingBar(
                    ttsManager = viewModel.ttsManager,
                    currentLanguage = currentLanguage,
                    onClose = { showTtsBar = false },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 74.dp)
                )
            }

            // Floating Active Session Timer HUD
            AnimatedVisibility(
                visible = showControls || activeSessionState.isIdle || activeSessionState.isPaused || (isFaceAssistedEnabled && facePresenceState == FacePresenceState.NoFace),
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (showControls && !showTtsBar) 68.dp else if (showTtsBar) 140.dp else 24.dp)
            ) {
                ActiveSessionTimerHUD(
                    sessionState = activeSessionState,
                    streakData = streakData,
                    facePresenceState = facePresenceState,
                    isFaceAssistedEnabled = isFaceAssistedEnabled,
                    onTogglePause = { viewModel.toggleActiveSessionPause() },
                    onUserInteraction = { viewModel.recordUserInteraction() }
                )
            }

            // Bottom Controls Bar (Page slider & Navigation)
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = activeTheme.surfaceColor.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Page navigation slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.previousPage() },
                                enabled = currentPage > 1
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Page", tint = activeTheme.textColor)
                            }

                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { viewModel.setPage(it.toInt()) },
                                valueRange = 1f..currentBookObj.totalPages.toFloat().coerceAtLeast(1f),
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = activeTheme.accentColor,
                                    activeTrackColor = activeTheme.accentColor
                                )
                            )

                            IconButton(
                                onClick = { viewModel.nextPage() },
                                enabled = currentPage < currentBookObj.totalPages
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = activeTheme.textColor)
                            }
                        }

                        // Page progress and auto-scroll button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page $currentPage of ${currentBookObj.totalPages}  (${(currentPage.toFloat() / currentBookObj.totalPages.coerceAtLeast(1) * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = activeTheme.textColor
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { viewModel.toggleAutoScroll() },
                                    label = { Text(if (readerPreferences.isAutoScrollActive) "Auto-Scroll: ON" else "Auto-Scroll", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.KeyboardDoubleArrowDown,
                                            contentDescription = null,
                                            tint = if (readerPreferences.isAutoScrollActive) activeTheme.accentColor else activeTheme.textColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal Sheets
        if (showVoiceAuthDialog) {
            VoiceprintDialog(
                viewModel = viewModel,
                mode = VoiceAuthMode.VERIFY,
                currentLanguage = currentLanguage,
                onSuccess = {
                    showVoiceAuthDialog = false
                    showTtsBar = true
                    viewModel.playTtsForCurrentChapter()
                },
                onDismiss = { showVoiceAuthDialog = false }
            )
        }

        if (showThemeSheet) {
            ReaderThemeSheet(
                preferences = readerPreferences,
                onUpdateTheme = { viewModel.updateReaderTheme(it) },
                onSelectReadingMode = { viewModel.selectReadingMode(it) },
                onUpdateFontSize = { viewModel.updateFontSize(it) },
                onUpdateLineSpacing = { viewModel.updateLineSpacing(it) },
                onUpdateFontFamily = { viewModel.updateFontFamily(it) },
                onUpdateMargins = { viewModel.updateMargins(it) },
                onToggleNightLight = { viewModel.toggleNightLight(it) },
                onUpdateBrightness = { viewModel.updateBrightness(it) },
                onTogglePagedMode = { viewModel.togglePagedMode() },
                bookTitle = currentBookObj.title,
                onDismiss = { showThemeSheet = false }
            )
        }

        if (showTocSheet) {
            TableOfContentsDialog(
                chapters = chapters,
                currentChapterIndex = currentChapterIndex,
                bookmarks = bookmarks,
                highlights = highlights,
                onSelectChapter = { index -> viewModel.selectChapter(index) },
                onSelectBookmark = { bm -> viewModel.setPage(bm.page) },
                onDismiss = { showTocSheet = false }
            )
        }

        if (showSearchSheet) {
            InBookSearchDialog(
                searchQuery = inBookSearchQuery,
                results = inBookSearchResults,
                onQueryChange = { viewModel.searchInCurrentBook(it) },
                onSelectResult = { _ ->
                    showSearchSheet = false
                },
                onDismiss = { showSearchSheet = false }
            )
        }

        if (showHighlightSheet) {
            HighlightsBottomSheet(
                selectedText = selectedTextForHighlight,
                onSaveHighlight = { color, note ->
                    viewModel.addHighlight(selectedTextForHighlight, note, color)
                },
                onDismiss = { showHighlightSheet = false }
            )
        }

        if (showReviewsSheet) {
            val bookReviews = allReviews.filter { it.bookId == currentBookObj.id }
            BookReviewsSheet(
                book = currentBookObj,
                reviews = bookReviews,
                onDismiss = { showReviewsSheet = false },
                onSubmitReview = { rating, title, text, userName ->
                    viewModel.submitReview(currentBookObj.id, currentBookObj.title, rating, title, text, userName)
                },
                onDeleteReview = { id -> viewModel.deleteReview(id) },
                onHelpfulClick = { id -> viewModel.incrementReviewHelpful(id) }
            )
        }

        if (showShareModal) {
            SocialShareModal(
                contentType = ShareContentType.BookProgress(currentBookObj, streakData),
                onDismiss = { showShareModal = false }
            )
        }

        if (showSpeedReader) {
            val activeText = viewModel.getCurrentReadingText()
            val isPdf = currentBookObj.format == BookFormat.PDF
            val activeTitle = if (isPdf) "Page $currentPage • ${currentBookObj.title}" else (chapters.getOrNull(currentChapterIndex)?.title ?: currentBookObj.title)
            SpeedReaderModal(
                chapterTitle = activeTitle,
                content = if (activeText.isNotBlank()) activeText else "Start reading ${currentBookObj.title}...",
                currentPage = currentPage,
                totalPages = currentBookObj.totalPages,
                onPageChange = { newPage -> viewModel.setPage(newPage) },
                onTokensRead = { tokens -> viewModel.recordSpeedReadTokens(tokens) },
                onDismiss = { showSpeedReader = false }
            )
        }

        if (showAiAssistant) {
            val activeChapter = viewModel.getCurrentReadingChapter()
            AiAssistantSheet(
                book = currentBookObj,
                chapter = activeChapter,
                onDismiss = { showAiAssistant = false },
                onSaveToNotes = { noteText ->
                    viewModel.addHighlight(
                        text = "${activeChapter.title} - AI Insight",
                        note = noteText,
                        color = HighlightColor.AMBER
                    )
                }
            )
        }

        if (showSoundscapes) {
            AmbientSoundscapeSheet(
                ambientEngine = viewModel.ambientEngine,
                onDismiss = { showSoundscapes = false }
            )
        }

        if (showWordInspector.isNotBlank()) {
            WordInspectorDialog(
                initialWord = inspectingWord,
                bookTitle = currentBookObj.title,
                sentenceContext = inspectingSentence,
                onAddToVault = { newWord ->
                    viewModel.vocabVaultManager.addWord(newWord)
                    viewModel.questsManager.recordWordLookup()
                },
                onDismiss = { showWordInspector = "" }
            )
        }

        if (showVocabVault) {
            VocabVaultModal(
                vocabVaultManager = viewModel.vocabVaultManager,
                onEarnXp = { xp -> viewModel.questsManager.addXp(xp) },
                onDismiss = { showVocabVault = false }
            )
        }

        if (showMindMap) {
            CharacterMindMapDialog(
                book = currentBookObj,
                onDismiss = { showMindMap = false }
            )
        }

        if (showEpubExport) {
            val bookHighlights by viewModel.currentBookHighlights.collectAsState()
            EpubConverterModal(
                book = currentBookObj,
                highlights = bookHighlights,
                onOpenConvertedBook = { convertedBook ->
                    viewModel.openBook(convertedBook)
                },
                onImportEpubToLibrary = { file, title, author ->
                    viewModel.addConvertedEpubToLibrary(file, title, author, autoOpen = false)
                },
                onDismiss = { showEpubExport = false }
            )
        }
    }
}

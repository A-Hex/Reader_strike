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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedTextForHighlight by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val currentBookObj = book ?: return

    // Resolve Theme
    val activeTheme = ReaderTheme.ALL_THEMES.find { it.id == readerPreferences.themeId } ?: ReaderTheme.Obsidian

    // Auto-scroll loop
    val listState = rememberLazyListState()
    LaunchedEffect(readerPreferences.isAutoScrollActive) {
        if (readerPreferences.isAutoScrollActive) {
            while (true) {
                delay(300)
                listState.scrollBy(12f)
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

    LaunchedEffect(currentBookObj.id, currentPage) {
        if (currentBookObj.format == BookFormat.PDF) {
            isPdfLoading = true
            if (currentBookObj.localFilePath != null) {
                val file = File(currentBookObj.localFilePath)
                if (file.exists()) {
                    viewModel.pdfManager.openPdfFile(file)
                    pdfBitmap = viewModel.pdfManager.renderPage(currentPage - 1, 1080)
                }
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

            if (currentBookObj.format == BookFormat.PDF && pdfBitmap != null) {
                // PDF Viewer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = readerPreferences.horizontalMarginDp.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = pdfBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page $currentPage",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
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

                    itemsIndexed(paragraphs) { pIndex, paragraph ->
                        val matchingHighlight = highlights.find { hl ->
                            hl.chapterIndex == currentChapterIndex && paragraph.contains(hl.text)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (matchingHighlight != null) matchingHighlight.color.toComposeColor().copy(alpha = 0.25f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .clickable {
                                    selectedTextForHighlight = paragraph.take(180)
                                    showHighlightSheet = true
                                }
                        ) {
                            Text(
                                text = paragraph,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = readerPreferences.fontSizeSp.sp,
                                    lineHeight = (readerPreferences.fontSizeSp * readerPreferences.lineSpacingMultiplier).sp,
                                    letterSpacing = readerPreferences.letterSpacingSp.sp,
                                    fontFamily = currentFontFamily,
                                    color = activeTheme.textColor
                                )
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                        // Next chapter button at the bottom of the chapter
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
                                    text = "Next: ${chapters.getOrNull(currentChapterIndex + 1)?.title ?: "Next Chapter"} →",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        // AI Assistant
                        IconButton(onClick = { showAiAssistant = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Reading Assistant", tint = NaturalOchreAccent)
                        }

                        // RSVP Speed Reader
                        IconButton(onClick = { showSpeedReader = true }) {
                            Icon(Icons.Default.Bolt, contentDescription = "Speed Reader (RSVP)", tint = activeTheme.accentColor)
                        }

                        // TTS Audio Playback
                        IconButton(onClick = {
                            showTtsBar = !showTtsBar
                            if (showTtsBar) viewModel.playTtsForCurrentChapter()
                        }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Audio Reader", tint = if (showTtsBar) activeTheme.accentColor else activeTheme.textColor)
                        }

                        // Search in Book
                        IconButton(onClick = { showSearchSheet = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = activeTheme.textColor)
                        }

                        // Write/View Reviews
                        IconButton(onClick = { showReviewsSheet = true }) {
                            Icon(Icons.Default.RateReview, contentDescription = "Book Reviews", tint = activeTheme.textColor)
                        }

                        // Share Progress
                        IconButton(onClick = { showShareModal = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Reading Progress", tint = activeTheme.textColor)
                        }

                        // Table of Contents
                        IconButton(onClick = { showTocSheet = true }) {
                            Icon(Icons.Default.List, contentDescription = "Table of Contents", tint = activeTheme.textColor)
                        }

                        // Theme & Typography Settings
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
                onClose = { showTtsBar = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 74.dp)
            )
        }

        // Floating Active Session Timer HUD
        AnimatedVisibility(
            visible = showControls || activeSessionState.isIdle || activeSessionState.isPaused,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (showControls && !showTtsBar) 68.dp else if (showTtsBar) 140.dp else 24.dp)
        ) {
            ActiveSessionTimerHUD(
                sessionState = activeSessionState,
                streakData = streakData,
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
                            // Auto-scroll toggle
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
    if (showThemeSheet) {
        ReaderThemeSheet(
            preferences = readerPreferences,
            onUpdateTheme = { viewModel.updateReaderTheme(it) },
            onUpdateFontSize = { viewModel.updateFontSize(it) },
            onUpdateLineSpacing = { viewModel.updateLineSpacing(it) },
            onUpdateFontFamily = { viewModel.updateFontFamily(it) },
            onUpdateMargins = { viewModel.updateMargins(it) },
            onToggleNightLight = { viewModel.toggleNightLight(it) },
            onUpdateBrightness = { viewModel.updateBrightness(it) },
            onTogglePagedMode = { viewModel.togglePagedMode() },
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
            onSelectResult = { snippet ->
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
        val currentChapter = chapters.getOrNull(currentChapterIndex)
        SpeedReaderModal(
            chapterTitle = currentChapter?.title ?: currentBookObj.title,
            content = currentChapter?.content ?: "Start reading ${currentBookObj.title}...",
            onDismiss = { showSpeedReader = false }
        )
    }

    if (showAiAssistant) {
        val currentChapter = chapters.getOrNull(currentChapterIndex) ?: BookChapter(
            index = 0,
            title = currentBookObj.title,
            content = "Chapter overview for ${currentBookObj.title}",
            wordCount = 1200
        )
        AiAssistantSheet(
            book = currentBookObj,
            chapter = currentChapter,
            onDismiss = { showAiAssistant = false }
        )
    }
}

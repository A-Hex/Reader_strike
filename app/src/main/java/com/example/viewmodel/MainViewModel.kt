package com.example.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SampleBooksData
import com.example.data.repository.BookRepository
import com.example.data.repository.LocalBackupRepository
import com.example.data.repository.QuestsAndShieldsManager
import com.example.data.repository.VocabVaultManager
import com.example.data.repository.VoiceProfileRepository
import com.example.model.*
import com.example.reader.*
import com.example.receiver.ReadingStreakWidgetProvider
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.BackupResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    val bookRepository = BookRepository(context, database)
    val localBackupRepository = LocalBackupRepository(database)

    val pdfManager = PdfManager(context)
    val ttsManager = TtsManager(context)
    val ambientEngine = AmbientAudioEngine()
    val vocabVaultManager = VocabVaultManager(context)
    val questsManager = QuestsAndShieldsManager(context)

    // Voice Narration Profile Studio (TTS Book Reading)
    val voiceProfileRepository = VoiceProfileRepository(context)
    val audioRecorder = AudioRecorder(context)

    // Smart Privacy-First Face Presence Tracking
    val facePresenceEngine = FacePresenceEngine(context)

    // Library UI state
    val allBooks = bookRepository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow(ReadingStatus.ALL)
    val selectedStatus: StateFlow<ReadingStatus> = _selectedStatus.asStateFlow()

    private val _selectedFormat = MutableStateFlow<BookFormat?>(null)
    val selectedFormat: StateFlow<BookFormat?> = _selectedFormat.asStateFlow()

    private val _selectedSort = MutableStateFlow(SortOption.RECENTLY_READ)
    val selectedSort: StateFlow<SortOption> = _selectedSort.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val prefs = context.getSharedPreferences("ahex_reader_prefs", Context.MODE_PRIVATE)
    private val _dailyGoalMinutes = MutableStateFlow(prefs.getInt("daily_goal_minutes", 20))
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    private val _isFaceAssistedEnabled = MutableStateFlow(prefs.getBoolean("face_assisted_enabled", false))
    val isFaceAssistedEnabled: StateFlow<Boolean> = _isFaceAssistedEnabled.asStateFlow()

    fun setFaceAssistedEnabled(enabled: Boolean) {
        _isFaceAssistedEnabled.value = enabled
        prefs.edit().putBoolean("face_assisted_enabled", enabled).apply()
        if (!enabled) {
            facePresenceEngine.stopAnalyzing()
        }
    }

    fun updateDailyGoal(minutes: Int) {
        val validMinutes = minutes.coerceIn(5, 240)
        _dailyGoalMinutes.value = validMinutes
        prefs.edit().putInt("daily_goal_minutes", validMinutes).apply()

        val currentToday = _streakData.value.todayMinutesRead
        _activeSessionState.value = _activeSessionState.value.copy(
            dailyGoalMinutes = validMinutes,
            isDailyGoalReached = currentToday >= validMinutes
        )
        refreshStreakData()
        Toast.makeText(context, "Daily reading goal set to $validMinutes minutes", Toast.LENGTH_SHORT).show()
    }

    private val _currentLanguage = MutableStateFlow(
        when (prefs.getString("app_language", "en")) {
            "ar" -> AppLanguage.ARABIC
            "fr" -> AppLanguage.FRENCH
            else -> AppLanguage.ENGLISH
        }
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()
        ttsManager.configureForLanguage(language)
        Toast.makeText(context, "Language changed to ${language.displayName}", Toast.LENGTH_SHORT).show()
    }

    // Filtered Books
    val filteredBooks: StateFlow<List<Book>> = combine(
        allBooks,
        _searchQuery,
        _selectedStatus,
        _selectedFormat,
        _selectedSort
    ) { books, query, status, format, sort ->
        var list = books

        if (status != ReadingStatus.ALL) {
            list = when (status) {
                ReadingStatus.FAVORITES -> list.filter { it.isFavorite }
                ReadingStatus.DOWNLOADED -> list.filter { it.isDownloaded }
                else -> list.filter { it.status == status }
            }
        }

        if (format != null) {
            list = list.filter { it.format == format }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { book ->
                book.title.lowercase().contains(q) ||
                book.author.lowercase().contains(q) ||
                book.genre.lowercase().contains(q) ||
                book.tags.any { it.lowercase().contains(q) } ||
                book.description.lowercase().contains(q)
            }
        }

        when (sort) {
            SortOption.RECENTLY_READ -> list.sortedByDescending { it.lastReadTimestamp }
            SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOption.AUTHOR -> list.sortedBy { it.author.lowercase() }
            SortOption.PROGRESS -> list.sortedByDescending { it.readingProgress }
            SortOption.DATE_ADDED -> list.sortedByDescending { it.addedTimestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reader state
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    private val _currentChapters = MutableStateFlow<List<BookChapter>>(emptyList())
    val currentChapters: StateFlow<List<BookChapter>> = _currentChapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _readerPreferences = MutableStateFlow(ReaderPreferences())
    val readerPreferences: StateFlow<ReaderPreferences> = _readerPreferences.asStateFlow()

    private val _inBookSearchQuery = MutableStateFlow("")
    val inBookSearchQuery: StateFlow<String> = _inBookSearchQuery.asStateFlow()

    private val _inBookSearchResults = MutableStateFlow<List<String>>(emptyList())
    val inBookSearchResults: StateFlow<List<String>> = _inBookSearchResults.asStateFlow()

    // Active Reading Session State & Precise Timer
    private val _activeSessionState = MutableStateFlow(ActiveSessionState())
    val activeSessionState: StateFlow<ActiveSessionState> = _activeSessionState.asStateFlow()

    private var sessionAccumulatedSeconds: Long = 0L
    private var sessionPagesTurned: Int = 0
    private var isGoalCelebratedThisSession: Boolean = false
    private var noFaceConsecutiveSeconds: Int = 0

    // Highlights & Bookmarks
    val allHighlights = bookRepository.allHighlights.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentBookHighlights: StateFlow<List<Highlight>> = _currentBook.flatMapLatest { book ->
        if (book != null) bookRepository.getHighlightsForBook(book.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentBookBookmarks: StateFlow<List<Bookmark>> = _currentBook.flatMapLatest { book ->
        if (book != null) bookRepository.getBookmarksForBook(book.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reviews & Community Ratings
    val allReviews = bookRepository.allReviews.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getReviewsForBook(bookId: String): Flow<List<BookReview>> {
        return bookRepository.getReviewsForBook(bookId)
    }

    // Streak & Stats state
    private val _streakData = MutableStateFlow(ReadingStreakData())
    val streakData: StateFlow<ReadingStreakData> = _streakData.asStateFlow()

    // Discover catalog
    private val _catalogBooks = MutableStateFlow(SampleBooksData.DISCOVER_CATALOG)
    val catalogBooks: StateFlow<List<Book>> = _catalogBooks.asStateFlow()

    val bookRecommendations: StateFlow<List<BookRecommendation>> = combine(
        allBooks,
        allHighlights,
        _catalogBooks
    ) { library, highlights, catalog ->
        com.example.recommendation.RecommendationEngine.generateRecommendations(
            userLibrary = library,
            userHighlights = highlights,
            catalog = catalog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localBackupInfo = localBackupRepository.backupInfo

    private var readingSessionJob: Job? = null

    init {
        viewModelScope.launch {
            bookRepository.seedInitialDataIfEmpty()
            refreshStreakData()
        }

        // Sync initial voice profile settings with TTS engine
        val profile = voiceProfileRepository.voiceProfile.value
        val mode = voiceProfileRepository.voiceMode.value
        ttsManager.setVoiceProfileConfig(mode, profile)
    }

    fun setVoiceMode(mode: VoiceMode) {
        voiceProfileRepository.setVoiceMode(mode)
        val profile = voiceProfileRepository.voiceProfile.value
        ttsManager.setVoiceProfileConfig(mode, profile)
        val msg = if (mode == VoiceMode.USER_CLONED_VOICE) "Custom Voice Narrator activated!" else "Default System Voice activated"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun setTtsSpeed(rate: Float) {
        ttsManager.setSpeed(rate)
        if (voiceProfileRepository.voiceMode.value == VoiceMode.USER_CLONED_VOICE) {
            voiceProfileRepository.updateSpeed(rate)
        }
    }

    fun setTtsPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
        if (voiceProfileRepository.voiceMode.value == VoiceMode.USER_CLONED_VOICE) {
            voiceProfileRepository.updatePitch(pitch)
        }
    }

    fun testVoiceNarration(pitch: Float = 1.0f, speed: Float = 1.0f, onComplete: () -> Unit = {}) {
        val sample = "Books are the quietest and most constant of friends; they are the most accessible and wisest of counselors."
        ttsManager.speakTextSample(sample, pitch, speed) {
            onComplete()
        }
    }

    fun refreshStreakData() {
        viewModelScope.launch {
            val data = bookRepository.calculateStreakData(dailyGoalMinutes = _dailyGoalMinutes.value)
            _streakData.value = data
            questsManager.updateStreak(data.currentStreakDays)

            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ReadingStreakWidgetProvider::class.java)
                )
                if (ids.isNotEmpty()) {
                    for (id in ids) {
                        ReadingStreakWidgetProvider.updateAppWidget(context, appWidgetManager, id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedStatus(status: ReadingStatus) {
        _selectedStatus.value = status
    }

    fun setSelectedFormat(format: BookFormat?) {
        _selectedFormat.value = format
    }

    fun setSelectedSort(sort: SortOption) {
        _selectedSort.value = sort
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            bookRepository.toggleFavorite(book.id, book.isFavorite)
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
            Toast.makeText(context, "Book removed from library", Toast.LENGTH_SHORT).show()
        }
    }

    fun openBook(book: Book) {
        _currentBook.value = book
        _currentPage.value = book.currentPage.coerceAtLeast(1)

        // Load chapters & extract text
        if (book.localFilePath != null) {
            val file = File(book.localFilePath)
            if (file.exists()) {
                if (book.format == BookFormat.EPUB) {
                    file.inputStream().use { stream ->
                        val parsed = EpubParser.parseEpubStream(stream, book.title)
                        _currentChapters.value = parsed.chapters
                    }
                } else if (book.format == BookFormat.TXT) {
                    file.inputStream().use { stream ->
                        val parsed = EpubParser.parsePlainTextStream(stream, book.title)
                        _currentChapters.value = parsed.chapters
                    }
                } else if (book.format == BookFormat.PDF) {
                    val extracted = PdfTextExtractor.extractChaptersFromPdf(file, book.title)
                    if (extracted.isNotEmpty()) {
                        _currentChapters.value = extracted
                    } else {
                        _currentChapters.value = SampleBooksData.getSampleChaptersForBook(book.id)
                    }
                }
            } else {
                _currentChapters.value = SampleBooksData.getSampleChaptersForBook(book.id)
            }
        } else {
            _currentChapters.value = SampleBooksData.getSampleChaptersForBook(book.id)
        }

        _currentChapterIndex.value = 0

        // Load saved theme for this specific book
        val bookTheme = com.example.util.ThemeManager.getThemeForBook(context, book.id)
        _readerPreferences.value = _readerPreferences.value.copy(themeId = bookTheme.id)

        startReadingSession(book)
    }

    fun getCurrentReadingText(): String {
        val book = _currentBook.value ?: return ""
        return PdfTextExtractor.getResolvedTextForBookPage(
            book = book,
            pageNumber = _currentPage.value,
            chapters = _currentChapters.value
        )
    }

    fun getCurrentReadingChapter(): BookChapter {
        val book = _currentBook.value
        val currentChapters = _currentChapters.value
        if (currentChapters.isNotEmpty()) {
            if (book?.format == BookFormat.PDF) {
                val pageIdx = (_currentPage.value - 1).coerceIn(0, currentChapters.size - 1)
                val ch = currentChapters.getOrNull(pageIdx)
                if (ch != null && ch.content.isNotBlank()) return ch
            }
            val idx = _currentChapterIndex.value.coerceIn(0, currentChapters.size - 1)
            return currentChapters[idx]
        }
        val text = getCurrentReadingText()
        val isPdf = book?.format == BookFormat.PDF
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return BookChapter(
            index = if (isPdf) _currentPage.value - 1 else 0,
            title = if (isPdf) "Page ${_currentPage.value}" else "Chapter 1",
            content = text,
            wordCount = words.size
        )
    }

    fun closeReader() {
        stopReadingSession()
        ttsManager.stop()
        facePresenceEngine.stopAnalyzing()
        _currentBook.value = null
    }

    fun recordUserInteraction() {
        if (_activeSessionState.value.isIdle || _activeSessionState.value.idleSecondsCount > 0) {
            _activeSessionState.value = _activeSessionState.value.copy(
                isIdle = false,
                idleSecondsCount = 0
            )
        }
    }

    fun toggleActiveSessionPause() {
        val current = _activeSessionState.value
        val newPaused = !current.isPaused
        _activeSessionState.value = current.copy(
            isPaused = newPaused,
            isIdle = false,
            idleSecondsCount = 0
        )
        if (newPaused && ttsManager.engineState.value is TtsEngineState.Playing) {
            ttsManager.pause()
        }
    }

    fun selectChapter(index: Int) {
        if (index in 0 until _currentChapters.value.size) {
            _currentChapterIndex.value = index
            val book = _currentBook.value ?: return
            val progress = (index + 1).toFloat() / _currentChapters.value.size.toFloat()
            _currentPage.value = ((index + 1) * (book.totalPages / _currentChapters.value.size.coerceAtLeast(1))).coerceIn(1, book.totalPages)
            recordUserInteraction()
            viewModelScope.launch {
                bookRepository.updateReadingProgress(book.id, _currentPage.value, progress, 1)
                refreshStreakData()
            }
        }
    }

    fun nextPage() {
        val book = _currentBook.value ?: return
        val newPage = (_currentPage.value + 1).coerceAtMost(book.totalPages)
        _currentPage.value = newPage
        val progress = newPage.toFloat() / book.totalPages.toFloat()
        
        sessionPagesTurned++
        _activeSessionState.value = _activeSessionState.value.copy(
            sessionPagesRead = sessionPagesTurned,
            isIdle = false,
            idleSecondsCount = 0
        )

        if (_currentChapters.value.isNotEmpty()) {
            val chapterIdx = ((newPage - 1).toFloat() / book.totalPages * _currentChapters.value.size).toInt().coerceIn(0, _currentChapters.value.size - 1)
            _currentChapterIndex.value = chapterIdx
        }

        viewModelScope.launch {
            bookRepository.updateReadingProgress(book.id, newPage, progress, 1)
            refreshStreakData()
        }
    }

    fun previousPage() {
        val book = _currentBook.value ?: return
        val newPage = (_currentPage.value - 1).coerceAtLeast(1)
        _currentPage.value = newPage
        val progress = newPage.toFloat() / book.totalPages.toFloat()

        recordUserInteraction()

        if (_currentChapters.value.isNotEmpty()) {
            val chapterIdx = ((newPage - 1).toFloat() / book.totalPages * _currentChapters.value.size).toInt().coerceIn(0, _currentChapters.value.size - 1)
            _currentChapterIndex.value = chapterIdx
        }

        viewModelScope.launch {
            bookRepository.updateReadingProgress(book.id, newPage, progress, 0)
        }
    }

    fun setPage(page: Int) {
        val book = _currentBook.value ?: return
        val validPage = page.coerceIn(1, book.totalPages)
        _currentPage.value = validPage
        val progress = validPage.toFloat() / book.totalPages.toFloat()

        recordUserInteraction()

        if (_currentChapters.value.isNotEmpty()) {
            val chapterIdx = ((validPage - 1).toFloat() / book.totalPages * _currentChapters.value.size).toInt().coerceIn(0, _currentChapters.value.size - 1)
            _currentChapterIndex.value = chapterIdx
        }

        viewModelScope.launch {
            bookRepository.updateReadingProgress(book.id, validPage, progress, 1)
            refreshStreakData()
        }
    }

    fun recordSpeedReadTokens(count: Int) {
        questsManager.recordSpeedReadTokens(count)
        refreshStreakData()
    }

    fun addHighlight(text: String, note: String?, color: HighlightColor) {
        val book = _currentBook.value ?: return
        val currentChapter = _currentChapters.value.getOrNull(_currentChapterIndex.value)
        val chapterTitle = currentChapter?.title ?: "Page ${_currentPage.value}"
        
        viewModelScope.launch {
            bookRepository.addHighlight(
                bookId = book.id,
                bookTitle = book.title,
                chapterIndex = _currentChapterIndex.value,
                chapterTitle = chapterTitle,
                text = text,
                note = note,
                color = color,
                page = _currentPage.value
            )
            Toast.makeText(context, "Highlight saved!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteHighlight(id: String) {
        viewModelScope.launch {
            bookRepository.deleteHighlight(id)
            Toast.makeText(context, "Highlight removed", Toast.LENGTH_SHORT).show()
        }
    }

    fun addBookmark(title: String = "", note: String? = null) {
        val book = _currentBook.value ?: return
        val currentChapter = _currentChapters.value.getOrNull(_currentChapterIndex.value)
        viewModelScope.launch {
            bookRepository.addBookmark(
                bookId = book.id,
                bookTitle = book.title,
                chapterIndex = _currentChapterIndex.value,
                chapterTitle = currentChapter?.title ?: "",
                page = _currentPage.value,
                title = title.ifBlank { "Bookmark - Page ${_currentPage.value}" },
                note = note
            )
            Toast.makeText(context, "Bookmark added!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            bookRepository.deleteBookmark(id)
            Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateReaderTheme(themeId: String) {
        _readerPreferences.value = _readerPreferences.value.copy(themeId = themeId)
        val book = _currentBook.value
        if (book != null) {
            com.example.util.ThemeManager.saveThemeForBook(context, book.id, themeId)
        }
    }

    fun selectReadingMode(mode: com.example.util.ReadingMode) {
        val book = _currentBook.value
        val selectedTheme = com.example.util.ThemeManager.switchModeForBook(context, book?.id, mode)
        _readerPreferences.value = _readerPreferences.value.copy(themeId = selectedTheme.id)
    }

    fun updateFontSize(sizeSp: Float) {
        _readerPreferences.value = _readerPreferences.value.copy(fontSizeSp = sizeSp.coerceIn(12f, 32f))
    }

    fun updateLineSpacing(spacing: Float) {
        _readerPreferences.value = _readerPreferences.value.copy(lineSpacingMultiplier = spacing.coerceIn(1.2f, 2.5f))
    }

    fun updateFontFamily(font: FontFamilyPreference) {
        _readerPreferences.value = _readerPreferences.value.copy(fontFamily = font)
    }

    fun updateMargins(marginDp: Float) {
        _readerPreferences.value = _readerPreferences.value.copy(horizontalMarginDp = marginDp.coerceIn(10f, 40f))
    }

    fun toggleNightLight(enabled: Boolean) {
        _readerPreferences.value = _readerPreferences.value.copy(isNightLightFilter = enabled)
    }

    fun updateBrightness(level: Float) {
        _readerPreferences.value = _readerPreferences.value.copy(brightnessLevel = level.coerceIn(0.2f, 1.0f))
    }

    fun toggleAutoScroll() {
        val next = !_readerPreferences.value.isAutoScrollActive
        _readerPreferences.value = _readerPreferences.value.copy(isAutoScrollActive = next)
    }

    fun togglePagedMode() {
        _readerPreferences.value = _readerPreferences.value.copy(isPagedMode = !_readerPreferences.value.isPagedMode)
    }

    fun searchInCurrentBook(query: String) {
        _inBookSearchQuery.value = query
        if (query.isBlank()) {
            _inBookSearchResults.value = emptyList()
            return
        }
        val results = mutableListOf<String>()
        val q = query.lowercase()
        _currentChapters.value.forEachIndexed { _, ch ->
            val paragraphs = ch.content.split("\n\n")
            paragraphs.forEach { p ->
                if (p.lowercase().contains(q)) {
                    val snippet = p.trim().take(120) + "..."
                    results.add("[${ch.title}] $snippet")
                }
            }
        }
        _inBookSearchResults.value = results
    }

    // Multilingual Segmented TTS Playback
    fun playTtsForCurrentChapter() {
        val text = getCurrentReadingText().ifBlank {
            _currentChapters.value.getOrNull(_currentChapterIndex.value)?.content ?: ""
        }
        if (text.isNotBlank()) {
            val segments = TextSegmenter.segment(text)
            ttsManager.startReadingSegments(
                segments = segments,
                startSegmentIndex = 0,
                preferredLanguage = _currentLanguage.value,
                bookLanguageCode = _currentBook.value?.languageCode
            )
        }
    }

    fun importDocument(uri: Uri, displayName: String) {
        viewModelScope.launch {
            val book = bookRepository.importBookFromUri(uri, displayName)
            if (book != null) {
                Toast.makeText(context, "Added '${book.title}' to Library!", Toast.LENGTH_LONG).show()
                refreshStreakData()
            } else {
                Toast.makeText(context, "Failed to import file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addConvertedEpubToLibrary(file: File, title: String, author: String, autoOpen: Boolean = false) {
        viewModelScope.launch {
            val book = bookRepository.addConvertedEpubBook(file, title, author)
            if (book != null) {
                Toast.makeText(context, "Added '${book.title}' as EPUB to your Library!", Toast.LENGTH_SHORT).show()
                refreshStreakData()
                if (autoOpen) {
                    openBook(book)
                }
            } else {
                Toast.makeText(context, "Failed to register converted EPUB.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun downloadBook(book: Book) {
        viewModelScope.launch {
            bookRepository.downloadCatalogBook(book)
            Toast.makeText(context, "Downloaded '${book.title}' for offline reading!", Toast.LENGTH_LONG).show()
            refreshStreakData()
        }
    }

    fun exportBackup(uri: Uri, onComplete: (BackupResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = localBackupRepository.exportBackup(context, uri)
            when (result) {
                is BackupResult.Success -> {
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                }
                is BackupResult.Error -> {
                    Toast.makeText(context, "Export error: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
            onComplete(result)
        }
    }

    fun restoreBackup(uri: Uri, onComplete: (BackupResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = localBackupRepository.restoreBackup(context, uri)
            when (result) {
                is BackupResult.Success -> {
                    Toast.makeText(context, "Library restored from backup!", Toast.LENGTH_SHORT).show()
                    refreshStreakData()
                }
                is BackupResult.Error -> {
                    Toast.makeText(context, "Restore error: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
            onComplete(result)
        }
    }

    fun submitReview(
        bookId: String,
        bookTitle: String,
        rating: Float,
        reviewTitle: String,
        reviewText: String,
        userName: String
    ) {
        viewModelScope.launch {
            bookRepository.addReview(
                bookId = bookId,
                bookTitle = bookTitle,
                rating = rating,
                reviewTitle = reviewTitle,
                reviewText = reviewText,
                userName = userName
            )
            Toast.makeText(context, "Review & rating submitted!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteReview(id: String) {
        viewModelScope.launch {
            bookRepository.deleteReview(id)
            Toast.makeText(context, "Review removed", Toast.LENGTH_SHORT).show()
        }
    }

    fun incrementReviewHelpful(id: String) {
        viewModelScope.launch {
            bookRepository.incrementHelpful(id)
        }
    }

    fun shareReadingStats() {
        val shareText = com.example.util.SocialShareHelper.formatStatsShareText(_streakData.value)
        com.example.util.SocialShareHelper.shareContent(context, shareText, "My Reading Streak Stats")
    }

    fun shareDailyGoalProgress() {
        val shareText = com.example.util.SocialShareHelper.formatDailyGoalShareText(
            goalMinutes = _dailyGoalMinutes.value,
            todayMinutes = _streakData.value.todayMinutesRead,
            streakDays = _streakData.value.currentStreakDays
        )
        com.example.util.SocialShareHelper.shareContent(context, shareText, "Daily Reading Goal Milestone")
    }

    fun shareHighlightQuote(highlight: Highlight) {
        val shareText = com.example.util.SocialShareHelper.formatHighlightShareText(highlight)
        com.example.util.SocialShareHelper.shareContent(context, shareText, "Favorite Reading Highlight")
    }

    fun shareBookProgress(book: Book) {
        val shareText = com.example.util.SocialShareHelper.formatBookProgressShareText(book, _streakData.value)
        com.example.util.SocialShareHelper.shareContent(context, shareText, "Book Reading Progress")
    }

    private fun startReadingSession(book: Book) {
        sessionAccumulatedSeconds = 0L
        sessionPagesTurned = 0
        isGoalCelebratedThisSession = false
        noFaceConsecutiveSeconds = 0
        val todayMinutes = _streakData.value.todayMinutesRead
        val dailyGoal = _streakData.value.dailyGoalMinutes

        _activeSessionState.value = ActiveSessionState(
            isSessionRunning = true,
            isPaused = false,
            isIdle = false,
            currentBookId = book.id,
            currentBookTitle = book.title,
            sessionDurationSeconds = 0L,
            sessionPagesRead = 0,
            todayMinutesAccumulated = todayMinutes,
            dailyGoalMinutes = dailyGoal,
            idleSecondsCount = 0,
            isDailyGoalReached = todayMinutes >= dailyGoal
        )

        readingSessionJob?.cancel()
        readingSessionJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _activeSessionState.value
                val isTtsPlaying = ttsManager.engineState.value is TtsEngineState.Playing
                val isFaceAssisted = _isFaceAssistedEnabled.value
                val faceState = facePresenceEngine.presenceState.value

                // If face-assisted mode is on, verify presence unless user is listening via TTS
                val isFaceAttentive = !isFaceAssisted || isTtsPlaying || (faceState == FacePresenceState.Attentive)

                if (isFaceAssisted && !isTtsPlaying && faceState != FacePresenceState.Attentive) {
                    noFaceConsecutiveSeconds++
                } else {
                    noFaceConsecutiveSeconds = 0
                }

                val allowCounting = if (isFaceAssisted && !isTtsPlaying) {
                    noFaceConsecutiveSeconds < 4 && isFaceAttentive
                } else {
                    !current.isPaused && current.idleSecondsCount < 90
                }

                if (isTtsPlaying || allowCounting) {
                    sessionAccumulatedSeconds++
                    val newTotalTodayMinutes = todayMinutes + (sessionAccumulatedSeconds / 60).toInt()
                    val goalMet = newTotalTodayMinutes >= dailyGoal

                    if (goalMet && !isGoalCelebratedThisSession && !current.isDailyGoalReached) {
                        isGoalCelebratedThisSession = true
                        Toast.makeText(context, "🔥 Daily Streak Goal Achieved! ($dailyGoal min completed)", Toast.LENGTH_SHORT).show()
                    }

                    _activeSessionState.value = current.copy(
                        sessionDurationSeconds = sessionAccumulatedSeconds,
                        isIdle = false,
                        idleSecondsCount = if (isTtsPlaying) 0 else (current.idleSecondsCount + 1),
                        isDailyGoalReached = goalMet
                    )

                    if (sessionAccumulatedSeconds % 30 == 0L) {
                        val progress = (_currentPage.value.toFloat() / (_currentBook.value?.totalPages ?: 100).toFloat()).coerceIn(0f, 1f)
                        bookRepository.updateReadingProgress(book.id, _currentPage.value, progress, 1)
                        refreshStreakData()
                    }
                } else if (!isTtsPlaying && current.idleSecondsCount >= 90) {
                    _activeSessionState.value = current.copy(isIdle = true)
                }
            }
        }
    }

    fun stopReadingSession() {
        readingSessionJob?.cancel()
        readingSessionJob = null
        val book = _currentBook.value
        val totalSecs = sessionAccumulatedSeconds

        _activeSessionState.value = _activeSessionState.value.copy(
            isSessionRunning = false,
            isPaused = false
        )

        if (book != null && totalSecs >= 10) {
            val minutes = (totalSecs / 60).toInt().coerceAtLeast(1)
            val progress = (_currentPage.value.toFloat() / book.totalPages.toFloat()).coerceIn(0f, 1f)
            viewModelScope.launch {
                bookRepository.updateReadingProgress(book.id, _currentPage.value, progress, minutes)
                refreshStreakData()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopReadingSession()
        ttsManager.shutdown()
        ambientEngine.release()
        pdfManager.close()
        facePresenceEngine.stopAnalyzing()
    }
}

package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SampleBooksData
import com.example.data.repository.BookRepository
import com.example.data.repository.CloudSyncRepository
import com.example.model.*
import com.example.reader.EpubParser
import com.example.reader.PdfManager
import com.example.reader.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    val bookRepository = BookRepository(context, database)
    val cloudSyncRepository = CloudSyncRepository(database)

    val pdfManager = PdfManager(context)
    val ttsManager = TtsManager(context)

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

    // Filtered Books
    val filteredBooks: StateFlow<List<Book>> = combine(
        allBooks,
        _searchQuery,
        _selectedStatus,
        _selectedFormat,
        _selectedSort
    ) { books, query, status, format, sort ->
        var list = books

        // Filter by Status / Shelf
        if (status != ReadingStatus.ALL) {
            list = when (status) {
                ReadingStatus.FAVORITES -> list.filter { it.isFavorite }
                ReadingStatus.DOWNLOADED -> list.filter { it.isDownloaded }
                else -> list.filter { it.status == status }
            }
        }

        // Filter by format
        if (format != null) {
            list = list.filter { it.format == format }
        }

        // Filter by Search Query (title, author, genre, tags)
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

        // Sort
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

    // Recommendation Engine: analyzes user reading history, genre preferences, and highlighted content
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

    // Cloud sync state
    val cloudSyncInfo = cloudSyncRepository.syncInfo

    // Active session timer
    private var readingSessionJob: Job? = null
    private var sessionSecondsRead = 0

    init {
        viewModelScope.launch {
            bookRepository.seedInitialDataIfEmpty()
            refreshStreakData()
        }
    }

    fun refreshStreakData() {
        viewModelScope.launch {
            _streakData.value = bookRepository.calculateStreakData()
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

        // Load chapters
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
                }
            } else {
                _currentChapters.value = SampleBooksData.getSampleChaptersForBook(book.id)
            }
        } else {
            _currentChapters.value = SampleBooksData.getSampleChaptersForBook(book.id)
        }

        _currentChapterIndex.value = 0
        startReadingSession(book.id)
    }

    fun closeReader() {
        stopReadingSession()
        ttsManager.stop()
        _currentBook.value = null
    }

    fun selectChapter(index: Int) {
        if (index in 0 until _currentChapters.value.size) {
            _currentChapterIndex.value = index
            val book = _currentBook.value ?: return
            val progress = (index + 1).toFloat() / _currentChapters.value.size.toFloat()
            _currentPage.value = ((index + 1) * (book.totalPages / _currentChapters.value.size.coerceAtLeast(1))).coerceIn(1, book.totalPages)
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
        
        // Auto chapter switch if paged
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

        if (_currentChapters.value.isNotEmpty()) {
            val chapterIdx = ((validPage - 1).toFloat() / book.totalPages * _currentChapters.value.size).toInt().coerceIn(0, _currentChapters.value.size - 1)
            _currentChapterIndex.value = chapterIdx
        }

        viewModelScope.launch {
            bookRepository.updateReadingProgress(book.id, validPage, progress, 1)
            refreshStreakData()
        }
    }

    // Highlights & Bookmarks actions
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

    // Reader Customization Preferences
    fun updateReaderTheme(themeId: String) {
        _readerPreferences.value = _readerPreferences.value.copy(themeId = themeId)
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

    // In-Book Search
    fun searchInCurrentBook(query: String) {
        _inBookSearchQuery.value = query
        if (query.isBlank()) {
            _inBookSearchResults.value = emptyList()
            return
        }
        val results = mutableListOf<String>()
        val q = query.lowercase()
        _currentChapters.value.forEachIndexed { idx, ch ->
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

    // TTS playback
    fun playTtsForCurrentChapter() {
        val chapter = _currentChapters.value.getOrNull(_currentChapterIndex.value)
        if (chapter != null) {
            ttsManager.startReading(chapter.content)
        }
    }

    // Document Import
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

    // Download public domain book
    fun downloadBook(book: Book) {
        viewModelScope.launch {
            bookRepository.downloadCatalogBook(book)
            Toast.makeText(context, "Downloaded '${book.title}' for offline reading!", Toast.LENGTH_LONG).show()
            refreshStreakData()
        }
    }

    // Cloud Sync Actions
    fun triggerSync() {
        viewModelScope.launch {
            val success = cloudSyncRepository.performCloudSync()
            if (success) {
                Toast.makeText(context, "Cloud sync complete!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Cloud sync failed. Check connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportBackup(): String {
        var json = ""
        viewModelScope.launch {
            json = cloudSyncRepository.exportFullLibraryJson()
        }
        return json
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            val success = cloudSyncRepository.restoreLibraryFromJson(jsonString)
            if (success) {
                Toast.makeText(context, "Library restored from cloud backup!", Toast.LENGTH_SHORT).show()
                refreshStreakData()
            } else {
                Toast.makeText(context, "Invalid backup format.", Toast.LENGTH_SHORT).show()
            }
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

    fun shareReadingStats(targetInstagram: Boolean = false) {
        val shareText = com.example.util.SocialShareHelper.formatStatsShareText(_streakData.value)
        com.example.util.SocialShareHelper.shareToSocialPlatform(context, shareText, targetInstagram)
    }

    fun shareHighlightQuote(highlight: Highlight, targetInstagram: Boolean = false) {
        val shareText = com.example.util.SocialShareHelper.formatHighlightShareText(highlight)
        com.example.util.SocialShareHelper.shareToSocialPlatform(context, shareText, targetInstagram)
    }

    fun shareBookProgress(book: Book, targetInstagram: Boolean = false) {
        val shareText = com.example.util.SocialShareHelper.formatBookProgressShareText(book, _streakData.value)
        com.example.util.SocialShareHelper.shareToSocialPlatform(context, shareText, targetInstagram)
    }

    fun openInstagramProfile() {
        com.example.util.SocialShareHelper.openInstagramProfile(context)
    }

    private fun startReadingSession(bookId: String) {
        sessionSecondsRead = 0
        readingSessionJob?.cancel()
        readingSessionJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                sessionSecondsRead++
                if (sessionSecondsRead % 60 == 0) {
                    val minutes = 1
                    val progress = (_currentPage.value.toFloat() / (_currentBook.value?.totalPages ?: 100).toFloat()).coerceIn(0f, 1f)
                    bookRepository.updateReadingProgress(bookId, _currentPage.value, progress, minutes)
                    refreshStreakData()
                }
            }
        }
    }

    private fun stopReadingSession() {
        readingSessionJob?.cancel()
        readingSessionJob = null
        val book = _currentBook.value ?: return
        if (sessionSecondsRead > 15) {
            val minutes = (sessionSecondsRead / 60).coerceAtLeast(1)
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
        pdfManager.close()
    }
}

package com.example.data.repository

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.entity.BookEntity
import com.example.data.remote.OnlineBookSearchService
import com.example.model.*
import com.example.reader.EpubParser
import com.example.reader.PdfTextExtractor
import com.example.util.TextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

/** Why an import failed. Surfaced to the user instead of substituting placeholder content. */
enum class ImportFailure {
    NO_DOWNLOAD_SOURCE,
    NETWORK,
    FILE_TOO_SMALL,
    NOT_A_BOOK,
    UNREADABLE,
    STORAGE
}

class BookImportException(
    val reason: ImportFailure,
    override val message: String
) : Exception(message)

class BookSearchRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val searchService: OnlineBookSearchService = OnlineBookSearchService(context)
) {
    private val bookDao = database.bookDao()

    private companion object {
        /** Anything smaller than this cannot be a legitimate book file. */
        const val MIN_VALID_BYTES = 1_024L

        /** Rough words-per-page used to derive a page count when the format has no page model. */
        const val WORDS_PER_PAGE = 300
    }

    suspend fun searchOnlineAndMatchLibrary(query: String): List<SearchBookResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val remoteResults = searchService.searchAllSources(query)
        val localBooks = bookDao.getAllBooks().first().map { it.toModel() }
        val localTitles = localBooks.map { TextNormalizer.normalize(it.title) }.toSet()
        val localStableIds = localBooks
            .mapNotNull { it.tags.find { tag -> tag.startsWith("src_id:") }?.removePrefix("src_id:") }
            .toSet()

        return@withContext remoteResults.map { remote ->
            val isInLib = localStableIds.contains(remote.stableId) ||
                localTitles.contains(TextNormalizer.normalize(remote.title)) ||
                localBooks.any { TextNormalizer.matches(it.title, remote.title) }
            remote.copy(isAlreadyInLibrary = isInLib)
        }
    }

    /**
     * Downloads a search result and imports it into the library.
     *
     * The file is validated before anything is written to the database. If the download, the
     * container format, or text extraction fails, this returns [Result.failure] — it does not
     * generate substitute prose, because a "downloaded" book that is not the requested book is
     * worse than a clear error.
     */
    suspend fun downloadAndImportBook(
        result: SearchBookResult,
        onProgress: (Float) -> Unit
    ): Result<Book> = withContext(Dispatchers.IO) {
        val downloadUrl = result.downloadUrl
        if (downloadUrl.isNullOrBlank()) {
            return@withContext Result.failure(
                BookImportException(
                    ImportFailure.NO_DOWNLOAD_SOURCE,
                    "This catalogue entry has no downloadable file. Open it via its source page instead."
                )
            )
        }

        try {
            val booksDir = File(context.filesDir, "books").apply { mkdirs() }
            if (!booksDir.exists()) {
                return@withContext Result.failure(
                    BookImportException(ImportFailure.STORAGE, "Could not create the local book directory.")
                )
            }

            val ext = when (result.format) {
                BookFormat.PDF -> "pdf"
                BookFormat.TXT -> "txt"
                else -> "epub"
            }
            val targetFile = File(booksDir, "${result.stableId}.$ext")

            onProgress(0.05f)
            val downloaded = searchService.downloadBookFile(downloadUrl, targetFile) { progress ->
                onProgress(0.05f + progress * 0.65f)
            }

            if (!downloaded || !targetFile.exists()) {
                return@withContext Result.failure(
                    BookImportException(
                        ImportFailure.NETWORK,
                        "Download failed. The source may be unavailable, rate limited, or require a different edition."
                    )
                )
            }

            if (targetFile.length() < MIN_VALID_BYTES) {
                targetFile.delete()
                return@withContext Result.failure(
                    BookImportException(
                        ImportFailure.FILE_TOO_SMALL,
                        "The downloaded file was almost empty (${targetFile.length()} bytes). Nothing was imported."
                    )
                )
            }

            onProgress(0.75f)

            val validation = validateFile(targetFile, result.format)
            if (validation != null) {
                targetFile.delete()
                return@withContext Result.failure(validation)
            }

            val parsed = extractChapters(targetFile, result)
                ?: return@withContext Result.failure(
                    BookImportException(
                        ImportFailure.UNREADABLE,
                        "The file downloaded correctly but contains no extractable text (it may be a scanned image). " +
                            "Nothing was imported, so your library stays accurate."
                    )
                )

            if (parsed.chapters.isEmpty()) {
                targetFile.delete()
                return@withContext Result.failure(
                    BookImportException(ImportFailure.UNREADABLE, "No readable chapters were found in this file.")
                )
            }

            onProgress(0.9f)

            val totalWords = parsed.chapters.sumOf { it.wordCount }
            val estimatedPages = (totalWords / WORDS_PER_PAGE).coerceAtLeast(parsed.chapters.size)

            val isArabic = result.languageCode.equals("ar", ignoreCase = true) ||
                result.title.any { it in '\u0600'..'\u06FF' } ||
                result.authorDisplay.any { it in '\u0600'..'\u06FF' }

            val fileSizeKb = (targetFile.length() / 1024).coerceAtLeast(1)
            val formattedSize = if (fileSizeKb > 1024) {
                "${(fileSizeKb / 1024.0 * 10).toInt() / 10.0} MB"
            } else {
                "$fileSizeKb KB"
            }

            val coverPath = if (result.format == BookFormat.EPUB) {
                runCatching { EpubParser.extractEpubCover(context, targetFile) }.getOrNull()
            } else {
                null
            }

            val palette = PALETTES[Math.abs(result.stableId.hashCode()) % PALETTES.size]

            val newBook = Book(
                id = "src-" + UUID.randomUUID().toString().take(8),
                title = result.title,
                author = result.authorDisplay,
                description = result.description,
                format = result.format,
                status = ReadingStatus.WANT_TO_READ,
                coverGradientStart = palette.first,
                coverGradientEnd = palette.second,
                coverImageUrl = result.coverUrl ?: coverPath,
                totalPages = estimatedPages,
                currentPage = 1,
                readingProgress = 0f,
                isFavorite = false,
                isDownloaded = true,
                localFilePath = targetFile.absolutePath,
                fileSize = formattedSize,
                genre = result.format.displayName + " · " + result.source,
                tags = listOf("Online", result.source, "src_id:${result.stableId}"),
                // No rating is asserted: this app has no rating data for downloaded editions.
                rating = 0f,
                languageCode = if (isArabic) "ar" else (result.languageCode ?: "en"),
                addedTimestamp = System.currentTimeMillis()
            )

            bookDao.insertBook(BookEntity.fromModel(newBook))
            onProgress(1.0f)
            Result.success(newBook)
        } catch (t: Throwable) {
            if (t is kotlin.coroutines.cancellation.CancellationException) throw t
            Result.failure(
                BookImportException(
                    ImportFailure.UNREADABLE,
                    "Import failed: ${t.message ?: t::class.java.simpleName}"
                )
            )
        }
    }

    /**
     * Verifies the bytes on disk actually match the claimed format. Returns null when valid,
     * or the failure to report.
     */
    private fun validateFile(file: File, format: BookFormat): BookImportException? {
        if (!file.exists() || file.length() < MIN_VALID_BYTES) {
            return BookImportException(ImportFailure.FILE_TOO_SMALL, "The downloaded file is empty or truncated.")
        }

        val header = ByteArray(8)
        val read = runCatching { file.inputStream().use { it.read(header) } }.getOrDefault(-1)
        if (read <= 0) {
            return BookImportException(ImportFailure.UNREADABLE, "The downloaded file could not be read.")
        }
        val headerText = String(header, 0, read, Charsets.ISO_8859_1)

        return when (format) {
            BookFormat.PDF -> {
                if (!headerText.startsWith("%PDF-")) {
                    BookImportException(
                        ImportFailure.NOT_A_BOOK,
                        "The source returned an HTML or error page instead of a PDF. Try another edition."
                    )
                } else {
                    null
                }
            }

            BookFormat.EPUB -> {
                val isZip = headerText.startsWith("PK")
                if (!isZip) {
                    BookImportException(
                        ImportFailure.NOT_A_BOOK,
                        "The source returned something that is not a valid EPUB archive."
                    )
                } else if (!hasEpubMimetype(file)) {
                    BookImportException(
                        ImportFailure.NOT_A_BOOK,
                        "The archive is a zip file but is not a valid EPUB (missing mimetype entry)."
                    )
                } else {
                    null
                }
            }

            BookFormat.TXT -> {
                val sample = runCatching { file.readText(Charsets.UTF_8).take(4_000) }.getOrDefault("")
                if (sample.isBlank() || !PdfTextExtractor.isHumanReadableText(sample)) {
                    BookImportException(
                        ImportFailure.NOT_A_BOOK,
                        "The downloaded text file does not contain readable text."
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun hasEpubMimetype(file: File): Boolean = runCatching {
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            var guard = 0
            while (entry != null && guard < 5_000) {
                if (entry.name == "mimetype") return@use true
                entry = zip.nextEntry
                guard++
            }
            false
        }
    }.getOrDefault(false)

    private data class ExtractedContent(
        val chapters: List<BookChapter>
    )

    /** Extracts real text from the validated file. Returns null when nothing readable exists. */
    private fun extractChapters(file: File, result: SearchBookResult): ExtractedContent? {
        val chapters: List<BookChapter> = when (result.format) {
            BookFormat.EPUB -> {
                val parsed = runCatching { EpubParser.parseEpubFile(file, result.title) }.getOrNull()
                    ?: return null
                parsed.chapters.filter { it.content.isNotBlank() }
            }

            BookFormat.TXT -> {
                val parsed = runCatching {
                    file.inputStream().use { stream -> EpubParser.parsePlainTextStream(stream, result.title) }
                }.getOrNull() ?: return null
                parsed.chapters.filter { it.content.isNotBlank() }
            }

            BookFormat.PDF -> {
                val extracted = runCatching { PdfTextExtractor.extractChaptersFromPdf(file, result.title) }
                    .getOrNull()
                    .orEmpty()
                extracted.filter {
                    it.content.isNotBlank() && PdfTextExtractor.isHumanReadableText(it.content)
                }
            }
        }

        return if (chapters.isEmpty()) null else ExtractedContent(chapters)
    }

    private val PALETTES = listOf(
        Pair(0xFF1E3A8AL, 0xFF172554L),
        Pair(0xFF0D9488L, 0xFF064E3BL),
        Pair(0xFF7C2D12L, 0xFF431407L),
        Pair(0xFF4C1D95L, 0xFF2E1065L),
        Pair(0xFFB45309L, 0xFF78350FL)
    )
}

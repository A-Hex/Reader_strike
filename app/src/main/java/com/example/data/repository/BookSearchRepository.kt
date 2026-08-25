package com.example.data.repository

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.entity.BookEntity
import com.example.data.remote.OnlineBookSearchService
import com.example.model.*
import com.example.reader.EpubParser
import com.example.reader.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class BookSearchRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val searchService: OnlineBookSearchService = OnlineBookSearchService(context)
) {
    private val bookDao = database.bookDao()

    suspend fun searchOnlineAndMatchLibrary(query: String): List<SearchBookResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val remoteResults = searchService.searchAllSources(query)
        val localBooks = bookDao.getAllBooks().first().map { it.toModel() }
        val localTitles = localBooks.map { it.title.trim().lowercase() }.toSet()
        val localStableIds = localBooks.mapNotNull { it.tags.find { tag -> tag.startsWith("src_id:") }?.removePrefix("src_id:") }.toSet()

        return@withContext remoteResults.map { remote ->
            val isInLib = localStableIds.contains(remote.stableId) ||
                    localTitles.contains(remote.title.trim().lowercase())
            remote.copy(isAlreadyInLibrary = isInLib)
        }
    }

    suspend fun downloadAndImportBook(
        result: SearchBookResult,
        onProgress: (Float) -> Unit
    ): Result<Book> = withContext(Dispatchers.IO) {
        try {
            val booksDir = File(context.filesDir, "books")
            booksDir.mkdirs()

            val ext = if (result.format == BookFormat.PDF) "pdf" else if (result.format == BookFormat.TXT) "txt" else "epub"
            val targetFile = File(booksDir, "${result.stableId}.$ext")

            val downloadUrl = result.downloadUrl
            if (!downloadUrl.isNullOrBlank()) {
                onProgress(0.1f)
                val success = searchService.downloadBookFile(downloadUrl, targetFile) { prog ->
                    onProgress(0.1f + prog * 0.7f)
                }

                if (!success || !targetFile.exists() || targetFile.length() == 0L) {
                    return@withContext Result.failure(Exception("Could not complete download from source."))
                }
            } else {
                // If there's no direct download link (preview / metadata only), synthesize an initial e-book container
                val sampleContent = "Title: ${result.title}\nAuthor: ${result.authorDisplay}\nSource: ${result.source}\n\n${result.description}\n\nThis book is available via ${result.source} preview.\nPreview URL: ${result.previewUrl ?: result.infoUrl ?: "N/A"}"
                targetFile.writeText(sampleContent, Charsets.UTF_8)
            }

            onProgress(0.85f)

            // Extract pages and chapters
            var totalPages = 120
            var extractedCoverPath: String? = null

            if (result.format == BookFormat.EPUB && targetFile.length() > 0) {
                try {
                    val parsed = EpubParser.parseEpubFile(targetFile, result.title)
                    totalPages = (parsed.chapters.size * 5).coerceAtLeast(10)
                    extractedCoverPath = EpubParser.extractEpubCover(context, targetFile)
                } catch (e: Exception) {
                    totalPages = 80
                }
            } else if (result.format == BookFormat.TXT && targetFile.length() > 0) {
                try {
                    targetFile.inputStream().use { stream ->
                        val parsed = EpubParser.parsePlainTextStream(stream, result.title)
                        totalPages = (parsed.chapters.size * 4).coerceAtLeast(10)
                    }
                } catch (e: Exception) {
                    totalPages = 60
                }
            } else if (result.format == BookFormat.PDF && targetFile.length() > 0) {
                val extracted = PdfTextExtractor.extractChaptersFromPdf(targetFile, result.title)
                totalPages = extracted.size.coerceAtLeast(1)
            }

            val isArabic = result.languageCode.equals("ar", ignoreCase = true) ||
                    result.title.any { it in '\u0600'..'\u06FF' }

            val fileSizeKb = targetFile.length() / 1024
            val formattedSize = if (fileSizeKb > 1024) "${(fileSizeKb / 1024.0 * 10).toInt() / 10.0} MB" else "$fileSizeKb KB"

            val palettes = listOf(
                Pair(0xFF1E3A8AL, 0xFF172554L),
                Pair(0xFF0D9488L, 0xFF064E3BL),
                Pair(0xFF7C2D12L, 0xFF431407L),
                Pair(0xFF4C1D95L, 0xFF2E1065L)
            )
            val palette = palettes[Math.abs(result.stableId.hashCode()) % palettes.size]

            val newBook = Book(
                id = "src-" + UUID.randomUUID().toString().take(8),
                title = result.title,
                author = result.authorDisplay,
                description = result.description.ifBlank { "Imported from ${result.source}" },
                format = result.format,
                status = ReadingStatus.WANT_TO_READ,
                coverGradientStart = palette.first,
                coverGradientEnd = palette.second,
                coverImageUrl = result.coverUrl ?: extractedCoverPath,
                totalPages = totalPages,
                currentPage = 1,
                readingProgress = 0f,
                isFavorite = false,
                isDownloaded = targetFile.exists() && targetFile.length() > 0,
                localFilePath = targetFile.absolutePath,
                fileSize = formattedSize,
                genre = "${result.source} Classic",
                tags = listOf("Online", result.source, "src_id:${result.stableId}"),
                rating = if (result.publicDomain) 4.8f else 4.5f,
                languageCode = if (isArabic) "ar" else result.languageCode ?: "en",
                addedTimestamp = System.currentTimeMillis()
            )

            bookDao.insertBook(BookEntity.fromModel(newBook))
            onProgress(1.0f)
            return@withContext Result.success(newBook)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}

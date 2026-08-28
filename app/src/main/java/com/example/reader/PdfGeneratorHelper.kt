package com.example.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.data.SampleBooksData
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfGeneratorHelper {

    private const val TAG = "PdfGeneratorHelper"
    private const val PAGE_WIDTH = 595  // Standard A4 width in points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points
    private const val MARGIN_HORIZONTAL = 48
    private const val MARGIN_TOP = 54
    private const val MARGIN_BOTTOM = 54

    /**
     * Ensures a valid, readable on-disk PDF file exists for the given book.
     * If the file already exists and has a valid %PDF header, returns it.
     * Otherwise, generates a beautifully formatted multi-page PDF with vector typography.
     */
    suspend fun getOrCreatePdfForBook(
        context: Context,
        book: Book,
        customChapters: List<BookChapter>? = null
    ): File = withContext(Dispatchers.IO) {
        val pdfDir = File(context.filesDir, "generated_pdfs").apply { mkdirs() }
        val safeName = book.title.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_").take(30)
        val targetFile = File(pdfDir, "book_${book.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")}_$safeName.pdf")

        if (targetFile.exists() && targetFile.length() > 500 && isValidPdfFile(targetFile)) {
            return@withContext targetFile
        }

        // Check if existing localFilePath is valid
        if (!book.localFilePath.isNullOrBlank()) {
            val existing = File(book.localFilePath)
            if (existing.exists() && existing.length() > 500 && isValidPdfFile(existing)) {
                return@withContext existing
            }
        }

        // Resolve chapters for this book
        val chapters = if (!customChapters.isNullOrEmpty()) {
            customChapters
        } else {
            val sampleCh = SampleBooksData.getSampleChaptersForBook(book.id)
            if (sampleCh.isNotEmpty()) sampleCh else listOf(
                BookChapter(
                    index = 0,
                    title = book.title,
                    content = book.description.ifBlank { "Content for ${book.title} by ${book.author}." }
                )
            )
        }

        generatePdfDocument(targetFile, book, chapters)
        return@withContext targetFile
    }

    /**
     * Checks if a file begins with standard PDF header "%PDF-"
     */
    fun isValidPdfFile(file: File): Boolean {
        if (!file.exists() || file.length() < 10) return false
        return try {
            val header = ByteArray(8)
            file.inputStream().use { it.read(header) }
            val headerStr = String(header, Charsets.ISO_8859_1)
            headerStr.startsWith("%PDF-")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Synthesizes a real multi-page Android PdfDocument from title, author and text content.
     */
    fun generatePdfDocument(
        outputFile: File,
        title: String,
        author: String,
        content: String,
        languageCode: String = "en"
    ) {
        val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
        val chapters = if (paragraphs.size > 1) {
            paragraphs.chunked(3).mapIndexed { idx, chunk ->
                BookChapter(
                    index = idx,
                    title = if (idx == 0) title else "Section ${idx + 1}",
                    content = chunk.joinToString("\n\n")
                )
            }
        } else {
            listOf(
                BookChapter(
                    index = 0,
                    title = title,
                    content = content
                )
            )
        }

        val dummyBook = Book(
            id = "gen-${outputFile.nameWithoutExtension}",
            title = title,
            author = author,
            description = content.take(150),
            format = BookFormat.PDF,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF1E3A8AL,
            coverGradientEnd = 0xFF172554L,
            totalPages = chapters.size.coerceAtLeast(1),
            currentPage = 1,
            readingProgress = 0f,
            isFavorite = false,
            isDownloaded = true,
            localFilePath = outputFile.absolutePath,
            fileSize = "1 MB",
            genre = "Literature",
            tags = emptyList(),
            rating = 5f,
            languageCode = languageCode
        )

        generatePdfDocument(outputFile, dummyBook, chapters)
    }

    /**
     * Synthesizes a real multi-page Android PdfDocument and saves it to the target file.
     */
    fun generatePdfDocument(
        outputFile: File,
        book: Book,
        chapters: List<BookChapter>
    ) {
        val document = PdfDocument()
        var pageNumber = 1

        val isArabic = book.languageCode.equals("ar", ignoreCase = true) ||
                book.title.any { it in '\u0600'..'\u06FF' } ||
                book.author.any { it in '\u0600'..'\u06FF' }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 13.5f
            typeface = if (isArabic) Typeface.SANS_SERIF else Typeface.SERIF
        }

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#49454F")
            textSize = 9.5f
            typeface = Typeface.SANS_SERIF
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            textSize = 20f
            isFakeBoldText = true
            typeface = if (isArabic) Typeface.SANS_SERIF else Typeface.SERIF
        }

        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A8E72")
            textSize = 14f
            typeface = Typeface.SANS_SERIF
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        val contentWidth = PAGE_WIDTH - (MARGIN_HORIZONTAL * 2)

        try {
            // Page 1: Cover / Title Page
            val coverPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val coverPage = document.startPage(coverPageInfo)
            val coverCanvas = coverPage.canvas
            coverCanvas.drawColor(Color.WHITE)

            // Decorative top border
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5A8E72")
                style = Paint.Style.FILL
            }
            coverCanvas.drawRect(MARGIN_HORIZONTAL.toFloat(), MARGIN_TOP.toFloat(), (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), (MARGIN_TOP + 4).toFloat(), accentPaint)

            // Title
            val titleLayout = StaticLayout.Builder.obtain(book.title, 0, book.title.length, titlePaint, contentWidth)
                .setAlignment(if (isArabic) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.15f)
                .build()

            coverCanvas.save()
            coverCanvas.translate(MARGIN_HORIZONTAL.toFloat(), (MARGIN_TOP + 40).toFloat())
            titleLayout.draw(coverCanvas)
            coverCanvas.restore()

            // Author
            val authorY = (MARGIN_TOP + 40) + titleLayout.height + 16
            val authorText = if (isArabic) "المؤلف: ${book.author}" else "By ${book.author}"
            val authorLayout = StaticLayout.Builder.obtain(authorText, 0, authorText.length, subtitlePaint, contentWidth)
                .setAlignment(if (isArabic) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
                .build()

            coverCanvas.save()
            coverCanvas.translate(MARGIN_HORIZONTAL.toFloat(), authorY.toFloat())
            authorLayout.draw(coverCanvas)
            coverCanvas.restore()

            // Divider
            val divY = authorY + authorLayout.height + 24
            coverCanvas.drawLine(MARGIN_HORIZONTAL.toFloat(), divY.toFloat(), (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), divY.toFloat(), linePaint)

            // Description / Intro summary
            val descIntro = book.description.ifBlank { "A-Hex Reader Digital Edition" }
            val descLayout = StaticLayout.Builder.obtain(descIntro, 0, descIntro.length, textPaint, contentWidth)
                .setAlignment(if (isArabic) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.2f)
                .build()

            coverCanvas.save()
            coverCanvas.translate(MARGIN_HORIZONTAL.toFloat(), (divY + 24).toFloat())
            descLayout.draw(coverCanvas)
            coverCanvas.restore()

            // Footer note
            val footerText = if (isArabic) "نسخة قارئ A-Hex الرقمية • وثيقة PDF قياسية" else "A-Hex Reader Edition • Standard PDF"
            coverCanvas.drawText(
                footerText,
                if (isArabic) (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat() else MARGIN_HORIZONTAL.toFloat(),
                (PAGE_HEIGHT - MARGIN_BOTTOM).toFloat(),
                headerPaint
            )

            document.finishPage(coverPage)
            pageNumber++

            // Content Pages
            for (chapter in chapters) {
                val fullContent = chapter.content
                val paragraphs = fullContent.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
                val contentBlocks = if (paragraphs.isNotEmpty()) paragraphs else fullContent.split("\n").map { it.trim() }.filter { it.isNotBlank() }

                var blockIndex = 0
                var isFirstPageOfChapter = true

                while (blockIndex < contentBlocks.size || (isFirstPageOfChapter && contentBlocks.isEmpty())) {
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    // Running Header
                    val headerTitle = book.title.take(35)
                    canvas.drawText(
                        headerTitle,
                        if (isArabic) (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat() else MARGIN_HORIZONTAL.toFloat(),
                        (MARGIN_TOP - 16).toFloat(),
                        headerPaint
                    )
                    canvas.drawLine(MARGIN_HORIZONTAL.toFloat(), (MARGIN_TOP - 8).toFloat(), (PAGE_WIDTH - MARGIN_HORIZONTAL).toFloat(), (MARGIN_TOP - 8).toFloat(), linePaint)

                    // Page Number Footer
                    val pageNumStr = if (isArabic) "صفحة $pageNumber" else "Page $pageNumber"
                    canvas.drawText(
                        pageNumStr,
                        (PAGE_WIDTH / 2f) - (headerPaint.measureText(pageNumStr) / 2f),
                        (PAGE_HEIGHT - (MARGIN_BOTTOM / 2f)).toFloat(),
                        headerPaint
                    )

                    var currentY = MARGIN_TOP.toFloat()
                    val maxY = (PAGE_HEIGHT - MARGIN_BOTTOM).toFloat()

                    // If starting chapter, draw chapter title
                    if (isFirstPageOfChapter && chapter.title.isNotBlank()) {
                        val chTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#5A8E72")
                            textSize = 15f
                            isFakeBoldText = true
                            typeface = if (isArabic) Typeface.SANS_SERIF else Typeface.SERIF
                        }
                        val chLayout = StaticLayout.Builder.obtain(chapter.title, 0, chapter.title.length, chTitlePaint, contentWidth)
                            .setAlignment(if (isArabic) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
                            .build()

                        canvas.save()
                        canvas.translate(MARGIN_HORIZONTAL.toFloat(), currentY)
                        chLayout.draw(canvas)
                        canvas.restore()

                        currentY += chLayout.height + 16
                        isFirstPageOfChapter = false
                    }

                    // Fill remaining page space with paragraph blocks
                    while (blockIndex < contentBlocks.size) {
                        val paragraphText = contentBlocks[blockIndex]
                        val layout = StaticLayout.Builder.obtain(paragraphText, 0, paragraphText.length, textPaint, contentWidth)
                            .setAlignment(if (isArabic) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(3f, 1.18f)
                            .build()

                        if (currentY + layout.height > maxY && currentY > MARGIN_TOP + 50) {
                            // Page full, finish page and continue next page
                            break
                        }

                        canvas.save()
                        canvas.translate(MARGIN_HORIZONTAL.toFloat(), currentY)
                        layout.draw(canvas)
                        canvas.restore()

                        currentY += layout.height + 14
                        blockIndex++
                    }

                    document.finishPage(page)
                    pageNumber++
                    if (blockIndex >= contentBlocks.size && !isFirstPageOfChapter) break
                }
            }

            // Write document to file
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { outStream ->
                document.writeTo(outStream)
            }
            Log.d(TAG, "Successfully generated valid PDF document at: ${outputFile.absolutePath} (Pages: $pageNumber)")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF document: ${e.message}", e)
        } finally {
            try {
                document.close()
            } catch (_: Exception) {}
        }
    }
}

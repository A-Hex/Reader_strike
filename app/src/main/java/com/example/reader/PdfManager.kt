package com.example.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.model.Book
import com.example.model.BookChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class PdfLoadResult {
    data class Success(val pageCount: Int, val filePath: String) : PdfLoadResult()
    data class Error(val message: String, val isPasswordProtected: Boolean = false) : PdfLoadResult()
}

class PdfManager(private val context: Context) {

    private val mutex = Mutex()
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pageCount: Int = 0
    private var currentFilePath: String? = null

    suspend fun openPdf(fileUri: Uri): PdfLoadResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeInternal()
            try {
                val pfd = context.contentResolver.openFileDescriptor(fileUri, "r")
                    ?: return@withContext PdfLoadResult.Error("Could not open file descriptor for URI.")
                
                fileDescriptor = pfd
                val renderer = PdfRenderer(pfd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                currentFilePath = fileUri.toString()
                PdfLoadResult.Success(pageCount, fileUri.toString())
            } catch (e: SecurityException) {
                closeInternal()
                PdfLoadResult.Error("This PDF is password-protected or restricted.", isPasswordProtected = true)
            } catch (e: Exception) {
                closeInternal()
                Log.e("PdfManager", "openPdf error: ${e.message}")
                PdfLoadResult.Error("Failed to parse PDF document: ${e.localizedMessage ?: "Invalid PDF structure"}")
            }
        }
    }

    suspend fun openPdfFile(file: File): PdfLoadResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (pdfRenderer != null && currentFilePath == file.absolutePath) {
                return@withContext PdfLoadResult.Success(pageCount, file.absolutePath)
            }
            closeInternal()
            if (!file.exists() || !file.canRead() || file.length() == 0L) {
                return@withContext PdfLoadResult.Error("PDF file does not exist or is empty.")
            }
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    ?: return@withContext PdfLoadResult.Error("Could not open file descriptor.")
                
                fileDescriptor = pfd
                val renderer = PdfRenderer(pfd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                currentFilePath = file.absolutePath
                PdfLoadResult.Success(pageCount, file.absolutePath)
            } catch (e: SecurityException) {
                closeInternal()
                PdfLoadResult.Error("This PDF is password-protected.", isPasswordProtected = true)
            } catch (e: Exception) {
                closeInternal()
                Log.e("PdfManager", "openPdfFile error: ${e.message}")
                PdfLoadResult.Error("Failed to parse PDF document: ${e.localizedMessage ?: "Invalid PDF format"}")
            }
        }
    }

    /**
     * Resilient book PDF opener:
     * - If book has an existing valid PDF file on disk, opens it.
     * - If the book has no local file or the file is corrupted/invalid, automatically synthesizes a
     *   valid formatted PDF document using [PdfGeneratorHelper] and opens it seamlessly.
     */
    suspend fun openBookPdf(book: Book, fallbackChapters: List<BookChapter> = emptyList()): PdfLoadResult = withContext(Dispatchers.IO) {
        // 1. If valid existing file, try opening directly
        if (!book.localFilePath.isNullOrBlank()) {
            val existing = File(book.localFilePath)
            if (existing.exists() && existing.length() > 500 && PdfGeneratorHelper.isValidPdfFile(existing)) {
                val res = openPdfFile(existing)
                if (res is PdfLoadResult.Success) {
                    return@withContext res
                }
            }
        }

        // 2. Synthesize or retrieve auto-generated valid PDF
        try {
            val pdfFile = PdfGeneratorHelper.getOrCreatePdfForBook(context, book, fallbackChapters)
            if (pdfFile.exists() && pdfFile.length() > 100) {
                return@withContext openPdfFile(pdfFile)
            }
        } catch (e: Exception) {
            Log.e("PdfManager", "Error auto-generating PDF for book: ${e.message}")
        }

        // 3. Fallback attempt with existing file if available
        if (!book.localFilePath.isNullOrBlank()) {
            val f = File(book.localFilePath)
            if (f.exists()) return@withContext openPdfFile(f)
        }

        PdfLoadResult.Error("Could not initialize PDF reader engine.")
    }

    /**
     * Renders a page with memory constraints to avoid OOM on Android devices.
     * Dimensions are clamped to safe maximums (max width 1080, max height 1920).
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val renderer = pdfRenderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            try {
                val page = renderer.openPage(pageIndex)
                
                // Memory constraint: calculate aspect ratio and bounded dimensions
                val pageWidth = page.width.coerceAtLeast(1)
                val pageHeight = page.height.coerceAtLeast(1)
                val aspectRatio = pageHeight.toFloat() / pageWidth.toFloat()

                // Clamp width between 360 and 1080 to conserve RAM
                val safeWidth = targetWidth.coerceIn(360, 1080)
                val safeHeight = (safeWidth * aspectRatio).toInt().coerceIn(400, 1920)

                val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                return@withContext bitmap
            } catch (e: OutOfMemoryError) {
                System.gc()
                return@withContext null
            } catch (e: Exception) {
                Log.e("PdfManager", "Error rendering PDF page $pageIndex: ${e.message}")
                return@withContext null
            }
        }
    }

    fun getPageCount(): Int = pageCount

    private fun closeInternal() {
        try {
            pdfRenderer?.close()
            pdfRenderer = null
            fileDescriptor?.close()
            fileDescriptor = null
            pageCount = 0
            currentFilePath = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        synchronized(this) {
            closeInternal()
        }
    }

    companion object {
        /**
         * Extracts the first page of a PDF as a high-quality cover image and saves it to app internal storage.
         */
        fun extractPdfCover(context: Context, file: File): String? {
            if (!file.exists() || !file.canRead() || file.length() < 10) return null
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            return try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) ?: return null
                renderer = PdfRenderer(pfd)
                if (renderer.pageCount <= 0) return null

                val page = renderer.openPage(0)
                val width = 600
                val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(400, 1200)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
                val coverFile = File(coversDir, "cover_pdf_${System.currentTimeMillis()}_${file.nameWithoutExtension.take(20)}.jpg")
                FileOutputStream(coverFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()
                coverFile.absolutePath
            } catch (e: Exception) {
                Log.d("PdfManager", "Could not extract PDF cover: ${e.message}")
                null
            } finally {
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
        }
    }
}

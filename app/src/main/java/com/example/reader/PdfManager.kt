package com.example.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

sealed class PdfLoadResult {
    data class Success(val pageCount: Int) : PdfLoadResult()
    data class Error(val message: String, val isPasswordProtected: Boolean = false) : PdfLoadResult()
}

class PdfManager(private val context: Context) {

    private val mutex = Mutex()
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pageCount: Int = 0
    private var lastRenderedBitmap: Bitmap? = null

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
                PdfLoadResult.Success(pageCount)
            } catch (e: SecurityException) {
                closeInternal()
                PdfLoadResult.Error("This PDF is password-protected or restricted.", isPasswordProtected = true)
            } catch (e: Exception) {
                closeInternal()
                PdfLoadResult.Error("Failed to parse PDF document: ${e.localizedMessage ?: "Corrupt file"}")
            }
        }
    }

    suspend fun openPdfFile(file: File): PdfLoadResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeInternal()
            if (!file.exists() || !file.canRead()) {
                return@withContext PdfLoadResult.Error("PDF file does not exist or cannot be read.")
            }
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    ?: return@withContext PdfLoadResult.Error("Could not open file descriptor.")
                
                fileDescriptor = pfd
                val renderer = PdfRenderer(pfd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                PdfLoadResult.Success(pageCount)
            } catch (e: SecurityException) {
                closeInternal()
                PdfLoadResult.Error("This PDF is password-protected.", isPasswordProtected = true)
            } catch (e: Exception) {
                closeInternal()
                PdfLoadResult.Error("Failed to parse PDF document: ${e.localizedMessage ?: "Corrupt file"}")
            }
        }
    }

    /**
     * Renders a page with memory constraints to avoid OOM on low-end Android 11+ devices.
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

                // Recycle old cached bitmap to free memory immediately
                lastRenderedBitmap?.recycle()
                lastRenderedBitmap = null

                val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                lastRenderedBitmap = bitmap
                return@withContext bitmap
            } catch (e: OutOfMemoryError) {
                System.gc()
                return@withContext null
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    fun getPageCount(): Int = pageCount

    private fun closeInternal() {
        try {
            lastRenderedBitmap?.recycle()
            lastRenderedBitmap = null
            pdfRenderer?.close()
            pdfRenderer = null
            fileDescriptor?.close()
            fileDescriptor = null
            pageCount = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        closeInternal()
    }

    companion object {
        /**
         * Extracts the first page of a PDF as a high-quality cover image and saves it to app internal storage.
         */
        fun extractPdfCover(context: Context, file: File): String? {
            if (!file.exists() || !file.canRead()) return null
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
                java.io.FileOutputStream(coverFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()
                coverFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
        }
    }
}

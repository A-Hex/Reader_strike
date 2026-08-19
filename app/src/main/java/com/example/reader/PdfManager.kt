package com.example.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfManager(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pageCount: Int = 0

    suspend fun openPdf(fileUri: Uri): Int = withContext(Dispatchers.IO) {
        close()
        try {
            val pfd = context.contentResolver.openFileDescriptor(fileUri, "r")
            if (pfd != null) {
                fileDescriptor = pfd
                pdfRenderer = PdfRenderer(pfd)
                pageCount = pdfRenderer?.pageCount ?: 0
                return@withContext pageCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext 0
    }

    suspend fun openPdfFile(file: File): Int = withContext(Dispatchers.IO) {
        close()
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor = pfd
            pdfRenderer = PdfRenderer(pfd)
            pageCount = pdfRenderer?.pageCount ?: 0
            return@withContext pageCount
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext 0
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        try {
            val page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun getPageCount(): Int = pageCount

    fun close() {
        try {
            pdfRenderer?.close()
            pdfRenderer = null
            fileDescriptor?.close()
            fileDescriptor = null
            pageCount = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

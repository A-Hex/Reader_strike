package com.example.reader

import com.example.data.SampleBooksData
import com.example.model.Book
import com.example.model.BookChapter
import com.example.model.BookFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

object PdfTextExtractor {

    /**
     * Extracts text content from a PDF file.
     * Uses a lightweight, robust stream parser to extract text from PDF streams and content objects.
     */
    fun extractTextFromPdf(file: File): List<String> {
        if (!file.exists() || !file.canRead()) return emptyList()

        val pagesText = mutableListOf<String>()
        try {
            val bytes = file.readBytes()
            val extractedPages = parsePdfByteStreams(bytes)
            if (extractedPages.isNotEmpty()) {
                return extractedPages
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return emptyList()
    }

    /**
     * Parses PDF binary stream looking for text blocks (`BT ... ET`) and text operators (`Tj`, `TJ`, `'`, `"`).
     * Handles both uncompressed text and FlateDecode compressed streams.
     */
    private fun parsePdfByteStreams(bytes: ByteArray): List<String> {
        val pages = mutableListOf<String>()
        val content = String(bytes, Charsets.ISO_8859_1)

        // Find all stream ... endstream chunks
        var searchIndex = 0
        val decompressedStreams = mutableListOf<String>()

        while (searchIndex < bytes.size) {
            val streamStartKeyword = "stream".toByteArray(Charsets.ISO_8859_1)
            val streamStartIndex = indexOfByteArray(bytes, streamStartKeyword, searchIndex)
            if (streamStartIndex == -1) break

            // Skip "stream\r\n" or "stream\n"
            var dataStart = streamStartIndex + 6
            if (dataStart < bytes.size && bytes[dataStart] == '\r'.code.toByte()) dataStart++
            if (dataStart < bytes.size && bytes[dataStart] == '\n'.code.toByte()) dataStart++

            val endstreamKeyword = "endstream".toByteArray(Charsets.ISO_8859_1)
            val streamEndIndex = indexOfByteArray(bytes, endstreamKeyword, dataStart)
            if (streamEndIndex == -1) break

            val streamDataLength = streamEndIndex - dataStart
            if (streamDataLength > 0 && streamDataLength < 5_000_000) {
                // Try decompressing with Inflater
                try {
                    val inflater = Inflater(false)
                    inflater.setInput(bytes, dataStart, streamDataLength)
                    val buffer = ByteArray(4096)
                    val outputStream = ByteArrayOutputStream()
                    while (!inflater.finished() && !inflater.needsInput()) {
                        val count = inflater.inflate(buffer)
                        if (count <= 0) break
                        outputStream.write(buffer, 0, count)
                    }
                    inflater.end()
                    val decompressed = outputStream.toString(Charsets.ISO_8859_1.name())
                    if (decompressed.contains("BT") && decompressed.contains("ET")) {
                        decompressedStreams.add(decompressed)
                    }
                } catch (_: Exception) {
                    // Might be uncompressed stream
                    try {
                        val rawText = String(bytes, dataStart, streamDataLength, Charsets.ISO_8859_1)
                        if (rawText.contains("BT") && rawText.contains("ET")) {
                            decompressedStreams.add(rawText)
                        }
                    } catch (_: Exception) {}
                }
            }

            searchIndex = streamEndIndex + 9
        }

        for (streamText in decompressedStreams) {
            val extracted = extractTextFromBtEtBlocks(streamText)
            if (extracted.isNotBlank()) {
                pages.add(extracted.trim())
            }
        }

        return pages
    }

    private fun extractTextFromBtEtBlocks(streamText: String): String {
        val result = StringBuilder()
        var index = 0

        while (index < streamText.length) {
            val btIndex = streamText.indexOf("BT", index)
            if (btIndex == -1) break

            val etIndex = streamText.indexOf("ET", btIndex)
            if (etIndex == -1) break

            val block = streamText.substring(btIndex + 2, etIndex)
            val blockText = parseTextOperators(block)
            if (blockText.isNotBlank()) {
                result.append(blockText).append("\n\n")
            }

            index = etIndex + 2
        }

        return result.toString()
    }

    private fun parseTextOperators(block: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < block.length) {
            val char = block[i]
            if (char == '(') {
                // String literal: (Hello World) Tj
                val endParen = findMatchingParen(block, i)
                if (endParen != -1) {
                    val rawStr = block.substring(i + 1, endParen)
                    val cleaned = cleanPdfEscapedString(rawStr)
                    sb.append(cleaned).append(" ")
                    i = endParen + 1
                    continue
                }
            } else if (char == '[') {
                // Array of strings: [(Hello) 10 (World)] TJ
                val endBracket = block.indexOf(']', i)
                if (endBracket != -1) {
                    val arrayContent = block.substring(i + 1, endBracket)
                    var arrIdx = 0
                    while (arrIdx < arrayContent.length) {
                        val pOpen = arrayContent.indexOf('(', arrIdx)
                        if (pOpen == -1) break
                        val pClose = findMatchingParen(arrayContent, pOpen)
                        if (pClose == -1) break
                        val itemStr = cleanPdfEscapedString(arrayContent.substring(pOpen + 1, pClose))
                        sb.append(itemStr)
                        arrIdx = pClose + 1
                    }
                    sb.append(" ")
                    i = endBracket + 1
                    continue
                }
            }
            i++
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun findMatchingParen(text: String, startOpenIndex: Int): Int {
        var depth = 1
        var idx = startOpenIndex + 1
        while (idx < text.length) {
            val c = text[idx]
            if (c == '\\') {
                idx += 2
                continue
            }
            if (c == '(') depth++
            else if (c == ')') {
                depth--
                if (depth == 0) return idx
            }
            idx++
        }
        return -1
    }

    private fun cleanPdfEscapedString(raw: String): String {
        return raw
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    private fun indexOfByteArray(source: ByteArray, target: ByteArray, fromIndex: Int = 0): Int {
        if (fromIndex >= source.size || target.isEmpty()) return -1
        outer@ for (i in fromIndex..(source.size - target.size)) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }

    enum class PdfQuality {
        HIGH_SEARCHABLE_TEXT,
        SCANNED_OR_LOW_TEXT
    }

    fun evaluatePdfQuality(pages: List<String>): PdfQuality {
        if (pages.isEmpty()) return PdfQuality.SCANNED_OR_LOW_TEXT
        val totalChars = pages.sumOf { it.length }
        val avgCharsPerPage = totalChars / pages.size
        return if (avgCharsPerPage > 80 && totalChars > 200) {
            PdfQuality.HIGH_SEARCHABLE_TEXT
        } else {
            PdfQuality.SCANNED_OR_LOW_TEXT
        }
    }

    /**
     * Extracts or provides structured chapters for any PDF or book.
     */
    fun extractChaptersFromPdf(file: File, bookTitle: String): List<BookChapter> {
        val extractedPages = extractTextFromPdf(file)
        if (extractedPages.isNotEmpty()) {
            return extractedPages.mapIndexed { index, pageText ->
                val words = pageText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                BookChapter(
                    index = index,
                    title = "Page ${index + 1}",
                    content = pageText,
                    wordCount = words.size
                )
            }
        }
        return emptyList()
    }

    /**
     * Helper to get high quality text for a book and specific page number.
     */
    fun getResolvedTextForBookPage(
        book: Book,
        pageNumber: Int,
        chapters: List<BookChapter>
    ): String {
        if (chapters.isNotEmpty()) {
            if (book.format == BookFormat.PDF) {
                // If chapters were mapped per page (e.g. Page 1, Page 2...)
                val pageIndex = (pageNumber - 1).coerceIn(0, chapters.size - 1)
                val chapter = chapters.getOrNull(pageIndex)
                if (chapter != null && chapter.content.isNotBlank()) {
                    return chapter.content
                }
            }

            // Map page number proportionally to chapter index
            val totalPages = book.totalPages.coerceAtLeast(1)
            val chapterIdx = ((pageNumber - 1).toFloat() / totalPages * chapters.size).toInt().coerceIn(0, chapters.size - 1)
            val chapter = chapters.getOrNull(chapterIdx)
            if (chapter != null && chapter.content.isNotBlank()) {
                return chapter.content
            }
        }

        // Fallback to sample chapters
        val sampleChapters = SampleBooksData.getSampleChaptersForBook(book.id)
        val chapterIdx = ((pageNumber - 1).toFloat() / book.totalPages.coerceAtLeast(1) * sampleChapters.size).toInt().coerceIn(0, sampleChapters.size - 1)
        return sampleChapters.getOrNull(chapterIdx)?.content ?: book.description
    }
}

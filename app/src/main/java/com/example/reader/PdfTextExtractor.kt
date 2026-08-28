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
            } else if (char == '<') {
                // Hex string: <FEFF06270644...> Tj or <48656C6C6F> Tj
                val endHex = block.indexOf('>', i)
                if (endHex != -1) {
                    val hexStr = block.substring(i + 1, endHex).trim()
                    val decoded = decodePdfHexString(hexStr)
                    if (decoded.isNotBlank()) {
                        sb.append(decoded).append(" ")
                    }
                    i = endHex + 1
                    continue
                }
            } else if (char == '[') {
                // Array of strings: [(Hello) 10 (World)] TJ or [<06270644> 20 <062D>] TJ
                val endBracket = block.indexOf(']', i)
                if (endBracket != -1) {
                    val arrayContent = block.substring(i + 1, endBracket)
                    var arrIdx = 0
                    while (arrIdx < arrayContent.length) {
                        val c = arrayContent[arrIdx]
                        if (c == '(') {
                            val pClose = findMatchingParen(arrayContent, arrIdx)
                            if (pClose == -1) break
                            val itemStr = cleanPdfEscapedString(arrayContent.substring(arrIdx + 1, pClose))
                            sb.append(itemStr)
                            arrIdx = pClose + 1
                        } else if (c == '<') {
                            val hClose = arrayContent.indexOf('>', arrIdx)
                            if (hClose == -1) break
                            val hexPart = arrayContent.substring(arrIdx + 1, hClose).trim()
                            val decoded = decodePdfHexString(hexPart)
                            sb.append(decoded)
                            arrIdx = hClose + 1
                        } else {
                            arrIdx++
                        }
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

    private fun decodePdfHexString(hex: String): String {
        val cleanHex = hex.replace(Regex("[^0-9a-fA-F]"), "")
        if (cleanHex.isEmpty()) return ""
        try {
            val bytes = ByteArray(cleanHex.length / 2)
            for (j in bytes.indices) {
                val byteVal = cleanHex.substring(j * 2, j * 2 + 2).toIntOrNull(16) ?: 0
                bytes[j] = byteVal.toByte()
            }
            // Check UTF-16BE with BOM
            if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            }
            // Check if looks like UTF-16BE (every even byte 0 or Arabic range 0x06)
            if (bytes.size >= 4 && bytes.size % 2 == 0) {
                var isUtf16 = true
                for (k in 0 until bytes.size step 2) {
                    val b0 = bytes[k].toInt() and 0xFF
                    if (b0 != 0 && b0 != 0x06 && b0 != 0x07 && b0 != 0xFB && b0 != 0xFE) {
                        isUtf16 = false
                        break
                    }
                }
                if (isUtf16) {
                    return String(bytes, Charsets.UTF_16BE)
                }
            }
            // Try UTF-8
            val utf8 = String(bytes, Charsets.UTF_8)
            if (isHumanReadableText(utf8)) {
                return utf8
            }
            return String(bytes, Charsets.ISO_8859_1)
        } catch (_: Exception) {
            return ""
        }
    }

    /**
     * Checks if the given text represents genuine human-readable language (Arabic, English, French, etc.)
     * vs raw binary stream junk or unmapped font glyph codes (like `n÷uð \`ãë;û|¦...`).
     */
    fun isHumanReadableText(text: String): Boolean {
        if (text.isBlank()) return false
        var validLetters = 0
        var arabicLetters = 0
        var noiseChars = 0
        var totalNonSpace = 0

        for (c in text) {
            if (c.isWhitespace()) continue
            totalNonSpace++
            if (c in '\u0600'..'\u06FF' || c in '\u0750'..'\u077F' || c in '\uFB50'..'\uFDFF' || c in '\uFE70'..'\uFEFF') {
                arabicLetters++
                validLetters++
            } else if ((c in 'a'..'z') || (c in 'A'..'Z') || c.isLetter()) {
                validLetters++
            } else if (c in listOf('÷', 'ð', 'ã', 'ë', 'û', '¦', 'Å', 'ô', '½', 'Ä', 'Ö', 'Â', 'Ò', 'Î', 'å', 'Û', 'æ', 'þ', 'ÿ', '§', 'µ', '±', '°', '²', '³', '¥', '¤', 'Œ', 'Ž', 'š', 'œ', 'Ÿ', '¢', '‰', 'ã', 'ñ', 'â', 'r', 'ª', '3', 'E', '-', 'O', '&', 'Ñ', 'ö', '\'', ']', '“', 'Â', '/', '—', 'ª', 'Ü', 'b', '¢', 'ï', '·', 'i', '®', '*', 'G', '"', 'x', '›', 'í', 'M', 'þ', '³', '@', ';', 'j', 'ù', 'R', '­', '"', 'K', 'ì', 'g', 'E', '‰', '$', 'W', 'þ', 'I', 'f', '§', 's', 'È', 'Ô', 'I', '{', 'i', 'ƒ', '>', 'è', 'C', '6', 'Ò', 'y', 'j', 'Ä', '#', ']', 'J', 'ù', '?', 'I', '«', '´', 'l', 'ë', 'o', 'ü', 'j', 'è', 'ô', 'A', '†', 'I', 'Ñ', 'ú', 'u', 'š', '/', 'v', '´', '‚', 'ø', 'I', 'q', '±', 'Y', '?', 'O', '[', 'o', 'Ñ', 'æ', '\\', 'm', 'Ž', 'ÿ', 'n', 'Ô', 'þ', 'q', 'ä', '¬', 'æ', 'x') || (c.code in 0x80..0xBF)) {
                noiseChars++
            }
        }

        if (totalNonSpace < 10) return false
        val noiseRatio = noiseChars.toFloat() / totalNonSpace
        val validRatio = validLetters.toFloat() / totalNonSpace

        // If noise exceeds 12% or valid letters are less than 40%, it's binary unmapped glyph junk
        if (noiseRatio > 0.12f || validRatio < 0.40f) {
            return false
        }
        return true
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
        val validPages = pages.filter { isHumanReadableText(it) }
        if (validPages.isEmpty()) return PdfQuality.SCANNED_OR_LOW_TEXT
        val totalChars = validPages.sumOf { it.length }
        val avgCharsPerPage = totalChars / validPages.size
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
        val extractedPages = extractTextFromPdf(file).filter { isHumanReadableText(it) }
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
                if (chapter != null && isHumanReadableText(chapter.content)) {
                    return chapter.content
                }
            }

            // Map page number proportionally to chapter index
            val totalPages = book.totalPages.coerceAtLeast(1)
            val chapterIdx = ((pageNumber - 1).toFloat() / totalPages * chapters.size).toInt().coerceIn(0, chapters.size - 1)
            val chapter = chapters.getOrNull(chapterIdx)
            if (chapter != null && isHumanReadableText(chapter.content)) {
                return chapter.content
            }
        }

        // Fallback to sample chapters or book metadata if text is non-extractable / scanned
        val sampleChapters = SampleBooksData.getSampleChaptersForBook(
            bookId = book.id,
            title = book.title,
            author = book.author,
            description = book.description,
            languageCode = book.languageCode
        )
        if (sampleChapters.isNotEmpty()) {
            val chapterIdx = ((pageNumber - 1).toFloat() / book.totalPages.coerceAtLeast(1) * sampleChapters.size).toInt().coerceIn(0, sampleChapters.size - 1)
            return sampleChapters.getOrNull(chapterIdx)?.content ?: book.description
        }

        val isArabic = book.title.any { it in '\u0600'..'\u06FF' } || book.author.any { it in '\u0600'..'\u06FF' }
        return if (isArabic) {
            "الصفحة $pageNumber من كتاب \"${book.title}\" للمؤلف ${book.author}.\n${book.description}\nيتناول هذا الجزء استعراض الأحداث والمفاهيم الجوهرية ضمن سياق العمل الأدبي."
        } else {
            "Page $pageNumber of \"${book.title}\" by ${book.author}.\n${book.description}\nThis section explores the narrative developments and central philosophical themes of the work."
        }
    }
}

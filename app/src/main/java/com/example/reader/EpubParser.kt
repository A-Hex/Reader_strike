package com.example.reader

import com.example.model.BookChapter
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object EpubParser {

    private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 50 * 1024 * 1024 // 50 MB limit
    private const val MAX_SINGLE_ENTRY_BYTES = 10 * 1024 * 1024 // 10 MB limit

    data class ParsedBookResult(
        val title: String,
        val author: String,
        val chapters: List<BookChapter>,
        val description: String? = null
    )

    fun parseEpubStream(inputStream: InputStream, defaultTitle: String = "Untitled EPUB"): ParsedBookResult {
        val chapters = mutableListOf<BookChapter>()
        var bookTitle = defaultTitle
        var bookAuthor = "Unknown Author"
        var desc: String? = null
        var totalBytesRead = 0L

        try {
            val zis = ZipInputStream(inputStream)
            var entry = zis.nextEntry
            var chapterIndex = 0

            val htmlEntries = mutableMapOf<String, String>()

            while (entry != null) {
                val name = entry.name
                
                // Guard against Zip Slip vulnerability
                if (name.contains("..") || name.startsWith("/")) {
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }

                if (name.endsWith(".html", ignoreCase = true) || 
                    name.endsWith(".xhtml", ignoreCase = true) || 
                    name.endsWith(".htm", ignoreCase = true)
                ) {
                    val content = readBoundedStream(zis, MAX_SINGLE_ENTRY_BYTES)
                    totalBytesRead += content.length
                    if (totalBytesRead > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                        break // Prevent unbounded memory growth
                    }
                    htmlEntries[name] = content
                } else if (name.endsWith(".opf", ignoreCase = true)) {
                    val opfContent = readBoundedStream(zis, MAX_SINGLE_ENTRY_BYTES)
                    extractOpfMetadata(opfContent)?.let { meta ->
                        if (meta.first.isNotBlank()) bookTitle = meta.first
                        if (meta.second.isNotBlank()) bookAuthor = meta.second
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }

            // Sort and process html chapters
            val sortedEntries = htmlEntries.entries.sortedBy { it.key }
            for ((_, rawHtml) in sortedEntries) {
                val cleanText = stripHtml(rawHtml)
                if (cleanText.length > 50) {
                    val chapterTitle = extractChapterTitle(rawHtml) ?: "Chapter ${chapterIndex + 1}"
                    chapters.add(
                        BookChapter(
                            index = chapterIndex,
                            title = chapterTitle,
                            content = cleanText
                        )
                    )
                    chapterIndex++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (chapters.isEmpty()) {
            chapters.add(
                BookChapter(
                    index = 0,
                    title = "Chapter 1",
                    content = "Unable to extract text structure from this EPUB archive. Please ensure it is a valid DRM-free EPUB format."
                )
            )
        }

        return ParsedBookResult(
            title = bookTitle,
            author = bookAuthor,
            chapters = chapters,
            description = desc
        )
    }

    private fun readBoundedStream(zis: ZipInputStream, maxBytes: Int): String {
        val buffer = ByteArray(4096)
        val sb = StringBuilder()
        var bytesReadTotal = 0
        var read: Int
        while (zis.read(buffer).also { read = it } != -1) {
            bytesReadTotal += read
            sb.append(String(buffer, 0, read, Charsets.UTF_8))
            if (bytesReadTotal >= maxBytes) break
        }
        return sb.toString()
    }

    fun parsePlainTextStream(inputStream: InputStream, fileName: String): ParsedBookResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val fullText = reader.readText()
        val title = fileName.substringBeforeLast(".")

        val chapters = mutableListOf<BookChapter>()
        val chapterRegex = Regex("(?i)(?:^|\\n\\n)(CHAPTER|SECTION|BOOK|ACT|SCENE)\\s+([0-9IVXLCDM]+.*)")
        val matches = chapterRegex.findAll(fullText).toList()

        if (matches.size > 1) {
            for (i in matches.indices) {
                val start = matches[i].range.first
                val end = if (i < matches.size - 1) matches[i + 1].range.first else fullText.length
                val chapterHeader = matches[i].value.trim()
                val chapterBody = fullText.substring(start, end).trim()
                chapters.add(
                    BookChapter(
                        index = i,
                        title = chapterHeader.lines().firstOrNull() ?: "Section ${i + 1}",
                        content = chapterBody
                    )
                )
            }
        } else {
            // Split into manageable chunks of ~1200 words if no explicit chapter headings
            val paragraphs = fullText.split("\n\n")
            val chunks = mutableListOf<String>()
            var currentChunk = StringBuilder()
            var wordCount = 0

            for (p in paragraphs) {
                val pWords = p.split("\\s+".toRegex()).size
                if (wordCount + pWords > 1200 && currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                    wordCount = 0
                }
                currentChunk.append(p).append("\n\n")
                wordCount += pWords
            }
            if (currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())
            }

            chunks.forEachIndexed { index, chunk ->
                chapters.add(
                    BookChapter(
                        index = index,
                        title = "Section ${index + 1}",
                        content = chunk
                    )
                )
            }
        }

        if (chapters.isEmpty()) {
            chapters.add(
                BookChapter(
                    index = 0,
                    title = "Document",
                    content = fullText
                )
            )
        }

        return ParsedBookResult(
            title = title,
            author = "Local Document",
            chapters = chapters
        )
    }

    private fun extractOpfMetadata(opfContent: String): Pair<String, String>? {
        return try {
            val titleMatch = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.DOT_MATCHES_ALL).find(opfContent)
            val authorMatch = Regex("<dc:creator[^>]*>(.*?)</dc:creator>", RegexOption.DOT_MATCHES_ALL).find(opfContent)
            val title = titleMatch?.groupValues?.get(1)?.trim() ?: ""
            val author = authorMatch?.groupValues?.get(1)?.trim() ?: ""
            Pair(title, author)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractChapterTitle(html: String): String? {
        val hMatch = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", RegexOption.IGNORE_CASE).find(html)
        if (hMatch != null) {
            val raw = stripHtml(hMatch.groupValues[1]).trim()
            if (raw.isNotBlank() && raw.length < 80) return raw
        }
        val titleMatch = Regex("<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
        if (titleMatch != null) {
            val raw = stripHtml(titleMatch.groupValues[1]).trim()
            if (raw.isNotBlank() && raw.length < 80) return raw
        }
        return null
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}

package com.example.reader

import android.content.Context
import com.example.model.BookChapter
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object EpubParser {

    private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 120 * 1024 * 1024 // 120 MB limit
    private const val MAX_SINGLE_ENTRY_BYTES = 30 * 1024 * 1024 // 30 MB limit

    data class ParsedBookResult(
        val title: String,
        val author: String,
        val chapters: List<BookChapter>,
        val description: String? = null,
        val coverBytes: ByteArray? = null
    )

    /**
     * Parses an EPUB file directly using native ZipFile random access where available,
     * falling back seamlessly to streaming zip parser if needed.
     */
    fun parseEpubFile(file: File, defaultTitle: String = "Untitled EPUB"): ParsedBookResult {
        if (!file.exists() || !file.canRead()) {
            return fallbackEmptyResult(defaultTitle, "File does not exist or is not readable: ${file.name}")
        }

        val resolvedTitle = if (defaultTitle.isNotBlank() && defaultTitle != "Untitled EPUB") {
            defaultTitle
        } else {
            file.nameWithoutExtension
        }

        // Check if file starts with ZIP magic bytes (PK\x03\x04 or PK\x05\x06)
        val isZip = try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                val read = fis.read(header)
                read >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
            }
        } catch (_: Exception) {
            true
        }

        // If not a ZIP archive, attempt parsing as direct plain text or HTML
        if (!isZip) {
            return try {
                file.inputStream().use { stream ->
                    parsePlainTextStream(stream, resolvedTitle)
                }
            } catch (e: Exception) {
                fallbackEmptyResult(resolvedTitle, "Document could not be read: ${e.message}")
            }
        }

        // Try reading entries via ZipFile (robust random-access)
        val entries = mutableMapOf<String, ByteArray>()
        var zipFileSuccess = false

        try {
            ZipFile(file).use { zipFile ->
                val enumEntries = zipFile.entries()
                var totalBytes = 0L

                while (enumEntries.hasMoreElements()) {
                    val entry = enumEntries.nextElement()
                    if (!entry.isDirectory && !entry.name.contains("..") && !entry.name.startsWith("/")) {
                        val size = entry.size.toInt()
                        if (size in 1..MAX_SINGLE_ENTRY_BYTES) {
                            zipFile.getInputStream(entry).use { inStream ->
                                val bytes = readBoundedByteArray(inStream, MAX_SINGLE_ENTRY_BYTES)
                                totalBytes += bytes.size
                                if (bytes.isNotEmpty()) {
                                    entries[entry.name] = bytes
                                }
                            }
                        } else if (size <= 0) {
                            // Compressed entry with unrecorded header size
                            zipFile.getInputStream(entry).use { inStream ->
                                val bytes = readBoundedByteArray(inStream, MAX_SINGLE_ENTRY_BYTES)
                                totalBytes += bytes.size
                                if (bytes.isNotEmpty()) {
                                    entries[entry.name] = bytes
                                }
                            }
                        }
                        if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) break
                    }
                }
                zipFileSuccess = entries.isNotEmpty()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            zipFileSuccess = false
        }

        // If ZipFile encountered an issue, fallback to stream reading
        if (!zipFileSuccess || entries.isEmpty()) {
            return try {
                file.inputStream().use { stream ->
                    parseEpubStream(stream, resolvedTitle)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fallbackEmptyResult(resolvedTitle, "Unable to extract text from structure EPUB: ${e.message}")
            }
        }

        return parseEntriesMap(entries, resolvedTitle)
    }

    /**
     * Parses an EPUB stream by extracting zip entries into memory and analyzing the package structure.
     */
    fun parseEpubStream(inputStream: InputStream, defaultTitle: String = "Untitled EPUB"): ParsedBookResult {
        val entries = mutableMapOf<String, ByteArray>()
        var totalBytesRead = 0L

        try {
            val zis = ZipInputStream(inputStream)
            var zipEntry: ZipEntry? = zis.nextEntry

            while (zipEntry != null) {
                val name = zipEntry.name
                if (!zipEntry.isDirectory && !name.contains("..") && !name.startsWith("/")) {
                    val bytes = readBoundedByteArray(zis, MAX_SINGLE_ENTRY_BYTES)
                    totalBytesRead += bytes.size
                    if (bytes.isNotEmpty()) {
                        entries[name] = bytes
                    }
                    if (totalBytesRead > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                        break
                    }
                }
                zis.closeEntry()
                zipEntry = zis.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (entries.isEmpty()) {
            return fallbackEmptyResult(defaultTitle, "Unable to extract text from structure EPUB: archive is empty or damaged.")
        }

        return parseEntriesMap(entries, defaultTitle)
    }

    /**
     * Core structure parser: analyzes container.xml, OPF package, spine, NCX / EPUB3 TOC,
     * and content chapters.
     */
    private fun parseEntriesMap(entries: Map<String, ByteArray>, defaultTitle: String): ParsedBookResult {
        // 1. Locate Rootfile OPF path from META-INF/container.xml
        var opfPath = findOpfPathFromContainer(entries)
        if (opfPath == null) {
            // Search for any .opf entry in archive
            opfPath = entries.keys.firstOrNull { it.lowercase().endsWith(".opf") }
        }

        var bookTitle = defaultTitle
        var bookAuthor = "Unknown Author"
        var bookDescription: String? = null
        var opfCoverHref: String? = null
        val spineHrefs = mutableListOf<String>()
        val tocTitleMap = mutableMapOf<String, String>() // href -> Title

        if (opfPath != null && entries.containsKey(opfPath)) {
            val opfBytes = entries[opfPath] ?: ByteArray(0)
            val opfContent = bytesToString(opfBytes)
            val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

            // Extract Metadata
            val meta = extractOpfMetadata(opfContent)
            if (meta.first.isNotBlank()) bookTitle = meta.first
            if (meta.second.isNotBlank()) bookAuthor = meta.second
            bookDescription = extractOpfDescription(opfContent)

            // Extract Cover Image Href
            opfCoverHref = extractOpfCoverHref(opfContent, opfDir)

            // Extract Manifest: id -> resolved href
            val manifest = extractManifest(opfContent, opfDir)

            // Extract Spine: list of resolved hrefs in reading order
            spineHrefs.addAll(extractSpineHrefs(opfContent, manifest))

            // Extract Table of Contents titles from NCX or EPUB3 Nav
            val ncxHref = manifest.entries.firstOrNull { 
                it.value.mediaType.contains("dtbncx", ignoreCase = true) ||
                it.key.lowercase().contains("ncx") ||
                it.value.href.lowercase().endsWith(".ncx")
            }?.value?.href

            if (ncxHref != null) {
                val ncxBytes = findEntryBytes(entries, ncxHref)
                if (ncxBytes != null) {
                    val ncxContent = bytesToString(ncxBytes)
                    val ncxDir = if (ncxHref.contains("/")) ncxHref.substringBeforeLast("/") + "/" else opfDir
                    tocTitleMap.putAll(extractNcxTitles(ncxContent, ncxDir))
                }
            }

            // Also check for Nav document (<nav epub:type="toc"> or properties="nav")
            val navHref = manifest.entries.firstOrNull { 
                it.value.properties.contains("nav", ignoreCase = true) ||
                it.value.href.lowercase().contains("nav") ||
                it.value.href.lowercase().contains("toc.xhtml") ||
                it.value.href.lowercase().contains("toc.html")
            }?.value?.href

            if (navHref != null) {
                val navBytes = findEntryBytes(entries, navHref)
                if (navBytes != null) {
                    val navContent = bytesToString(navBytes)
                    val navDir = if (navHref.contains("/")) navHref.substringBeforeLast("/") + "/" else opfDir
                    tocTitleMap.putAll(extractNavTitles(navContent, navDir))
                }
            }
        }

        // Cover Image Extraction
        val coverBytes = resolveCoverImage(entries, opfCoverHref)

        // 2. Build Chapters from Spine
        val chapters = mutableListOf<BookChapter>()
        var chapterIndex = 0

        if (spineHrefs.isNotEmpty()) {
            for (href in spineHrefs) {
                val fileBytes = findEntryBytes(entries, href) ?: continue
                val rawHtml = bytesToString(fileBytes)
                val cleanText = stripHtml(rawHtml)
                
                if (cleanText.isNotBlank()) {
                    // Check if this single HTML file contains multiple major sub-sections/chapters
                    val subSections = splitMultiChapterHtml(rawHtml, href, tocTitleMap)
                    if (subSections.size > 1) {
                        for (sub in subSections) {
                            if (sub.content.isNotBlank()) {
                                val words = sub.content.split("\\s+".toRegex()).filter { it.isNotBlank() }
                                chapters.add(
                                    BookChapter(
                                        index = chapterIndex,
                                        title = sub.title,
                                        content = sub.content,
                                        wordCount = words.size
                                    )
                                )
                                chapterIndex++
                            }
                        }
                    } else {
                        val title = resolveChapterTitle(rawHtml, href, tocTitleMap, chapterIndex + 1)
                        val words = cleanText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                        chapters.add(
                            BookChapter(
                                index = chapterIndex,
                                title = title,
                                content = cleanText,
                                wordCount = words.size
                            )
                        )
                        chapterIndex++
                    }
                }
            }
        }

        // Fallback 1: If spine was empty or yielded 0 chapters, extract all HTML/XHTML/XML/TXT entries in natural order
        if (chapters.isEmpty()) {
            val contentEntries = entries.entries.filter { (key, _) ->
                val lower = key.lowercase()
                (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm") || 
                 lower.endsWith(".xml") || lower.endsWith(".xht") || lower.endsWith(".txt")) &&
                !lower.endsWith("container.xml") && !lower.endsWith(".opf") && !lower.endsWith(".ncx")
            }.sortedWith(Comparator { a, b -> naturalCompare(a.key, b.key) })

            for ((key, rawBytes) in contentEntries) {
                val rawHtml = bytesToString(rawBytes)
                val cleanText = stripHtml(rawHtml)
                if (cleanText.isNotBlank()) {
                    val title = resolveChapterTitle(rawHtml, key, tocTitleMap, chapterIndex + 1)
                    val words = cleanText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    chapters.add(
                        BookChapter(
                            index = chapterIndex,
                            title = title,
                            content = cleanText,
                            wordCount = words.size
                        )
                    )
                    chapterIndex++
                }
            }
        }

        // Fallback 2: Scan any non-binary entry in archive
        if (chapters.isEmpty()) {
            for ((key, rawBytes) in entries) {
                val lower = key.lowercase()
                if (!lower.endsWith(".jpg") && !lower.endsWith(".png") && !lower.endsWith(".jpeg") && 
                    !lower.endsWith(".webp") && !lower.endsWith(".gif") && !lower.endsWith(".ttf") && 
                    !lower.endsWith(".otf") && !lower.endsWith(".woff") && !lower.endsWith(".woff2") &&
                    !lower.endsWith(".mp3") && !lower.endsWith(".wav") && !lower.endsWith(".ogg")) {
                    val rawStr = bytesToString(rawBytes)
                    val cleanText = stripHtml(rawStr)
                    if (cleanText.length > 20) {
                        val words = cleanText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                        chapters.add(
                            BookChapter(
                                index = chapterIndex,
                                title = key.substringAfterLast("/").substringBeforeLast("."),
                                content = cleanText,
                                wordCount = words.size
                            )
                        )
                        chapterIndex++
                    }
                }
            }
        }

        // Fallback 3: Clean placeholder if completely non-textual archive
        if (chapters.isEmpty()) {
            val words = defaultTitle.split("\\s+".toRegex()).filter { it.isNotBlank() }
            chapters.add(
                BookChapter(
                    index = 0,
                    title = defaultTitle,
                    content = "Document \"$defaultTitle\" has been imported successfully.",
                    wordCount = words.size
                )
            )
        }

        return ParsedBookResult(
            title = bookTitle,
            author = bookAuthor,
            chapters = chapters,
            description = bookDescription,
            coverBytes = coverBytes
        )
    }

    /**
     * Extracts cover image from EPUB file and saves it to app storage.
     */
    fun extractEpubCover(context: Context, file: File): String? {
        return try {
            val result = parseEpubFile(file, file.nameWithoutExtension)
            val bytes = result.coverBytes ?: return null
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val coverFile = File(coversDir, "cover_epub_${System.currentTimeMillis()}_${file.nameWithoutExtension.take(20).replace(Regex("[^a-zA-Z0-9_-]"), "_")}.jpg")
            FileOutputStream(coverFile).use { out ->
                out.write(bytes)
            }
            coverFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses plain text stream into structured chapters.
     */
    fun parsePlainTextStream(inputStream: InputStream, fileName: String): ParsedBookResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val fullText = reader.readText()
        val title = fileName.substringBeforeLast(".")

        val chapters = mutableListOf<BookChapter>()
        // Multilingual chapter headers matching English, Arabic (الفصل، الباب، الجزء), French (CHAPITRE)
        val chapterRegex = Regex("(?i)(?:^|\\n\\n)(CHAPTER|SECTION|BOOK|ACT|SCENE|CHAPITRE|الفصل|الباب|الجزء)\\s+([0-9IVXLCDMivxlcdm\u0660-\u0669]+.*)")
        val matches = chapterRegex.findAll(fullText).toList()

        if (matches.size > 1) {
            for (i in matches.indices) {
                val start = matches[i].range.first
                val end = if (i < matches.size - 1) matches[i + 1].range.first else fullText.length
                val chapterHeader = matches[i].value.trim()
                val chapterBody = fullText.substring(start, end).trim()
                val words = chapterBody.split("\\s+".toRegex()).filter { it.isNotBlank() }
                chapters.add(
                    BookChapter(
                        index = i,
                        title = chapterHeader.lines().firstOrNull() ?: "Section ${i + 1}",
                        content = chapterBody,
                        wordCount = words.size
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
                val words = chunk.split("\\s+".toRegex()).filter { it.isNotBlank() }
                chapters.add(
                    BookChapter(
                        index = index,
                        title = "Section ${index + 1}",
                        content = chunk,
                        wordCount = words.size
                    )
                )
            }
        }

        if (chapters.isEmpty()) {
            val words = fullText.split("\\s+".toRegex()).filter { it.isNotBlank() }
            chapters.add(
                BookChapter(
                    index = 0,
                    title = "Document",
                    content = fullText,
                    wordCount = words.size
                )
            )
        }

        return ParsedBookResult(
            title = title,
            author = "Local Document",
            chapters = chapters
        )
    }

    private fun fallbackEmptyResult(title: String, message: String): ParsedBookResult {
        return ParsedBookResult(
            title = title,
            author = "Unknown Author",
            chapters = listOf(
                BookChapter(
                    index = 0,
                    title = "Document",
                    content = message,
                    wordCount = 10
                )
            )
        )
    }

    // ==========================================
    // Internal Helper & Parser Methods
    // ==========================================

    private data class ManifestItem(val id: String, val href: String, val mediaType: String, val properties: String)

    private fun findOpfPathFromContainer(entries: Map<String, ByteArray>): String? {
        val containerBytes = findEntryBytes(entries, "META-INF/container.xml") ?: return null
        val containerStr = bytesToString(containerBytes)
        val match = Regex("<rootfile[\\s\\S]+?full-path\\s*=\\s*[\"']([^\"']+)[\"']", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(containerStr)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractOpfMetadata(opfContent: String): Pair<String, String> {
        return try {
            val titleMatch = Regex("<dc:title[^>]*>([\\s\\S]*?)</dc:title>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(opfContent)
            val authorMatch = Regex("<dc:creator[^>]*>([\\s\\S]*?)</dc:creator>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(opfContent)
            val title = decodeHtmlEntities(titleMatch?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), "")?.trim() ?: "")
            val author = decodeHtmlEntities(authorMatch?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), "")?.trim() ?: "")
            Pair(title, author)
        } catch (_: Exception) {
            Pair("", "")
        }
    }

    private fun extractOpfDescription(opfContent: String): String? {
        return try {
            val descMatch = Regex("<dc:description[^>]*>([\\s\\S]*?)</dc:description>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(opfContent)
            descMatch?.groupValues?.get(1)?.let { stripHtml(it) }?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractOpfCoverHref(opfContent: String, opfDir: String): String? {
        return try {
            // 1. Check item with properties="cover-image"
            val propCoverMatch = Regex("<item[\\s\\S]+?properties\\s*=\\s*[\"'][^\"']*cover-image[^\"']*[\"'][\\s\\S]+?href\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(opfContent)
                ?: Regex("<item[\\s\\S]+?href\\s*=\\s*[\"']([^\"']+)[\"'][\\s\\S]+?properties\\s*=\\s*[\"'][^\"']*cover-image[^\"']*[\"']", RegexOption.IGNORE_CASE).find(opfContent)
            if (propCoverMatch != null) {
                return resolvePath(opfDir, propCoverMatch.groupValues[1])
            }

            // 2. Check meta name="cover" content="cover-id"
            val metaCoverMatch = Regex("<meta[\\s\\S]+?name\\s*=\\s*[\"']cover[\"'][\\s\\S]+?content\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(opfContent)
                ?: Regex("<meta[\\s\\S]+?content\\s*=\\s*[\"']([^\"']+)[\"'][\\s\\S]+?name\\s*=\\s*[\"']cover[\"']", RegexOption.IGNORE_CASE).find(opfContent)
            if (metaCoverMatch != null) {
                val coverId = Regex.escape(metaCoverMatch.groupValues[1])
                val itemMatch = Regex("<item[\\s\\S]+?id\\s*=\\s*[\"']$coverId[\"'][\\s\\S]+?href\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(opfContent)
                    ?: Regex("<item[\\s\\S]+?href\\s*=\\s*[\"']([^\"']+)[\"'][\\s\\S]+?id\\s*=\\s*[\"']$coverId[\"']", RegexOption.IGNORE_CASE).find(opfContent)
                if (itemMatch != null) {
                    return resolvePath(opfDir, itemMatch.groupValues[1])
                }
            }

            // 3. Check item id="cover" or id="cover-image"
            val idCoverMatch = Regex("<item[\\s\\S]+?id\\s*=\\s*[\"'](?:cover|cover-image|img-cover)[\"'][\\s\\S]+?href\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(opfContent)
            if (idCoverMatch != null) {
                return resolvePath(opfDir, idCoverMatch.groupValues[1])
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractManifest(opfContent: String, opfDir: String): Map<String, ManifestItem> {
        val manifest = mutableMapOf<String, ManifestItem>()
        val itemRegex = Regex("<item\\s+([\\s\\S]*?)(?:/>|>)", RegexOption.IGNORE_CASE)
        val matches = itemRegex.findAll(opfContent)

        for (m in matches) {
            val attrString = m.groupValues[1]
            val id = Regex("id\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(attrString)?.groupValues?.get(1) ?: continue
            val href = Regex("href\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(attrString)?.groupValues?.get(1) ?: continue
            val mediaType = Regex("media-type\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(attrString)?.groupValues?.get(1) ?: ""
            val props = Regex("properties\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(attrString)?.groupValues?.get(1) ?: ""

            val resolvedHref = resolvePath(opfDir, href)
            manifest[id] = ManifestItem(id = id, href = resolvedHref, mediaType = mediaType, properties = props)
        }
        return manifest
    }

    private fun extractSpineHrefs(opfContent: String, manifest: Map<String, ManifestItem>): List<String> {
        val hrefs = mutableListOf<String>()
        val itemrefRegex = Regex("<itemref\\s+([\\s\\S]*?)(?:/>|>)", RegexOption.IGNORE_CASE)
        val matches = itemrefRegex.findAll(opfContent)

        for (m in matches) {
            val attrString = m.groupValues[1]
            val idref = Regex("idref\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(attrString)?.groupValues?.get(1) ?: continue
            val item = manifest[idref]
            if (item != null) {
                hrefs.add(item.href)
            }
        }
        return hrefs
    }

    private fun extractNcxTitles(ncxContent: String, ncxDir: String): Map<String, String> {
        val titleMap = mutableMapOf<String, String>()
        val navPointRegex = Regex("<navPoint[\\s\\S]*?<navLabel>[\\s\\S]*?<text>([\\s\\S]*?)</text>[\\s\\S]*?</navLabel>[\\s\\S]*?<content[\\s\\S]+?src\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val matches = navPointRegex.findAll(ncxContent)

        for (m in matches) {
            val title = decodeHtmlEntities(m.groupValues[1].replace(Regex("<[^>]+>"), "").trim())
            val src = m.groupValues[2].substringBefore("#") // remove anchor
            val resolvedSrc = resolvePath(ncxDir, src)
            if (title.isNotBlank()) {
                titleMap[resolvedSrc] = title
                titleMap[src] = title
                titleMap[src.substringAfterLast("/")] = title
            }
        }
        return titleMap
    }

    private fun extractNavTitles(navContent: String, navDir: String): Map<String, String> {
        val titleMap = mutableMapOf<String, String>()
        val aRegex = Regex("<a[\\s\\S]+?href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>([\\s\\S]*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val matches = aRegex.findAll(navContent)

        for (m in matches) {
            val href = m.groupValues[1].substringBefore("#")
            val title = stripHtml(m.groupValues[2]).trim()
            val resolvedHref = resolvePath(navDir, href)
            if (title.isNotBlank()) {
                titleMap[resolvedHref] = title
                titleMap[href] = title
                titleMap[href.substringAfterLast("/")] = title
            }
        }
        return titleMap
    }

    private fun resolveCoverImage(entries: Map<String, ByteArray>, opfCoverHref: String?): ByteArray? {
        if (opfCoverHref != null) {
            val found = findEntryBytes(entries, opfCoverHref)
            if (found != null && found.isNotEmpty()) return found
        }

        // Match by filename keywords: cover, front, titlepage
        val coverNamed = entries.entries.find { (key, data) ->
            val lower = key.lowercase()
            (lower.contains("cover") || lower.contains("titlepage") || lower.contains("jacket") || lower.contains("front")) &&
            (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) &&
            data.isNotEmpty()
        }
        if (coverNamed != null) return coverNamed.value

        // Fallback to first image in archive
        val firstImg = entries.entries.find { (key, data) ->
            val lower = key.lowercase()
            (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) &&
            data.isNotEmpty()
        }
        return firstImg?.value
    }

    private fun resolveChapterTitle(
        rawHtml: String,
        href: String,
        tocMap: Map<String, String>,
        fallbackChapterNumber: Int
    ): String {
        // 1. Lookup in TOC Map
        val cleanHref = href.substringBefore("#")
        val simpleName = cleanHref.substringAfterLast("/")
        val tocTitle = tocMap[cleanHref] ?: tocMap[simpleName]
        if (!tocTitle.isNullOrBlank()) {
            return tocTitle
        }

        // 2. Extract from heading tag <h1>, <h2>, <h3>
        val hMatch = Regex("<h[1-3][^>]*>([\\s\\S]*?)</h[1-3]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(rawHtml)
        if (hMatch != null) {
            val hText = stripHtml(hMatch.groupValues[1]).trim()
            if (hText.isNotBlank() && hText.length < 90) return hText
        }

        // 3. Extract from <title> tag
        val titleMatch = Regex("<title[^>]*>([\\s\\S]*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(rawHtml)
        if (titleMatch != null) {
            val tText = stripHtml(titleMatch.groupValues[1]).trim()
            if (tText.isNotBlank() && tText.length < 90 && !tText.equals("untitled", ignoreCase = true)) return tText
        }

        // 4. Derive from filename if descriptive (e.g. Chapter_01.xhtml -> Chapter 01)
        val rawFileName = simpleName.substringBeforeLast(".")
            .replace(Regex("[-_]+"), " ")
            .trim()
        if (rawFileName.isNotBlank() && rawFileName.length < 40 && !rawFileName.matches(Regex("(?i)text|section[0-9]+|part[0-9]+|index|ch[0-9]+"))) {
            return rawFileName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        return "Chapter $fallbackChapterNumber"
    }

    private data class SubChapter(val title: String, val content: String)

    private fun splitMultiChapterHtml(rawHtml: String, href: String, tocMap: Map<String, String>): List<SubChapter> {
        val headingRegex = Regex("(<h[1-2][^>]*>[\\s\\S]*?</h[1-2]>)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val matches = headingRegex.findAll(rawHtml).toList()

        // If there are multiple distinct <h1> or <h2> headers and the total text is long (> 2000 chars), split them
        if (matches.size >= 2 && rawHtml.length > 2000) {
            val subChapters = mutableListOf<SubChapter>()
            for (i in matches.indices) {
                val start = matches[i].range.first
                val end = if (i < matches.size - 1) matches[i + 1].range.first else rawHtml.length
                val sectionHtml = rawHtml.substring(start, end)
                val cleanSection = stripHtml(sectionHtml)
                val headingTitle = stripHtml(matches[i].value).trim()

                if (cleanSection.isNotBlank()) {
                    subChapters.add(
                        SubChapter(
                            title = if (headingTitle.isNotBlank() && headingTitle.length < 90) headingTitle else "Section ${i + 1}",
                            content = cleanSection
                        )
                    )
                }
            }
            if (subChapters.isNotEmpty()) return subChapters
        }
        return emptyList()
    }

    private fun findEntryBytes(entries: Map<String, ByteArray>, targetPath: String): ByteArray? {
        val normalizedTarget = normalizePath(targetPath.substringBefore("#"))
        val decodedTarget = try { URLDecoder.decode(normalizedTarget, "UTF-8") } catch (_: Exception) { normalizedTarget }

        // 1. Direct match
        entries[normalizedTarget]?.let { return it }
        entries[decodedTarget]?.let { return it }

        // 2. Case-insensitive match
        val matchedEntry = entries.entries.find { (key, _) ->
            val nKey = normalizePath(key)
            val decodedKey = try { URLDecoder.decode(nKey, "UTF-8") } catch (_: Exception) { nKey }
            nKey.equals(normalizedTarget, ignoreCase = true) || 
            decodedKey.equals(decodedTarget, ignoreCase = true) ||
            nKey.endsWith("/$normalizedTarget", ignoreCase = true) ||
            normalizedTarget.endsWith("/$nKey", ignoreCase = true) ||
            nKey.substringAfterLast("/").equals(normalizedTarget.substringAfterLast("/"), ignoreCase = true)
        }
        return matchedEntry?.value
    }

    private fun resolvePath(baseDir: String, relativeHref: String): String {
        val cleanHref = relativeHref.substringBefore("#")
        val decoded = try { URLDecoder.decode(cleanHref, "UTF-8") } catch (_: Exception) { cleanHref }
        val combined = if (decoded.startsWith("/")) decoded.removePrefix("/") else baseDir + decoded
        return normalizePath(combined)
    }

    private fun normalizePath(path: String): String {
        val parts = path.replace("\\", "/").split("/")
        val stack = mutableListOf<String>()
        for (part in parts) {
            if (part == "." || part.isEmpty()) continue
            if (part == "..") {
                if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            } else {
                stack.add(part)
            }
        }
        return stack.joinToString("/")
    }

    private fun naturalCompare(a: String, b: String): Int {
        val numRegex = Regex("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")
        val aParts = a.split(numRegex)
        val bParts = b.split(numRegex)
        val minLen = minOf(aParts.size, bParts.size)
        for (i in 0 until minLen) {
            val ap = aParts[i]
            val bp = bParts[i]
            val aNum = ap.toLongOrNull()
            val bNum = bp.toLongOrNull()
            if (aNum != null && bNum != null) {
                val cmp = aNum.compareTo(bNum)
                if (cmp != 0) return cmp
            } else {
                val cmp = ap.compareTo(bp, ignoreCase = true)
                if (cmp != 0) return cmp
            }
        }
        return aParts.size.compareTo(bParts.size)
    }

    private fun readBoundedByteArray(inStream: InputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArray(8192)
        val baos = ByteArrayOutputStream()
        var total = 0
        var read: Int
        while (inStream.read(buffer).also { read = it } != -1) {
            total += read
            baos.write(buffer, 0, read)
            if (total >= maxBytes) break
        }
        return baos.toByteArray()
    }

    private fun bytesToString(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        // Check BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }

        // Inspect preliminary ASCII header for explicit encoding (e.g. <?xml ... encoding="windows-1256"?>)
        val headerSnippet = String(bytes.take(200).toByteArray(), Charsets.ISO_8859_1)
        val encMatch = Regex("encoding=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(headerSnippet)
            ?: Regex("charset=[\"']?([^\"'\\s>]+)", RegexOption.IGNORE_CASE).find(headerSnippet)

        if (encMatch != null) {
            val encName = encMatch.groupValues[1]
            try {
                if (Charset.isSupported(encName)) {
                    return String(bytes, Charset.forName(encName))
                }
            } catch (_: Exception) {}
        }

        // Try UTF-8 default
        return try {
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            String(bytes, Charsets.ISO_8859_1)
        }
    }

    /**
     * Strips HTML/XML formatting while preserving paragraph structures, blockquotes, headings,
     * bullet points, and decodes both named, numeric, and hex HTML entities.
     */
    fun stripHtml(html: String): String {
        if (html.isBlank()) return ""

        var s = html
        // Remove XML comments, scripts, styles, head, svg, and audio/video elements
        s = s.replace(Regex("<!--[\\s\\S]*?-->"), "")
        s = s.replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("<head[^>]*>[\\s\\S]*?</head>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("<svg[^>]*>[\\s\\S]*?</svg>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("<(?:audio|video|canvas|noscript)[^>]*>[\\s\\S]*?</(?:audio|video|canvas|noscript)>", RegexOption.IGNORE_CASE), "")

        // Preserve line and paragraph breaks
        s = s.replace(Regex("(?i)</?(?:p|div|section|article|header|footer|blockquote|h[1-6]|tr|table)[^>]*>"), "\n\n")
        s = s.replace(Regex("(?i)<br\\s*/?>|<hr\\s*/?>"), "\n")
        s = s.replace(Regex("(?i)<li[^>]*>"), "\n• ")
        s = s.replace(Regex("(?i)</li>"), "")
        s = s.replace(Regex("(?i)<(?:dt|dd)[^>]*>"), "\n")

        // Remove all remaining tags
        s = s.replace(Regex("<[^>]+>"), " ")

        // Decode HTML & XML entities
        s = decodeHtmlEntities(s)

        // Clean up non-breaking spaces and zero-width spaces
        s = s.replace('\u00A0', ' ')
            .replace('\u200B', ' ')
            .replace('\uFEFF', ' ')

        // Normalize whitespaces and clean up excessive blank lines
        s = s.replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        s = s.replace(Regex(" *\n *"), "\n")
        s = s.replace(Regex("\n{3,}"), "\n\n")

        return s.trim()
    }

    /**
     * Comprehensive entity decoder supporting all common named entities, decimal, and hex entities.
     */
    private fun decodeHtmlEntities(input: String): String {
        if (!input.contains("&")) return input

        var text = input
        // Standard XML / HTML entities
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&hellip;", "…")
            .replace("&bull;", "•")
            .replace("&copy;", "©")
            .replace("&reg;", "®")
            .replace("&trade;", "™")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("&eacute;", "é")
            .replace("&egrave;", "è")
            .replace("&ecirc;", "ê")
            .replace("&agrave;", "à")
            .replace("&ccedil;", "ç")
            .replace("&uuml;", "ü")
            .replace("&ouml;", "ö")
            .replace("&auml;", "ä")
            .replace("&szlig;", "ß")
            .replace("&times;", "×")
            .replace("&divide;", "÷")
            .replace("&plusmn;", "±")
            .replace("&deg;", "°")
            .replace("&middot;", "·")

        // Hexadecimal entities: &#x2014; or &#X2014; -> char
        text = text.replace(Regex("&#[xX]([0-9a-fA-F]+);")) { match ->
            try {
                val hex = match.groupValues[1]
                val code = hex.toInt(16)
                Character.toChars(code).joinToString("")
            } catch (_: Exception) {
                match.value
            }
        }

        // Decimal entities: &#8212; -> char
        text = text.replace(Regex("&#([0-9]+);")) { match ->
            try {
                val dec = match.groupValues[1]
                val code = dec.toInt(10)
                Character.toChars(code).joinToString("")
            } catch (_: Exception) {
                match.value
            }
        }

        return text
    }
}

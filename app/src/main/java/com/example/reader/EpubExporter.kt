package com.example.reader

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.SampleBooksData
import com.example.model.Book
import com.example.model.BookChapter
import com.example.model.BookFormat
import com.example.model.Highlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class EpubFontTheme(val displayName: String, val fontFamily: String, val description: String) {
    SERIF("Classic Literary", "Georgia, 'Palatino Linotype', 'Book Antiqua', Palatino, serif", "Traditional book typography with elegant serifs"),
    SANS_SERIF("Modern Clean", "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif", "Clean and readable modern sans-serif"),
    MONOSPACE("Focus Monospace", "'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace", "Minimalist typewriter aesthetic"),
    WARM_ELEGANT("Warm Warmth", "Garamond, Baskerville, 'Baskerville Old Face', 'Hoefler Text', serif", "Warm, historical publishing aesthetic")
}

data class EpubExportOptions(
    val customTitle: String? = null,
    val customAuthor: String? = null,
    val includeCoverPage: Boolean = true,
    val includeTableOfContents: Boolean = true,
    val includeHighlightsAndNotes: Boolean = true,
    val fontTheme: EpubFontTheme = EpubFontTheme.SERIF,
    val enableDropCaps: Boolean = true,
    val languageCode: String = "en"
)

data class EpubConversionResult(
    val success: Boolean,
    val file: File? = null,
    val shareableUri: Uri? = null,
    val fileSizeFormatted: String = "0 KB",
    val chapterCount: Int = 0,
    val totalWords: Int = 0,
    val bookTitle: String = "",
    val errorMessage: String? = null
)

object EpubExporter {

    suspend fun convertBookToEpub(
        context: Context,
        book: Book,
        highlights: List<Highlight> = emptyList(),
        options: EpubExportOptions = EpubExportOptions(),
        onProgress: ((Float, String) -> Unit)? = null
    ): EpubConversionResult = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(0.1f, "Extracting text and chapters...")

            val effectiveTitle = options.customTitle?.trim()?.ifBlank { null } ?: book.title
            val effectiveAuthor = options.customAuthor?.trim()?.ifBlank { null } ?: book.author

            // 1. Resolve chapters from file or sample repository
            val chapters = resolveBookChapters(book, effectiveTitle)
            if (chapters.isEmpty()) {
                return@withContext EpubConversionResult(
                    success = false,
                    errorMessage = "No readable chapters found to convert into EPUB format."
                )
            }

            onProgress?.invoke(0.35f, "Formatting typography & XHTML documents...")

            // 2. Prepare export directory and filename
            val sanitizedTitle = effectiveTitle.replace("[^a-zA-Z0-9.-]".toRegex(), "_").take(35)
            val exportDir = File(context.filesDir, "exported_epubs").apply { mkdirs() }
            val outputFile = File(exportDir, "${sanitizedTitle}_${System.currentTimeMillis()}.epub")

            val bookUuid = "urn:uuid:" + UUID.randomUUID().toString()
            val creationDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val totalWords = chapters.sumOf { it.wordCount }

            onProgress?.invoke(0.6f, "Packaging EPUB archive...")

            // 3. Create compliant EPUB zip package
            FileOutputStream(outputFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // STEP A: Write 'mimetype' as first uncompressed (STORED) entry
                    writeMimetypeEntry(zos)

                    // STEP B: Write META-INF/container.xml
                    writeZipEntry(zos, "META-INF/container.xml", generateContainerXml())

                    // STEP C: Write OEBPS/styles.css
                    writeZipEntry(zos, "OEBPS/styles.css", generateCss(options))

                    // STEP D: Write Title / Cover Page
                    if (options.includeCoverPage) {
                        writeZipEntry(
                            zos,
                            "OEBPS/titlepage.xhtml",
                            generateTitlePageXhtml(effectiveTitle, effectiveAuthor, book.genre, book.description)
                        )
                    }

                    // STEP E: Write Chapters XHTML
                    chapters.forEachIndexed { idx, chapter ->
                        val chapterFilename = "OEBPS/chapter_${idx + 1}.xhtml"
                        val chapterContent = generateChapterXhtml(
                            chapterIndex = idx + 1,
                            chapter = chapter,
                            options = options
                        )
                        writeZipEntry(zos, chapterFilename, chapterContent)
                    }

                    // STEP F: Optional Highlights & Notes Appendix
                    val hasHighlights = options.includeHighlightsAndNotes && highlights.isNotEmpty()
                    if (hasHighlights) {
                        writeZipEntry(
                            zos,
                            "OEBPS/highlights.xhtml",
                            generateHighlightsXhtml(effectiveTitle, highlights)
                        )
                    }

                    // STEP G: Write Navigation document (nav.xhtml - EPUB 3)
                    writeZipEntry(
                        zos,
                        "OEBPS/nav.xhtml",
                        generateNavXhtml(effectiveTitle, chapters, hasHighlights, options)
                    )

                    // STEP H: Write NCX Table of Contents (toc.ncx - EPUB 2 backward compatibility)
                    writeZipEntry(
                        zos,
                        "OEBPS/toc.ncx",
                        generateTocNcx(bookUuid, effectiveTitle, effectiveAuthor, chapters, hasHighlights, options)
                    )

                    // STEP I: Write OPF package manifest (OEBPS/content.opf)
                    writeZipEntry(
                        zos,
                        "OEBPS/content.opf",
                        generateContentOpf(
                            bookUuid = bookUuid,
                            title = effectiveTitle,
                            author = effectiveAuthor,
                            genre = book.genre,
                            description = book.description,
                            creationDate = creationDate,
                            chaptersCount = chapters.size,
                            hasHighlights = hasHighlights,
                            options = options
                        )
                    )
                }
            }

            onProgress?.invoke(0.9f, "Finalizing package permissions...")

            val shareableUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outputFile
                )
            } catch (e: Exception) {
                Uri.fromFile(outputFile)
            }

            val fileSizeFormatted = formatFileSize(outputFile.length())

            onProgress?.invoke(1.0f, "EPUB Conversion Complete!")

            EpubConversionResult(
                success = true,
                file = outputFile,
                shareableUri = shareableUri,
                fileSizeFormatted = fileSizeFormatted,
                chapterCount = chapters.size,
                totalWords = totalWords,
                bookTitle = effectiveTitle
            )
        } catch (e: Exception) {
            e.printStackTrace()
            EpubConversionResult(
                success = false,
                errorMessage = "Conversion failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun resolveBookChapters(book: Book, fallbackTitle: String): List<BookChapter> {
        if (book.localFilePath != null) {
            val file = File(book.localFilePath)
            if (file.exists()) {
                when (book.format) {
                    BookFormat.EPUB -> {
                        file.inputStream().use { stream ->
                            val parsed = EpubParser.parseEpubStream(stream, fallbackTitle)
                            if (parsed.chapters.isNotEmpty()) return parsed.chapters
                        }
                    }
                    BookFormat.TXT -> {
                        file.inputStream().use { stream ->
                            val parsed = EpubParser.parsePlainTextStream(stream, fallbackTitle)
                            if (parsed.chapters.isNotEmpty()) return parsed.chapters
                        }
                    }
                    BookFormat.PDF -> {
                        val extracted = PdfTextExtractor.extractChaptersFromPdf(file, fallbackTitle)
                        if (extracted.isNotEmpty()) return extracted
                    }
                }
            }
        }
        return SampleBooksData.getSampleChaptersForBook(book.id)
    }

    private fun writeMimetypeEntry(zos: ZipOutputStream) {
        val mimetypeBytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply { update(mimetypeBytes) }
        val entry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimetypeBytes.size.toLong()
            compressedSize = mimetypeBytes.size.toLong()
            this.crc = crc.value
        }
        zos.putNextEntry(entry)
        zos.write(mimetypeBytes)
        zos.closeEntry()
    }

    private fun writeZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val entry = ZipEntry(path).apply {
            method = ZipEntry.DEFLATED
        }
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun generateContainerXml(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
    <rootfiles>
        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
    </rootfiles>
</container>"""
    }

    private fun generateCss(options: EpubExportOptions): String {
        return """@charset "UTF-8";
body {
    font-family: ${options.fontTheme.fontFamily};
    line-height: 1.65;
    margin: 5% 7%;
    color: #1a1a1a;
    background-color: #fdfdfd;
    text-align: justify;
    hyphens: auto;
}

h1, h2, h3, h4 {
    font-weight: 700;
    line-height: 1.25;
    margin-top: 1.8em;
    margin-bottom: 0.6em;
    text-align: center;
    color: #111827;
}

h1.book-title {
    font-size: 2.2em;
    margin-top: 25%;
    margin-bottom: 0.2em;
    letter-spacing: -0.02em;
}

p.book-author {
    font-size: 1.25em;
    text-align: center;
    color: #4b5563;
    font-style: italic;
    margin-bottom: 2em;
}

p.book-meta {
    font-size: 0.9em;
    text-align: center;
    color: #6b7280;
}

h2.chapter-title {
    font-size: 1.5em;
    border-bottom: 1px solid #e5e7eb;
    padding-bottom: 0.4em;
    margin-top: 1.2em;
    margin-bottom: 1.2em;
}

p {
    margin-top: 0;
    margin-bottom: 1em;
    text-indent: 1.5em;
}

p.no-indent, p.first-paragraph {
    text-indent: 0;
}

span.dropcap {
    float: left;
    font-size: 3.2em;
    line-height: 0.85;
    margin-top: 0.08em;
    margin-right: 0.12em;
    font-weight: bold;
    color: #0f172a;
}

blockquote {
    margin: 1.5em 1em;
    padding: 0.8em 1.2em;
    border-left: 3px solid #6366f1;
    background-color: #f8fafc;
    font-style: italic;
    color: #334155;
}

.highlight-box {
    margin: 1em 0;
    padding: 0.9em 1.1em;
    border-radius: 6px;
    background-color: #fefce8;
    border-left: 4px solid #eab308;
}

.highlight-note {
    font-size: 0.9em;
    color: #475569;
    margin-top: 0.4em;
    font-style: normal;
}

.badge {
    display: inline-block;
    padding: 0.2em 0.6em;
    border-radius: 4px;
    font-size: 0.75em;
    background-color: #e0e7ff;
    color: #3730a3;
    font-weight: 600;
}

hr.divider {
    border: none;
    border-top: 1px solid #e2e8f0;
    margin: 2em auto;
    width: 40%;
}
"""
    }

    private fun generateTitlePageXhtml(title: String, author: String, genre: String, description: String): String {
        val escapedTitle = escapeXml(title)
        val escapedAuthor = escapeXml(author)
        val escapedGenre = escapeXml(genre)
        val escapedDesc = escapeXml(description)

        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">
<head>
    <title>$escapedTitle</title>
    <link rel="stylesheet" type="text/css" href="styles.css"/>
</head>
<body epub:type="cover titlepage">
    <div style="text-align: center; margin-top: 15%;">
        <div class="badge">$escapedGenre</div>
        <h1 class="book-title">$escapedTitle</h1>
        <p class="book-author">by $escapedAuthor</p>
        <hr class="divider"/>
        <p class="book-meta">$escapedDesc</p>
        <div style="margin-top: 25%;">
            <p class="book-meta" style="font-size: 0.8em;">Exported from <strong>A-Hex Reader</strong></p>
        </div>
    </div>
</body>
</html>"""
    }

    private fun generateChapterXhtml(chapterIndex: Int, chapter: BookChapter, options: EpubExportOptions): String {
        val escapedTitle = escapeXml(chapter.title.ifBlank { "Chapter $chapterIndex" })
        val paragraphs = chapter.content
            .split("\n\n", "\r\n\r\n", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val paragraphsHtml = StringBuilder()
        paragraphs.forEachIndexed { pIdx, paragraph ->
            val escapedText = escapeXml(paragraph)
            if (pIdx == 0 && options.enableDropCaps && escapedText.length > 1 && escapedText.first().isLetter()) {
                val firstChar = escapedText.first()
                val rest = escapedText.substring(1)
                paragraphsHtml.append("""    <p class="first-paragraph"><span class="dropcap">$firstChar</span>$rest</p>""" + "\n")
            } else {
                paragraphsHtml.append("    <p>$escapedText</p>\n")
            }
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="${options.languageCode}">
<head>
    <title>$escapedTitle</title>
    <link rel="stylesheet" type="text/css" href="styles.css"/>
</head>
<body epub:type="bodymatter chapter">
    <section>
        <h2 class="chapter-title">$escapedTitle</h2>
$paragraphsHtml
    </section>
</body>
</html>"""
    }

    private fun generateHighlightsXhtml(bookTitle: String, highlights: List<Highlight>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">
<head>
    <title>Highlights &amp; Annotations</title>
    <link rel="stylesheet" type="text/css" href="styles.css"/>
</head>
<body epub:type="backmatter appendix">
    <section>
        <h2 class="chapter-title">Reading Notes &amp; Highlights</h2>
        <p class="no-indent">Annotated insights collected while reading <em>${escapeXml(bookTitle)}</em>:</p>
""")

        highlights.forEach { hl ->
            val colorHex = when (hl.color.name) {
                "YELLOW" -> "#fef08a"
                "GREEN" -> "#bbf7d0"
                "BLUE" -> "#bfdbfe"
                "PURPLE" -> "#e9d5ff"
                "ORANGE" -> "#fed7aa"
                "PINK" -> "#fbcfe8"
                else -> "#fef08a"
            }
            sb.append("""
        <div class="highlight-box" style="background-color: $colorHex; border-left: 4px solid #3b82f6;">
            <p class="no-indent" style="font-weight: 500;">"${escapeXml(hl.text)}"</p>
""")
            if (!hl.note.isNullOrBlank()) {
                sb.append("""            <p class="highlight-note"><strong>Note:</strong> ${escapeXml(hl.note)}</p>""" + "\n")
            }
            sb.append("""            <p class="book-meta" style="text-align: left; font-size: 0.75em; margin-bottom: 0;">Chapter: ${escapeXml(hl.chapterTitle)} | Color: ${hl.color.displayName}</p>
        </div>
""")
        }

        sb.append("""
    </section>
</body>
</html>""")
        return sb.toString()
    }

    private fun generateNavXhtml(
        title: String,
        chapters: List<BookChapter>,
        hasHighlights: Boolean,
        options: EpubExportOptions
    ): String {
        val navList = StringBuilder()
        if (options.includeCoverPage) {
            navList.append("""            <li><a href="titlepage.xhtml">Title Page</a></li>""" + "\n")
        }
        chapters.forEachIndexed { idx, ch ->
            val chapterTitle = escapeXml(ch.title.ifBlank { "Chapter ${idx + 1}" })
            navList.append("""            <li><a href="chapter_${idx + 1}.xhtml">$chapterTitle</a></li>""" + "\n")
        }
        if (hasHighlights) {
            navList.append("""            <li><a href="highlights.xhtml">Reading Highlights &amp; Notes</a></li>""" + "\n")
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="${options.languageCode}">
<head>
    <title>Table of Contents</title>
    <link rel="stylesheet" type="text/css" href="styles.css"/>
</head>
<body>
    <nav epub:type="toc" id="toc">
        <h2 class="chapter-title">Table of Contents</h2>
        <ol>
$navList
        </ol>
    </nav>
</body>
</html>"""
    }

    private fun generateTocNcx(
        bookUuid: String,
        title: String,
        author: String,
        chapters: List<BookChapter>,
        hasHighlights: Boolean,
        options: EpubExportOptions
    ): String {
        var playOrder = 1
        val navPoints = StringBuilder()

        if (options.includeCoverPage) {
            navPoints.append("""    <navPoint id="navPoint-$playOrder" playOrder="$playOrder">
        <navLabel><text>Title Page</text></navLabel>
        <content src="titlepage.xhtml"/>
    </navPoint>""" + "\n")
            playOrder++
        }

        chapters.forEachIndexed { idx, ch ->
            val chapterTitle = escapeXml(ch.title.ifBlank { "Chapter ${idx + 1}" })
            navPoints.append("""    <navPoint id="navPoint-$playOrder" playOrder="$playOrder">
        <navLabel><text>$chapterTitle</text></navLabel>
        <content src="chapter_${idx + 1}.xhtml"/>
    </navPoint>""" + "\n")
            playOrder++
        }

        if (hasHighlights) {
            navPoints.append("""    <navPoint id="navPoint-$playOrder" playOrder="$playOrder">
        <navLabel><text>Reading Highlights &amp; Notes</text></navLabel>
        <content src="highlights.xhtml"/>
    </navPoint>""" + "\n")
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
    <head>
        <meta name="dtb:uid" content="$bookUuid"/>
        <meta name="dtb:depth" content="1"/>
        <meta name="dtb:totalPageCount" content="0"/>
        <meta name="dtb:maxPageNumber" content="0"/>
    </head>
    <docTitle>
        <text>${escapeXml(title)}</text>
    </docTitle>
    <docAuthor>
        <text>${escapeXml(author)}</text>
    </docAuthor>
    <navMap>
$navPoints
    </navMap>
</ncx>"""
    }

    private fun generateContentOpf(
        bookUuid: String,
        title: String,
        author: String,
        genre: String,
        description: String,
        creationDate: String,
        chaptersCount: Int,
        hasHighlights: Boolean,
        options: EpubExportOptions
    ): String {
        val manifestItems = StringBuilder()
        val spineItems = StringBuilder()

        manifestItems.append("""        <item id="css" href="styles.css" media-type="text/css"/>""" + "\n")
        manifestItems.append("""        <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>""" + "\n")
        manifestItems.append("""        <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>""" + "\n")

        if (options.includeCoverPage) {
            manifestItems.append("""        <item id="titlepage" href="titlepage.xhtml" media-type="application/xhtml+xml"/>""" + "\n")
            spineItems.append("""        <itemref idref="titlepage"/>""" + "\n")
        }

        for (i in 1..chaptersCount) {
            manifestItems.append("""        <item id="chapter_$i" href="chapter_$i.xhtml" media-type="application/xhtml+xml"/>""" + "\n")
            spineItems.append("""        <itemref idref="chapter_$i"/>""" + "\n")
        }

        if (hasHighlights) {
            manifestItems.append("""        <item id="highlights" href="highlights.xhtml" media-type="application/xhtml+xml"/>""" + "\n")
            spineItems.append("""        <itemref idref="highlights"/>""" + "\n")
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="3.0" xml:lang="${options.languageCode}">
    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
        <dc:identifier id="BookId">$bookUuid</dc:identifier>
        <dc:title>${escapeXml(title)}</dc:title>
        <dc:creator id="creator">${escapeXml(author)}</dc:creator>
        <dc:language>${options.languageCode}</dc:language>
        <dc:date>$creationDate</dc:date>
        <dc:subject>${escapeXml(genre)}</dc:subject>
        <dc:description>${escapeXml(description)}</dc:description>
        <dc:publisher>A-Hex E-Reader Suite</dc:publisher>
        <meta property="dcterms:modified">${creationDate}T00:00:00Z</meta>
    </metadata>
    <manifest>
$manifestItems
    </manifest>
    <spine toc="ncx">
$spineItems
    </spine>
    <guide>
        <reference type="toc" title="Table of Contents" href="nav.xhtml"/>
        ${if (options.includeCoverPage) """<reference type="cover" title="Cover" href="titlepage.xhtml"/>""" else ""}
    </guide>
</package>"""
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${(bytes / 1024.0 * 10).toInt() / 10.0} KB"
            else -> "${(bytes / (1024.0 * 1024.0) * 10).toInt() / 10.0} MB"
        }
    }
}

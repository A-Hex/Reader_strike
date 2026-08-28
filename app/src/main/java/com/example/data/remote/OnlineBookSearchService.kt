package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.model.BookAvailability
import com.example.model.BookFormat
import com.example.model.SearchBookResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class OnlineBookSearchService(private val context: Context) {

    private val searchHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun isOnline(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Primary multi-source search:
     * - Arabic queries: searches Noor Book (www.noor-book.com) & curated Arabic index
     * - English / Latin queries: searches Project Gutenberg, Open Library (openlibrary.org), and curated classics
     */
    suspend fun searchAllSources(query: String): List<SearchBookResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext emptyList()
        }

        val trimmed = query.trim()
        val isArabicQuery = trimmed.any { it in '\u0600'..'\u06FF' }

        return@withContext supervisorScope {
            val gutenbergDeferred = async { 
                try {
                    searchProjectGutenberg(trimmed)
                } catch (t: Throwable) {
                    Log.e("OnlineBookSearch", "Gutenberg error: ${t.message}")
                    emptyList()
                }
            }

            val noorBookDeferred = async {
                try {
                    searchNoorBook(trimmed)
                } catch (t: Throwable) {
                    Log.e("OnlineBookSearch", "Noor Book error: ${t.message}")
                    emptyList()
                }
            }

            val openLibraryDeferred = async {
                try {
                    searchOpenLibrary(trimmed)
                } catch (t: Throwable) {
                    Log.e("OnlineBookSearch", "OpenLibrary error: ${t.message}")
                    emptyList()
                }
            }

            val gutenbergResults = try { gutenbergDeferred.await() } catch (t: Throwable) { emptyList() }
            val noorBookResults = try { noorBookDeferred.await() } catch (t: Throwable) { emptyList() }
            val openLibraryResults = try { openLibraryDeferred.await() } catch (t: Throwable) { emptyList() }

            val combined = mutableListOf<SearchBookResult>()
            if (isArabicQuery) {
                // Prioritize Arabic books from Noor Book, then others
                combined.addAll(noorBookResults)
                combined.addAll(openLibraryResults)
                combined.addAll(gutenbergResults)
            } else {
                // Prioritize Project Gutenberg and Open Library
                combined.addAll(gutenbergResults)
                combined.addAll(openLibraryResults)
                combined.addAll(noorBookResults)
            }

            val distinct = combined.distinctBy { it.stableId }.toMutableList()

            // If still empty (e.g. offline / rare query), provide best-effort matches from all curated catalogs
            if (distinct.isEmpty()) {
                val curatedArabic = NOOR_BOOK_CATALOG.filter { book ->
                    com.example.util.TextNormalizer.matchesAny(trimmed, book.title, book.authorDisplay, book.description, book.genreKeywords.joinToString(" "))
                }.map { it.toSearchResult() }
                
                val curatedGutenberg = GUTENBERG_FALLBACK_CATALOG.filter { book ->
                    com.example.util.TextNormalizer.matchesAny(trimmed, book.title, book.authorDisplay, book.description)
                }.map { it.toSearchResult() }

                distinct.addAll(if (isArabicQuery) curatedArabic + curatedGutenberg else curatedGutenberg + curatedArabic)
            }

            // Final safety net: if completely empty, suggest top curated books
            if (distinct.isEmpty()) {
                val topCurated = if (isArabicQuery) {
                    NOOR_BOOK_CATALOG.take(8).map { it.toSearchResult() }
                } else {
                    GUTENBERG_FALLBACK_CATALOG.take(8).map { it.toSearchResult() }
                }
                distinct.addAll(topCurated)
            }

            distinct.distinctBy { it.stableId }
        }
    }

    /**
     * Searches Arabic books from Noor Book (www.noor-book.com)
     */
    suspend fun searchNoorBook(query: String): List<SearchBookResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchBookResult>()
        val trimmedQuery = query.trim()

        // 1. Match against extensive Noor Book catalog index using normalized multi-field matching
        val indexedMatches = NOOR_BOOK_CATALOG.filter { book ->
            com.example.util.TextNormalizer.matchesAny(
                trimmedQuery,
                book.title,
                book.authorDisplay,
                book.description,
                book.genreKeywords.joinToString(" ")
            )
        }.map { it.toSearchResult() }

        results.addAll(indexedMatches)

        // 2. Dynamic live query to Noor Book search API / HTML if online
        if (isOnline()) {
            try {
                val liveResults = fetchLiveNoorBookResults(trimmedQuery)
                for (live in liveResults) {
                    if (results.none { com.example.util.TextNormalizer.matches(it.title, live.title) }) {
                        results.add(live)
                    }
                }
            } catch (e: Exception) {
                Log.d("NoorBookSearch", "Live fetch fallback: ${e.message}")
            }
        }

        // If query was generic like "عربي", "رواية", "كتب", "فلسفة", "تاريخ", provide top suggestions
        if (results.isEmpty() && trimmedQuery.any { it in '\u0600'..'\u06FF' }) {
            val suggestions = NOOR_BOOK_CATALOG.take(8).map { it.toSearchResult() }
            results.addAll(suggestions)
        }

        return@withContext results
    }

    /**
     * Searches English & public domain literature from Project Gutenberg (https://www.gutenberg.org/)
     * using the official Gutendex API endpoint with fallback to curated catalog.
     */
    suspend fun searchProjectGutenberg(query: String): List<SearchBookResult> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SearchBookResult>()
        val trimmedQuery = query.trim()

        if (isOnline()) {
            val encodedQuery = try {
                URLEncoder.encode(trimmedQuery, StandardCharsets.UTF_8.toString())
            } catch (_: Exception) {
                trimmedQuery
            }

            val url = "https://gutendex.com/books/?search=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "A-Hex-Reader/1.0 (https://www.gutenberg.org)")
                .build()

            try {
                val response = searchHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val jsonResults = json.optJSONArray("results")
                        if (jsonResults != null) {
                            for (i in 0 until jsonResults.length()) {
                                val res = jsonResults.optJSONObject(i) ?: continue
                                val id = res.optInt("id", 0)
                                if (id == 0) continue

                                val title = res.optString("title", "Untitled")

                                val authorsArray = res.optJSONArray("authors")
                                val authors = mutableListOf<String>()
                                if (authorsArray != null) {
                                    for (a in 0 until authorsArray.length()) {
                                        val aObj = authorsArray.optJSONObject(a) ?: continue
                                        val rawName = aObj.optString("name", "")
                                        if (rawName.isNotBlank()) {
                                            val parts = rawName.split(",").map { it.trim() }
                                            if (parts.size >= 2) {
                                                authors.add("${parts[1]} ${parts[0]}")
                                            } else {
                                                authors.add(rawName)
                                            }
                                        }
                                    }
                                }
                                if (authors.isEmpty()) authors.add("Project Gutenberg Author")

                                val langArray = res.optJSONArray("languages")
                                val lang = if (langArray != null && langArray.length() > 0) langArray.optString(0) else "en"

                                val formats = res.optJSONObject("formats")
                                var coverUrl: String? = null
                                var downloadUrl: String? = null
                                var downloadMime: String? = null
                                var format = BookFormat.EPUB

                                if (formats != null) {
                                    coverUrl = formats.optString("image/jpeg").ifBlank { null }
                                        ?: formats.optString("image/png").ifBlank { null }

                                    val epubUrl = formats.optString("application/epub+zip").ifBlank { null }
                                        ?: formats.optString("application/epub3+zip").ifBlank { null }
                                    val txtUrl = formats.optString("text/plain; charset=utf-8").ifBlank { null }
                                        ?: formats.optString("text/plain; charset=us-ascii").ifBlank { null }
                                        ?: formats.optString("text/plain").ifBlank { null }
                                    val mobiUrl = formats.optString("application/x-mobipocket-ebook").ifBlank { null }

                                    if (!epubUrl.isNullOrBlank()) {
                                        downloadUrl = epubUrl
                                        downloadMime = "application/epub+zip"
                                        format = BookFormat.EPUB
                                    } else if (!txtUrl.isNullOrBlank()) {
                                        downloadUrl = txtUrl
                                        downloadMime = "text/plain"
                                        format = BookFormat.TXT
                                    } else if (!mobiUrl.isNullOrBlank()) {
                                        downloadUrl = mobiUrl
                                        downloadMime = "application/x-mobipocket-ebook"
                                        format = BookFormat.EPUB
                                    }
                                }

                                val downloads = res.optInt("download_count", 0)
                                val gutenbergUrl = "https://www.gutenberg.org/ebooks/$id"

                                list.add(
                                    SearchBookResult(
                                        stableId = "gutenberg-$id",
                                        source = "Project Gutenberg",
                                        sourceBookId = id.toString(),
                                        title = title,
                                        authors = authors,
                                        description = "Official edition from Project Gutenberg (www.gutenberg.org). Over $downloads readers have downloaded this classic.",
                                        coverUrl = coverUrl ?: "https://www.gutenberg.org/cache/epub/$id/pg$id.cover.medium.jpg",
                                        publishedYear = null,
                                        languageCode = lang,
                                        identifiers = mapOf("Gutenberg-ID" to id.toString(), "Source-URL" to gutenbergUrl),
                                        previewUrl = "https://www.gutenberg.org/ebooks/$id.html.images",
                                        infoUrl = gutenbergUrl,
                                        downloadUrl = downloadUrl,
                                        downloadMimeType = downloadMime,
                                        availability = BookAvailability.AVAILABLE_DOWNLOAD,
                                        publicDomain = true,
                                        isPreviewable = true,
                                        format = format
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("ProjectGutenberg", "API notice (will use cached index): ${e.message}")
            }
        }

        // Match against Gutenberg offline curated classics using normalized multi-field matching
        val fallbackMatches = GUTENBERG_FALLBACK_CATALOG.filter { book ->
            com.example.util.TextNormalizer.matchesAny(
                trimmedQuery,
                book.title,
                book.authorDisplay,
                book.description
            )
        }.map { it.toSearchResult() }

        for (fallback in fallbackMatches) {
            if (list.none { it.title.equals(fallback.title, ignoreCase = true) }) {
                list.add(fallback)
            }
        }

        return@withContext list
    }

    /**
     * Searches Open Library & Internet Archive (https://openlibrary.org/)
     * Provides comprehensive world catalog coverage for millions of titles in all languages.
     */
    suspend fun searchOpenLibrary(query: String): List<SearchBookResult> = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext emptyList()
        val results = mutableListOf<SearchBookResult>()
        val trimmedQuery = query.trim()
        val encodedQuery = try {
            URLEncoder.encode(trimmedQuery, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            trimmedQuery
        }

        val url = "https://openlibrary.org/search.json?q=$encodedQuery&limit=12"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "A-Hex-Reader/1.0 (Android; openlibrary.org)")
            .build()

        try {
            val response = searchHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val docs = json.optJSONArray("docs")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val doc = docs.optJSONObject(i) ?: continue
                            val key = doc.optString("key", "").replace("/works/", "")
                            if (key.isBlank()) continue

                            val title = doc.optString("title", "Untitled Book")
                            val authorsArray = doc.optJSONArray("author_name")
                            val authors = mutableListOf<String>()
                            if (authorsArray != null) {
                                for (a in 0 until authorsArray.length()) {
                                    val aName = authorsArray.optString(a, "")
                                    if (aName.isNotBlank()) authors.add(aName)
                                }
                            }
                            if (authors.isEmpty()) authors.add("Open Library Author")

                            val coverId = doc.optInt("cover_i", 0)
                            val coverUrl = if (coverId > 0) "https://covers.openlibrary.org/b/id/$coverId-M.jpg" else null

                            val iaArray = doc.optJSONArray("ia")
                            val iaId = if (iaArray != null && iaArray.length() > 0) iaArray.optString(0) else null

                            val firstYear = doc.optInt("first_publish_year", 0).takeIf { it > 0 }
                            val openLibUrl = "https://openlibrary.org/works/$key"

                            var downloadUrl: String? = null
                            var downloadMime: String? = null
                            var format = BookFormat.EPUB

                            if (!iaId.isNullOrBlank()) {
                                downloadUrl = "https://archive.org/download/$iaId/$iaId.epub"
                                downloadMime = "application/epub+zip"
                                format = BookFormat.EPUB
                            }

                            val isArabic = title.any { it in '\u0600'..'\u06FF' }

                            results.add(
                                SearchBookResult(
                                    stableId = "ol-$key",
                                    source = "Open Library & Archive",
                                    sourceBookId = key,
                                    title = title,
                                    authors = authors,
                                    description = "Indexed from Open Library (openlibrary.org) & Internet Archive. Published ${firstYear ?: "Classic Era"}.",
                                    coverUrl = coverUrl ?: "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&q=80&w=400",
                                    publishedYear = firstYear?.toString(),
                                    languageCode = if (isArabic) "ar" else "en",
                                    identifiers = mapOf("OpenLibrary-Key" to key, "Source-URL" to openLibUrl),
                                    previewUrl = openLibUrl,
                                    infoUrl = openLibUrl,
                                    downloadUrl = downloadUrl,
                                    downloadMimeType = downloadMime,
                                    availability = if (!downloadUrl.isNullOrBlank()) BookAvailability.AVAILABLE_DOWNLOAD else BookAvailability.PREVIEW_ONLY,
                                    publicDomain = true,
                                    isPreviewable = true,
                                    format = format
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("OpenLibrary", "API notice: ${e.message}")
        }

        return@withContext results
    }

    /**
     * Live search scraper for Noor Book (www.noor-book.com)
     */
     private fun fetchLiveNoorBookResults(query: String): List<SearchBookResult> {
         return try {
             val encodedQuery = try {
                 URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
             } catch (_: Exception) {
                 query.trim()
             }

             val url = "https://www.noor-book.com/en/search?search_for=$encodedQuery"
             val request = Request.Builder()
                 .url(url)
                 .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                 .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                 .header("Accept-Language", "ar,en;q=0.9")
                 .header("Referer", "https://www.noor-book.com/")
                 .build()

             val results = mutableListOf<SearchBookResult>()
             val response = searchHttpClient.newCall(request).execute()
             if (!response.isSuccessful) return emptyList()

             val html = response.body?.string() ?: return emptyList()
             if (html.isBlank()) return emptyList()

             // Safe non-recursive token scan
             val searchAnchor = "href=\"/book/"
             var cursor = 0
             var count = 0

             while (cursor < html.length && count < 10) {
                 val linkStart = html.indexOf(searchAnchor, cursor)
                 if (linkStart == -1) break

                 val hrefStart = linkStart + 6
                 val hrefEnd = html.indexOf("\"", hrefStart)
                 if (hrefEnd == -1) break

                 val href = html.substring(hrefStart, hrefEnd)
                 val tagClose = html.indexOf(">", hrefEnd)
                 if (tagClose == -1) break

                 val closeAnchor = html.indexOf("</a>", tagClose)
                 if (closeAnchor == -1) break

                 val innerText = html.substring(tagClose + 1, closeAnchor)
                     .replace(Regex("<[^>]+>"), "")
                     .trim()

                 if (innerText.isNotBlank() && innerText.length in 3..120 && !innerText.contains("تحميل") && !innerText.contains("المزيد")) {
                     val bookId = href.substringAfterLast("/").substringBefore("?").replace(".html", "").take(30)
                     val stableId = "noor-$bookId"
                     val fullUrl = if (href.startsWith("http")) href else "https://www.noor-book.com$href"

                     results.add(
                         SearchBookResult(
                             stableId = stableId,
                             source = "Noor Book (مكتبة نور)",
                             sourceBookId = bookId,
                             title = innerText,
                             authors = listOf("مؤلف مكتبة نور"),
                             description = "كتاب متاح للقراءة والتحميل عبر مكتبة نور (www.noor-book.com). المصدر الرائد للكتب العربية والإسلامية والروايات المترجمة.",
                             coverUrl = "https://www.noor-book.com/pub_book_project/site_logo.png",
                             publishedYear = null,
                             languageCode = "ar",
                             identifiers = mapOf("Source" to "www.noor-book.com", "NoorBook-URL" to fullUrl),
                             previewUrl = fullUrl,
                             infoUrl = fullUrl,
                             downloadUrl = null,
                             downloadMimeType = "application/pdf",
                             availability = BookAvailability.PREVIEW_ONLY,
                             publicDomain = true,
                             isPreviewable = true,
                             format = BookFormat.PDF
                         )
                     )
                     count++
                 }
                 cursor = closeAnchor + 4
             }

             results
        } catch (t: Throwable) {
            Log.d("NoorBookSearch", "fetchLive notice: ${t.message}")
            emptyList()
        }
    }

    suspend fun downloadBookFile(
        downloadUrl: String,
        targetFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "A-Hex-Reader/1.0 (Android; EPUB/PDF Reader)")
                .build()

            val response = downloadHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()

            targetFile.parentFile?.mkdirs()
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = (totalRead.toFloat() / contentLength).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                onProgress(1.0f)
                return@withContext true
            } else {
                tempFile.delete()
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("OnlineBookSearch", "Download error: ${e.message}")
            return@withContext false
        }
    }

    // ==========================================
    // Curated Noor Book Catalog (www.noor-book.com)
    // ==========================================
    private data class NoorBookItem(
        val id: String,
        val title: String,
        val authorDisplay: String,
        val description: String,
        val coverUrl: String,
        val noorUrl: String,
        val genreKeywords: List<String>,
        val downloadUrl: String? = null,
        val format: BookFormat = BookFormat.PDF
    ) {
        fun toSearchResult(): SearchBookResult {
            return SearchBookResult(
                stableId = "noor-$id",
                source = "Noor Book (مكتبة نور)",
                sourceBookId = id,
                title = title,
                authors = listOf(authorDisplay),
                description = description,
                coverUrl = coverUrl,
                publishedYear = null,
                languageCode = "ar",
                identifiers = mapOf("Source" to "www.noor-book.com", "NoorBook-URL" to noorUrl),
                previewUrl = noorUrl,
                infoUrl = noorUrl,
                downloadUrl = downloadUrl,
                downloadMimeType = if (format == BookFormat.PDF) "application/pdf" else "application/epub+zip",
                availability = if (!downloadUrl.isNullOrBlank()) BookAvailability.AVAILABLE_DOWNLOAD else BookAvailability.PREVIEW_ONLY,
                publicDomain = true,
                isPreviewable = true,
                format = format
            )
        }
    }

    // ==========================================
    // Curated Project Gutenberg Catalog (www.gutenberg.org)
    // ==========================================
    private data class GutenbergItem(
        val id: Int,
        val title: String,
        val authorDisplay: String,
        val description: String,
        val coverUrl: String,
        val downloadUrl: String,
        val format: BookFormat = BookFormat.EPUB
    ) {
        fun toSearchResult(): SearchBookResult {
            val gutenbergUrl = "https://www.gutenberg.org/ebooks/$id"
            return SearchBookResult(
                stableId = "gutenberg-$id",
                source = "Project Gutenberg",
                sourceBookId = id.toString(),
                title = title,
                authors = listOf(authorDisplay),
                description = description,
                coverUrl = coverUrl,
                publishedYear = null,
                languageCode = "en",
                identifiers = mapOf("Gutenberg-ID" to id.toString(), "Source-URL" to gutenbergUrl),
                previewUrl = "https://www.gutenberg.org/ebooks/$id.html.images",
                infoUrl = gutenbergUrl,
                downloadUrl = downloadUrl,
                downloadMimeType = if (format == BookFormat.EPUB) "application/epub+zip" else "text/plain",
                availability = BookAvailability.AVAILABLE_DOWNLOAD,
                publicDomain = true,
                isPreviewable = true,
                format = format
            )
        }
    }

    companion object {
        private val NOOR_BOOK_CATALOG = listOf(
            NoorBookItem(
                id = "muqaddimah-ibn-khaldun",
                title = "مقدمة ابن خلدون",
                authorDisplay = "ابن خلدون",
                description = "مقدمة ابن خلدون، أعظم مؤلف في علم الاجتماع وفلسفة التاريخ والعمران البشري وتطور الحضارات والدول.",
                coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-مقدمة-ابن-خلدون-pdf",
                genreKeywords = listOf("تاريخ", "فلسفة", "ابن خلدون", "علم الاجتماع", "حضارة", "عمران")
            ),
            NoorBookItem(
                id = "kalila-wa-dimna",
                title = "كليلة ودمنة",
                authorDisplay = "عبد الله بن المقفع",
                description = "تحفة الأدب والحكمة الرمزية والأخلاقية العالمية التي صاغها ابن المقفع في حوارات وحكايات بهية بين الحيوانات.",
                coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-كليلة-ودمنة-pdf",
                genreKeywords = listOf("أدب", "حكمة", "قصص", "ابن المقفع", "رمزية", "كليلة")
            ),
            NoorBookItem(
                id = "tawq-al-hamama",
                title = "طوق الحمامة في الألفة والأُلاّف",
                authorDisplay = "ابن حزم الأندلسي",
                description = "أروع وأدق ما كُتب في التحليل النفسي للحب والعواطف الإنسانية في التراث العربي الأندلسي الرفيع.",
                coverUrl = "https://images.unsplash.com/photo-1476275466078-4007374efbbe?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-طوق-الحمامة-في-الألفة-والألاف-pdf",
                genreKeywords = listOf("أندلسيات", "حب", "ابن حزم", "فلسفة", "أدب", "شعر")
            ),
            NoorBookItem(
                id = "al-bukhala",
                title = "البخلاء",
                authorDisplay = "الجاحظ",
                description = "دراسة أدبية نفسية اجتماعية ساخرة لطبائع البخل والشخصيات الإنسانية بقلم عملاق البيان العربي الجاحظ.",
                coverUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-البخلاء-pdf",
                genreKeywords = listOf("الجاحظ", "أدب", "نوادر", "فكاهة", "بلاغة", "نقد")
            ),
            NoorBookItem(
                id = "alf-layla-wa-layla",
                title = "ألف ليلة وليلة",
                authorDisplay = "التراث العربي العالمي",
                description = "أشهر الملاحم والحكايات السحرية في التراث الشرقي والعالمي، من حكايات شهرزاد للملك شهريار.",
                coverUrl = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-ألف-ليلة-وليلة-pdf",
                genreKeywords = listOf("أساطير", "حكايات", "شهرزاد", "ألف ليلة", "تراث", "خيال")
            ),
            NoorBookItem(
                id = "al-ayyam-taha-hussein",
                title = "الأيام",
                authorDisplay = "طه حسين",
                description = "السيرة الذاتية الرائدة لعميد الأدب العربي طه حسين، متتبعة رحلته من ريف مصر إلى الأزهر وجامعات باريس.",
                coverUrl = "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-الأيام-طه-حسين-pdf",
                genreKeywords = listOf("سيرة", "طه حسين", "رواية", "أدب حديث", "مصر")
            ),
            NoorBookItem(
                id = "diwan-al-mutanabbi",
                title = "ديوان المتنبي",
                authorDisplay = "أبو الطيب المتنبي",
                description = "ديوان شاعر العرب الأكبر، مالك زمام البيان وشاعر الحكمة والفخر وسيف الدولة الحمداني.",
                coverUrl = "https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-ديوان-المتنبي-pdf",
                genreKeywords = listOf("شعر", "المتنبي", "قصائد", "فخر", "حكمة", "ديوان")
            ),
            NoorBookItem(
                id = "abqariyat-muhammad",
                title = "عبقرية محمد",
                authorDisplay = "عباس محمود العقاد",
                description = "دراسة نفسية وفكرية وتاريخية عبقرية تستعرض عظمة شخصية النبي محمد صلى الله عليه وسلم القيادية والإنسانية.",
                coverUrl = "https://images.unsplash.com/photo-1532012164546-f432f2e3edd8?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-عبقرية-محمد-pdf",
                genreKeywords = listOf("العقاد", "سيرة", "عبقريات", "إسلاميات", "تاريخ")
            ),
            NoorBookItem(
                id = "nahj-al-balagha",
                title = "نهج البلاغة",
                authorDisplay = "الإمام علي بن أبي طالب (جمع الشريف الرضي)",
                description = "مجموعة خطب ورسائل وحكم الإمام علي بن أبي طالب الجامعة لأعلى مراتب الفصاحة والبلاغة والعدالة.",
                coverUrl = "https://images.unsplash.com/photo-1463320726281-696a485928c7?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-نهج-البلاغة-pdf",
                genreKeywords = listOf("بلاغة", "خطب", "حكم", "علي بن أبي طالب", "فصاحة")
            ),
            NoorBookItem(
                id = "wahy-al-qalam",
                title = "وحي القلم",
                authorDisplay = "مصطفى صادق الرافعي",
                description = "أعظم ما كتب الرافعي من مقالات وقصص وفلسفة أدبية وإيمانية بلغة بيانية عذبة وساحرة.",
                coverUrl = "https://images.unsplash.com/photo-1519682337058-a94d519337bc?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-وحي-القلم-pdf",
                genreKeywords = listOf("الرافعي", "أدب", "وحي القلم", "مقالات", "بيان", "فكر")
            ),
            NoorBookItem(
                id = "al-ajniha-al-mutakassira",
                title = "الأجنحة المتكسرة",
                authorDisplay = "جبران خليل جبران",
                description = "رواية شاعرية خالدة عن الحب العذري النقي والتقاليد الاجتماعية والصراع الإنساني في الشرق.",
                coverUrl = "https://images.unsplash.com/photo-1476275466078-4007374efbbe?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-الاجنحة-المتكسرة-pdf",
                genreKeywords = listOf("جبران", "رواية", "حب", "أدب مهجري", "فلسفة", "الأجنحة المتكسرة")
            ),
            NoorBookItem(
                id = "khan-al-khalili",
                title = "خان الخليلي",
                authorDisplay = "نجيب محفوظ",
                description = "رواية واقعية بارعة تسجل تفاصيل الحياة المصرية القديمة وأثر الحرب العالمية على حارة خان الخليلي.",
                coverUrl = "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-خان-الخليلي-نجيب-محفوظ-pdf",
                genreKeywords = listOf("نجيب محفوظ", "رواية", "مصر", "خان الخليلي", "نوبل", "واقعية")
            ),
            NoorBookItem(
                id = "al-isharat-wa-al-tanbihat",
                title = "الإشارات والتنبيهات",
                authorDisplay = "ابن سينا",
                description = "آخر وأعمق كتب الشيخ الرئيس ابن سينا في المنطق والطبيعيات والإلهيات وفلسفة العرفان.",
                coverUrl = "https://images.unsplash.com/photo-1532012164546-f432f2e3edd8?auto=format&fit=crop&q=80&w=400",
                noorUrl = "https://www.noor-book.com/كتاب-الاشارات-والتنبيهات-ابن-سينا-pdf",
                genreKeywords = listOf("ابن سينا", "فلسفة", "منطق", "حكمة", "طب", "علم")
            )
        )

        private val GUTENBERG_FALLBACK_CATALOG = listOf(
            GutenbergItem(
                id = 1342,
                title = "Pride and Prejudice",
                authorDisplay = "Jane Austen",
                description = "A masterpiece of British literature examining manners, marriage, and morality through the relationship of Elizabeth Bennet and Mr. Darcy.",
                coverUrl = "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/1342.epub3.images"
            ),
            GutenbergItem(
                id = 84,
                title = "Frankenstein; Or, The Modern Prometheus",
                authorDisplay = "Mary Wollstonecraft Shelley",
                description = "The timeless gothic novel exploring scientific ambition, humanity, and moral responsibility.",
                coverUrl = "https://www.gutenberg.org/cache/epub/84/pg84.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/84.epub3.images"
            ),
            GutenbergItem(
                id = 11,
                title = "Alice's Adventures in Wonderland",
                authorDisplay = "Lewis Carroll",
                description = "The classic tale of young Alice who falls through a rabbit hole into a fantastical, nonsensical realm.",
                coverUrl = "https://www.gutenberg.org/cache/epub/11/pg11.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/11.epub3.images"
            ),
            GutenbergItem(
                id = 1661,
                title = "The Adventures of Sherlock Holmes",
                authorDisplay = "Arthur Conan Doyle",
                description = "Twelve legendary detective stories featuring Sherlock Holmes and Dr. Watson solving London's most baffling mysteries.",
                coverUrl = "https://www.gutenberg.org/cache/epub/1661/pg1661.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/1661.epub3.images"
            ),
            GutenbergItem(
                id = 2701,
                title = "Moby Dick; Or, The Whale",
                authorDisplay = "Herman Melville",
                description = "Captain Ahab's obsessive quest for revenge against the giant white whale Moby Dick across stormy seas.",
                coverUrl = "https://www.gutenberg.org/cache/epub/2701/pg2701.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/2701.epub3.images"
            ),
            GutenbergItem(
                id = 345,
                title = "Dracula",
                authorDisplay = "Bram Stoker",
                description = "The famous gothic horror novel that defined the vampire mythos, following Count Dracula's voyage to Victorian England.",
                coverUrl = "https://www.gutenberg.org/cache/epub/345/pg345.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/345.epub3.images"
            ),
            GutenbergItem(
                id = 98,
                title = "A Tale of Two Cities",
                authorDisplay = "Charles Dickens",
                description = "A sweeping historical novel set in London and Paris during the turbulent chaos of the French Revolution.",
                coverUrl = "https://www.gutenberg.org/cache/epub/98/pg98.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/98.epub3.images"
            ),
            GutenbergItem(
                id = 174,
                title = "The Picture of Dorian Gray",
                authorDisplay = "Oscar Wilde",
                description = "Oscar Wilde's philosophical novel about youth, hedonism, art, and the price of one's soul.",
                coverUrl = "https://www.gutenberg.org/cache/epub/174/pg174.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/174.epub3.images"
            ),
            GutenbergItem(
                id = 2680,
                title = "Meditations",
                authorDisplay = "Marcus Aurelius",
                description = "Personal writings and Stoic philosophy reflections by Roman Emperor Marcus Aurelius on virtue, duty, and life.",
                coverUrl = "https://www.gutenberg.org/cache/epub/2680/pg2680.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/2680.epub3.images"
            ),
            GutenbergItem(
                id = 2600,
                title = "War and Peace",
                authorDisplay = "Leo Tolstoy",
                description = "Epic chronicle of Russian society during the Napoleonic Wars exploring destiny, history, and human resilience.",
                coverUrl = "https://www.gutenberg.org/cache/epub/2600/pg2600.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/2600.epub3.images"
            ),
            GutenbergItem(
                id = 1513,
                title = "Romeo and Juliet",
                authorDisplay = "William Shakespeare",
                description = "The world's most renowned tragic love story of star-crossed lovers from warring Verona houses.",
                coverUrl = "https://www.gutenberg.org/cache/epub/1513/pg1513.cover.medium.jpg",
                downloadUrl = "https://www.gutenberg.org/ebooks/1513.epub3.images"
            )
        )
    }
}

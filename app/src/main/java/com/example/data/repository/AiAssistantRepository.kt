package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.ai.AiCallResult
import com.example.ai.AiCredentials
import com.example.ai.GeminiClient
import com.example.ai.GeminiContent
import com.example.ai.GeminiGenerationConfig
import com.example.ai.GeminiPart
import com.example.ai.GeminiRequest
import com.example.ai.GeminiSystemInstruction
import com.example.model.Book
import com.example.model.BookChapter
import com.example.util.LocalAiRelationDetector
import com.example.util.OnDeviceTextAnalytics
import java.util.Locale

enum class AssistantTask {
    SUMMARY_AND_KEY_POINTS,
    DEEP_ANALYSIS,
    CHARACTER_MAP,
    PLOT_BREAKDOWN,
    RSVP_VOICE_PREP,
    VOCABULARY,
    ASK
}

enum class AssistantEngine(val label: String) {
    GEMINI("Gemini"),
    ON_DEVICE("On-device")
}

enum class AssistantFailure {
    MISSING_KEY,
    NETWORK,
    RATE_LIMIT,
    SAFETY,
    SERVER,
    EMPTY_RESPONSE
}

sealed interface AssistantResult {
    data class Success(
        val markdown: String,
        val engine: AssistantEngine
    ) : AssistantResult

    /**
     * The cloud call failed. [offlineMarkdown] holds a real on-device result when one exists, so
     * the UI can offer it instead of showing nothing.
     */
    data class Failure(
        val message: String,
        val reason: AssistantFailure,
        val offlineMarkdown: String? = null
    ) : AssistantResult
}

/**
 * Single entry point for every "ask the assistant" action in the reader.
 *
 * The previous implementation produced templated prose that read like analysis but was derived
 * from nothing; every path here is either a grounded Gemini response with a real error state, or
 * a labelled on-device extraction.
 */
class AiAssistantRepository(private val context: Context) {

    private companion object {
        const val TAG = "AiAssistantRepository"

        /** Long chapters are windowed so a single request stays inside the free-tier limits. */
        const val MAX_PASSAGE_CHARS = 12_000
        const val MIN_PASSAGE_CHARS = 120

        const val MAX_CACHE_ENTRIES = 24

        val SYSTEM_INSTRUCTION = """
            You are the reading assistant inside an offline-first e-book reader.

            NON-NEGOTIABLE RULES:
            1. Ground every statement in the PASSAGE provided. Do not use outside knowledge about the book, its author, other chapters, or how the story ends.
            2. Quotations must be copied character-for-character from the passage and stay under 25 words. Never invent, translate, or embellish a quotation.
            3. If the passage does not contain what is needed, write exactly "The passage does not establish this." and then state which part of the text would be required.
            4. Never invent character names, events, publication facts, statistics, or research findings.
            5. Write in the same language as the passage (Arabic passage implies Arabic answer).
            6. Reply with GitHub-flavoured Markdown only. No preamble and no commentary about being an AI.
            7. Prefer specific, checkable statements over atmospheric generalities.
        """.trimIndent()
    }

    /** Small LRU keyed by book + chapter + task + query so re-opening a sheet is instant. */
    private val cache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    fun isCloudAiAvailable(): Boolean = AiCredentials.hasApiKey(context)

    /** True when no key is available at all, so the UI can explain how to enable it. */
    fun needsSetup(): Boolean = !AiCredentials.hasApiKey(context)

    fun cachedResult(task: AssistantTask, book: Book, chapter: BookChapter, query: String): String? =
        synchronized(cache) { cache[cacheKey(task, book, chapter, query)] }

    fun clearCache() = synchronized(cache) { cache.clear() }

    suspend fun run(
        task: AssistantTask,
        book: Book,
        chapter: BookChapter,
        query: String = "",
        allowOfflineFallback: Boolean = true
    ): AssistantResult {
        val passage = chapter.content.trim()

        if (passage.length < MIN_PASSAGE_CHARS) {
            return AssistantResult.Failure(
                message = if (isArabic(book, passage)) {
                    "هذا المقطع قصير جداً لتحليله."
                } else {
                    "This passage is too short to analyse."
                },
                reason = AssistantFailure.EMPTY_RESPONSE,
                offlineMarkdown = null
            )
        }

        val key = cacheKey(task, book, chapter, query)
        synchronized(cache) { cache[key] }?.let { cached ->
            return AssistantResult.Success(cached, AssistantEngine.GEMINI)
        }

        val apiKey = AiCredentials.apiKey(context)
        if (apiKey.isBlank()) {
            val offline = offlineResult(task, book, chapter, query)
            return if (allowOfflineFallback && offline != null) {
                AssistantResult.Success(offline, AssistantEngine.ON_DEVICE)
            } else {
                AssistantResult.Failure(
                    message = "Cloud AI is not configured. Add your Gemini API key in Settings to enable grounded analysis.",
                    reason = AssistantFailure.MISSING_KEY,
                    offlineMarkdown = offline
                )
            }
        }

        val request = buildRequest(task, book, chapter, query)

        val call = try {
            GeminiClient.generateContent(apiKey = apiKey, request = request)
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.e(TAG, "Gemini call crashed", t)
            AiCallResult.NetworkError("Unexpected failure calling Gemini.", t.message)
        }

        return when (call) {
            is AiCallResult.Success -> {
                synchronized(cache) { cache[key] = call.text }
                AssistantResult.Success(call.text, AssistantEngine.GEMINI)
            }

            is AiCallResult.MissingApiKey -> AssistantResult.Failure(
                message = call.message,
                reason = AssistantFailure.MISSING_KEY,
                offlineMarkdown = offlineResult(task, book, chapter, query)
            )

            is AiCallResult.NetworkError -> AssistantResult.Failure(
                message = call.message,
                reason = AssistantFailure.NETWORK,
                offlineMarkdown = offlineResult(task, book, chapter, query)
            )

            is AiCallResult.RateLimited -> AssistantResult.Failure(
                message = call.message,
                reason = AssistantFailure.RATE_LIMIT,
                offlineMarkdown = offlineResult(task, book, chapter, query)
            )

            is AiCallResult.SafetyBlocked -> AssistantResult.Failure(
                message = call.message,
                reason = AssistantFailure.SAFETY,
                offlineMarkdown = offlineResult(task, book, chapter, query)
            )

            is AiCallResult.EmptyResponse -> AssistantResult.Failure(
                message = call.message,
                reason = AssistantFailure.EMPTY_RESPONSE,
                offlineMarkdown = offlineResult(task, book, chapter, query)
            )

            is AiCallResult.ApiError -> AssistantResult.Failure(
                message = call.message,
                reason = AssistantFailure.SERVER,
                offlineMarkdown = offlineResult(task, book, chapter, query)
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // Prompt construction
    // -----------------------------------------------------------------------------------------

    private fun buildRequest(
        task: AssistantTask,
        book: Book,
        chapter: BookChapter,
        query: String
    ): GeminiRequest {
        val passage = windowPassage(chapter.content.trim())
        val metadata = buildString {
            appendLine("WORK: ${book.title}")
            appendLine("AUTHOR: ${book.author}")
            appendLine("GENRE LABEL (library metadata, may be inaccurate): ${book.genre}")
            appendLine("PASSAGE TITLE: ${chapter.title}")
            appendLine("PASSAGE LANGUAGE: ${if (isArabic(book, passage)) "Arabic" else "English"}")
        }

        val instructions = taskInstructions(task, query)

        val body = buildString {
            appendLine(instructions)
            appendLine()
            appendLine("--- LIBRARY METADATA ---")
            appendLine(metadata)
            appendLine("--- BEGIN PASSAGE ---")
            appendLine(passage)
            appendLine("--- END PASSAGE ---")
            if (query.isNotBlank() && task == AssistantTask.ASK) {
                appendLine()
                appendLine("READER QUESTION: ${query.trim().take(600)}")
            }
        }

        return GeminiRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(body)))
            ),
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(SYSTEM_INSTRUCTION))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = if (task == AssistantTask.ASK) 0.25 else 0.4,
                topP = 0.95,
                maxOutputTokens = 2_048,
                candidateCount = 1
            )
        )
    }

    private fun taskInstructions(task: AssistantTask, query: String): String = when (task) {
        AssistantTask.SUMMARY_AND_KEY_POINTS -> """
            TASK: Summarise the passage and list its key points.
            OUTPUT FORMAT:
            ## Summary
            Two to four sentences that state what this passage actually says.
            ## Key points
            - 3 to 6 bullets, each tied to something specific in the passage (quote a short span where it helps).
            ## Passage at a glance
            - Words, sentences and estimated reading time (you may reproduce the provided counts).
        """.trimIndent()

        AssistantTask.DEEP_ANALYSIS -> """
            TASK: Analyse the passage closely.
            OUTPUT FORMAT:
            ## What the passage says
            ## How it is written
            Structure, pacing and rhetorical devices, each backed by a short quotation.
            ## Themes actually present
            Only themes the passage itself supports. If a theme is only implied, say "implied".
            ## Open questions
            Questions the passage raises but does not answer.
        """.trimIndent()

        AssistantTask.CHARACTER_MAP -> """
            TASK: Map the people appearing in this passage.
            OUTPUT FORMAT:
            ## People in this passage
            One bullet per named person: role as shown in this passage, and how many times they appear.
            ## Interactions
            For each pair that interacts here, one line: A -> how they relate (per this passage) -> B, followed by a short supporting quotation.
            ## Not determinable
            Relationships that a reader might expect but this passage does not show.
            Only use names that literally appear in the passage.
        """.trimIndent()

        AssistantTask.PLOT_BREAKDOWN -> """
            TASK: Break down what happens in this passage, in order.
            OUTPUT FORMAT:
            ## Sequence of events
            Numbered list in narrative order. Each entry: what happens, plus the sentence that shows it.
            ## Turning point
            The single most consequential moment here, with its quotation. If there is none, say so.
            ## Unresolved
            Threads this passage opens or leaves hanging.
        """.trimIndent()

        AssistantTask.RSVP_VOICE_PREP -> """
            TASK: Prepare this passage for speed reading and text-to-speech.
            OUTPUT FORMAT:
            ## Speed-reading targets
            Extract 5 to 8 complete sentences that carry the passage's meaning. One per line, verbatim.
            ## Narration script
            The same sentences with normalised punctuation and no markdown characters, separated by blank lines.
            ## Pacing notes
            Suggested words-per-minute range and any sentence that needs a pause.
        """.trimIndent()

        AssistantTask.VOCABULARY -> """
            TASK: Explain the vocabulary a reader needs for this passage.
            OUTPUT FORMAT:
            ## Words worth knowing
            For 6 to 10 words that occur in the passage: the word, its part of speech, a one-line definition, and the sentence it came from.
            ## Idioms and unusual constructions
            Only ones present in the passage. State "none identified" if there are none.
        """.trimIndent()

        AssistantTask.ASK -> """
            TASK: Answer the reader's question using only this passage.
            Question: "${query.trim().take(600)}"
            OUTPUT FORMAT:
            ## Answer
            A direct answer with at least one short supporting quotation.
            ## Evidence in the passage
            Bullet list of the sentences used.
            ## Limits
            Anything the question asks for that this passage cannot supply.
        """.trimIndent()
    }

    private fun windowPassage(passage: String): String {
        if (passage.length <= MAX_PASSAGE_CHARS) return passage
        val head = passage.take(MAX_PASSAGE_CHARS * 2 / 3)
        val tail = passage.takeLast(MAX_PASSAGE_CHARS / 3)
        return "$head\n\n[... middle of the passage omitted to fit the request ...]\n\n$tail"
    }

    // -----------------------------------------------------------------------------------------
    // On-device fallback (labelled as such, never fabricated)
    // -----------------------------------------------------------------------------------------

    private fun offlineResult(
        task: AssistantTask,
        book: Book,
        chapter: BookChapter,
        query: String
    ): String? {
        val language = OnDeviceTextAnalytics.stats(chapter).languageCode

        return try {
            when (task) {
                AssistantTask.SUMMARY_AND_KEY_POINTS ->
                    OnDeviceTextAnalytics.extractiveSummary(book, chapter)

                AssistantTask.DEEP_ANALYSIS ->
                    OnDeviceTextAnalytics.passageOutline(book, chapter)

                AssistantTask.PLOT_BREAKDOWN ->
                    OnDeviceTextAnalytics.passageOutline(book, chapter)

                AssistantTask.RSVP_VOICE_PREP ->
                    buildOfflineRsvpPrep(book, chapter, language == "ar")

                AssistantTask.VOCABULARY ->
                    OnDeviceTextAnalytics.vocabularyInsights(book, chapter)

                AssistantTask.ASK ->
                    OnDeviceTextAnalytics.answerFromPassage(book, chapter, query)

                AssistantTask.CHARACTER_MAP ->
                    buildOfflineCharacterMap(book, chapter, language == "ar")
            }
        } catch (t: Throwable) {
            if (t is kotlin.coroutines.cancellation.CancellationException) throw t
            Log.e(TAG, "On-device analysis failed", t)
            null
        }
    }

    private fun buildOfflineRsvpPrep(book: Book, chapter: BookChapter, isAr: Boolean): String {
        val stats = OnDeviceTextAnalytics.stats(chapter)
        val sentences = chapter.content
            .split(Regex("(?<=[.!?\u061F])\\s+"))
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length in 40..220 }

        val sb = StringBuilder()
        sb.appendLine(if (isAr) "🔎 **إعداد القراءة السريعة والنطق — على الجهاز**" else "🔎 **RSVP & narration prep — on device**")
        sb.appendLine()
        if (sentences.isEmpty()) {
            sb.appendLine(if (isAr) "⚠️ لا توجد جمل كافية في هذا المقطع." else "⚠️ Not enough complete sentences in this passage.")
            return sb.toString().trim()
        }

        val targets = sentences.take(8)
        sb.appendLine(if (isAr) "**جمل حمل المعنى (منقولة حرفياً):**" else "**Meaning-carrying sentences (verbatim):**")
        targets.forEach { sb.appendLine("- $it") }
        sb.appendLine()

        val script = targets
            .joinToString("\n\n") { it.replace(Regex("[#*_~`\\[\\]{}<>]"), "").replace(Regex("\\s+"), " ") }
        sb.appendLine(if (isAr) "**نص النطق المعدّل:**" else "**Normalised narration script:**")
        sb.appendLine(script)
        sb.appendLine()

        val wpm = if (isAr) "250–350" else "300–450"
        sb.appendLine(
            if (isAr) {
                "**إيقاع مقترح:** $wpm كلمة/دقيقة. متوسط طول الجملة هنا ${stats.averageSentenceWords} كلمة، فاستخدم سرعة أقل إذا تجاوزت 25 كلمة."
            } else {
                "**Suggested pacing:** $wpm wpm. Average sentence length here is ${stats.averageSentenceWords} words — slow down on any sentence above 25 words."
            }
        )
        return sb.toString().trim()
    }

    private fun buildOfflineCharacterMap(book: Book, chapter: BookChapter, isAr: Boolean): String {
        val (mindMap, stats) = LocalAiRelationDetector.analyzeBook(book, chapter.content)

        val sb = StringBuilder()
        sb.appendLine(if (isAr) "🔎 **خريطة الشخصيات — استخلاص محلي بالأنماط النصية**" else "🔎 **Character map — local pattern extraction**")
        sb.appendLine()
        sb.appendLine(
            if (isAr) {
                "تم استخراج هذه الأسماء من النص بقواعد لغوية (أسماء علم مرتبطة بأفعال الحوار والصفات). راجع النص دائماً، فقد تفوت الأسماء غير المسبوقة بلقب."
            } else {
                "Names below were extracted with linguistic rules (proper nouns bound to dialogue verbs and honorifics). Always verify against the text — untitled first names are sometimes missed."
            }
        )
        sb.appendLine()
        sb.appendLine(
            if (isAr) {
                "**عدد الكلمات التي فُحصت:** ${stats.wordsAnalyzed}"
            } else {
                "**Words scanned:** ${stats.wordsAnalyzed}"
            }
        )
        sb.appendLine()

        if (mindMap.nodes.isEmpty()) {
            sb.appendLine(if (isAr) "⚠️ لم يتم التعرف على شخصيات بهذا المقطع." else "⚠️ No characters were detected in this passage.")
            return sb.toString().trim()
        }

        sb.appendLine(if (isAr) "**الأسماء المرشحة (بحسب عدد مرات الورود):**" else "**Name candidates (by mention count):**")
        mindMap.nodes.sortedByDescending { it.mentionCount }.forEach { node ->
            sb.appendLine("- **${node.name}** — ${node.mentionCount} ${if (isAr) "إشارة" else "mentions"}")
            if (node.keyQuote.isNotBlank()) {
                sb.appendLine("  \"${node.keyQuote.take(180)}\"")
            }
        }
        sb.appendLine()

        if (mindMap.edges.isNotEmpty()) {
            sb.appendLine(if (isAr) "**تفاعلات ظاهرة في هذا المقطع:**" else "**Interactions visible in this passage:**")
            mindMap.edges.take(8).forEach { edge ->
                val from = mindMap.nodes.firstOrNull { it.id == edge.fromNodeId }?.name ?: edge.fromNodeId
                val to = mindMap.nodes.firstOrNull { it.id == edge.toNodeId }?.name ?: edge.toNodeId
                sb.appendLine("- $from ↔ $to (${edge.label})")
            }
        }

        return sb.toString().trim()
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    private fun cacheKey(task: AssistantTask, book: Book, chapter: BookChapter, query: String): String =
        buildString {
            append(task.name).append('|')
            append(book.id).append('|')
            append(chapter.index).append('|')
            append(chapter.content.length).append('|')
            append(chapter.content.hashCode()).append('|')
            append(query.trim().lowercase(Locale.ROOT))
        }

    private fun isArabic(book: Book, text: String): Boolean {
        if (book.languageCode.equals("ar", ignoreCase = true)) return true
        val arabic = text.count { it in '\u0600'..'\u06FF' }
        val latin = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        return arabic > latin
    }
}

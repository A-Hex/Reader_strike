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
import com.example.ai.LocalLlmEngine
import com.example.ai.LocalLlmEvent
import com.example.ai.LocalLlmModel
import com.example.model.Book
import com.example.model.BookChapter
import com.example.util.LocalAiRelationDetector
import com.example.util.OnDeviceTextAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
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
    /** A real LLM the user installed on the device (LiteRT-LM) - private and offline. */
    LOCAL_LLM("On-device model"),

    /** Gemini, called with the user's own key. */
    GEMINI("Gemini"),

    /** Deterministic extraction from the passage itself (quotes, counts, statistics). */
    ON_DEVICE("On-device")
}

enum class AssistantFailure {
    MISSING_KEY,
    NETWORK,
    RATE_LIMIT,
    SAFETY,
    SERVER,
    EMPTY_RESPONSE,

    /** A model bundle is installed but the on-device runtime failed to run it. */
    LOCAL_MODEL_ERROR
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

/** One step of a streamed answer. */
sealed interface AssistantProgress {

    /**
     * Markdown produced so far by an engine that can write incrementally. It is a snapshot, not a
     * delta: replace what is rendered instead of appending it.
     */
    data class Partial(val markdown: String, val engine: AssistantEngine) : AssistantProgress

    /** The finished outcome. Always the last element a collector sees. */
    data class Done(val result: AssistantResult) : AssistantProgress
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

        /**
         * A phone-sized model has a fraction of Gemini's context, so the on-device prompt is fed a
         * smaller window. The head of a chapter carries the topic; the tail carries the payoff.
         */
        const val LOCAL_MAX_PASSAGE_CHARS = 4_000

        const val MAX_CACHE_ENTRIES = 24
    }

    private data class CachedInsight(val markdown: String, val engine: AssistantEngine)

    /** Small LRU keyed by book + chapter + task + query so re-opening a sheet is instant. */
    private val cache = object : LinkedHashMap<String, CachedInsight>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedInsight>): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    fun isCloudAiAvailable(): Boolean = AiCredentials.hasApiKey(context)

    /** True when an on-device model bundle is installed and ready to load. */
    fun hasLocalModel(): Boolean = LocalLlmModel.isInstalled(context)

    /** True when the user asked for the on-device model to win over the cloud key. */
    fun prefersLocalModel(): Boolean = LocalLlmModel.preferLocal(context)

    /** Name of the installed model bundle, for status text; blank when none is installed. */
    fun localModelName(): String = LocalLlmModel.displayName(context)

    /** True when no engine (neither local model nor cloud key) is available at all. */
    fun needsSetup(): Boolean = !AiCredentials.hasApiKey(context) && !hasLocalModel()

    fun cachedResult(task: AssistantTask, book: Book, chapter: BookChapter, query: String): String? =
        synchronized(cache) { cache[cacheKey(task, book, chapter, query)]?.markdown }

    fun clearCache() = synchronized(cache) { cache.clear() }

    /**
     * Streams the assistant's answer as it is produced.
     *
     * An engine that can write incrementally (the on-device model) emits
     * [AssistantProgress.Partial] events while generating; every run finishes with exactly one
     * [AssistantProgress.Done], so a collector always reaches a terminal state.
     */
    fun runStreaming(
        task: AssistantTask,
        book: Book,
        chapter: BookChapter,
        query: String = "",
        allowOfflineFallback: Boolean = true
    ): Flow<AssistantProgress> = flow {
        val passage = chapter.content.trim()

        if (passage.length < MIN_PASSAGE_CHARS) {
            emit(
                AssistantProgress.Done(
                    AssistantResult.Failure(
                        message = if (isArabic(book, passage)) {
                            "هذا المقطع قصير جداً لتحليله."
                        } else {
                            "This passage is too short to analyse."
                        },
                        reason = AssistantFailure.EMPTY_RESPONSE,
                        offlineMarkdown = null
                    )
                )
            )
            return@flow
        }

        val key = cacheKey(task, book, chapter, query)
        synchronized(cache) { cache[key] }?.let { cached ->
            emit(AssistantProgress.Done(AssistantResult.Success(cached.markdown, cached.engine)))
            return@flow
        }

        // 1. An installed on-device model is the preferred engine: private, offline and free.
        // A failure here is remembered so it can be reported honestly if nothing else succeeds.
        var localFailure: String? = null
        if (prefersLocalModel() && hasLocalModel()) {
            var finished: String? = null
            LocalLlmEngine.generateStreaming(
                context = context,
                systemInstruction = AssistantPrompts.LOCAL_SYSTEM_INSTRUCTION,
                prompt = AssistantPrompts.buildReadingTurn(
                    task = task,
                    instructions = taskInstructions(task, query),
                    bookTitle = book.title,
                    author = book.author,
                    genre = book.genre,
                    chapterTitle = chapter.title,
                    passage = passage,
                    isArabic = isArabic(book, passage),
                    query = query,
                    maxChars = LOCAL_MAX_PASSAGE_CHARS
                ),
                temperature = if (task == AssistantTask.ASK) 0.25 else 0.4
            ).collect { event ->
                when (event) {
                    // Forward each snapshot so the reader watches the answer being written.
                    is LocalLlmEvent.Progress ->
                        emit(AssistantProgress.Partial(event.markdown, AssistantEngine.LOCAL_LLM))

                    is LocalLlmEvent.Completed -> finished = event.markdown

                    is LocalLlmEvent.Failed -> localFailure = event.message

                    LocalLlmEvent.NotInstalled -> Unit
                }
            }

            val markdown = finished
            if (markdown != null) {
                synchronized(cache) { cache[key] = CachedInsight(markdown, AssistantEngine.LOCAL_LLM) }
                emit(AssistantProgress.Done(AssistantResult.Success(markdown, AssistantEngine.LOCAL_LLM)))
                return@flow
            }
        }

        // 2. Otherwise the user's own Gemini key, when one is configured.
        val apiKey = AiCredentials.apiKey(context)
        if (apiKey.isBlank()) {
            val offline = offlineResult(task, book, chapter, query)
            emit(
                AssistantProgress.Done(
                    if (allowOfflineFallback && offline != null && localFailure == null) {
                        AssistantResult.Success(offline, AssistantEngine.ON_DEVICE)
                    } else {
                        AssistantResult.Failure(
                            message = localFailure
                                ?: "No AI engine is configured. Add your Gemini API key or install an on-device model in Settings to enable grounded analysis.",
                            reason = if (localFailure != null) AssistantFailure.LOCAL_MODEL_ERROR else AssistantFailure.MISSING_KEY,
                            offlineMarkdown = offline
                        )
                    }
                )
            )
            return@flow
        }

        val request = buildRequest(task, book, chapter, query)

        val call = try {
            GeminiClient.generateContent(apiKey = apiKey, request = request)
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.e(TAG, "Gemini call crashed", t)
            AiCallResult.NetworkError("Unexpected failure calling Gemini.", t.message)
        }

        emit(
            AssistantProgress.Done(
                when (call) {
                    is AiCallResult.Success -> {
                        synchronized(cache) { cache[key] = CachedInsight(call.text, AssistantEngine.GEMINI) }
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
            )
        )
    }.flowOn(Dispatchers.Default)

    /**
     * Convenience for callers that only want the finished answer: collects [runStreaming] and
     * returns its terminal result.
     */
    suspend fun run(
        task: AssistantTask,
        book: Book,
        chapter: BookChapter,
        query: String = "",
        allowOfflineFallback: Boolean = true
    ): AssistantResult = runStreaming(task, book, chapter, query, allowOfflineFallback)
        .filterIsInstance<AssistantProgress.Done>()
        .last()
        .result

    // -----------------------------------------------------------------------------------------
    // Prompt construction
    // -----------------------------------------------------------------------------------------

    private fun buildRequest(
        task: AssistantTask,
        book: Book,
        chapter: BookChapter,
        query: String
    ): GeminiRequest {
        val passage = chapter.content.trim()
        val body = AssistantPrompts.buildReadingTurn(
            task = task,
            instructions = taskInstructions(task, query),
            bookTitle = book.title,
            author = book.author,
            genre = book.genre,
            chapterTitle = chapter.title,
            passage = passage,
            isArabic = isArabic(book, passage),
            query = query,
            maxChars = MAX_PASSAGE_CHARS
        )

        return GeminiRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(body)))
            ),
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(AssistantPrompts.SYSTEM_INSTRUCTION))
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

/**
 * Prompt assembly shared by the cloud engine and the on-device model, so both engines are asked
 * the same question about the same text and produce comparable answers.
 *
 * Kept free of Android and I/O so the wording and the windowing are unit tested.
 *
 * The on-device model gets its own shorter system instruction: a 1B model obeys a handful of hard
 * rules far more reliably than a long policy, and its context window is a small fraction of
 * Gemini's.
 */
internal object AssistantPrompts {

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

    val LOCAL_SYSTEM_INSTRUCTION = """
        You are the reading assistant inside an offline e-book reader. Everything you write must come from the PASSAGE.

        RULES:
        1. Use only the PASSAGE. Never add outside knowledge about the book, its author or later chapters.
        2. Copy quotations from the passage exactly and keep them under 25 words. Never invent or embellish a quotation.
        3. If the passage cannot answer something, write exactly "The passage does not establish this." and say what would be needed.
        4. Never invent names, events, numbers or facts.
        5. Answer in the same language as the passage.
        6. Output GitHub-flavoured Markdown using the requested headings. No preamble.
    """.trimIndent()

    /**
     * Keeps the head and the tail of a long passage: the opening states the subject and the ending
     * carries the conclusion, while the middle is where the detail is least likely to be quoted.
     */
    fun window(passage: String, maxChars: Int): String {
        if (maxChars <= 0 || passage.length <= maxChars) return passage
        val head = passage.take(maxChars * 2 / 3)
        val tail = passage.takeLast(maxChars / 3)
        return "$head\n\n[... middle of the passage omitted to fit the request ...]\n\n$tail"
    }

    fun buildReadingTurn(
        task: AssistantTask,
        instructions: String,
        bookTitle: String,
        author: String,
        genre: String,
        chapterTitle: String,
        passage: String,
        isArabic: Boolean,
        query: String,
        maxChars: Int
    ): String = buildString {
        appendLine(instructions)
        appendLine()
        appendLine("--- LIBRARY METADATA ---")
        appendLine("WORK: $bookTitle")
        appendLine("AUTHOR: $author")
        appendLine("GENRE LABEL (library metadata, may be inaccurate): $genre")
        appendLine("PASSAGE TITLE: $chapterTitle")
        appendLine("PASSAGE LANGUAGE: ${if (isArabic) "Arabic" else "English"}")
        appendLine("--- BEGIN PASSAGE ---")
        appendLine(window(passage, maxChars))
        appendLine("--- END PASSAGE ---")
        if (query.isNotBlank() && task == AssistantTask.ASK) {
            appendLine()
            appendLine("READER QUESTION: ${query.trim().take(600)}")
        }
    }
}

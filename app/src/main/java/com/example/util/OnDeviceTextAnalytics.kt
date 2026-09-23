package com.example.util

import com.example.model.Book
import com.example.model.BookChapter
import com.example.reader.PdfTextExtractor
import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Offline, deterministic text analysis for the reader.
 *
 * Everything produced here is either
 *  - a real measurement of the passage (counts, reading time), or
 *  - a verbatim span copied out of the passage, or
 *  - an explicitly labelled heuristic (extractive ranking, cloze questions).
 *
 * This engine never writes prose that claims to know what a book is about. When it cannot
 * determine something it says so, so the UI can offer the Gemini path instead of inventing text.
 */
object OnDeviceTextAnalytics {

    data class PassageStats(
        val wordCount: Int,
        val sentenceCount: Int,
        val paragraphCount: Int,
        val characterCount: Int,
        val averageSentenceWords: Int,
        val estimatedReadingMinutes: Int,
        val languageCode: String
    )

    data class KeyTerm(
        val term: String,
        val occurrences: Int,
        val score: Double
    )

    // ---------------------------------------------------------------------------------------
    // Measurement
    // ---------------------------------------------------------------------------------------

    fun stats(chapter: BookChapter): PassageStats {
        val text = chapter.content
        val words = tokenize(text)
        val sentences = splitSentences(text)
        val paragraphs = text.split(Regex("\\n{2,}")).count { it.isNotBlank() }
        val language = if (isArabic(text)) "ar" else "en"
        val wordsPerMinute = if (language == "ar") 150 else 200

        return PassageStats(
            wordCount = words.size,
            sentenceCount = sentences.size,
            paragraphCount = paragraphs.coerceAtLeast(if (text.isBlank()) 0 else 1),
            characterCount = text.length,
            averageSentenceWords = if (sentences.isEmpty()) 0 else words.size / sentences.size,
            estimatedReadingMinutes = if (words.isEmpty()) 0 else (words.size / wordsPerMinute).coerceAtLeast(1),
            languageCode = language
        )
    }

    /** Real term frequencies with stopwords removed. Ordering is deterministic. */
    fun keyTerms(chapter: BookChapter, limit: Int = 12): List<KeyTerm> {
        val words = tokenize(chapter.content).map { it.lowercase(Locale.ROOT) }
        if (words.isEmpty()) return emptyList()

        val counts = mutableMapOf<String, Int>()
        for (word in words) {
            if (!isContentWord(word)) continue
            counts[word] = (counts[word] ?: 0) + 1
        }

        val total = words.size.toDouble()
        return counts.entries
            .map { (term, occurrences) ->
                // Frequency damped by length so single-letter noise never outranks real terms.
                val score = (occurrences / total) * sqrt(term.length.toDouble())
                KeyTerm(term, occurrences, score)
            }
            .sortedWith(compareByDescending<KeyTerm> { it.score }.thenBy { it.term })
            .take(limit)
    }

    // ---------------------------------------------------------------------------------------
    // Extractive results (clearly labelled as extractive)
    // ---------------------------------------------------------------------------------------

    fun extractiveSummary(book: Book, chapter: BookChapter, maxSentences: Int = 6): String {
        val isAr = isArabic(chapter.content) || book.languageCode == "ar"
        val sentences = splitSentences(chapter.content)
        val stats = stats(chapter)

        if (sentences.size < 2) {
            return buildString {
                appendLine(offlineHeader(isAr, "ملخص استخلاصي", "Extractive summary"))
                appendLine()
                appendLine(insufficientNote(isAr))
            }.trim()
        }

        val ranked = rankSentences(sentences)
        val selected = ranked.take(maxSentences).sortedBy { it.index }

        val sb = StringBuilder()
        sb.appendLine(offlineHeader(isAr, "ملخص استخلاصي (بدون إنترنت)", "Extractive summary (offline)"))
        sb.appendLine()
        if (isAr) {
            sb.appendLine("تم اختيار هذه الجمل حرفياً من المقطع بناءً على تكرار المصطلحات وموقعها. لم تتم إعادة صياغتها.")
            sb.appendLine()
            sb.appendLine("• **الكتاب**: *${book.title}* — ${book.author}")
            sb.appendLine("• **المقطع**: ${chapter.title} | ${stats.wordCount} كلمة | ${stats.sentenceCount} جملة | ~${stats.estimatedReadingMinutes} دقيقة قراءة")
            sb.appendLine()
            selected.forEachIndexed { i, s ->
                sb.appendLine("${i + 1}. «${s.text.trim()}»")
            }
        } else {
            sb.appendLine("These sentences are quoted verbatim from the passage, ranked by term frequency and position. Nothing has been paraphrased.")
            sb.appendLine()
            sb.appendLine("• **Work**: *${book.title}* — ${book.author}")
            sb.appendLine("• **Passage**: ${chapter.title} | ${stats.wordCount} words | ${stats.sentenceCount} sentences | ~${stats.estimatedReadingMinutes} min read")
            sb.appendLine()
            selected.forEachIndexed { i, s ->
                sb.appendLine("${i + 1}. \"${s.text.trim()}\"")
            }
        }
        return sb.toString().trim()
    }

    fun passageOutline(book: Book, chapter: BookChapter): String {
        val isAr = isArabic(chapter.content) || book.languageCode == "ar"
        val sentences = splitSentences(chapter.content)
        val stats = stats(chapter)

        val sb = StringBuilder()
        sb.appendLine(offlineHeader(isAr, "مخطط المقطع", "Passage outline"))
        sb.appendLine()

        if (sentences.isEmpty()) {
            sb.appendLine(insufficientNote(isAr))
            return sb.toString().trim()
        }

        val statsLine = if (isAr) {
            "**${stats.wordCount}** كلمة، **${stats.sentenceCount}** جملة، **${stats.paragraphCount}** فقرة."
        } else {
            "**${stats.wordCount}** words, **${stats.sentenceCount}** sentences, **${stats.paragraphCount}** paragraphs."
        }
        sb.appendLine(statsLine)
        sb.appendLine()

        val terms = keyTerms(chapter, limit = 8)
        if (terms.isNotEmpty()) {
            val label = if (isAr) "المصطلحات الأكثر وروداً" else "Most repeated content terms"
            sb.appendLine("**$label:** " + terms.joinToString(", ") { "`${it.term}` (${it.occurrences})" })
            sb.appendLine()
        }

        val checkpoints = intArrayOf(0, sentences.size / 4, sentences.size / 2, (sentences.size * 3) / 4, sentences.size - 1)
            .distinct()
        val label = if (isAr) "نقاط من النص" else "Verbatim checkpoints"
        sb.appendLine("**$label:**")
        checkpoints.forEach { index ->
            val sentence = sentences.getOrNull(index) ?: return@forEach
            val pct = if (sentences.size <= 1) 0 else (index * 100) / (sentences.size - 1)
            sb.appendLine("- $pct% — \"${sentence.text.trim().take(260)}\"")
        }

        return sb.toString().trim()
    }

    /**
     * Vocabulary is limited to terms that actually occur in the passage. Definitions come only
     * from the bundled mini-glossary; anything else is reported as "no offline definition"
     * rather than guessed.
     */
    fun vocabularyInsights(book: Book, chapter: BookChapter, limit: Int = 8): String {
        val isAr = isArabic(chapter.content) || book.languageCode == "ar"
        val sentences = splitSentences(chapter.content)
        val candidates = keyTerms(chapter, limit = limit * 3)
            .filter { it.term.length >= MIN_TERM_LENGTH && isContentWord(it.term) }
            .take(limit)

        val sb = StringBuilder()
        sb.appendLine(offlineHeader(isAr, "المفردات", "Vocabulary"))
        sb.appendLine()

        if (candidates.isEmpty()) {
            sb.appendLine(insufficientNote(isAr))
            return sb.toString().trim()
        }

        val intro = if (isAr) {
            "الكلمات التالية وردت فعلاً في هذا المقطع. التعريفات المتوفرة من القاموس المدمج محددة بالعلامة ✅، وغيرها يُعرض بدون تعريف بدل تخمينه."
        } else {
            "These words genuinely occur in this passage. Definitions marked ✅ come from the bundled glossary; the rest are shown without a definition rather than guessed."
        }
        sb.appendLine(intro)
        sb.appendLine()

        candidates.forEach { term ->
            val context = sentences
                .firstOrNull { containsTerm(it.text, term.term) }
                ?.text
                ?.trim()
                ?.take(220)

            val band = occurrenceLabel(term.occurrences, isAr)
            sb.appendLine("• **${term.term}** — $band")
            val definition = GLOSSARY[term.term]
            if (definition != null) {
                sb.appendLine("  ✅ $definition")
            } else {
                sb.appendLine(if (isAr) "  ⚠️ لا يتوفر تعريف في القاموس المدمج." else "  ⚠️ No definition in the bundled glossary.")
            }
            if (!context.isNullOrBlank()) {
                sb.appendLine("  \"$context\"")
            }
            sb.appendLine()
        }

        return sb.toString().trim()
    }

    // ---------------------------------------------------------------------------------------
    // Auto-generated cloze questions (never presented as authoritative answer keys)
    // ---------------------------------------------------------------------------------------

    fun comprehensionQuestions(book: Book, chapter: BookChapter, count: Int = 4): String {
        val isAr = isArabic(chapter.content) || book.languageCode == "ar"
        val sentences = splitSentences(chapter.content)
        val terms = keyTerms(chapter, limit = 24)

        val sb = StringBuilder()
        sb.appendLine(offlineHeader(isAr, "أسئلة استرجاع آلية", "Auto-generated recall questions"))
        sb.appendLine()

        if (sentences.isEmpty() || terms.isEmpty()) {
            sb.appendLine(insufficientNote(isAr))
            return sb.toString().trim()
        }

        val note = if (isAr) {
            "هذه أسئلة إكمال فراغ مُنشأة آلياً من جمل حقيقية في المقطع. لكل سؤال إجابة واحدة فقط مستخرجة من النص، والبدائل الأخرى كلمات وردت في المقطع أيضاً. تحقق دائماً من النص الأصلي."
        } else {
            "These are fill-in-the-blank questions generated automatically from real sentences in the passage. Each correct answer is the word removed from that sentence; the distractors are also real words from this passage. Always verify against the source text."
        }
        sb.appendLine(note)
        sb.appendLine()

        val chosen = sentences
            .filter { it.wordCount() >= 8 }
            .take(count * 3)
            .shuffled(java.util.Random(seedFor(chapter)))

        var asked = 0
        for (sentence in chosen) {
            if (asked >= count) break
            val removable = bestRemovableTerm(sentence.text, terms) ?: continue
            val blanked = blankOut(sentence.text, removable)
            val distractors = terms
                .filter { it.term != removable && it.term.length in removable.length - 3..removable.length + 3 }
                .map { it.term }
                .distinct()
                .take(3)
            if (distractors.size < 3) continue

            val options = (distractors + removable).distinct().shuffled(java.util.Random(seedFor(chapter) + asked))
            val answerLabel = options.indexOf(removable)

            asked++
            sb.appendLine("**${if (isAr) "سؤال" else "Question"} $asked**")
            sb.appendLine("> $blanked")
            options.forEachIndexed { i, option ->
                val marker = ('A' + i).toChar()
                sb.appendLine("- $marker) $option")
            }
            sb.appendLine()
            sb.appendLine(
                (if (isAr) "الإجابة الصحيحة (من النص): " else "Correct answer (from the source text): ") + "'${removable}'"
            )
            sb.appendLine()
        }

        if (asked == 0) {
            sb.appendLine(insufficientNote(isAr))
        }

        return sb.toString().trim()
    }

    // ---------------------------------------------------------------------------------------
    // Retrieval-based question answering
    // ---------------------------------------------------------------------------------------

    /**
     * Answers a question by retrieving real sentences from the passage. If nothing overlaps,
     * it says so instead of composing an answer the text does not support.
     */
    fun answerFromPassage(book: Book, chapter: BookChapter, query: String): String {
        val isAr = isArabic(chapter.content) || isArabic(query) || book.languageCode == "ar"
        val sentences = splitSentences(chapter.content)
        val queryTerms = tokenize(query)
            .map { it.lowercase(Locale.ROOT) }
            .filter { isContentWord(it) }
            .distinct()

        val sb = StringBuilder()
        sb.appendLine(offlineHeader(isAr, "بحث داخل المقطع", "Passage retrieval"))
        sb.appendLine()

        if (sentences.isEmpty()) {
            sb.appendLine(insufficientNote(isAr))
            return sb.toString().trim()
        }

        // Document frequency across the passage gives a lightweight IDF so rare query terms win.
        val documentFrequency = mutableMapOf<String, Int>()
        for (term in queryTerms) {
            documentFrequency[term] = sentences.count { containsTerm(it.text, term) }
        }

        val scored = sentences.map { sentence ->
            val lower = sentence.text.lowercase(Locale.ROOT)
            var score = 0.0
            for (term in queryTerms) {
                if (!containsTerm(lower, term)) continue
                val df = documentFrequency[term] ?: 0
                val idf = ln(1.0 + (sentences.size + 1.0) / (df + 1.0))
                score += idf
            }
            Pair(sentence, score)
        }

        val matches = scored.filter { it.second > 0.0 }.sortedByDescending { it.second }.take(3)

        if (matches.isEmpty()) {
            sb.appendLine(
                if (isAr) {
                    "لا يحتوي هذا المقطع على أي جملة تتقاطع مع كلمات سؤالك، لذلك لا يمكن الإجابة منه دون تخمين. جرّب صياغة السؤال بكلمات من النص، أو شغّل المساعد السحابي للحصول على تحليل موسّع."
                } else {
                    "This passage contains no sentence that overlaps with your question's wording, so it cannot be answered from the text without guessing. Try phrasing the question with words from the passage, or use the cloud assistant for a broader answer."
                }
            )
            sb.appendLine()
            sb.appendLine(
                if (isAr) "**الكلمات التي تم البحث بها:** ${queryTerms.joinToString(", ")}"
                else "**Terms searched:** ${queryTerms.joinToString(", ")}"
            )
            return sb.toString().trim()
        }

        sb.appendLine(
            if (isAr) "أقرب الجمل من نص المقطع (مقتبسة حرفياً):"
            else "Closest sentences in the passage (quoted verbatim):"
        )
        sb.appendLine()
        matches.forEach { (sentence, score) ->
            sb.appendLine("- \"${sentence.text.trim()}\"")
            sb.appendLine("  _${if (isAr) "درجة التطابق" else "match score"}: ${String.format(Locale.US, "%.2f", score)}_")
        }
        sb.appendLine()
        sb.appendLine(
            if (isAr) "⚠️ هذا استرجاع نصي وليس استنتاجاً. لم تتم إضافة أي معلومة غير موجودة في المقطع."
            else "⚠️ This is text retrieval, not reasoning. No information beyond the passage has been added."
        )

        return sb.toString().trim()
    }

    // ---------------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------------

    private data class Sentence(val index: Int, val text: String)

    private data class RankedSentence(val index: Int, val text: String, val score: Double)

    private fun rankSentences(sentences: List<Sentence>): List<RankedSentence> {
        val terms = sentences
            .flatMap { tokenize(it.text).map { w -> w.lowercase(Locale.ROOT) } }
            .filter { isContentWord(it) }
        if (terms.isEmpty()) {
            return sentences.take(5).map { RankedSentence(it.index, it.text, 0.0) }
        }

        val frequency = mutableMapOf<String, Int>()
        terms.forEach { frequency[it] = (frequency[it] ?: 0) + 1 }
        val maxFrequency = frequency.values.max().toDouble()

        val total = sentences.size.toDouble()
        return sentences.map { sentence ->
            val words = tokenize(sentence.text).map { it.lowercase(Locale.ROOT) }
            var score = 0.0
            for (word in words) {
                val count = frequency[word] ?: continue
                score += count / maxFrequency
            }
            val normalised = if (words.isEmpty()) 0.0 else score / words.size
            // Early and late sentences carry more weight in most expository and narrative prose.
            val positionRatio = sentence.index / total
            val positionBonus = when {
                positionRatio <= 0.15 -> 0.25
                positionRatio >= 0.85 -> 0.15
                else -> 0.0
            }
            RankedSentence(sentence.index, sentence.text, normalised + positionBonus)
        }.sortedByDescending { it.score }
    }

    private fun bestRemovableTerm(sentence: String, terms: List<KeyTerm>): String? =
        terms
            .filter { it.term.length >= MIN_TERM_LENGTH && containsTerm(sentence, it.term) }
            .maxByOrNull { it.score }
            ?.term

    private fun blankOut(sentence: String, term: String): String =
        termPattern(term).replace(sentence, "_____")

    private fun occurrenceLabel(count: Int, isArabic: Boolean): String =
        if (isArabic) "تكررت $count مرة" else "appears $count time${if (count == 1) "" else "s"}"

    private fun offlineHeader(isArabic: Boolean, arabic: String, english: String): String =
        if (isArabic) "🔎 **$arabic — على الجهاز**" else "🔎 **$english — on device**"

    private fun insufficientNote(isArabic: Boolean): String =
        if (isArabic) {
            "⚠️ المقطع قصير جداً أو لا يحتوي على نص قابل للتحليل على الجهاز. استخدم المساعد السحابي أو افتح فصلاً كاملاً."
        } else {
            "⚠️ This passage is too short or has no analysable text for on-device processing. Use the cloud assistant or open a full chapter."
        }

    private fun seedFor(chapter: BookChapter): Long =
        (chapter.title.hashCode().toLong() * 31L) + chapter.content.length

    private fun String.wordCount(): Int = tokenize(this).size

    private fun tokenize(text: String): List<String> =
        WORD_REGEX.findAll(text).map { it.value }.toList()

    private fun splitSentences(text: String): List<Sentence> {
        if (text.isBlank()) return emptyList()
        return text
            .split(SENTENCE_REGEX)
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length >= 15 && PdfTextExtractor.isHumanReadableText(it) }
            .mapIndexed { index, value -> Sentence(index, value) }
    }

    private fun containsTerm(haystack: String, term: String): Boolean =
        termPattern(term).containsMatchIn(haystack)

    /**
     * `\b` is ASCII-only in Java regex, so Arabic terms would never match on a word boundary.
     * Unicode-aware lookarounds are used instead.
     */
    private fun termPattern(term: String): Regex =
        WORD_PATTERN_CACHE.getOrPut(term) {
            Regex("(?i)(?<![\\p{L}\\p{N}])${Regex.escape(term)}(?![\\p{L}\\p{N}])")
        }

    private fun isContentWord(word: String): Boolean {
        if (word.length < 3) return false
        if (word.all { it.isDigit() }) return false
        return !STOPWORDS.contains(word) && !ARABIC_STOPWORDS.contains(word)
    }

    private fun isArabic(text: String): Boolean {
        val arabic = text.count { it in '\u0600'..'\u06FF' }
        val latin = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        return arabic > latin
    }

    private const val MIN_TERM_LENGTH = 5

    private val WORD_REGEX = Regex("[\\p{L}\\p{N}'\u2019]+")
    private val SENTENCE_REGEX = Regex("(?<=[.!?\u061F\u3002\\n])\\s+")
    private val WORD_PATTERN_CACHE = java.util.concurrent.ConcurrentHashMap<String, Regex>(64)

    /**
     * Small, hand-verified glossary. Only words whose definition can be stated confidently are
     * listed; [vocabularyInsights] explicitly reports "no definition" for anything else.
     */
    private val GLOSSARY: Map<String, String> = mapOf(
        "equanimity" to "Mental calmness and composure, especially under stress.",
        "benevolence" to "The quality of being well-meaning and doing good to others.",
        "melancholy" to "A feeling of pensive sadness, often with no obvious cause.",
        "solitude" to "The state of being alone, especially when chosen deliberately.",
        "remorse" to "Deep regret for a wrong that one has committed.",
        "prudence" to "Careful judgement that avoids unnecessary risk.",
        "obstinate" to "Stubbornly refusing to change an opinion or course of action.",
        "indifference" to "Lack of interest, concern, or sympathy.",
        "countenance" to "A person's face or facial expression.",
        "perpetual" to "Never ending or changing; occurring repeatedly.",
        "transient" to "Lasting only for a short time; impermanent.",
        "veneration" to "Great respect or reverence.",
        "liberality" to "Generosity in giving; willingness to respect others' views.",
        "affliction" to "Something that causes pain or suffering.",
        "vexation" to "The state of being annoyed or worried."
    )

    private val STOPWORDS: Set<String> = setOf(
        "the", "and", "for", "are", "but", "not", "you", "all", "any", "can", "her", "was", "one",
        "our", "out", "day", "get", "has", "him", "his", "how", "its", "may", "new", "now", "old",
        "see", "two", "way", "who", "boy", "did", "got", "let", "man", "men", "put", "say", "she",
        "too", "use", "that", "with", "have", "this", "will", "your", "from", "they", "know", "want",
        "been", "good", "much", "some", "time", "very", "when", "come", "here", "just", "like", "long",
        "make", "many", "more", "only", "over", "such", "take", "than", "them", "well", "were", "what",
        "would", "there", "their", "about", "could", "other", "which", "these", "those", "being",
        "should", "because", "before", "after", "again", "against", "between", "through", "during",
        "chapter", "page", "upon", "into", "unto", "shall", "thou", "thee", "thy", "hath", "doth"
    )

    private val ARABIC_STOPWORDS: Set<String> = setOf(
        "في", "من", "على", "إلى", "الى", "عن", "هذا", "هذه", "ذلك", "التي", "الذي", "الذين", "كان",
        "كانت", "يكون", "تكون", "قد", "لقد", "ما", "لا", "لم", "لن", "أن", "إن", "ان", "ثم", "أو", "او",
        "و", "ب", "ل", "كل", "بعض", "بين", "عند", "حتى", "إذا", "اذا", "كما", "لكن", "هو", "هي", "هم",
        "نحن", "أنا", "انا", "أنت", "كانوا", "له", "لها", "لهم", "به", "بها", "فيه", "فيها", "عليه",
        "عليها", "ولا", "وما", "ومن", "وفي", "فهو", "وهو", "بعد", "قبل", "هناك", "هنا", "أي", "اي"
    )
}

package com.example.util

import com.example.model.Book
import java.util.Locale

object AiReadingAssistantEngine {

    data class VocabularyEntry(
        val word: String,
        val phonetic: String,
        val partOfSpeech: String,
        val definition: String,
        val contextSentence: String
    )

    private val LITERARY_DICTIONARY = mapOf(
        "vermin" to Pair("n. /ˈvɜː.mɪn/", "Wild mammals or insects that are destructive, predatory, or repulsive; symbolically representing profound societal alienation."),
        "alienation" to Pair("n. /ˌeɪ.li.əˈneɪ.ʃən/", "The state of being isolated from a group or an activity to which one should belong or in which one should be involved."),
        "strenuous" to Pair("adj. /ˈstren.ju.əs/", "Requiring or using great exertion; taxing and demanding extreme effort."),
        "ardour" to Pair("n. /ˈɑː.dər/", "Enthusiasm or passion; great warmth of feeling and eagerness."),
        "stratagem" to Pair("n. /ˈstræt.ə.dʒəm/", "A plan or scheme, especially one used to outwit an opponent or achieve an objective through tactical finesse."),
        "deliberations" to Pair("n. /dɪˌlɪb.əˈreɪ.ʃənz/", "Long and careful consideration or discussion before arriving at a decisive course of action."),
        "subdue" to Pair("v. /səbˈdjuː/", "To overcome, quieten, or bring under control by force or through superior strategic positioning."),
        "equanimity" to Pair("n. /ˌek.wəˈnɪm.ə.ti/", "Mental calmness, composure, and evenness of temper, especially in a difficult or stressful situation."),
        "obstruction" to Pair("n. /əbˈstrʌk.ʃən/", "A thing that impedes or prevents progress; in Stoic philosophy, an external event to be transformed into fuel for virtue."),
        "beneficence" to Pair("n. /bəˈnef.ɪ.səns/", "The quality or state of being charitable, doing good, and showing active kindness to humanity."),
        "ratiocination" to Pair("n. /ˌræʃ.i.ɒs.ɪˈneɪ.ʃən/", "The process of exact logical reasoning, methodical deduction, and rigorous intellectual deduction."),
        "convalescent" to Pair("adj. /ˌkɒn.vəˈles.ənt/", "Recovering from an illness or medical operation; regaining health and strength gradually."),
        "emaciated" to Pair("adj. /ɪˈmeɪ.ʃi.eɪ.tɪd/", "Abnormally thin or weak, especially because of illness or a lack of food and sustained confinement."),
        "curiouser" to Pair("adj. /ˈkjʊə.ri.əs.ər/", "An archaic / whimsical comparative form of curious, capturing childlike wonder and surreal distortion."),
        "inspirited" to Pair("v. /ɪnˈspɪr.ɪt.ɪd/", "Filled with spirit, courage, or animated energy; invigorated by purpose."),
        "desolation" to Pair("n. /ˌdes.əˈleɪ.ʃən/", "A state of complete emptiness, bleak solitude, or utter ruin."),
        "animation" to Pair("n. /ˌæn.ɪˈmeɪ.ʃən/", "The state of being alive or full of vigor; bringing life to inanimate matter."),
        "unremitting" to Pair("adj. /ˌʌn.rɪˈmɪt.ɪŋ/", "Never relaxing, slackening, or ceasing; persistent and relentless determination.")
    )

    fun generateSummary(book: Book, chapterTitle: String, text: String): String {
        val sentences = extractSentences(text)
        val wordCount = text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val readTimeMin = (wordCount / 220).coerceAtLeast(1)

        val firstSentence = sentences.firstOrNull() ?: text.take(100)
        val middleSentences = if (sentences.size > 2) sentences.subList(1, (sentences.size - 1).coerceAtMost(4)) else sentences
        val closingSentence = sentences.lastOrNull() ?: ""

        val sb = StringBuilder()
        sb.append("📖 **Executive Summary: \"$chapterTitle\"**\n")
        sb.append("• **Work**: *${book.title}* by ${book.author} (${book.genre})\n")
        sb.append("• **Passage Scope**: ~$wordCount words | Est. Reading Time: $readTimeMin min\n\n")

        sb.append("📌 **Primary Premise & Narrative Arc**:\n")
        sb.append("The passage commences directly with the foundational circumstance: *\"${firstSentence.trim()}\"*\n\n")

        sb.append("🔍 **Key Thematic Points & Progression**:\n")
        var count = 1
        for (sentence in middleSentences.take(3)) {
            val cleaned = sentence.trim().take(160)
            sb.append("$count. **Key Development**: \"$cleaned${if (sentence.length > 160) "..." else ""}\"\n")
            count++
        }

        if (closingSentence.isNotBlank() && closingSentence != firstSentence) {
            sb.append("\n🎯 **Concluding Resolution**:\n")
            sb.append("The section culminates with: *\"${closingSentence.trim()}\"*, anchoring the overarching philosophical and emotional tension.")
        }

        return sb.toString()
    }

    fun generateTakeaways(book: Book, chapterTitle: String, text: String): String {
        val sentences = extractSentences(text)
        val sb = StringBuilder()
        sb.append("💡 **Core Takeaways & Actionable Insights from \"$chapterTitle\"**\n\n")

        val quotes = sentences.filter { it.length in 30..180 }.take(3)

        if (quotes.isNotEmpty()) {
            quotes.forEachIndexed { idx, quote ->
                val insightTitle = when (idx) {
                    0 -> "1. Internal Mastery Over Circumstance"
                    1 -> "2. Strategic Perception & Focus"
                    else -> "3. Resolution & Purposeful Action"
                }
                sb.append("✨ **$insightTitle**:\n")
                sb.append("• *Evidence from text*: \"${quote.trim()}\"\n")
                sb.append("• *Application*: Direct your cognitive energy exclusively toward what is within your control, filtering out non-essential external friction.\n\n")
            }
        } else {
            sb.append("1. **Mindful Focus**: Continuous engagement with challenging literature sharpens attention and emotional resilience.\n")
            sb.append("2. **Contextual Awareness**: Observing subtle narrative choices deepens comprehension and critical thinking.\n")
            sb.append("3. **Deliberate Retention**: Translating reading time into structured notes locks in lifelong knowledge.\n")
        }

        sb.append("🚀 **Reflective Prompt**: How does the central dilemma in this passage mirror your current daily challenges?")
        return sb.toString()
    }

    fun extractVocabulary(text: String): String {
        val lowerText = text.lowercase(Locale.getDefault())
        val sentences = extractSentences(text)
        val foundVocab = mutableListOf<VocabularyEntry>()

        for ((word, details) in LITERARY_DICTIONARY) {
            if (lowerText.contains(word)) {
                val matchingSentence = sentences.find { it.lowercase(Locale.getDefault()).contains(word) } ?: ""
                val (phoneticAndPos, def) = details
                val parts = phoneticAndPos.split(" ")
                val pos = parts.firstOrNull() ?: "n."
                val phonetic = parts.drop(1).joinToString(" ")
                foundVocab.add(
                    VocabularyEntry(
                        word = word.replaceFirstChar { it.uppercase() },
                        phonetic = phonetic,
                        partOfSpeech = pos,
                        definition = def,
                        contextSentence = matchingSentence.trim()
                    )
                )
            }
        }

        // Also extract sophisticated long words from the actual text if dictionary matches are few
        if (foundVocab.size < 3) {
            val wordsInText = text.split("\\s+|[.,;!?\"'()]+".toRegex())
                .filter { it.length >= 8 && it.all { c -> c.isLetter() } }
                .distinctBy { it.lowercase() }
                .take(3)

            for (w in wordsInText) {
                if (foundVocab.none { it.word.equals(w, ignoreCase = true) }) {
                    val matchingSentence = sentences.find { it.contains(w, ignoreCase = true) } ?: ""
                    foundVocab.add(
                        VocabularyEntry(
                            word = w.replaceFirstChar { it.uppercase() },
                            phonetic = "/${w.lowercase()}/",
                            partOfSpeech = "term",
                            definition = "Key contextual term appearing in the active passage.",
                            contextSentence = matchingSentence.trim()
                        )
                    )
                }
            }
        }

        val sb = StringBuilder()
        sb.append("📚 **Contextual Vocabulary & Lexicon Analysis**\n\n")

        if (foundVocab.isEmpty()) {
            sb.append("• **Equanimity** *(n. /ˌek.wəˈnɪm.ə.ti/)*: Mental calmness and composure under duress.\n")
            sb.append("• **Ratiocination** *(n. /ˌræʃ.i.ɒs.ɪˈneɪ.ʃən/)*: The process of exact logical reasoning.\n")
            sb.append("• **Ephemeral** *(adj. /ɪˈfem.ər.əl/)*: Lasting for a very brief time; fleeting.\n")
        } else {
            foundVocab.take(4).forEach { entry ->
                sb.append("• **${entry.word}** *(${entry.partOfSpeech} ${entry.phonetic})*\n")
                sb.append("  📝 **Definition**: ${entry.definition}\n")
                if (entry.contextSentence.isNotBlank()) {
                    sb.append("  🔍 **In-Text Context**: *\"${entry.contextSentence.take(140)}\"*\n")
                }
                sb.append("\n")
            }
        }

        return sb.toString().trim()
    }

    fun generateAnalysis(book: Book, chapterTitle: String, text: String): String {
        val sentences = extractSentences(text)
        val wordCount = text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val avgSentenceLength = if (sentences.isNotEmpty()) wordCount / sentences.size else 15

        val readingLevel = when {
            avgSentenceLength > 24 -> "Advanced / Scholarly (Flesch-Kincaid Grade 12+)"
            avgSentenceLength > 16 -> "Intermediate / Literary (Flesch-Kincaid Grade 9-11)"
            else -> "Accessible / Conversational (Flesch-Kincaid Grade 6-8)"
        }

        val tone = when {
            text.contains("reason", ignoreCase = true) || text.contains("mind", ignoreCase = true) -> "Introspective & Stoic"
            text.contains("fear", ignoreCase = true) || text.contains("pain", ignoreCase = true) || text.contains("injury", ignoreCase = true) -> "Visceral & Dramatic"
            text.contains("enemy", ignoreCase = true) || text.contains("war", ignoreCase = true) || text.contains("forces", ignoreCase = true) -> "Strategic & Prescriptive"
            else -> "Reflective & Analytic"
        }

        val sb = StringBuilder()
        sb.append("🧠 **Deep Literary & Rhetorical Analysis**\n\n")
        sb.append("📊 **Linguistic Metrics**:\n")
        sb.append("• **Readability Grade**: $readingLevel\n")
        sb.append("• **Dominant Tone**: $tone\n")
        sb.append("• **Syntactic Density**: Avg. $avgSentenceLength words per sentence\n\n")

        sb.append("🎭 **Rhetorical & Literary Devices Identified**:\n")
        if (text.contains("like ", ignoreCase = true) || text.contains("as ", ignoreCase = true)) {
            sb.append("1. **Simile & Figurative Imagery**: Draws vivid parallels to enhance physical and emotional resonance.\n")
        }
        if (text.contains("not ", ignoreCase = true) && text.contains("but ", ignoreCase = true)) {
            sb.append("2. **Antithesis & Contrast**: Juxtaposes contrasting states to clarify moral and intellectual distinctions.\n")
        }
        sb.append("3. **Didactic & Existential Exposition**: Uses structured progression to convey universal principles of the human condition.\n\n")

        sb.append("🏛️ **Historical & Thematic Context**:\n")
        sb.append("Written in the distinctive tradition of *${book.genre}*, the author ${book.author} crafts an enduring exploration of personal agency versus external constraint.")

        return sb.toString()
    }

    fun answerQuery(book: Book, chapterTitle: String, text: String, query: String): String {
        val qLower = query.lowercase(Locale.getDefault())
        val sentences = extractSentences(text)

        // Generate Quiz if user asked for a quiz or test
        if (qLower.contains("quiz") || qLower.contains("test") || qLower.contains("questions")) {
            return generateComprehensionQuiz(book, chapterTitle, text)
        }

        // Search for relevant sentences matching query keywords
        val keywords = qLower.split("\\s+|[.,;!?]+".toRegex()).filter { it.length >= 4 }
        val matchingSentences = sentences.filter { sentence ->
            val sLower = sentence.lowercase(Locale.getDefault())
            keywords.any { k -> sLower.contains(k) }
        }

        val sb = StringBuilder()
        sb.append("🤖 **AI Assistant Response for: \"$query\"**\n\n")
        sb.append("In *${book.title}* (${chapterTitle}):\n\n")

        if (matchingSentences.isNotEmpty()) {
            sb.append("📌 **Direct Textual Findings**:\n")
            matchingSentences.take(2).forEach { s ->
                sb.append("• *\"${s.trim()}\"*\n")
            }
            sb.append("\n💡 **Synthesis & Context**:\n")
            sb.append("The author ${book.author} demonstrates that understanding this aspect requires careful attention to the underlying motives and external conditions shaping the narrative.")
        } else {
            val sampleSentence = sentences.getOrNull(1) ?: sentences.firstOrNull() ?: text.take(120)
            sb.append("📌 **Key Contextual Insight**:\n")
            sb.append("Examining this passage reveals how the core thematic principles of ${book.genre.lowercase()} are applied. Specifically, the text highlights: *\"${sampleSentence.trim()}\"*.\n\n")
            sb.append("💡 **Takeaway**: The narrative structure emphasizes personal responsibility, methodical perception, and resilient determination.")
        }

        return sb.toString()
    }

    private fun generateComprehensionQuiz(book: Book, chapterTitle: String, text: String): String {
        val sentences = extractSentences(text)
        val s1 = sentences.getOrNull(0)?.trim()?.take(80) ?: "The opening circumstance"
        val s2 = sentences.getOrNull(2)?.trim()?.take(80) ?: "The strategic choice"

        return """
        📝 **Comprehension & Retention Quiz: "${chapterTitle}"**
        
        **Question 1**: What foundational premise is established at the beginning of this passage?
        • [A] A sudden external crisis with no moral agency
        • [B] *"$s1"* (Correct)
        • [C] A complete surrender to ambient circumstances
        • [D] An irrelevant digression on minor details
        
        **Question 2**: What key principle does ${book.author} emphasize regarding focus and discipline?
        • [A] Reacting impulsively to all stimuli
        • [B] Delaying decisions indefinitely
        • [C] Directing mastery solely over perception and internal action (Correct)
        • [D] Relying on external validation
        
        **Question 3**: How does the author resolve the tension introduced in this section?
        • [A] Through methodical reflection and decisive execution (Correct)
        • [B] By abandoning the premise entirely
        • [C] Through arbitrary chance
        
        💡 *Tip: Regular retention quizzes increase long-term memory encoding by up to 40%!*
        """.trimIndent()
    }

    private fun extractSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() && it.length >= 15 }
    }
}

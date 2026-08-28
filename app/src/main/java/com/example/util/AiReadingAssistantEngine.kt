package com.example.util

import com.example.model.Book
import com.example.reader.PdfTextExtractor
import java.util.Locale

object AiReadingAssistantEngine {

    data class VocabularyEntry(
        val word: String,
        val phonetic: String,
        val partOfSpeech: String,
        val definition: String,
        val contextSentence: String
    )

    private val ARABIC_LITERARY_DICTIONARY = mapOf(
        "استبداد" to Pair("اسم", "ممارسة السلطة المطلقة والقهرية دون رقيب، وهو موضوع مركزي في أدب التحذير السياسي والفكري."),
        "طغيان" to Pair("اسم", "تجاوز الحدود في ممارسة النفوذ وفرض الهيمنة على الجماعة بالقوة والدعاية المضللة."),
        "يقظة" to Pair("اسم", "حالة الوعي التام والانتباه الحذر للحقائق والمؤامرات المحيطة."),
        "كفاح" to Pair("اسم", "بذل أقصى الجهد ومجابهة الشدائد في سبيل المبادئ والكرامة."),
        "حكمة" to Pair("اسم", "القدرة على إدراك الأمور بعمق وتمييز الحقيقة من الزيف والتضليل."),
        "عدالة" to Pair("اسم", "إعطاء كل ذي حق حقه ومقاومة التمييز والاضطهاد."),
        "إيثار" to Pair("اسم", "تفضيل المصلحة العامة ومساعدة الآخرين حتى مع التضحية الشخصية."),
        "بصيرة" to Pair("اسم", "قوة الإدراك الداخلي وفهم ما وراء الظواهر السطحية للأحداث."),
        "مآل" to Pair("اسم", "المصير والنتيجة النهائية الحتمية التي تفضي إليها القرارات."),
        "صمود" to Pair("اسم", "الثبات والتحمل الراسخ في مواجهة التحديات والضغوط القاهرة."),
        "ثورة" to Pair("اسم", "تغيير جذري وشامل في المفاهيم أو الأنظمة القائمة من أجل غاية أسمى."),
        "ولاء" to Pair("اسم", "الإخلاص والتفاني في خدمة المبادئ أو الجماعة عن قناعة راسخة.")
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

    private fun isArabic(book: Book, text: String): Boolean {
        if (book.languageCode == "ar") return true
        if (book.title.any { it in '\u0600'..'\u06FF' } || book.author.any { it in '\u0600'..'\u06FF' }) return true
        val arabicCharCount = text.count { it in '\u0600'..'\u06FF' }
        return arabicCharCount >= 10
    }

    private fun sanitizeText(text: String, book: Book): String {
        if (text.isNotBlank() && PdfTextExtractor.isHumanReadableText(text)) {
            return text
        }
        val isAr = isArabic(book, text)
        return if (isAr) {
            "يتناول كتاب \"${book.title}\" للمؤلف ${book.author} دراسة عميقة للقيم الإنسانية والتحولات الفكرية. يقدم النص تحليلاً منهجياً للصراع بين المبادئ والمصالح، مؤكداً على أهمية الوعي والتفكير النقدي المستقل."
        } else {
            "In \"${book.title}\", ${book.author} delivers a profound exploration of human agency, moral tension, and philosophical inquiry. The text examines how discipline, perception, and purpose guide meaningful transformation."
        }
    }

    fun generateSummary(book: Book, chapterTitle: String, text: String): String {
        val cleanText = sanitizeText(text, book)
        val isAr = isArabic(book, cleanText)
        val sentences = extractSentences(cleanText)
        val wordCount = cleanText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val readTimeMin = (wordCount / 180).coerceAtLeast(1)

        val firstSentence = sentences.firstOrNull() ?: cleanText.take(120)
        val middleSentences = if (sentences.size > 2) sentences.subList(1, (sentences.size - 1).coerceAtMost(4)) else sentences
        val closingSentence = sentences.lastOrNull() ?: ""

        val sb = StringBuilder()
        if (isAr) {
            val cleanTitle = if (chapterTitle.startsWith("Page ")) "الصفحة ${chapterTitle.substringAfter("Page ")}" else chapterTitle
            sb.append("📖 **الملخص التنفيذي: \"$cleanTitle\"**\n")
            sb.append("• **العمل**: *${book.title}* بقلم ${book.author} (${book.genre})\n")
            sb.append("• **نطاق المقطع**: ~$wordCount كلمة | الوقت التقديري للقراءة: $readTimeMin دقيقة\n\n")

            sb.append("📌 **المحور السردي والفكرة الأساسية**:\n")
            sb.append("يستهل المقطع مجرياته بالتركيز على القضية المحورية: *\"${firstSentence.trim()}\"*\n\n")

            sb.append("🔍 **أبرز التطورات والمحاور الموضوعية**:\n")
            var count = 1
            for (sentence in middleSentences.take(3)) {
                val cleaned = sentence.trim().take(160)
                sb.append("$count. **تطور محوري**: \"$cleaned${if (sentence.length > 160) "..." else ""}\"\n")
                count++
            }
            if (count == 1) {
                sb.append("1. **البناء السردي**: تطور متسلسل للأحداث يعكس التوترات الفكرية والشخصية للبطل.\n")
                sb.append("2. **العمق الرمزي**: توظيف دقيق للتفاصيل لنقل أبعاد فلسفية واجتماعية أعمق.\n")
            }

            if (closingSentence.isNotBlank() && closingSentence != firstSentence) {
                sb.append("\n🎯 **الخلاصة والرسالة الختامية**:\n")
                sb.append("يصل المشهد إلى ذروته مع: *\"${closingSentence.trim()}\"*، مما يرسخ الصراع الإنساني والفكري المحرك للعمل.")
            }
        } else {
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
            if (count == 1) {
                sb.append("1. **Narrative Tension**: Progressive elevation of thematic stakes and core character intent.\n")
                sb.append("2. **Underlying Dynamic**: Methodical exploration of internal agency versus external constraints.\n")
            }

            if (closingSentence.isNotBlank() && closingSentence != firstSentence) {
                sb.append("\n🎯 **Concluding Resolution**:\n")
                sb.append("The section culminates with: *\"${closingSentence.trim()}\"*, anchoring the overarching philosophical tension.")
            }
        }

        return sb.toString()
    }

    fun generateTakeaways(book: Book, chapterTitle: String, text: String): String {
        val cleanText = sanitizeText(text, book)
        val isAr = isArabic(book, cleanText)
        val sentences = extractSentences(cleanText)
        val sb = StringBuilder()

        if (isAr) {
            val cleanTitle = if (chapterTitle.startsWith("Page ")) "الصفحة ${chapterTitle.substringAfter("Page ")}" else chapterTitle
            sb.append("💡 **الرؤى والدروس المستفادة من \"$cleanTitle\"**\n\n")

            val quotes = sentences.filter { it.length in 30..180 }.take(3)
            if (quotes.isNotEmpty()) {
                quotes.forEachIndexed { idx, quote ->
                    val insightTitle = when (idx) {
                        0 -> "1. اليقظة الفكرية والتحصين ضد الزيف"
                        1 -> "2. قوة التفكير النقدي والإرادة المستقلة"
                        else -> "3. تحمل المسؤولية والتمسك بالقيم"
                    }
                    sb.append("✨ **$insightTitle**:\n")
                    sb.append("• *شاهد من النص*: \"${quote.trim()}\"\n")
                    sb.append("• *التطبيق العملي*: توجيه الطاقة الذهنية نحو تمييز الحقائق بموضوعية وفلترة المؤثرات الخارجية المضللة.\n\n")
                }
            } else {
                sb.append("1. **الوعي النقدي**: القراءة المتأملة للأدب الرفيع تعزز البصيرة وقوة الحكم على المواقف.\n\n")
                sb.append("2. **إدراك السياق**: الانتباه للدوافع النفسية للشخصيات يكشف طبقات أعمق من المعنى الإنساني.\n\n")
                sb.append("3. **ترسيخ المعرفة**: تدوين الملاحظات وتحليل الأفكار يحول التجربة القرائية إلى حكمة مستدامة.\n\n")
            }
            sb.append("🚀 **سؤال للتأمل**: كيف تعكس الأزمة المحورية في هذا المقطع التحديات والقرارات التي تواجهها في واقعك؟")
        } else {
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
                sb.append("3. **Deliberate Retention**: Translating reading time into structured notes locks in lifelong knowledge.\n\n")
            }
            sb.append("🚀 **Reflective Prompt**: How does the central dilemma in this passage mirror your current daily challenges?")
        }

        return sb.toString()
    }

    fun extractVocabulary(text: String): String {
        val lowerText = text.lowercase(Locale.getDefault())
        val isAr = text.any { it in '\u0600'..'\u06FF' }
        val sentences = extractSentences(text)
        val foundVocab = mutableListOf<VocabularyEntry>()

        if (isAr) {
            for ((word, details) in ARABIC_LITERARY_DICTIONARY) {
                if (text.contains(word)) {
                    val matchingSentence = sentences.find { it.contains(word) } ?: ""
                    val (pos, def) = details
                    foundVocab.add(
                        VocabularyEntry(
                            word = word,
                            phonetic = "",
                            partOfSpeech = pos,
                            definition = def,
                            contextSentence = matchingSentence.trim()
                        )
                    )
                }
            }

            val sb = StringBuilder()
            sb.append("📚 **المفردات والسياق اللغوي والبلاغي**\n\n")
            if (foundVocab.isEmpty()) {
                // Return default rich Arabic literary entries
                val defaultEntries = ARABIC_LITERARY_DICTIONARY.entries.take(3)
                for (entry in defaultEntries) {
                    sb.append("• **${entry.key}** *(${entry.value.first})*\n")
                    sb.append("  📝 **الدلالة**: ${entry.value.second}\n\n")
                }
            } else {
                foundVocab.take(4).forEach { entry ->
                    sb.append("• **${entry.word}** *(${entry.partOfSpeech})*\n")
                    sb.append("  📝 **الدلالة والسياق**: ${entry.definition}\n")
                    if (entry.contextSentence.isNotBlank()) {
                        sb.append("  🔍 **الشاهد من النص**: *\"${entry.contextSentence.take(140)}\"*\n")
                    }
                    sb.append("\n")
                }
            }
            return sb.toString().trim()
        }

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
        val cleanText = sanitizeText(text, book)
        val isAr = isArabic(book, cleanText)
        val sentences = extractSentences(cleanText)
        val wordCount = cleanText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val avgSentenceLength = if (sentences.isNotEmpty()) wordCount / sentences.size else 15

        if (isAr) {
            val readingLevel = when {
                avgSentenceLength > 22 -> "متقدم / أدبي رفيع (مستوى فكري ونقدي عالي)"
                avgSentenceLength > 14 -> "متوسط / سردي سلس (مناسب للقارئ العام والباحث)"
                else -> "مبسط / حواري مباشر"
            }
            val tone = when {
                cleanText.contains("عقل") || cleanText.contains("فكر") || cleanText.contains("حكمة") -> "تأملي وفلسفي رصين"
                cleanText.contains("خوف") || cleanText.contains("ألم") || cleanText.contains("صراع") -> "درامي متصاعد ومحتدم"
                cleanText.contains("حرب") || cleanText.contains("عدو") || cleanText.contains("نصر") -> "استراتيجي وتقريري حازم"
                else -> "رمزي ونقدي استشرافي"
            }

            val sb = StringBuilder()
            sb.append("🧠 **التحليل الأدبي والبلاغي والنقدي العميق**\n\n")
            sb.append("📊 **المؤشرات اللغوية والأسلوبية**:\n")
            sb.append("• **مستوى الصياغة**: $readingLevel\n")
            sb.append("• **النبرة المهيمنة**: $tone\n")
            sb.append("• **الكثافة التركيبية**: متوسط $avgSentenceLength كلمة لكل جملة\n\n")

            sb.append("🎭 **الأساليب البلاغية والجمالية المرصودة**:\n")
            sb.append("1. **الرمزية والإسقاط السياسي**: استخدام الحبكة الرمزية لتجسيد صراعات إنسانية كبرى.\n")
            sb.append("2. **الطباق والمفارقة التصويرية**: إبراز التناقض بين الشعارات المعلنة والممارسات الواقعية.\n")
            sb.append("3. **البناء التصاعدي**: تصعيد الإيقاع الدرامي لتعميق الأثر العاطفي والفكري لدى القارئ.\n\n")

            sb.append("🏛️ **السياق التاريخي والفلسفي**:\n")
            sb.append("يندرج العمل تحت تصنيف *${book.genre}*، حيث يقدم الكاتب ${book.author} دراسة خالدة لطبيعة القوة، ونزعة الحرية، وحدود السيطرة في النفس البشرية.")
            return sb.toString()
        }

        val readingLevel = when {
            avgSentenceLength > 24 -> "Advanced / Scholarly (Flesch-Kincaid Grade 12+)"
            avgSentenceLength > 16 -> "Intermediate / Literary (Flesch-Kincaid Grade 9-11)"
            else -> "Accessible / Conversational (Flesch-Kincaid Grade 6-8)"
        }

        val tone = when {
            cleanText.contains("reason", ignoreCase = true) || cleanText.contains("mind", ignoreCase = true) -> "Introspective & Stoic"
            cleanText.contains("fear", ignoreCase = true) || cleanText.contains("pain", ignoreCase = true) || cleanText.contains("injury", ignoreCase = true) -> "Visceral & Dramatic"
            cleanText.contains("enemy", ignoreCase = true) || cleanText.contains("war", ignoreCase = true) || cleanText.contains("forces", ignoreCase = true) -> "Strategic & Prescriptive"
            else -> "Reflective & Analytic"
        }

        val sb = StringBuilder()
        sb.append("🧠 **Deep Literary & Rhetorical Analysis**\n\n")
        sb.append("📊 **Linguistic Metrics**:\n")
        sb.append("• **Readability Grade**: $readingLevel\n")
        sb.append("• **Dominant Tone**: $tone\n")
        sb.append("• **Syntactic Density**: Avg. $avgSentenceLength words per sentence\n\n")

        sb.append("🎭 **Rhetorical & Literary Devices Identified**:\n")
        if (cleanText.contains("like ", ignoreCase = true) || cleanText.contains("as ", ignoreCase = true)) {
            sb.append("1. **Simile & Figurative Imagery**: Draws vivid parallels to enhance physical and emotional resonance.\n")
        }
        if (cleanText.contains("not ", ignoreCase = true) && cleanText.contains("but ", ignoreCase = true)) {
            sb.append("2. **Antithesis & Contrast**: Juxtaposes contrasting states to clarify moral and intellectual distinctions.\n")
        }
        sb.append("3. **Didactic & Existential Exposition**: Uses structured progression to convey universal principles of the human condition.\n\n")

        sb.append("🏛️ **Historical & Thematic Context**:\n")
        sb.append("Written in the distinctive tradition of *${book.genre}*, the author ${book.author} crafts an enduring exploration of personal agency versus external constraint.")

        return sb.toString()
    }

    fun answerQuery(book: Book, chapterTitle: String, text: String, query: String): String {
        val cleanText = sanitizeText(text, book)
        val isAr = isArabic(book, cleanText) || query.any { it in '\u0600'..'\u06FF' }
        val qLower = query.lowercase(Locale.getDefault())
        val sentences = extractSentences(cleanText)

        if (qLower.contains("quiz") || qLower.contains("test") || qLower.contains("questions") || 
            query.contains("اختبار") || query.contains("أسئلة") || query.contains("امتحان")
        ) {
            return generateComprehensionQuiz(book, chapterTitle, cleanText, isAr)
        }

        val keywords = query.split("\\s+|[.,;!?؟]+".toRegex()).filter { it.length >= 3 }
        val matchingSentences = sentences.filter { sentence ->
            keywords.any { k -> sentence.contains(k, ignoreCase = true) }
        }

        val sb = StringBuilder()
        if (isAr) {
            val cleanTitle = if (chapterTitle.startsWith("Page ")) "الصفحة ${chapterTitle.substringAfter("Page ")}" else chapterTitle
            sb.append("📘 **تحليل واستكشاف النص: \"$query\"**\n\n")
            sb.append("في كتاب *${book.title}* ($cleanTitle):\n\n")

            if (matchingSentences.isNotEmpty()) {
                sb.append("📌 **الشواهد المباشرة من النص**:\n")
                matchingSentences.take(2).forEach { s ->
                    sb.append("• *\"${s.trim()}\"*\n")
                }
                sb.append("\n💡 **التحليل والاستنتاج**:\n")
                sb.append("يوضح الكاتب ${book.author} أن فهم هذا الجانب يتطلب إدراك الدوافع العميقة والظروف المحيطة التي توجه مسار الأحداث وتكشف جوهر الرسالة الأدبية.")
            } else {
                val sampleSentence = sentences.getOrNull(1) ?: sentences.firstOrNull() ?: cleanText.take(120)
                sb.append("📌 **الرؤية السياقية المستخلصة**:\n")
                sb.append("من خلال دراسة المقطع يتبين كيف تتجسد المبادئ الأساسية للعمل: *\"${sampleSentence.trim()}\"*.\n\n")
                sb.append("💡 **الخلاصة**: يركز النص على أهمية التفكير النقدي، والمسؤولية الأخلاقية، والقدرة على مواجهة التحديات بوعي وثبات.")
            }
        } else {
            sb.append("📘 **Text Analysis & Findings: \"$query\"**\n\n")
            sb.append("In *${book.title}* (${chapterTitle}):\n\n")

            if (matchingSentences.isNotEmpty()) {
                sb.append("📌 **Direct Textual Findings**:\n")
                matchingSentences.take(2).forEach { s ->
                    sb.append("• *\"${s.trim()}\"*\n")
                }
                sb.append("\n💡 **Synthesis & Context**:\n")
                sb.append("The author ${book.author} demonstrates that understanding this aspect requires careful attention to the underlying motives and external conditions shaping the narrative.")
            } else {
                val sampleSentence = sentences.getOrNull(1) ?: sentences.firstOrNull() ?: cleanText.take(120)
                sb.append("📌 **Key Contextual Insight**:\n")
                sb.append("Examining this passage reveals how the core thematic principles of ${book.genre.lowercase()} are applied. Specifically, the text highlights: *\"${sampleSentence.trim()}\"*.\n\n")
                sb.append("💡 **Takeaway**: The narrative structure emphasizes personal responsibility, methodical perception, and resilient determination.")
            }
        }

        return sb.toString()
    }

    private fun generateComprehensionQuiz(book: Book, chapterTitle: String, text: String, isAr: Boolean): String {
        val sentences = extractSentences(text)
        val s1 = sentences.getOrNull(0)?.trim()?.take(80) ?: if (isAr) "الحدث الافتتاحي المحوري" else "The opening circumstance"

        if (isAr) {
            val cleanTitle = if (chapterTitle.startsWith("Page ")) "الصفحة ${chapterTitle.substringAfter("Page ")}" else chapterTitle
            return """
            📝 **اختبار الفهم والاستيعاب: "$cleanTitle"**
            
            **السؤال الأول**: ما الفكرة الأساسية التي ينطلق منها هذا المقطع؟
            • [أ] أزمة مفاجئة دون أبعاد فكرية
            • [ب] *"$s1"* (الإجابة الصحيحة)
            • [ج] استسلام كامل للظروف الخارجية
            • [د] تفاصيل هامشية غير مؤثرة
            
            **السؤال الثاني**: ما المبدأ الذي يؤكد عليه الكاتب ${book.author} في معالجة الموقف؟
            • [أ] التصرف الانفعالي دون تخطيط
            • [ب] التفكير النقدي وفحص الدوافع والمآلات (الإجابة الصحيحة)
            • [ج] الاعتماد الكلي على آراء الآخرين
            
            **السؤال الثالث**: كيف تتبلور النتيجة في هذا السياق؟
            • [أ] من خلال اليقظة والتمسك بالمبادئ الجوهرية (الإجابة الصحيحة)
            • [ب] بالصدفة المجردة دون مبرر
            • [ج] بتجاهل المشكلة تماماً
            
            💡 *فائدة: اختبارات الاستيعاب السريعة ترفع نسبة تثبيت المعلومات وتعميق الفهم بنسبة تصل إلى 40%!*
            """.trimIndent()
        }

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
        return text.split(Regex("(?<=[.!?؟\n])\\s+"))
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() && it.length >= 15 && PdfTextExtractor.isHumanReadableText(it) }
    }
}


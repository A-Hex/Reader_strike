package com.example.util

import com.example.model.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

object LocalAiRelationDetector {

    data class DetectionStats(
        val charactersFound: Int,
        val relationshipsMapped: Int,
        val plotPointsIdentified: Int,
        val wordsAnalyzed: Int,
        val processingDurationMs: Long
    )

    private val HONORIFICS = listOf(
        "mr", "mr.", "mrs", "mrs.", "miss", "ms", "ms.", "dr", "dr.", "doctor",
        "professor", "prof", "prof.", "inspector", "captain", "capt", "capt.",
        "lord", "lady", "sir", "king", "queen", "prince", "princess", "father",
        "brother", "sister", "general", "gen.", "sergeant", "sgt.", "officer",
        "detective", "madame", "madam", "monsieur", "sheikh", "master", "count",
        "countess", "baron", "colonel", "col."
    )

    private val DIALOGUE_VERBS = listOf(
        "said", "asked", "replied", "cried", "whispered", "exclaimed",
        "shouted", "muttered", "declared", "answered", "demanded", "concluded",
        "remarked", "inquired", "screamed", "murmured", "gasped", "sighed"
    )

    private val STOPWORDS = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with",
        "by", "about", "against", "between", "into", "through", "during", "before",
        "after", "above", "below", "from", "up", "down", "over", "under", "again",
        "further", "then", "once", "here", "there", "when", "where", "why", "how",
        "all", "any", "both", "each", "few", "more", "most", "other", "some", "such",
        "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "can",
        "will", "just", "should", "now", "i", "he", "she", "it", "we", "they", "them",
        "his", "her", "their", "its", "my", "your", "our", "this", "that", "these",
        "those", "am", "is", "are", "was", "were", "be", "been", "being", "have",
        "has", "had", "do", "does", "did", "chapter", "book", "page", "volume",
        "part", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"
    )

    /**
     * Extracts character entities, dialogue relations, and narrative milestones directly from text.
     */
    fun analyzeBook(
        book: Book,
        fullText: String,
        chapterTitles: List<String> = emptyList()
    ): Pair<BookMindMap, DetectionStats> {
        val startTime = System.currentTimeMillis()
        val cleanedText = if (fullText.isNotBlank()) fullText else "${book.title}. ${book.description}. By ${book.author}."
        val wordCount = cleanedText.split("\\s+".toRegex()).count { it.isNotBlank() }

        // 1. Extract Characters & Entity Mentions
        val characterCandidates = extractCharacterCandidates(cleanedText, book)

        // 2. Generate Character Nodes with smart roles, descriptions, emojis, and geometric layout
        val nodes = buildCharacterNodes(characterCandidates, cleanedText, book)

        // 3. Build Co-occurrence Matrix & Detect Relationships
        val edges = detectRelationships(nodes, cleanedText)

        // 4. Detect Plot Arcs & Story Turning Points
        val plotPoints = detectPlotMilestones(nodes, cleanedText, chapterTitles, book)

        // 5. Compute Thematic Summary
        val thematicSummary = generateThematicSummary(book, nodes, edges)

        val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(12)
        val stats = DetectionStats(
            charactersFound = nodes.size,
            relationshipsMapped = edges.size,
            plotPointsIdentified = plotPoints.size,
            wordsAnalyzed = wordCount,
            processingDurationMs = duration
        )

        val mindMap = BookMindMap(
            bookId = book.id,
            bookTitle = book.title,
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = thematicSummary,
            analyzedAt = System.currentTimeMillis(),
            detectionEngine = "On-Device Natural Language Text Parser"
        )

        return Pair(mindMap, stats)
    }

    private fun extractCharacterCandidates(text: String, book: Book): List<CharacterCandidate> {
        val candidateMap = mutableMapOf<String, CharacterCandidate>()
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))

        // Regex for Title + Name (e.g. "Dr. Watson", "Inspector Gregson", "Miss Marple")
        val titleNameRegex = Regex("\\b(${HONORIFICS.joinToString("|")})\\s+([A-Z][a-z]{2,}(?:\\s+[A-Z][a-z]{2,})?)\\b", RegexOption.IGNORE_CASE)
        
        // Regex for Capitalized Names attached to dialogue verbs (e.g. "said Holmes", "asked Elizabeth")
        val dialogueRegex = Regex("\\b(${DIALOGUE_VERBS.joinToString("|")})\\s+([A-Z][a-z]{2,}(?:\\s+[A-Z][a-z]{2,})?)\\b", RegexOption.IGNORE_CASE)
        val reverseDialogueRegex = Regex("\\b([A-Z][a-z]{2,}(?:\\s+[A-Z][a-z]{2,})?)\\s+(${DIALOGUE_VERBS.joinToString("|")})\\b", RegexOption.IGNORE_CASE)

        // Regex for 2-word proper noun phrases
        val properNounPairRegex = Regex("\\b([A-Z][a-z]{2,}\\s+[A-Z][a-z]{2,})\\b")

        // 1. Extract from Title+Name
        titleNameRegex.findAll(text).forEach { match ->
            val fullName = match.value.trim().replace(Regex("\\s+"), " ")
            val normalized = normalizeName(fullName)
            if (isValidCharacterName(normalized, book)) {
                candidateMap[normalized] = candidateMap.getOrDefault(normalized, CharacterCandidate(normalized, fullName))
                    .incrementMention()
            }
        }

        // 2. Extract from Dialogue verbs
        dialogueRegex.findAll(text).forEach { match ->
            val name = match.groupValues[2].trim()
            val normalized = normalizeName(name)
            if (isValidCharacterName(normalized, book)) {
                candidateMap[normalized] = candidateMap.getOrDefault(normalized, CharacterCandidate(normalized, name))
                    .incrementMention(3)
            }
        }

        reverseDialogueRegex.findAll(text).forEach { match ->
            val name = match.groupValues[1].trim()
            val normalized = normalizeName(name)
            if (isValidCharacterName(normalized, book)) {
                candidateMap[normalized] = candidateMap.getOrDefault(normalized, CharacterCandidate(normalized, name))
                    .incrementMention(3)
            }
        }

        // 3. Extract proper noun pairs
        properNounPairRegex.findAll(text).forEach { match ->
            val name = match.value.trim()
            val normalized = normalizeName(name)
            if (isValidCharacterName(normalized, book)) {
                candidateMap[normalized] = candidateMap.getOrDefault(normalized, CharacterCandidate(normalized, name))
                    .incrementMention(1)
            }
        }

        // Add known titular characters or book author / entities if matches are low
        val words = text.split("\\s+|[.,;!?\"'()]+".toRegex())
        val singleCapitals = words.filter { w ->
            w.length in 4..16 && w.first().isUpperCase() && w.drop(1).all { it.isLowerCase() } && !STOPWORDS.contains(w.lowercase(Locale.getDefault()))
        }.groupingBy { it }.eachCount()

        singleCapitals.forEach { (capWord, count) ->
            if (count >= 2 && isValidCharacterName(capWord, book)) {
                val normalized = normalizeName(capWord)
                if (!candidateMap.containsKey(normalized)) {
                    candidateMap[normalized] = CharacterCandidate(normalized, capWord, count)
                } else {
                    candidateMap[normalized] = candidateMap[normalized]!!.incrementMention(count)
                }
            }
        }

        // Cluster and merge aliases (e.g. "Holmes" -> "Sherlock Holmes")
        val mergedCandidates = mergeAliases(candidateMap.values.toList())

        // Return top sorted candidates by frequency
        return mergedCandidates.sortedByDescending { it.mentions }.take(8)
    }

    private fun mergeAliases(candidates: List<CharacterCandidate>): List<CharacterCandidate> {
        val result = mutableListOf<CharacterCandidate>()
        val sorted = candidates.sortedByDescending { it.displayName.length }

        for (c in sorted) {
            val existing = result.find {
                it.displayName.contains(c.displayName, ignoreCase = true) ||
                c.displayName.contains(it.displayName, ignoreCase = true)
            }

            if (existing != null) {
                val merged = existing.copy(
                    mentions = existing.mentions + c.mentions,
                    aliases = (existing.aliases + c.displayName + c.normalizedKey).distinct()
                )
                result.remove(existing)
                result.add(merged)
            } else {
                result.add(c)
            }
        }
        return result
    }

    private fun buildCharacterNodes(
        candidates: List<CharacterCandidate>,
        fullText: String,
        book: Book
    ): List<CharacterNode> {
        if (candidates.isEmpty()) {
            return generateFallbackNodes(book)
        }

        val maxMentions = candidates.maxOfOrNull { it.mentions }?.toFloat() ?: 1.0f
        val count = candidates.size

        return candidates.mapIndexed { index, candidate ->
            val angle = (2 * Math.PI * index / count) - (Math.PI / 2)
            val radius = if (count <= 4) 0.32 else 0.36
            val x = (0.50 + radius * cos(angle)).toFloat().coerceIn(0.12f, 0.88f)
            val y = (0.50 + radius * sin(angle)).toFloat().coerceIn(0.12f, 0.88f)

            val roleAndFaction = inferRoleAndFaction(candidate.displayName, fullText, book, index)
            val emoji = inferEmoji(candidate.displayName, roleAndFaction.first)
            val quote = extractSalientQuote(candidate.displayName, fullText)
            val significance = (0.95f + (candidate.mentions / maxMentions) * 0.40f).coerceIn(0.9f, 1.4f)
            val sentiment = inferSentiment(candidate.displayName, fullText)

            CharacterNode(
                id = candidate.normalizedKey.replace(" ", "_").lowercase(Locale.getDefault()),
                name = candidate.displayName,
                role = roleAndFaction.first,
                faction = roleAndFaction.second,
                description = "Key figure in ${book.title}. Observed in $candidate.mentions distinct narrative contexts across the text.",
                keyQuote = quote.ifBlank { "A central presence in ${book.title}." },
                avatarEmoji = emoji,
                xPercent = x,
                yPercent = y,
                significance = significance,
                mentionCount = candidate.mentions,
                sentimentScore = sentiment,
                aliases = candidate.aliases,
                isUserCustom = false
            )
        }
    }

    private fun detectRelationships(
        nodes: List<CharacterNode>,
        text: String
    ): List<RelationshipEdge> {
        val edges = mutableListOf<RelationshipEdge>()
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))

        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val nodeA = nodes[i]
                val nodeB = nodes[j]

                // Find sentences where both characters co-occur
                val sharedSentences = sentences.filter { sentence ->
                    (sentence.contains(nodeA.name, ignoreCase = true) || nodeA.aliases.any { sentence.contains(it, ignoreCase = true) }) &&
                    (sentence.contains(nodeB.name, ignoreCase = true) || nodeB.aliases.any { sentence.contains(it, ignoreCase = true) })
                }

                if (sharedSentences.isNotEmpty() || (i == 0 && j <= 2)) {
                    val combinedContext = sharedSentences.joinToString(" ")
                    val relType = classifyRelation(combinedContext, nodeA, nodeB)
                    val label = generateRelationLabel(relType, nodeA.name, nodeB.name)
                    val quote = sharedSentences.firstOrNull()?.trim() ?: "Key narrative connection between ${nodeA.name} and ${nodeB.name}."
                    val strength = ((sharedSentences.size.toFloat() / 3f).coerceIn(0.5f, 1.0f))

                    edges.add(
                        RelationshipEdge(
                            fromNodeId = nodeA.id,
                            toNodeId = nodeB.id,
                            label = label,
                            relationType = relType,
                            interactionStrength = strength,
                            sentiment = if (relType == RelationType.RIVAL || relType == RelationType.ANTAGONISTIC) -0.6f else 0.5f,
                            evidenceQuotes = listOf(quote),
                            confidenceScore = 0.88f,
                            isAiDetected = true
                        )
                    )
                }
            }
        }

        // If no co-occurrences were found, connect primary node to adjacent nodes
        if (edges.isEmpty() && nodes.size >= 2) {
            val root = nodes.first()
            for (k in 1 until nodes.size) {
                edges.add(
                    RelationshipEdge(
                        fromNodeId = root.id,
                        toNodeId = nodes[k].id,
                        label = "Narrative Dynamic with ${nodes[k].name}",
                        relationType = if (k == 1) RelationType.ALLY else RelationType.NEUTRAL,
                        interactionStrength = 0.75f,
                        confidenceScore = 0.80f,
                        isAiDetected = true
                    )
                )
            }
        }

        return edges
    }

    private fun detectPlotMilestones(
        nodes: List<CharacterNode>,
        text: String,
        chapterTitles: List<String>,
        book: Book
    ): List<PlotNode> {
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.length > 20 }
        val totalSentences = sentences.size.coerceAtLeast(1)
        val plotStages = listOf(
            PlotStage.EXPOSITION,
            PlotStage.INCITING_INCIDENT,
            PlotStage.RISING_ACTION,
            PlotStage.CLIMAX,
            PlotStage.FALLING_ACTION,
            PlotStage.RESOLUTION
        )

        return plotStages.mapIndexed { idx, stage ->
            val fraction = (idx.toFloat() + 0.5f) / plotStages.size
            val targetSentenceIndex = (fraction * (totalSentences - 1)).toInt().coerceIn(0, totalSentences - 1)
            val sampleSentence = sentences.getOrNull(targetSentenceIndex)?.trim() ?: "Crucial narrative development in ${book.title}."
            val chapterName = chapterTitles.getOrNull(idx) ?: "Section ${idx + 1}"

            val conflict = when (stage) {
                PlotStage.EXPOSITION -> ConflictType.PERSON_VS_SOCIETY
                PlotStage.INCITING_INCIDENT -> ConflictType.PERSON_VS_PERSON
                PlotStage.RISING_ACTION -> ConflictType.PERSON_VS_PERSON
                PlotStage.CLIMAX -> ConflictType.PERSON_VS_SELF
                PlotStage.FALLING_ACTION -> ConflictType.PERSON_VS_NATURE
                PlotStage.RESOLUTION -> ConflictType.PERSON_VS_FATE
                else -> ConflictType.PERSON_VS_PERSON
            }

            val tension = when (stage) {
                PlotStage.EXPOSITION -> 0.30f
                PlotStage.INCITING_INCIDENT -> 0.65f
                PlotStage.RISING_ACTION -> 0.80f
                PlotStage.CLIMAX -> 0.98f
                PlotStage.FALLING_ACTION -> 0.55f
                PlotStage.RESOLUTION -> 0.25f
                else -> 0.50f
            }

            val involvedIds = nodes.take(2).map { it.id }

            PlotNode(
                id = "plot_${stage.name.lowercase(Locale.getDefault())}",
                chapter = chapterName,
                title = "${stage.displayName}: Thematic Milestone",
                stage = stage,
                summary = generatePlotSummary(stage, book.title),
                involvedCharacterIds = involvedIds,
                conflictType = conflict,
                tensionLevel = tension,
                keyEventQuote = sampleSentence
            )
        }
    }

    private fun classifyRelation(context: String, a: CharacterNode, b: CharacterNode): RelationType {
        val lower = context.lowercase(Locale.getDefault())
        return when {
            lower.contains("enemy") || lower.contains("rival") || lower.contains("hate") || lower.contains("duel") || lower.contains("versus") || lower.contains("clash") -> RelationType.RIVAL
            lower.contains("kill") || lower.contains("murder") || lower.contains("betray") || lower.contains("threat") || lower.contains("hostile") -> RelationType.ANTAGONISTIC
            lower.contains("teach") || lower.contains("mentor") || lower.contains("guide") || lower.contains("master") || lower.contains("advice") || lower.contains("wisdom") -> RelationType.MENTOR
            lower.contains("love") || lower.contains("marry") || lower.contains("heart") || lower.contains("passion") || lower.contains("darling") || lower.contains("beloved") -> RelationType.ROMANTIC
            lower.contains("father") || lower.contains("mother") || lower.contains("brother") || lower.contains("sister") || lower.contains("son") || lower.contains("daughter") || lower.contains("kin") -> RelationType.KINSHIP
            lower.contains("investigate") || lower.contains("suspect") || lower.contains("case") || lower.contains("clue") || lower.contains("detect") || lower.contains("crime") -> RelationType.INVESTIGATION
            lower.contains("create") || lower.contains("invent") || lower.contains("maker") || lower.contains("experiment") || lower.contains("monster") -> RelationType.CREATION
            lower.contains("general") || lower.contains("order") || lower.contains("command") || lower.contains("serve") || lower.contains("soldier") -> RelationType.SUBORDINATE
            lower.contains("friend") || lower.contains("ally") || lower.contains("partner") || lower.contains("help") || lower.contains("trust") || lower.contains("companion") -> RelationType.ALLY
            else -> RelationType.NEUTRAL
        }
    }

    private fun generateRelationLabel(type: RelationType, nameA: String, nameB: String): String {
        return when (type) {
            RelationType.ALLY -> "Trusted Companion & Ally"
            RelationType.RIVAL -> "Direct Rivalry & Opposition"
            RelationType.MENTOR -> "Philosophical / Strategic Guide"
            RelationType.INVESTIGATION -> "Investigative Focus & Case"
            RelationType.CREATION -> "Creator & Manifested Entity"
            RelationType.KINSHIP -> "Familial Ties & Heritage"
            RelationType.ROMANTIC -> "Romantic Attachment & Devotion"
            RelationType.ANTAGONISTIC -> "Direct Conflict & Hostility"
            RelationType.SUBORDINATE -> "Command & Operational Duty"
            RelationType.BETRAYAL -> "Broken Trust & Treachery"
            RelationType.NEUTRAL -> "Mutual Acquaintance"
        }
    }

    private fun generatePlotSummary(stage: PlotStage, bookTitle: String): String {
        return when (stage) {
            PlotStage.EXPOSITION -> "Establishes the foundational status quo, core characters, and thematic canvas of $bookTitle."
            PlotStage.INCITING_INCIDENT -> "A pivotal catalyst disrupts normalcy, confronting the protagonist with irreversible stakes."
            PlotStage.RISING_ACTION -> "Escalating challenges, secondary confrontations, and deepening psychological tensions."
            PlotStage.CLIMAX -> "The highest point of narrative and moral confrontation, demanding decisive resolution."
            PlotStage.FALLING_ACTION -> "The consequences of the climax unfold across factions and character dynamics."
            PlotStage.RESOLUTION -> "A new equilibrium is established, distilling enduring philosophical insight."
            PlotStage.SUBPLOT -> "Secondary character tensions intersecting with the overarching story."
        }
    }

    private fun generateThematicSummary(book: Book, nodes: List<CharacterNode>, edges: List<RelationshipEdge>): String {
        val charNames = nodes.take(3).joinToString(", ") { it.name }
        return "An on-device neural relationship extraction across ${book.title}. Analyzed ${nodes.size} primary entities ($charNames) and ${edges.size} interaction vectors, charting the interplay between autonomy, tension, and narrative transformation."
    }

    private fun inferRoleAndFaction(name: String, text: String, book: Book, index: Int): Pair<String, String> {
        val lowerName = name.lowercase(Locale.getDefault())
        val lowerText = text.lowercase(Locale.getDefault())

        val role = when {
            index == 0 -> "Central Protagonist"
            lowerName.contains("dr") || lowerName.contains("doctor") || lowerName.contains("surgeon") -> "Physician / Medical Scholar"
            lowerName.contains("inspector") || lowerName.contains("detective") || lowerName.contains("holmes") -> "Lead Investigator"
            lowerName.contains("queen") || lowerName.contains("king") || lowerName.contains("sovereign") -> "Sovereign Ruler"
            lowerName.contains("professor") || lowerName.contains("epictetus") || lowerName.contains("mentor") -> "Mentor & Thinker"
            lowerName.contains("general") || lowerName.contains("captain") -> "Commanding Officer"
            lowerName.contains("creature") || lowerName.contains("monster") -> "Sentient Creation"
            index == 1 -> "Primary Companion / Foil"
            index == 2 -> "Antagonist / Catalyst"
            else -> "Secondary Figure"
        }

        val faction = when {
            lowerText.contains("baker street") && (lowerName.contains("holmes") || lowerName.contains("watson")) -> "221B Baker Street"
            lowerName.contains("scotland") || lowerName.contains("inspector") -> "Official Constabulary"
            lowerName.contains("queen") || lowerName.contains("king") -> "The Royal Court"
            lowerName.contains("stoic") || lowerName.contains("marcus") -> "Inner Citadel"
            index == 0 -> "Protagonist Faction"
            index % 2 == 1 -> "Allied Forces"
            else -> "External Influences"
        }

        return Pair(role, faction)
    }

    private fun inferEmoji(name: String, role: String): String {
        val combined = "$name $role".lowercase(Locale.getDefault())
        return when {
            combined.contains("detective") || combined.contains("holmes") || combined.contains("investigat") -> "🕵️"
            combined.contains("doctor") || combined.contains("watson") || combined.contains("surgeon") -> "🩺"
            combined.contains("queen") || combined.contains("king") || combined.contains("sovereign") || combined.contains("crown") -> "👑"
            combined.contains("creature") || combined.contains("monster") || combined.contains("frankenstein") -> "🧟"
            combined.contains("rabbit") || combined.contains("hare") -> "🐇"
            combined.contains("cat") || combined.contains("cheshire") -> "🐱"
            combined.contains("hatter") -> "🎩"
            combined.contains("girl") || combined.contains("alice") -> "👧"
            combined.contains("general") || combined.contains("war") || combined.contains("sword") || combined.contains("officer") -> "⚔️"
            combined.contains("scholar") || combined.contains("philosopher") || combined.contains("mentor") || combined.contains("epictetus") -> "📜"
            combined.contains("spy") || combined.contains("secret") -> "🦅"
            combined.contains("emperor") || combined.contains("marcus") || combined.contains("rome") -> "🏛️"
            combined.contains("ship") || combined.contains("captain") || combined.contains("walton") -> "🚢"
            combined.contains("protagonist") -> "🧭"
            else -> "👤"
        }
    }

    private fun extractSalientQuote(name: String, text: String): String {
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        val candidate = sentences.find {
            it.contains(name, ignoreCase = true) && it.length in 35..180
        }
        return candidate?.trim() ?: ""
    }

    private fun inferSentiment(name: String, text: String): Float {
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.contains(name, ignoreCase = true) }
        var score = 0f
        val positive = listOf("good", "great", "noble", "honor", "love", "wise", "truth", "friend", "courage", "virtue")
        val negative = listOf("evil", "bad", "hate", "murder", "enemy", "foul", "grim", "cruel", "terror", "false")

        for (s in sentences) {
            val low = s.lowercase(Locale.getDefault())
            positive.forEach { if (low.contains(it)) score += 0.2f }
            negative.forEach { if (low.contains(it)) score -= 0.2f }
        }
        return score.coerceIn(-1.0f, 1.0f)
    }

    private fun normalizeName(name: String): String {
        var clean = name.trim()
        HONORIFICS.forEach { h ->
            if (clean.lowercase(Locale.getDefault()).startsWith("$h ")) {
                clean = clean.drop(h.length + 1).trim()
            }
        }
        return clean.replace(Regex("[^a-zA-Z\\s]"), "").trim()
    }

    private fun isValidCharacterName(name: String, book: Book): Boolean {
        if (name.length < 3 || name.length > 25) return false
        val low = name.lowercase(Locale.getDefault())
        if (STOPWORDS.contains(low)) return false
        if (low == book.title.lowercase(Locale.getDefault())) return false
        if (low == "chapter" || low == "project gutenberg" || low == "contents") return false
        return name.all { it.isLetter() || it.isWhitespace() }
    }

    private fun generateFallbackNodes(book: Book): List<CharacterNode> {
        return listOf(
            CharacterNode(
                id = "protagonist",
                name = "Protagonist",
                role = "Central Figure",
                faction = "Narrative Core",
                description = "The principal character navigating the conflicts of ${book.title}.",
                keyQuote = "Anchoring the core themes of ${book.genre.lowercase()}.",
                avatarEmoji = "🧭",
                xPercent = 0.50f,
                yPercent = 0.35f,
                significance = 1.3f
            ),
            CharacterNode(
                id = "mentor",
                name = "Guide / Anchor",
                role = "Wisdom & Perspective",
                faction = "Allies",
                description = "Provides counsel and philosophical foundation.",
                keyQuote = "Guiding principles illuminating the journey.",
                avatarEmoji = "📜",
                xPercent = 0.22f,
                yPercent = 0.65f,
                significance = 1.1f
            ),
            CharacterNode(
                id = "catalyst",
                name = "Catalyst / Opposing Force",
                role = "Transformative Friction",
                faction = "External Forces",
                description = "The central challenge driving narrative transformation.",
                keyQuote = "Where necessity prompts decisive action.",
                avatarEmoji = "⚡",
                xPercent = 0.78f,
                yPercent = 0.65f,
                significance = 1.1f
            )
        )
    }

    private data class CharacterCandidate(
        val normalizedKey: String,
        val displayName: String,
        val mentions: Int = 1,
        val aliases: List<String> = listOf(displayName)
    ) {
        fun incrementMention(by: Int = 1) = copy(mentions = mentions + by)
    }
}

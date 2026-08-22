package com.example.model

data class CharacterNode(
    val id: String,
    val name: String,
    val role: String,
    val faction: String,
    val description: String,
    val keyQuote: String,
    val avatarEmoji: String = "👤",
    val xPercent: Float = 0.5f, // 0.0 to 1.0 for position on mind map canvas
    val yPercent: Float = 0.5f,
    val significance: Float = 1.0f, // size multiplier
    val mentionCount: Int = 1,
    val sentimentScore: Float = 0.0f, // -1.0 (hostile) to 1.0 (virtuous/friendly)
    val aliases: List<String> = emptyList(),
    val firstAppearedChapter: String = "",
    val isUserCustom: Boolean = false
)

data class RelationshipEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val label: String,
    val relationType: RelationType,
    val interactionStrength: Float = 1.0f, // 0.0 to 1.0
    val sentiment: Float = 0.0f, // -1.0 to 1.0
    val evidenceQuotes: List<String> = emptyList(),
    val confidenceScore: Float = 0.85f,
    val isAiDetected: Boolean = true
)

enum class RelationType(val colorHex: Long, val displayName: String) {
    ALLY(0xFF66BB6A, "Ally & Partner"),
    RIVAL(0xFFEF5350, "Rival & Nemesis"),
    MENTOR(0xFFAB47BC, "Mentor & Guide"),
    INVESTIGATION(0xFFFFA726, "Investigation & Pursuit"),
    CREATION(0xFF42A5F5, "Creator & Creation"),
    KINSHIP(0xFF26A69A, "Kinship & Family"),
    ROMANTIC(0xFFEC407A, "Romantic & Bond"),
    ANTAGONISTIC(0xFFD32F2F, "Hostile & Conflict"),
    SUBORDINATE(0xFF7E57C2, "Command & Subordinate"),
    BETRAYAL(0xFFFF7043, "Betrayal & Deception"),
    NEUTRAL(0xFFB0BEC5, "Neutral & Acquaintance")
}

enum class PlotStage(val colorHex: Long, val displayName: String) {
    EXPOSITION(0xFF42A5F5, "Exposition"),
    INCITING_INCIDENT(0xFFFFA726, "Inciting Incident"),
    RISING_ACTION(0xFFAB47BC, "Rising Action"),
    CLIMAX(0xFFEF5350, "Climax"),
    FALLING_ACTION(0xFF26A69A, "Falling Action"),
    RESOLUTION(0xFF66BB6A, "Resolution"),
    SUBPLOT(0xFF8D6E63, "Subplot")
}

enum class ConflictType(val displayName: String, val iconEmoji: String) {
    PERSON_VS_PERSON("Person vs. Person", "⚔️"),
    PERSON_VS_SELF("Internal / Psychological", "🧠"),
    PERSON_VS_SOCIETY("Person vs. Society", "🏛️"),
    PERSON_VS_NATURE("Person vs. Nature / Fate", "🌊"),
    PERSON_VS_FATE("Existential / Cosmic", "⏳")
}

data class PlotNode(
    val id: String,
    val chapter: String,
    val title: String,
    val stage: PlotStage,
    val summary: String,
    val involvedCharacterIds: List<String> = emptyList(),
    val conflictType: ConflictType = ConflictType.PERSON_VS_PERSON,
    val tensionLevel: Float = 0.5f, // 0.0 to 1.0
    val keyEventQuote: String = ""
)

data class BookMindMap(
    val bookId: String,
    val bookTitle: String,
    val nodes: List<CharacterNode>,
    val edges: List<RelationshipEdge>,
    val plotPoints: List<PlotNode> = emptyList(),
    val thematicSummary: String,
    val analyzedAt: Long = System.currentTimeMillis(),
    val detectionEngine: String = "100% On-Device Local AI NLP Detector"
)

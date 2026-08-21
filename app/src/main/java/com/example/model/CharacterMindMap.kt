package com.example.model

data class CharacterNode(
    val id: String,
    val name: String,
    val role: String,
    val faction: String,
    val description: String,
    val keyQuote: String,
    val avatarEmoji: String = "👤",
    val xPercent: Float, // 0.0 to 1.0 for position on mind map canvas
    val yPercent: Float,
    val significance: Float = 1.0f // size multiplier
)

data class RelationshipEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val label: String,
    val relationType: RelationType
)

enum class RelationType(val colorHex: Long) {
    ALLY(0xFF66BB6A),
    RIVAL(0xFFEF5350),
    MENTOR(0xFFAB47BC),
    INVESTIGATION(0xFFFFA726),
    CREATION(0xFF42A5F5),
    KINSHIP(0xFF26A69A),
    NEUTRAL(0xFFB0BEC5)
}

data class BookMindMap(
    val bookId: String,
    val bookTitle: String,
    val nodes: List<CharacterNode>,
    val edges: List<RelationshipEdge>,
    val thematicSummary: String
)

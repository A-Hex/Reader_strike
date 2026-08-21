package com.example.model

data class VocabWord(
    val id: String,
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val definition: String,
    val exampleSentence: String,
    val bookTitle: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val isMastered: Boolean = false,
    val reviewCount: Int = 0
)

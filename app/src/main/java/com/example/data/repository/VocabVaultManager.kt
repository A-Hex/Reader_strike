package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.VocabWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class VocabVaultManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vocab_vault_prefs", Context.MODE_PRIVATE)

    private val _vocabWords = MutableStateFlow<List<VocabWord>>(emptyList())
    val vocabWords: StateFlow<List<VocabWord>> = _vocabWords.asStateFlow()

    init {
        loadSavedWords()
    }

    private fun loadSavedWords() {
        val jsonString = prefs.getString("saved_words_json", null)
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<VocabWord>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        VocabWord(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            word = obj.getString("word"),
                            phonetic = obj.optString("phonetic", ""),
                            partOfSpeech = obj.optString("partOfSpeech", "n."),
                            definition = obj.getString("definition"),
                            exampleSentence = obj.optString("exampleSentence", ""),
                            bookTitle = obj.optString("bookTitle", "Library"),
                            addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
                            isMastered = obj.optBoolean("isMastered", false),
                            reviewCount = obj.optInt("reviewCount", 0)
                        )
                    )
                }
                _vocabWords.value = list
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Seed initial rich vocabulary for reader exploration
        val starterList = listOf(
            VocabWord(
                id = "vocab-1",
                word = "Equanimity",
                phonetic = "/ˌek.wəˈnɪm.ə.ti/",
                partOfSpeech = "noun",
                definition = "Mental calmness, composure, and evenness of temper, especially in a difficult situation.",
                exampleSentence = "He accepted both victories and hardships with unwavering equanimity.",
                bookTitle = "Meditations",
                isMastered = false,
                reviewCount = 2
            ),
            VocabWord(
                id = "vocab-2",
                word = "Ratiocination",
                phonetic = "/ˌræʃ.i.ɒs.ɪˈneɪ.ʃən/",
                partOfSpeech = "noun",
                definition = "The exact process of methodical logical reasoning and deduction.",
                exampleSentence = "Sherlock Holmes employed keen ratiocination to unravel the crime.",
                bookTitle = "Sherlock Holmes",
                isMastered = true,
                reviewCount = 5
            ),
            VocabWord(
                id = "vocab-3",
                word = "Ephemeral",
                phonetic = "/ɪˈfem.ər.əl/",
                partOfSpeech = "adjective",
                definition = "Lasting for a very short, fleeting period of time.",
                exampleSentence = "Fame and transient worries are ephemeral compared to inner virtue.",
                bookTitle = "Meditations",
                isMastered = false,
                reviewCount = 1
            ),
            VocabWord(
                id = "vocab-4",
                word = "Stratagem",
                phonetic = "/ˈstræt.ə.dʒəm/",
                partOfSpeech = "noun",
                definition = "A plan or scheme, especially one used to outwit an opponent in strategy.",
                exampleSentence = "Supreme excellence consists in breaking the enemy's resistance without fighting through subtle stratagem.",
                bookTitle = "The Art of War",
                isMastered = false,
                reviewCount = 3
            )
        )
        _vocabWords.value = starterList
        persistWords(starterList)
    }

    private fun persistWords(list: List<VocabWord>) {
        try {
            val array = JSONArray()
            for (w in list) {
                val obj = JSONObject().apply {
                    put("id", w.id)
                    put("word", w.word)
                    put("phonetic", w.phonetic)
                    put("partOfSpeech", w.partOfSpeech)
                    put("definition", w.definition)
                    put("exampleSentence", w.exampleSentence)
                    put("bookTitle", w.bookTitle)
                    put("addedTimestamp", w.addedTimestamp)
                    put("isMastered", w.isMastered)
                    put("reviewCount", w.reviewCount)
                }
                array.put(obj)
            }
            prefs.edit().putString("saved_words_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addWord(vocabWord: VocabWord): VocabWord {
        val existing = _vocabWords.value.find { it.word.equals(vocabWord.word, ignoreCase = true) }
        if (existing != null) {
            return existing
        }
        val updated = listOf(vocabWord) + _vocabWords.value
        _vocabWords.value = updated
        persistWords(updated)
        return vocabWord
    }

    fun addWord(
        word: String,
        phonetic: String,
        partOfSpeech: String,
        definition: String,
        exampleSentence: String,
        bookTitle: String
    ): VocabWord {
        val existing = _vocabWords.value.find { it.word.equals(word, ignoreCase = true) }
        if (existing != null) {
            return existing
        }
        val newWord = VocabWord(
            id = UUID.randomUUID().toString(),
            word = word.trim().replaceFirstChar { it.uppercase() },
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            definition = definition,
            exampleSentence = exampleSentence,
            bookTitle = bookTitle,
            addedTimestamp = System.currentTimeMillis()
        )
        val updated = listOf(newWord) + _vocabWords.value
        _vocabWords.value = updated
        persistWords(updated)
        return newWord
    }

    fun toggleMastered(wordId: String) {
        val updated = _vocabWords.value.map {
            if (it.id == wordId) it.copy(isMastered = !it.isMastered, reviewCount = it.reviewCount + 1)
            else it
        }
        _vocabWords.value = updated
        persistWords(updated)
    }

    fun recordReview(wordId: String, remembered: Boolean) {
        val updated = _vocabWords.value.map {
            if (it.id == wordId) {
                it.copy(
                    reviewCount = it.reviewCount + 1,
                    isMastered = if (remembered && it.reviewCount >= 2) true else it.isMastered
                )
            } else it
        }
        _vocabWords.value = updated
        persistWords(updated)
    }

    fun deleteWord(wordId: String) {
        val updated = _vocabWords.value.filterNot { it.id == wordId }
        _vocabWords.value = updated
        persistWords(updated)
    }
}

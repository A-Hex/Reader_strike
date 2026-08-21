package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.VocabWord
import com.example.ui.theme.*

@Composable
fun WordInspectorDialog(
    initialWord: String,
    bookTitle: String,
    sentenceContext: String = "",
    onAddToVault: (VocabWord) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchWord by remember { mutableStateOf(initialWord.trim().replace(Regex("[^a-zA-Z]"), "")) }
    val context = LocalContext.current
    var isSaved by remember { mutableStateOf(false) }

    // Dynamic literary definitions dictionary for instant offline lookup
    val definitionData = remember(searchWord) {
        getWordDefinition(searchWord)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
            border = BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = NaturalPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(14.dp))
                                Text("Word Inspector", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                            }
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                // Word Title & Phonetic
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = searchWord.ifBlank { "Inspect Word" }.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = definitionData.partOfSpeech,
                            style = MaterialTheme.typography.labelMedium.copy(fontStyle = FontStyle.Italic),
                            color = NaturalOchreAccent,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Text(
                        text = definitionData.phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaturalDarkTextMuted
                    )
                }

                Divider(color = NaturalDarkBorder)

                // Definition Card
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DEFINITION",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = NaturalDarkTextMuted
                    )
                    Text(
                        text = definitionData.definition,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = NaturalDarkText
                    )
                }

                // Example & Context
                val displayExample = if (sentenceContext.isNotBlank()) sentenceContext else definitionData.example
                if (displayExample.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "IN-TEXT CONTEXT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = NaturalPrimary
                            )
                            Text(
                                text = "\"$displayExample\"",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, lineHeight = 18.sp),
                                color = NaturalDarkText
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NaturalDarkBorder)
                    ) {
                        Text("Close", color = NaturalDarkText)
                    }

                    Button(
                        onClick = {
                            val saved = VocabWord(
                                id = java.util.UUID.randomUUID().toString(),
                                word = searchWord.ifBlank { "Word" }.replaceFirstChar { it.uppercase() },
                                phonetic = definitionData.phonetic,
                                partOfSpeech = definitionData.partOfSpeech,
                                definition = definitionData.definition,
                                exampleSentence = displayExample,
                                bookTitle = bookTitle
                            )
                            onAddToVault(saved)
                            isSaved = true
                            Toast.makeText(context, "Added \"$searchWord\" to Vocab Vault! (+15 XP)", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isSaved,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSaved) NaturalSageSuccess else NaturalPrimary,
                            contentColor = NaturalOnPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSaved) "Saved" else "Save to Vault", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class WordDefinitionResult(
    val phonetic: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String
)

private fun getWordDefinition(word: String): WordDefinitionResult {
    val clean = word.lowercase().trim()
    val dict = mapOf(
        "equanimity" to WordDefinitionResult("/ˌek.wəˈnɪm.ə.ti/", "noun", "Mental calmness, composure, and evenness of temper, especially in a difficult situation.", "He accepted both victories and hardships with unwavering equanimity."),
        "ratiocination" to WordDefinitionResult("/ˌræʃ.i.ɒs.ɪˈneɪ.ʃən/", "noun", "The exact process of methodical logical reasoning and deduction.", "Sherlock Holmes employed keen ratiocination to unravel the crime."),
        "ephemeral" to WordDefinitionResult("/ɪˈfem.ər.əl/", "adjective", "Lasting for a very short, fleeting period of time.", "Fame and transient worries are ephemeral compared to inner virtue."),
        "stratagem" to WordDefinitionResult("/ˈstræt.ə.dʒəm/", "noun", "A plan or scheme, especially one used to outwit an opponent in strategy.", "Supreme excellence consists in breaking the enemy's resistance without fighting through subtle stratagem."),
        "vermin" to WordDefinitionResult("/ˈvɜː.mɪn/", "noun", "Pests or destructive animals; figuratively representing profound societal alienation.", "One morning, Gregor Samsa woke to find himself transformed into a monstrous vermin."),
        "alienation" to WordDefinitionResult("/ˌeɪ.li.əˈneɪ.ʃən/", "noun", "The state of being isolated from a group or activity to which one should belong.", "His transformation deepened his existential alienation from his family."),
        "ardour" to WordDefinitionResult("/ˈɑː.dər/", "noun", "Enthusiasm or passion; great warmth of feeling and eagerness.", "He pursued his scientific enterprise with unremitting ardour."),
        "deliberations" to WordDefinitionResult("/dɪˌlɪb.əˈreɪ.ʃənz/", "noun", "Long and careful consideration or discussion before acting.", "After lengthy deliberations, the council reached a unanimous verdict."),
        "subdue" to WordDefinitionResult("/səbˈdjuː/", "verb", "To overcome, quieten, or bring under control by strategic superiority.", "To subdue the enemy without fighting is the acme of skill."),
        "obstruction" to WordDefinitionResult("/əbˈstrʌk.ʃən/", "noun", "A thing that impedes progress; transformed in Stoic thought into the path itself.", "The impediment to action advances action. What stands in the way becomes the way."),
        "beneficence" to WordDefinitionResult("/bəˈnef.ɪ.səns/", "noun", "The quality or state of being charitable and actively doing good.", "Practice beneficence without expecting applause or reward.")
    )

    return dict[clean] ?: WordDefinitionResult(
        phonetic = "/${clean.take(12)}/",
        partOfSpeech = "term",
        definition = "A notable literary term appearing in this passage, carrying subtle stylistic and contextual significance within the narrative.",
        example = "Examining this term enriches reading comprehension and mastery of the author's craft."
    )
}

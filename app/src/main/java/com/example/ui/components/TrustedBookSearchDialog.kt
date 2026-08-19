package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class TrustedSource(
    val id: String,
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val urlTemplate: String,
    val isPdfSpecific: Boolean = false,
    val badge: String = "Verified"
)

@Composable
fun TrustedBookSearchDialog(
    initialQuery: String = "",
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf(initialQuery) }

    val sources = remember {
        listOf(
            TrustedSource(
                id = "google_books",
                name = "Google Books",
                description = "World's most comprehensive index of full-text books and previews",
                icon = Icons.Default.MenuBook,
                urlTemplate = "https://books.google.com/books?q=%s",
                badge = "Google Official"
            ),
            TrustedSource(
                id = "google_pdf",
                name = "Google (PDF Books)",
                description = "Direct Google Search filtered specifically for indexable .PDF files",
                icon = Icons.Default.PictureAsPdf,
                urlTemplate = "https://www.google.com/search?q=%s+filetype:pdf",
                isPdfSpecific = true,
                badge = "PDF Direct"
            ),
            TrustedSource(
                id = "gutenberg",
                name = "Project Gutenberg",
                description = "Over 70,000 free public domain EPUB and Kindle eBooks",
                icon = Icons.Default.AutoStories,
                urlTemplate = "https://www.gutenberg.org/ebooks/search/?query=%s",
                badge = "70k+ Free Books"
            ),
            TrustedSource(
                id = "open_library",
                name = "Internet Archive Open Library",
                description = "Non-profit digital library offering millions of free books and documents",
                icon = Icons.Default.Public,
                urlTemplate = "https://openlibrary.org/search?q=%s",
                badge = "Non-profit Archive"
            ),
            TrustedSource(
                id = "standard_ebooks",
                name = "Standard Ebooks",
                description = "Free, carefully formatted modern editions of public domain classics",
                icon = Icons.Default.Verified,
                urlTemplate = "https://standardebooks.org/ebooks?query=%s",
                badge = "High Typography"
            ),
            TrustedSource(
                id = "wikisource",
                name = "Wikisource Library",
                description = "The free library of source texts in all languages",
                icon = Icons.Default.ImportContacts,
                urlTemplate = "https://en.wikisource.org/w/index.php?search=%s",
                badge = "Multilingual"
            )
        )
    }

    var selectedSourceId by remember { mutableStateOf<String?>("all") }

    fun openSourceSearch(source: TrustedSource, searchQuery: String) {
        val encoded = URLEncoder.encode(searchQuery.trim().ifEmpty { "classic books" }, StandardCharsets.UTF_8.toString())
        val url = String.format(source.urlTemplate, encoded)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NaturalPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = NaturalOnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = AppStrings.get("search_trusted_title", currentLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Google, PDFs & Verified Libraries",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalOchreAccent
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                // Query Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(AppStrings.get("search_hint", currentLanguage), fontSize = 13.sp, color = NaturalDarkTextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = NaturalPrimary)
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = NaturalDarkTextMuted)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = NaturalPrimary,
                        unfocusedBorderColor = NaturalDarkBorder
                    )
                )

                // Quick Popular Search Tags
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val popular = listOf("Philosophy PDF", "Marcus Aurelius", "The Great Gatsby", "Psychology of Money", "Machine Learning PDF", "Dune PDF")
                    items(popular) { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { query = tag }
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Trusted Sources List
                Text(
                    text = "Search across verified providers:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NaturalDarkTextMuted
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sources) { source ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openSourceSearch(source, query) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (source.isPdfSpecific) Color(0xFFEF4444).copy(alpha = 0.2f) else NaturalPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = source.icon,
                                        contentDescription = null,
                                        tint = if (source.isPdfSpecific) Color(0xFFEF4444) else NaturalPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = source.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            color = NaturalPrimary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = source.badge,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = NaturalPrimary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = source.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NaturalDarkTextMuted,
                                        maxLines = 2
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Search",
                                    tint = NaturalOchreAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Footer Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val googlePdfSource = sources.find { it.id == "google_pdf" } ?: sources[1]
                            openSourceSearch(googlePdfSource, query)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search Google PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val googleBooksSource = sources.find { it.id == "google_books" } ?: sources[0]
                            openSourceSearch(googleBooksSource, query)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google Books", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

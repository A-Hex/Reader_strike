package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel

@Composable
fun SmartBookSearchSheet(
    viewModel: MainViewModel,
    initialQuery: String = "",
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onOpenBookInReader: ((Book) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf(initialQuery) }
    var selectedSourceFilter by remember { mutableStateOf("All Sources") }

    val searchUiState by viewModel.onlineSearchUiState.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()

    var selectedResultForDetails by remember { mutableStateOf<SearchBookResult?>(null) }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            kotlinx.coroutines.delay(350)
            viewModel.searchOnlineBooks(query)
        } else {
            viewModel.clearOnlineSearch()
        }
    }

    val sourceOptions = listOf("All Sources", "Noor Book (مكتبة نور)", "Project Gutenberg", "Open Library & Archive")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                text = "Smart Book Search & Download",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Noor Book (www.noor-book.com) & Project Gutenberg",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalOchreAccent
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search by title, author, topic, or ISBN...", fontSize = 13.sp, color = NaturalDarkTextMuted) },
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

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(sourceOptions) { opt ->
                        val isSelected = selectedSourceFilter == opt
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSourceFilter = opt },
                            label = { Text(opt, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaturalPrimary,
                                selectedLabelColor = NaturalOnPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Download Progress Feedback if active
                if (downloadStatus is DownloadStatus.Downloading) {
                    val status = downloadStatus as DownloadStatus.Downloading
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NaturalPrimary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = status.statusMessage,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalPrimary
                                )
                                Text(
                                    text = "${(status.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalPrimary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { status.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = NaturalPrimary,
                                trackColor = NaturalDarkBorder
                            )
                        }
                    }
                }

                // Results Body
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = searchUiState) {
                        is SearchUiState.Idle -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = NaturalDarkTextMuted.copy(alpha = 0.6f),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Discover millions of free books",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Type any book title, author (e.g. 'Dostoevsky', 'Marcus Aurelius', 'Gatsby') or topic to search live indexes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalDarkTextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val quicks = listOf("مقدمة ابن خلدون", "Jane Austen", "كليلة ودمنة", "Sherlock Holmes")
                                    quicks.forEach { q ->
                                        FilledTonalButton(
                                            onClick = { query = q },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(q, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        is SearchUiState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = NaturalPrimary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Searching verified book indexes...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                        }
                        is SearchUiState.Empty -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, tint = NaturalDarkTextMuted, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = AppStrings.get("search_no_matching_books", currentLanguage),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = AppStrings.get("search_try_different_keywords", currentLanguage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalDarkTextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = AppStrings.get("search_popular_suggestions_title", currentLanguage),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val quickSuggestions = listOf("Pride and Prejudice", "مقدمة ابن خلدون", "Sherlock Holmes", "كليلة ودمنة", "Frankenstein", "الأيام لطه حسين", "The Time Machine")
                                    items(quickSuggestions) { s ->
                                        FilledTonalButton(
                                            onClick = { query = s },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(s, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        is SearchUiState.Error -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Search Unavailable", style = MaterialTheme.typography.titleSmall, color = Color(0xFFEF4444))
                                Text(state.message, style = MaterialTheme.typography.bodySmall, color = NaturalDarkTextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                        is SearchUiState.Success -> {
                            val filtered = remember(state.results, selectedSourceFilter) {
                                if (selectedSourceFilter == "All Sources") {
                                    state.results
                                } else {
                                    state.results.filter { it.source == selectedSourceFilter }
                                }
                            }

                            if (filtered.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAltOff,
                                        contentDescription = null,
                                        tint = NaturalDarkTextMuted,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No books found in $selectedSourceFilter",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Found ${state.results.size} books across other sources.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NaturalDarkTextMuted
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FilledTonalButton(
                                        onClick = { selectedSourceFilter = "All Sources" },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(AppStrings.get("search_all_sources_btn", currentLanguage, state.results.size))
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filtered, key = { it.stableId }) { item ->
                                        SearchResultCard(
                                            result = item,
                                            onSelect = { selectedResultForDetails = item },
                                            onDownloadClick = {
                                                viewModel.downloadSearchResult(item)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet / Dialog for Selected Search Result
    selectedResultForDetails?.let { result ->
        SearchBookDetailsDialog(
            result = result,
            onDismiss = { selectedResultForDetails = null },
            onDownload = {
                viewModel.downloadSearchResult(result) { downloadedBook ->
                    selectedResultForDetails = null
                    onOpenBookInReader?.invoke(downloadedBook)
                }
            }
        )
    }
}

@Composable
fun SearchResultCard(
    result: SearchBookResult,
    onSelect: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NaturalPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!result.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(result.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover of ${result.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = NaturalPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Information
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Text(
                    text = result.authorDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Source badge
                    Surface(
                        color = NaturalPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = result.source,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = NaturalPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    // Availability badge
                    Surface(
                        color = when (result.availability) {
                            BookAvailability.AVAILABLE_DOWNLOAD -> NaturalSageAccent.copy(alpha = 0.2f)
                            BookAvailability.PREVIEW_ONLY -> NaturalOchreAccent.copy(alpha = 0.2f)
                            BookAvailability.METADATA_ONLY -> Color.Gray.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = result.availability.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (result.availability) {
                                    BookAvailability.AVAILABLE_DOWNLOAD -> NaturalSageAccent
                                    BookAvailability.PREVIEW_ONLY -> NaturalOchreAccent
                                    BookAvailability.METADATA_ONLY -> NaturalDarkTextMuted
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Action
            if (result.isAlreadyInLibrary) {
                Surface(
                    color = NaturalSageBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = NaturalSageAccent, modifier = Modifier.size(14.dp))
                        Text("Saved", fontSize = 10.sp, color = NaturalSageAccent, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(NaturalPrimary.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (result.downloadUrl != null) Icons.Default.Download else Icons.Default.Add,
                        contentDescription = "Add or Download",
                        tint = NaturalPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBookDetailsDialog(
    result: SearchBookResult,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Book Details & Sources",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Cover
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NaturalPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!result.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(result.coverUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Cover for ${result.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(32.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = result.authorDisplay,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaturalDarkTextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Source: ${result.source}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalPrimary
                        )
                        if (result.publishedYear != null) {
                            Text(
                                text = "Published: ${result.publishedYear}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                        }
                        Text(
                            text = "Format: ${result.format.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalOchreAccent
                        )
                    }
                }

                Divider(color = NaturalDarkBorder.copy(alpha = 0.5f))

                // Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Description & Overview",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = result.description.ifBlank { "No detailed synopsis available for this public record." },
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted,
                        lineHeight = 18.sp,
                        modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (result.previewUrl != null) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.previewUrl)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Web Source", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = NaturalOnPrimary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (result.downloadUrl != null) "Download & Read" else "Save to Library",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

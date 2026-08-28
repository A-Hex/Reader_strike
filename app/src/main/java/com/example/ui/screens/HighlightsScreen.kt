package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Highlight
import com.example.model.HighlightColor
import com.example.ui.components.ShareContentType
import com.example.ui.components.SocialShareModal
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val highlights by viewModel.allHighlights.collectAsState()
    var selectedColorFilter by remember { mutableStateOf<HighlightColor?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var highlightToShare by remember { mutableStateOf<Highlight?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (highlightToShare != null) {
        SocialShareModal(
            contentType = ShareContentType.HighlightQuote(highlightToShare!!),
            onDismiss = { highlightToShare = null }
        )
    }

    val filteredHighlights = remember(highlights, selectedColorFilter, searchQuery) {
        highlights.filter { hl ->
            val matchesColor = selectedColorFilter == null || hl.color == selectedColorFilter
            val matchesQuery = searchQuery.isBlank() ||
                    hl.text.contains(searchQuery, ignoreCase = true) ||
                    (hl.note != null && hl.note.contains(searchQuery, ignoreCase = true)) ||
                    hl.bookTitle.contains(searchQuery, ignoreCase = true)
            matchesColor && matchesQuery
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Highlights & Notes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${highlights.size} annotations in your library",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                    }

                    // Export to Markdown Button
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                val md = viewModel.bookRepository.exportHighlightsToMarkdown()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("A-Hex streak Highlights", md)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Exported Markdown copied to clipboard!", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NaturalDarkSurfaceElevated,
                            contentColor = NaturalPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export MD", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search quotes, notes, or books...", color = NaturalDarkTextMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NaturalDarkTextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = NaturalDarkBorder,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Color Filter Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedColorFilter == null,
                            onClick = { selectedColorFilter = null },
                            label = { Text("All", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaturalDarkSurfaceElevated,
                                selectedLabelColor = NaturalPrimary
                            )
                        )
                    }
                    items(HighlightColor.entries) { colorOption ->
                        val isSelected = selectedColorFilter == colorOption
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedColorFilter = if (isSelected) null else colorOption
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(colorOption.toComposeColor())
                                )
                            },
                            label = { Text(colorOption.displayName, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaturalDarkSurfaceElevated,
                                selectedLabelColor = NaturalPrimary
                            )
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredHighlights.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Highlight,
                        contentDescription = null,
                        tint = NaturalDarkTextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Highlights Found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Highlight quotes while reading to collect and organize ideas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredHighlights, key = { it.id }) { hl ->
                        HighlightCard(
                            highlight = hl,
                            onShare = { highlightToShare = hl },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Quote", "\"${hl.text}\" - ${hl.bookTitle}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Quote copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { viewModel.deleteHighlight(hl.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightCard(
    highlight: Highlight,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(highlight.timestamp))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Book & Chapter Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(highlight.color.toComposeColor())
                    )
                    Text(
                        text = highlight.bookTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalPrimary
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = NaturalDarkTextMuted
                )
            }

            // Quote Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        NaturalDarkBackground,
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, NaturalDarkBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "\"${highlight.text}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 22.sp
                    )
                )
            }

            // Note (if exists)
            if (!highlight.note.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = NaturalOchreAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = highlight.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkText
                    )
                }
            }

            // Bottom Actions (Chapter info, Share, Copy, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${highlight.chapterTitle} • ${highlight.color.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaturalDarkTextMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share quote", tint = NaturalPrimary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NaturalDarkTextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}


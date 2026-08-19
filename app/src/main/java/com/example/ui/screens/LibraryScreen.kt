package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.ui.components.BookGridCard
import com.example.ui.components.BookListCard
import com.example.ui.components.BookCoverImage
import com.example.ui.components.BookReviewsSheet
import com.example.ui.components.ShareContentType
import com.example.ui.components.SocialShareModal
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToDiscover: () -> Unit,
    onNavigateToStreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val books by viewModel.filteredBooks.collectAsState()
    val allBooks by viewModel.allBooks.collectAsState()
    val highlights by viewModel.allHighlights.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val streakData by viewModel.streakData.collectAsState()
    val allReviews by viewModel.allReviews.collectAsState()

    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedBookForReviews by remember { mutableStateOf<Book?>(null) }
    var bookToShare by remember { mutableStateOf<Book?>(null) }

    // Find the most recently active or reading book for the Natural Tones Hero banner
    val currentlyReadingBook = remember(allBooks) {
        allBooks.firstOrNull { it.readingProgress > 0f && it.readingProgress < 1f }
            ?: allBooks.firstOrNull()
    }

    // File picker launcher for PDF / EPUB / TXT
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var displayName = "Imported_Book"
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        displayName = it.getString(nameIndex)
                    }
                }
            }
            viewModel.importDocument(uri, displayName)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // App Natural Tones Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NaturalPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = "A-Hex streak logo",
                                tint = NaturalOnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "A-Hex streak",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                            )
                            Text(
                                text = "CLOUD SYNCHRONIZED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    fontSize = 9.sp
                                ),
                                color = NaturalSecondary
                            )
                        }
                    }

                    // Top Action Icons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.triggerSync() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NaturalDarkSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToDiscover,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NaturalDarkSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Discover",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search title, author, genre, tag...", color = NaturalDarkTextMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NaturalDarkTextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = NaturalDarkBorder,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Shelf Filter Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ReadingStatus.entries) { status ->
                        val isSelected = selectedStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedStatus(status) },
                            label = { Text(status.displayName, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaturalDarkSurfaceElevated,
                                selectedLabelColor = NaturalPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) NaturalDarkBorder else Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Format filter pills and Sort/View toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedFormat == null,
                            onClick = { viewModel.setSelectedFormat(null) },
                            label = { Text("All", fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaturalDarkSurfaceElevated,
                                selectedLabelColor = NaturalPrimary
                            )
                        )
                        BookFormat.entries.forEach { format ->
                            FilterChip(
                                selected = selectedFormat == format,
                                onClick = {
                                    viewModel.setSelectedFormat(if (selectedFormat == format) null else format)
                                },
                                label = { Text(format.displayName, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NaturalDarkSurfaceElevated,
                                    selectedLabelColor = NaturalPrimary
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Sort button
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = NaturalDarkTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (selectedSort == option) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = NaturalPrimary)
                                                }
                                                Text(option.displayName)
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSelectedSort(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // View mode toggle (Grid vs List)
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View",
                                tint = NaturalDarkTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Public Domain catalog shortcut
                SmallFloatingActionButton(
                    onClick = onNavigateToDiscover,
                    containerColor = NaturalDarkSurfaceElevated,
                    contentColor = NaturalPrimary,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = "Download E-Books")
                }

                // Import local file FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/epub+zip",
                                "text/plain",
                                "application/octet-stream"
                            )
                        )
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Import E-Book", fontWeight = FontWeight.Bold) },
                    containerColor = NaturalPrimary,
                    contentColor = NaturalOnPrimary,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Natural Tones Hero: Currently Reading Banner (shown when not searching)
            if (searchQuery.isEmpty() && currentlyReadingBook != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { viewModel.openBook(currentlyReadingBook) },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Surface(
                                        color = NaturalDarkBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "CURRENTLY READING",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = NaturalPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentlyReadingBook.title,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${currentlyReadingBook.author} • ${currentlyReadingBook.format.displayName}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ),
                                        color = NaturalDarkTextMuted
                                    )
                                }

                                // Book Cover mini-preview
                                BookCoverImage(
                                    book = currentlyReadingBook,
                                    modifier = Modifier.size(width = 62.dp, height = 90.dp),
                                    cornerRadius = 10.dp,
                                    showFormatBadge = true,
                                    showFavoriteBadge = false,
                                    elevation = 4.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Progress indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Progress", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = NaturalDarkTextMuted)
                                Text("${(currentlyReadingBook.readingProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NaturalDarkBackground)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = currentlyReadingBook.readingProgress.coerceIn(0.02f, 1f))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NaturalPrimary)
                                )
                            }
                        }
                    }
                }

                // Natural Tones Earth Accent Dual Stat Grid (Streak & Highlights)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Reading Streak in Sage Green Natural Tone
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onNavigateToStreak() },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = NaturalSageBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = NaturalSageAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Reading Streak",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = NaturalSageAccent
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${streakData.currentStreakDays}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = NaturalSageAccent
                                        )
                                    )
                                    Text(
                                        text = "days",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalSageMuted,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }

                        // Highlights in Warm Sand / Ochre Natural Tone
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = NaturalOchreBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOchreBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Icon(
                                        imageVector = Icons.Default.HistoryEdu,
                                        contentDescription = null,
                                        tint = NaturalOchreAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Highlights",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = NaturalOchreAccent
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${highlights.size}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = NaturalOchreAccent
                                        )
                                    )
                                    Text(
                                        text = "total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalOchreMuted,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Local Library Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Local Library (${books.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = NaturalDarkTextMuted
                        )
                    )
                }
            }

            // Books List or Empty State
            if (books.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = NaturalDarkTextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching books found" else "No books in this shelf",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try searching for a different title or keyword" else "Import local EPUB/PDF/TXT files or browse the free classics store.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalDarkTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToDiscover,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = NaturalOnPrimary)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Browse Free Classics", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(books, key = { it.id }) { book ->
                    BookListCard(
                        book = book,
                        onClick = { viewModel.openBook(book) },
                        onToggleFavorite = { viewModel.toggleFavorite(book) },
                        onDelete = { viewModel.deleteBook(book.id) },
                        onOpenReviews = { selectedBookForReviews = book },
                        onShareProgress = { bookToShare = book }
                    )
                }
            }
        }
    }

    // Book Reviews Sheet
    selectedBookForReviews?.let { book ->
        val bookReviews = allReviews.filter { it.bookId == book.id }
        BookReviewsSheet(
            book = book,
            reviews = bookReviews,
            onDismiss = { selectedBookForReviews = null },
            onSubmitReview = { rating, title, text, userName ->
                viewModel.submitReview(book.id, book.title, rating, title, text, userName)
            },
            onDeleteReview = { id -> viewModel.deleteReview(id) },
            onHelpfulClick = { id -> viewModel.incrementReviewHelpful(id) }
        )
    }

    // Social Share Modal for Book Progress
    bookToShare?.let { book ->
        SocialShareModal(
            contentType = ShareContentType.BookProgress(book, streakData),
            onDismiss = { bookToShare = null }
        )
    }
}


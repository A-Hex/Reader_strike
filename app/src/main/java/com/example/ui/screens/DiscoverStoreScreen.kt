package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Book
import com.example.model.BookRecommendation
import com.example.ui.components.BookCoverImage
import com.example.ui.components.BookReviewsSheet
import com.example.ui.components.EpubConverterModal
import com.example.ui.components.TrustedBookSearchDialog
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel

@Composable
fun DiscoverStoreScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catalogBooks by viewModel.catalogBooks.collectAsState()
    val downloadedBooks by viewModel.allBooks.collectAsState()
    val recommendations by viewModel.bookRecommendations.collectAsState()
    val allReviews by viewModel.allReviews.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val downloadedIds = remember(downloadedBooks) { downloadedBooks.map { it.id }.toSet() }

    var selectedBookForReviews by remember { mutableStateOf<Book?>(null) }
    var selectedGenreFilter by remember { mutableStateOf("All") }
    var showTrustedSearchDialog by remember { mutableStateOf(false) }
    var bookToConvertToEpub by remember { mutableStateOf<Book?>(null) }

    val genres = remember(catalogBooks) {
        listOf("All") + catalogBooks.map { it.genre }.distinct()
    }

    val filteredCatalog = remember(catalogBooks, selectedGenreFilter) {
        if (selectedGenreFilter == "All") {
            catalogBooks
        } else {
            catalogBooks.filter { it.genre == selectedGenreFilter }
        }
    }

    // Reviews Sheet Dialog
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp)
    ) {
        // Hero Banner in Natural Tones
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_reading),
                        contentDescription = "Discover Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, NaturalDarkBackground.copy(alpha = 0.95f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "A-Hex Smart Discovery & Catalog",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NaturalDarkText
                            )
                        )
                        Text(
                            text = "Curated public domain classics with AI recommendation engine",
                            style = MaterialTheme.typography.bodySmall.copy(color = NaturalDarkTextMuted)
                        )
                    }
                }
            }
        }

        // Online Search Engine Card (Noor Book for Arabic & Gutenberg for English)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTrustedSearchDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NaturalPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = null,
                            tint = NaturalOnPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Online Book Search (Noor Book & Gutenberg)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Search www.noor-book.com for Arabic books and gutenberg.org for English classics",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalDarkTextMuted
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { showTrustedSearchDialog = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = NaturalPrimary.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Open Search", tint = NaturalPrimary)
                    }
                }
            }
        }

        // Section 1: AI Book Recommendations
        if (recommendations.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = NaturalPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Recommended For You",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Based on reading history, genres & highlighted quotes",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                        }
                    }

                    Surface(
                        color = NaturalPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Smart Engine",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            items(recommendations, key = { it.book.id }) { rec ->
                val isAlreadyInLibrary = downloadedIds.contains(rec.book.id)
                RecommendationCard(
                    recommendation = rec,
                    isAlreadyInLibrary = isAlreadyInLibrary,
                    onDownload = { viewModel.downloadBook(rec.book) },
                    onOpenReviews = { selectedBookForReviews = rec.book },
                    onConvertToEpub = { bookToConvertToEpub = rec.book }
                )
            }
        }

        // Section 2: Complete Catalog & Genre Filter
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Full Book Catalog (PDF & EPUB)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Genre Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(genres) { genre ->
                        val isSelected = selectedGenreFilter == genre
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGenreFilter = genre },
                            label = { Text(genre, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaturalPrimary,
                                selectedLabelColor = NaturalOnPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Catalog List
        items(filteredCatalog, key = { it.id }) { book ->
            val isAlreadyInLibrary = downloadedIds.contains(book.id)
            val reviewsForBook = allReviews.filter { it.bookId == book.id }
            val avgRating = if (reviewsForBook.isNotEmpty()) reviewsForBook.map { it.rating }.average().toFloat() else book.rating

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cover Thumbnail
                        BookCoverImage(
                            book = book,
                            modifier = Modifier.size(width = 65.dp, height = 90.dp),
                            cornerRadius = 10.dp,
                            showFormatBadge = true,
                            showFavoriteBadge = false,
                            elevation = 3.dp
                        )

                        // Metadata
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalDarkText
                            )
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalDarkTextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalDarkTextMuted,
                                maxLines = 2,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = NaturalOchreAccent, modifier = Modifier.size(14.dp))
                                Text(
                                    text = String.format("%.1f", avgRating),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalOchreAccent
                                )
                                Text(
                                    text = "• ${book.totalPages}p • ${book.fileSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                        }
                    }

                    // Bottom Row: Reviews Button + Convert to EPUB + Download Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedBookForReviews = book },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(15.dp), tint = NaturalPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reviews",
                                    fontSize = 11.sp,
                                    color = NaturalPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = { bookToConvertToEpub = book },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Transform,
                                    contentDescription = "Convert to EPUB",
                                    tint = NaturalPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (isAlreadyInLibrary) {
                            FilledTonalButton(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    disabledContainerColor = NaturalSageBg,
                                    disabledContentColor = NaturalSageAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NaturalSageAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("In Library", fontSize = 11.sp, color = NaturalSageAccent, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.downloadBook(book) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = NaturalOnPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download ${book.format.displayName}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTrustedSearchDialog) {
        TrustedBookSearchDialog(
            viewModel = viewModel,
            initialQuery = "",
            currentLanguage = currentLanguage,
            onDismiss = { showTrustedSearchDialog = false },
            onOpenBookInReader = { book ->
                showTrustedSearchDialog = false
                viewModel.openBook(book)
            }
        )
    }

    // EPUB Converter Modal
    bookToConvertToEpub?.let { book ->
        val highlights by viewModel.allHighlights.collectAsState()
        EpubConverterModal(
            book = book,
            highlights = highlights.filter { it.bookId == book.id },
            onOpenConvertedBook = { convertedBook ->
                viewModel.openBook(convertedBook)
            },
            onImportEpubToLibrary = { file, title, author ->
                viewModel.addConvertedEpubToLibrary(file, title, author, autoOpen = false)
            },
            onDismiss = { bookToConvertToEpub = null }
        )
    }
}

@Composable
fun RecommendationCard(
    recommendation: BookRecommendation,
    isAlreadyInLibrary: Boolean,
    onDownload: () -> Unit,
    onOpenReviews: () -> Unit,
    onConvertToEpub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val book = recommendation.book

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Recommendation Match Badge Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NaturalPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${recommendation.matchScorePercent}% Match",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary
                        )
                    }
                }

                Text(
                    text = recommendation.matchedGenre,
                    style = MaterialTheme.typography.labelSmall,
                    color = NaturalDarkTextMuted
                )
            }

            // Reason Text
            Text(
                text = "💡 ${recommendation.matchReason}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = NaturalOchreAccent
            )

            // Book Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BookCoverImage(
                    book = book,
                    modifier = Modifier.size(width = 58.dp, height = 80.dp),
                    cornerRadius = 8.dp,
                    showFormatBadge = true,
                    showFavoriteBadge = false,
                    elevation = 2.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalDarkText
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${book.totalPages} pages • ${book.fileSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onOpenReviews,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp), tint = NaturalPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reviews", fontSize = 12.sp, color = NaturalPrimary)
                    }

                    IconButton(
                        onClick = onConvertToEpub,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Transform,
                            contentDescription = "Convert to EPUB",
                            tint = NaturalPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isAlreadyInLibrary) {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            disabledContainerColor = NaturalSageBg,
                            disabledContentColor = NaturalSageAccent
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = NaturalSageAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("In Library", fontSize = 11.sp, color = NaturalSageAccent)
                    }
                } else {
                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = NaturalOnPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download & Read", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

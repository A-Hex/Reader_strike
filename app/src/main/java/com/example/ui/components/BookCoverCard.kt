package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.ui.theme.*

@Composable
fun BookGridCard(
    book: Book,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onOpenReviews: () -> Unit = {},
    onShareProgress: () -> Unit = {},
    onConvertToEpub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Book Cover Image with Badges and Overlay
            BookCoverImage(
                book = book,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
                cornerRadius = 0.dp,
                showFormatBadge = true,
                showFavoriteBadge = true,
                onToggleFavorite = onToggleFavorite,
                showOfflineBadge = true,
                elevation = 0.dp
            )

            // Progress Bar
            LinearProgressIndicator(
                progress = { book.readingProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = NaturalPrimary,
                trackColor = NaturalDarkBackground
            )

            // Book Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = NaturalDarkTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                text = { Text("Open Reader") },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp), tint = NaturalPrimary) },
                                text = { Text("Reviews & Ratings") },
                                onClick = {
                                    showMenu = false
                                    onOpenReviews()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(18.dp), tint = NaturalPrimary) },
                                text = { Text("Convert to EPUB") },
                                onClick = {
                                    showMenu = false
                                    onConvertToEpub()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = NaturalOchreAccent) },
                                text = { Text("Share Progress") },
                                onClick = {
                                    showMenu = false
                                    onShareProgress()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                text = { Text(if (book.isFavorite) "Remove from Favorites" else "Mark Favorite") },
                                onClick = {
                                    showMenu = false
                                    onToggleFavorite()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                text = { Text("Delete Book", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(book.readingProgress * 100).toInt()}% read",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NaturalPrimary
                    )
                    Text(
                        text = "p. ${book.currentPage}/${book.totalPages}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun BookListCard(
    book: Book,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onOpenReviews: () -> Unit = {},
    onShareProgress: () -> Unit = {},
    onConvertToEpub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Book Cover Thumbnail
            BookCoverImage(
                book = book,
                modifier = Modifier.size(width = 66.dp, height = 92.dp),
                cornerRadius = 10.dp,
                showFormatBadge = true,
                showFavoriteBadge = false,
                elevation = 2.dp
            )

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { book.readingProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = NaturalPrimary,
                    trackColor = NaturalDarkBackground
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(book.readingProgress * 100).toInt()}% • Page ${book.currentPage}/${book.totalPages}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = NaturalPrimary
                    )
                    Text(
                        text = book.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }
            }

            // Right icons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (book.isFavorite) Color(0xFFEF4444) else NaturalDarkTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = NaturalDarkTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            text = { Text("Open Reader") },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp), tint = NaturalPrimary) },
                            text = { Text("Reviews & Ratings") },
                            onClick = {
                                showMenu = false
                                onOpenReviews()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(18.dp), tint = NaturalPrimary) },
                            text = { Text("Convert to EPUB") },
                            onClick = {
                                showMenu = false
                                onConvertToEpub()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = NaturalOchreAccent) },
                            text = { Text("Share Progress") },
                            onClick = {
                                showMenu = false
                                onShareProgress()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                            text = { Text("Delete Book", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

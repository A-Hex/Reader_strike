package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.model.BookReview
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReviewsSheet(
    book: Book,
    reviews: List<BookReview>,
    onDismiss: () -> Unit,
    onSubmitReview: (rating: Float, title: String, text: String, userName: String) -> Unit,
    onDeleteReview: (id: String) -> Unit,
    onHelpfulClick: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWriteForm by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableStateOf(5) }
    var reviewTitle by remember { mutableStateOf("") }
    var reviewText by remember { mutableStateOf("") }
    var reviewerName by remember { mutableStateOf("A-Hex Reader") }

    val avgRating = remember(reviews, book.rating) {
        if (reviews.isNotEmpty()) {
            reviews.map { it.rating }.average().toFloat()
        } else {
            book.rating
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = NaturalDarkBorder)
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reviews & Ratings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaturalPrimary,
                        maxLines = 1
                    )
                }

                FilledTonalButton(
                    onClick = { showWriteForm = !showWriteForm },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (showWriteForm) NaturalDarkBorder else NaturalPrimary.copy(alpha = 0.15f),
                        contentColor = NaturalPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (showWriteForm) Icons.Default.Close else Icons.Default.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showWriteForm) "Cancel" else "Write Review",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Rating Summary Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", avgRating),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = NaturalOchreAccent
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = if (i <= avgRating.toInt()) Icons.Filled.Star else Icons.Filled.StarHalf,
                                    contentDescription = null,
                                    tint = NaturalOchreAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${reviews.size} community review${if (reviews.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                    }

                    // Rating bars distribution
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (star in 5 downTo 1) {
                            val count = reviews.count { it.rating.toInt() == star }
                            val fraction = if (reviews.isNotEmpty()) count.toFloat() / reviews.size else if (star >= 4) 0.7f else 0.1f
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$star★",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted,
                                    modifier = Modifier.width(22.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = NaturalPrimary,
                                    trackColor = NaturalDarkBorder
                                )
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted,
                                    modifier = Modifier.width(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Write Review Form Expandable
            AnimatedVisibility(
                visible = showWriteForm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Write your review for ${book.title}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary
                        )

                        // Star selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Your Rating:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (i in 1..5) {
                                    IconButton(
                                        onClick = { selectedRating = i },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (i <= selectedRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "$i Stars",
                                            tint = NaturalOchreAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = reviewTitle,
                            onValueChange = { reviewTitle = it },
                            label = { Text("Review Headline (e.g. Inspiring & profound)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            label = { Text("Share your thoughts with other readers...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = reviewerName,
                            onValueChange = { reviewerName = it },
                            label = { Text("Your Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (reviewText.isNotBlank()) {
                                    onSubmitReview(
                                        selectedRating.toFloat(),
                                        reviewTitle.ifBlank { "Reader Review" },
                                        reviewText,
                                        reviewerName
                                    )
                                    showWriteForm = false
                                    reviewTitle = ""
                                    reviewText = ""
                                }
                            },
                            enabled = reviewText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Post Review & Rating", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Reviews List
            Text(
                text = "Community Reviews (${reviews.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = NaturalDarkTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No reviews yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaturalDarkTextMuted
                        )
                        Text(
                            text = "Be the first to review this book!",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalPrimary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(reviews, key = { it.id }) { review ->
                        ReviewItemCard(
                            review = review,
                            onDelete = { onDeleteReview(review.id) },
                            onHelpful = { onHelpfulClick(review.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItemCard(
    review: BookReview,
    onDelete: () -> Unit,
    onHelpful: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(review.timestamp))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Reviewer header
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(review.userAvatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = review.userName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (review.isUserReview) {
                                Surface(
                                    color = NaturalPrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "You",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = NaturalPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }

                // Stars
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= review.rating.toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = NaturalOchreAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Title & Content
            if (review.reviewTitle.isNotBlank()) {
                Text(
                    text = review.reviewTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = review.reviewText,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = NaturalDarkText
            )

            // Footer (Helpful & Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onHelpful,
                    shape = RoundedCornerShape(8.dp),
                    color = NaturalDarkBackground.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Helpful",
                            tint = NaturalPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Helpful (${review.helpfulCount})",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }

                if (review.isUserReview) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete review",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

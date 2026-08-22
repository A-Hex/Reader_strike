package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.ui.theme.*
import java.io.File

@Composable
fun BookCoverImage(
    book: Book,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    showFormatBadge: Boolean = true,
    showFavoriteBadge: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    showOfflineBadge: Boolean = false,
    showTitleOverlay: Boolean = false,
    elevation: Dp = 4.dp
) {
    val loadedBitmap = remember(book.coverImageUrl, book.localFilePath) {
        try {
            if (!book.coverImageUrl.isNullOrBlank()) {
                val file = File(book.coverImageUrl)
                if (file.exists() && file.canRead()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    val isBuiltInSample = book.id in listOf(
        "book-metamorphosis", "book-meditations", "book-alice", 
        "book-frankenstein", "book-art-of-war"
    )

    Card(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(cornerRadius), clip = false)
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color(book.coverGradientStart)),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Cover Image Graphic / Illustration / Procedural Cover
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap,
                    contentDescription = "Cover for ${book.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (book.coverDrawableRes != null && book.coverDrawableRes != 0) {
                Image(
                    painter = painterResource(id = book.coverDrawableRes!!),
                    contentDescription = "Cover for ${book.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isBuiltInSample) {
                Image(
                    painter = painterResource(id = book.getEffectiveCoverRes()),
                    contentDescription = "Cover for ${book.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Dedicated Procedural Book Cover for imported documents and custom books
                ProceduralArtisticCover(book = book)
            }

            // 2. Realistic Book Spine Crease Effect on left side
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(10.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 3. Subtle Inner Vignette / Gloss Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = if (showTitleOverlay) 0.85f else 0.35f)
                            )
                        )
                    )
            )

            // 4. Format badge (EPUB / PDF / TXT)
            if (showFormatBadge) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = book.format.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NaturalPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 5. Favorite Badge
            if (showFavoriteBadge) {
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (book.isFavorite) Color(0xFFEF4444) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (book.isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 6. Optional Title & Author Overlay (for grid cards or full display)
            if (showTitleOverlay) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NaturalDarkTextMuted,
                            fontSize = 9.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 7. Offline Downloaded badge
            if (showOfflineBadge && book.isDownloaded && !showTitleOverlay) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Offline Available",
                        tint = NaturalPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProceduralArtisticCover(book: Book) {
    val goldColor = Color(0xFFD4AF37)
    val startColor = Color(book.coverGradientStart)
    val endColor = Color(book.coverGradientEnd)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(startColor, endColor, startColor.copy(alpha = 0.9f))
                )
            )
            .padding(10.dp)
    ) {
        // Inner ornate border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, goldColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Seal / Emblem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = goldColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Middle Title & Divider
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.5.dp)
                            .background(goldColor.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
                    )
                }

                // Bottom Author
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = goldColor.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}


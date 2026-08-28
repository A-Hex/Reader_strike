package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Bookmark
import com.example.model.BookChapter
import com.example.model.Highlight
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOfContentsDialog(
    chapters: List<BookChapter>,
    currentChapterIndex: Int,
    bookmarks: List<Bookmark>,
    highlights: List<Highlight>,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onSelectChapter: (Int) -> Unit,
    onSelectBookmark: (Bookmark) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val contentsLabel = AppStrings.get("toc_contents", currentLanguage)
    val bookmarksLabel = AppStrings.get("toc_bookmarks", currentLanguage)
    val highlightsLabel = AppStrings.get("toc_highlights", currentLanguage)
    val wordsLabel = AppStrings.get("toc_words", currentLanguage)
    val pageLabel = AppStrings.get("toc_page", currentLanguage)
    val noteLabel = AppStrings.get("toc_note", currentLanguage)

    val tabs = listOf("$contentsLabel (${chapters.size})", "$bookmarksLabel (${bookmarks.size})", "$highlightsLabel (${highlights.size})")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = AppStrings.get("toc_navigation", currentLanguage),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Chapters List
                    if (chapters.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AppStrings.get("toc_no_chapters", currentLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = NaturalDarkTextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(chapters) { index, chapter ->
                                val isCurrent = index == currentChapterIndex
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSelectChapter(index)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) NaturalSageBg else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                                    else androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FormatListBulleted,
                                                contentDescription = null,
                                                tint = if (isCurrent) NaturalSageAccent else NaturalDarkTextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = chapter.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrent) NaturalSageAccent else NaturalDarkText
                                                )
                                            )
                                        }
                                        Text(
                                            text = "${chapter.wordCount} $wordsLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NaturalDarkTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Bookmarks List
                    if (bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AppStrings.get("toc_no_bookmarks", currentLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = NaturalDarkTextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(bookmarks) { _, bookmark ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSelectBookmark(bookmark)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = NaturalWarmOchre
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bookmark.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = NaturalDarkText
                                            )
                                            if (!bookmark.note.isNullOrBlank()) {
                                                Text(
                                                    text = bookmark.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NaturalDarkTextMuted
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$pageLabel ${bookmark.page}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = NaturalPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Highlights List
                    if (highlights.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AppStrings.get("toc_no_highlights", currentLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = NaturalDarkTextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(highlights) { _, hl ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(hl.color.toComposeColor())
                                            )
                                            Text(
                                                text = hl.chapterTitle,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = NaturalPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${hl.text}\"",
                                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                            color = NaturalDarkText
                                        )
                                        if (!hl.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$noteLabel: ${hl.note}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NaturalDarkTextMuted
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
    }
}


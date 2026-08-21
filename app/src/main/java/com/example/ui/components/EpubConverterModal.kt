package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.model.BookFormat
import com.example.model.Highlight
import com.example.reader.EpubConversionResult
import com.example.reader.EpubExportOptions
import com.example.reader.EpubExporter
import com.example.reader.EpubFontTheme
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubConverterModal(
    book: Book,
    highlights: List<Highlight> = emptyList(),
    onOpenConvertedBook: ((Book) -> Unit)? = null,
    onImportEpubToLibrary: ((java.io.File, String, String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var customTitle by remember { mutableStateOf(book.title) }
    var customAuthor by remember { mutableStateOf(book.author) }
    var selectedFontTheme by remember { mutableStateOf(EpubFontTheme.SERIF) }
    var includeCoverPage by remember { mutableStateOf(true) }
    var includeToc by remember { mutableStateOf(true) }
    var includeHighlights by remember { mutableStateOf(highlights.isNotEmpty()) }
    var enableDropCaps by remember { mutableStateOf(true) }

    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableStateOf(0f) }
    var progressStatusText by remember { mutableStateOf("") }
    var conversionResult by remember { mutableStateOf<EpubConversionResult?>(null) }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isConverting) onDismiss()
        },
        containerColor = NaturalDarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = NaturalDarkTextMuted)
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NaturalPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Transform,
                            contentDescription = null,
                            tint = NaturalPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "EPUB Converter",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalDarkText
                        )
                        Text(
                            text = "Convert to standard EPUB 3 document",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isConverting
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NaturalDarkTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Book Target Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalDarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookCoverImage(
                        book = book,
                        modifier = Modifier
                            .width(54.dp)
                            .height(76.dp),
                        cornerRadius = 8.dp,
                        showFormatBadge = false,
                        showFavoriteBadge = false,
                        showOfflineBadge = false
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalDarkText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "by ${book.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalDarkTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Format Conversion Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = NaturalDarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
                            ) {
                                Text(
                                    text = book.format.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalDarkTextMuted,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = NaturalPrimary,
                                modifier = Modifier.size(14.dp)
                            )

                            Surface(
                                color = NaturalPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "EPUB 3",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Conversion State Machine
            val result = conversionResult
            if (result == null && !isConverting) {
                // Step 1: Configuration Form
                Text(
                    text = "METADATA & TYPOGRAPHY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NaturalPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Book Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NaturalDarkText,
                        unfocusedTextColor = NaturalDarkText,
                        focusedBorderColor = NaturalPrimary,
                        unfocusedBorderColor = NaturalDarkBorder,
                        focusedLabelColor = NaturalPrimary,
                        unfocusedLabelColor = NaturalDarkTextMuted
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customAuthor,
                    onValueChange = { customAuthor = it },
                    label = { Text("Author") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NaturalDarkText,
                        unfocusedTextColor = NaturalDarkText,
                        focusedBorderColor = NaturalPrimary,
                        unfocusedBorderColor = NaturalDarkBorder,
                        focusedLabelColor = NaturalPrimary,
                        unfocusedLabelColor = NaturalDarkTextMuted
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FONT STYLING",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NaturalPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Font themes grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EpubFontTheme.values().forEach { theme ->
                        val isSelected = selectedFontTheme == theme
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFontTheme = theme },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NaturalPrimary.copy(alpha = 0.15f) else NaturalDarkSurfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NaturalPrimary else NaturalDarkBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = theme.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) NaturalPrimary else NaturalDarkText
                                    )
                                    Text(
                                        text = theme.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NaturalDarkTextMuted
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFontTheme = theme },
                                    colors = RadioButtonDefaults.colors(selectedColor = NaturalPrimary)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "EPUB STRUCTURE OPTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NaturalPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalDarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include Cover & Title Page",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = NaturalDarkText
                                )
                                Text(
                                    text = "Generates front matter with metadata badge",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                            Switch(
                                checked = includeCoverPage,
                                onCheckedChange = { includeCoverPage = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = NaturalPrimary, checkedTrackColor = NaturalPrimary.copy(alpha = 0.3f))
                            )
                        }

                        HorizontalDivider(color = NaturalDarkBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include Table of Contents",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = NaturalDarkText
                                )
                                Text(
                                    text = "Generates standard EPUB 3 nav and NCX index",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                            Switch(
                                checked = includeToc,
                                onCheckedChange = { includeToc = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = NaturalPrimary, checkedTrackColor = NaturalPrimary.copy(alpha = 0.3f))
                            )
                        }

                        HorizontalDivider(color = NaturalDarkBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Embed Highlights & Notes Appendix",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = NaturalDarkText
                                )
                                Text(
                                    text = if (highlights.isNotEmpty()) "${highlights.size} annotations available" else "No highlights made yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                            Switch(
                                checked = includeHighlights && highlights.isNotEmpty(),
                                onCheckedChange = { includeHighlights = it },
                                enabled = highlights.isNotEmpty(),
                                colors = SwitchDefaults.colors(checkedThumbColor = NaturalPrimary, checkedTrackColor = NaturalPrimary.copy(alpha = 0.3f))
                            )
                        }

                        HorizontalDivider(color = NaturalDarkBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Decorative Drop Caps",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = NaturalDarkText
                                )
                                Text(
                                    text = "Style initial letters on first chapter paragraphs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                            Switch(
                                checked = enableDropCaps,
                                onCheckedChange = { enableDropCaps = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = NaturalPrimary, checkedTrackColor = NaturalPrimary.copy(alpha = 0.3f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isConverting = true
                        conversionProgress = 0.05f
                        progressStatusText = "Preparing conversion engine..."

                        scope.launch {
                            val options = EpubExportOptions(
                                customTitle = customTitle,
                                customAuthor = customAuthor,
                                includeCoverPage = includeCoverPage,
                                includeTableOfContents = includeToc,
                                includeHighlightsAndNotes = includeHighlights,
                                fontTheme = selectedFontTheme,
                                enableDropCaps = enableDropCaps
                            )

                            val res = EpubExporter.convertBookToEpub(
                                context = context,
                                book = book,
                                highlights = highlights,
                                options = options,
                                onProgress = { progress, status ->
                                    conversionProgress = progress
                                    progressStatusText = status
                                }
                            )

                            delay(400)
                            isConverting = false
                            conversionResult = res
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = NaturalDarkBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Convert to EPUB",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalDarkBackground
                    )
                }
            } else if (isConverting) {
                // Conversion In Progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { conversionProgress },
                        modifier = Modifier.size(64.dp),
                        color = NaturalPrimary,
                        trackColor = NaturalDarkBorder
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = progressStatusText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NaturalDarkText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(conversionProgress * 100).toInt()}% completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { conversionProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NaturalPrimary,
                        trackColor = NaturalDarkSurfaceVariant
                    )
                }
            } else if (result != null) {
                // Success / Error View
                if (result.success && result.file != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NaturalSageSuccess.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageSuccess.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(NaturalSageSuccess.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NaturalSageSuccess,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "EPUB Generated Successfully!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalDarkText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${result.file.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalDarkTextMuted,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${result.chapterCount}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalPrimary
                                    )
                                    Text(
                                        text = "Chapters",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalDarkTextMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${result.totalWords}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalPrimary
                                    )
                                    Text(
                                        text = "Words",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalDarkTextMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = result.fileSizeFormatted,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalPrimary
                                    )
                                    Text(
                                        text = "File Size",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalDarkTextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Action: Share EPUB (Send to Kindle, Books, Drive)
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/epub+zip"
                                putExtra(Intent.EXTRA_STREAM, result.shareableUri)
                                putExtra(Intent.EXTRA_SUBJECT, result.bookTitle)
                                putExtra(Intent.EXTRA_TEXT, "Here is '${result.bookTitle}' converted to EPUB with A-Hex Reader.")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share or Open EPUB With"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = NaturalDarkBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share EPUB (Kindle / Books / Apps)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalDarkBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Action: Add Converted Book into App Library & Open
                    OutlinedButton(
                        onClick = {
                            onImportEpubToLibrary?.invoke(
                                result.file,
                                customTitle,
                                customAuthor
                            )
                            Toast.makeText(context, "Added '${result.bookTitle}' as EPUB to your Library!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryAdd,
                            contentDescription = null,
                            tint = NaturalPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add to Library & Read Now",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary
                        )
                    }
                } else {
                    // Error state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.errorMessage ?: "Failed to generate EPUB document.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { conversionResult = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                    ) {
                        Text("Try Again", color = NaturalDarkBackground)
                    }
                }
            }
        }
    }
}

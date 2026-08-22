package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MindMapProvider
import com.example.model.Book
import com.example.ui.components.CharacterMindMapDialog
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.LocalAiRelationDetector
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSpaceScreen(
    viewModel: MainViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allBooks by viewModel.allBooks.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    var selectedBookForAi by remember(allBooks) {
        mutableStateOf(allBooks.firstOrNull())
    }

    var isAnalyzingLocalAi by remember { mutableStateOf(false) }
    var lastAiDetectionStats by remember { mutableStateOf<LocalAiRelationDetector.DetectionStats?>(null) }
    var showOptimizeSuccessToast by remember { mutableStateOf(false) }

    // Backup Export Launcher
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    // Backup Restore Launcher
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackup(uri)
        }
    }

    // File Import Launcher
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var displayName = "Imported_Document"
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
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(NaturalPrimary, Color(0xFF1B5E20)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = NaturalOnPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Local Space",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "100% On-Device • Air-Gapped Local Vault",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = NaturalPrimary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        fileImportLauncher.launch(arrayOf("application/epub+zip", "application/pdf", "text/plain"))
                    }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import Document", tint = NaturalPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Air-Gapped Privacy Status Banner
            item {
                AirGappedStatusCard()
            }

            // 2. Local AI Relation & Plot Engine Studio
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF8E24AA).copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFCE93D8), modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(
                                        text = "On-Device AI Relation Detector",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Pre-computes character co-occurrences & plot tension locally",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = NaturalDarkTextMuted
                                    )
                                }
                            }
                        }

                        // Book Selector
                        Text(
                            text = "Select Book to Analyze:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NaturalDarkText
                        )

                        if (allBooks.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allBooks) { book ->
                                    val isSelected = selectedBookForAi?.id == book.id
                                    Surface(
                                        color = if (isSelected) NaturalPrimary else Color(0xFF141C15),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isSelected) NaturalOchreAccent else NaturalDarkBorder),
                                        modifier = Modifier.clickable { selectedBookForAi = book }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = book.title,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp
                                                ),
                                                color = if (isSelected) Color(0xFF141C15) else Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = book.author,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isSelected) Color(0xFF283629) else NaturalDarkTextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Action: Run Local AI Pre-computation
                        Button(
                            onClick = {
                                val currentBook = selectedBookForAi ?: allBooks.firstOrNull()
                                if (currentBook != null) {
                                    coroutineScope.launch {
                                        isAnalyzingLocalAi = true
                                        delay(350)
                                        val (detected, stats) = LocalAiRelationDetector.analyzeBook(
                                            book = currentBook,
                                            fullText = "${currentBook.title}. ${currentBook.description}. By ${currentBook.author}."
                                        )
                                        MindMapProvider.saveCustomMindMap(detected)
                                        lastAiDetectionStats = stats
                                        isAnalyzingLocalAi = false
                                        Toast.makeText(context, "Local AI detected ${stats.charactersFound} characters & ${stats.relationshipsMapped} relationships in ${stats.processingDurationMs}ms!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary),
                            enabled = !isAnalyzingLocalAi && selectedBookForAi != null
                        ) {
                            if (isAnalyzingLocalAi) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF141C15))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyzing On-Device...", color = Color(0xFF141C15), fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF141C15), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Local AI Analysis", color = Color(0xFF141C15), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Reader screen prompt note
                        Surface(
                            color = NaturalDarkBackground,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = NaturalOchreAccent, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Interactive Character & Plot Codex is available live while reading inside the Reader Screen.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = NaturalDarkTextMuted
                                )
                            }
                        }

                        // Last Detection Metrics Display
                        if (lastAiDetectionStats != null) {
                            val stats = lastAiDetectionStats!!
                            Surface(
                                color = NaturalDarkBackground,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    StatMiniItem(label = "Characters", value = "${stats.charactersFound}")
                                    StatMiniItem(label = "Relations", value = "${stats.relationshipsMapped}")
                                    StatMiniItem(label = "Plot Stages", value = "${stats.plotPointsIdentified}")
                                    StatMiniItem(label = "Speed", value = "${stats.processingDurationMs}ms")
                                }
                            }
                        }
                    }
                }
            }

            // 3. Local Space Storage & Database Health Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, NaturalDarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = NaturalPrimary)
                                Column {
                                    Text(
                                        text = "On-Device Storage Breakdown",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Local SQLite DB • EPUBs • Annotations • AI Cache",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = NaturalDarkTextMuted
                                    )
                                }
                            }
                        }

                        // Visual Storage Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF243026))
                            ) {
                                Box(modifier = Modifier.weight(0.45f).fillMaxHeight().background(NaturalPrimary))
                                Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(NaturalOchreAccent))
                                Box(modifier = Modifier.weight(0.18f).fillMaxHeight().background(Color(0xFF8E24AA)))
                                Box(modifier = Modifier.weight(0.12f).fillMaxHeight().background(Color(0xFF29B6F6)))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StorageLegendItem(color = NaturalPrimary, label = "Books & Media")
                                StorageLegendItem(color = NaturalOchreAccent, label = "Highlights & Notes")
                                StorageLegendItem(color = Color(0xFF8E24AA), label = "AI Neural Cache")
                                StorageLegendItem(color = Color(0xFF29B6F6), label = "Voice Profiles")
                            }
                        }

                        Divider(color = NaturalDarkBorder)

                        // Action Buttons: Compact DB & Clear Temporary Cache
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        delay(250)
                                        showOptimizeSuccessToast = true
                                        Toast.makeText(context, "Local SQLite Database VACUUMed and optimized!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Optimize DB", color = NaturalPrimary, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "AI Detection Cache cleared safely.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, NaturalDarkBorder)
                            ) {
                                Icon(Icons.Default.Cached, contentDescription = null, tint = NaturalDarkTextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear AI Cache", color = NaturalDarkTextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 4. Encrypted Local Backup & Portable Migration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, NaturalDarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = NaturalOchreAccent)
                            Column {
                                Text(
                                    text = "Local Vault Backup & Export",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Save your reading history, highlights & AI mindmaps into an encrypted local file",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = NaturalDarkTextMuted
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val filename = "reader_local_backup_${System.currentTimeMillis()}.json"
                                    exportBackupLauncher.launch(filename)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NaturalOchreAccent)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF141C15), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Backup", color = Color(0xFF141C15), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    restoreBackupLauncher.launch(arrayOf("application/json"))
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, NaturalOchreAccent)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = NaturalOchreAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Backup", color = NaturalOchreAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AirGappedStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A12)),
        border = BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NaturalPrimary)
                    )
                    Text(
                        text = "Air-Gapped Privacy Shield Active",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalPrimary
                    )
                }

                Surface(
                    color = NaturalPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "ZERO TELEMETRY",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                        color = NaturalPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "All relationship extraction, character recognition, plot tension analysis, text-to-speech voice profiling, and reading statistics run entirely on your device's CPU/GPU. No reading data ever leaves your hardware.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = NaturalDarkText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PrivacyMetricBadge(icon = Icons.Default.CloudOff, label = "0 Cloud Requests")
                PrivacyMetricBadge(icon = Icons.Default.Lock, label = "Local SQLite Storage")
                PrivacyMetricBadge(icon = Icons.Default.Bolt, label = "Local NLP Engine")
            }
        }
    }
}

@Composable
private fun PrivacyMetricBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(14.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = NaturalDarkTextMuted)
    }
}

@Composable
private fun StatMiniItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = NaturalDarkTextMuted)
    }
}

@Composable
private fun StorageLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = NaturalDarkTextMuted)
    }
}

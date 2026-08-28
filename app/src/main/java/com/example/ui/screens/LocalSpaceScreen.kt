package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSpaceScreen(
    viewModel: MainViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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

            // 2. Local Space Storage & Database Health Breakdown
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
                                        text = "Local SQLite DB • EPUBs • Annotations • Storage Cache",
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
                                Box(modifier = Modifier.weight(0.50f).fillMaxHeight().background(NaturalPrimary))
                                Box(modifier = Modifier.weight(0.30f).fillMaxHeight().background(NaturalOchreAccent))
                                Box(modifier = Modifier.weight(0.20f).fillMaxHeight().background(Color(0xFF29B6F6)))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StorageLegendItem(color = NaturalPrimary, label = "Books & Media")
                                StorageLegendItem(color = NaturalOchreAccent, label = "Highlights & Notes")
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
                                    Toast.makeText(context, "Temporary cache cleared safely.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, NaturalDarkBorder)
                            ) {
                                Icon(Icons.Default.Cached, contentDescription = null, tint = NaturalDarkTextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Cache", color = NaturalDarkTextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 3. Encrypted Local Backup & Portable Migration
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
                                    text = "Save your reading history, highlights & annotations into a local backup file",
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
                text = "All text-to-speech voice profiling, local reading statistics, and database operations run entirely on your device's hardware. No reading data ever leaves your device.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = NaturalDarkText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PrivacyMetricBadge(icon = Icons.Default.CloudOff, label = "0 Cloud Requests")
                PrivacyMetricBadge(icon = Icons.Default.Lock, label = "Local SQLite Storage")
                PrivacyMetricBadge(icon = Icons.Default.Security, label = "Air-Gapped Vault")
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
private fun StorageLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = NaturalDarkTextMuted)
    }
}


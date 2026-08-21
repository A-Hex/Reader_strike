package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.BackupOperationState
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val books by viewModel.allBooks.collectAsState()
    val highlights by viewModel.allHighlights.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val backupInfo by viewModel.localBackupInfo.collectAsState()
    var showGoalPickerDialog by remember { mutableStateOf(false) }

    // Android Storage Access Framework document export & restore launchers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    if (showGoalPickerDialog) {
        DailyGoalPickerDialog(
            currentGoalMinutes = dailyGoalMinutes,
            onGoalSelected = { newGoal ->
                viewModel.updateDailyGoal(newGoal)
                showGoalPickerDialog = false
            },
            onDismiss = { showGoalPickerDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // App Identity Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "A-Hex streak logo",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, NaturalPrimary, RoundedCornerShape(16.dp))
                    )

                    Column {
                        Text(
                            text = "A-Hex streak",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Version 1.0.0 • Offline E-Reader",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalPrimary
                        )
                        Text(
                            text = "Multi-format PDF, EPUB & TXT Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }
            }
        }

        // Daily Reading Goal Target Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TrackChanges, contentDescription = null, tint = NaturalPrimary)
                            Text(
                                text = "Daily Reading Goal Target",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = NaturalPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "$dailyGoalMinutes min / day",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Customize your daily target minutes. This target determines your daily streak progress bar and celebration milestones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    Button(
                        onClick = { showGoalPickerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize Target Minutes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Local Database Backup & Restore (Document Picker)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = NaturalPrimary)
                            Text(
                                text = "Local Database Backup & Restore",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (backupInfo.state == BackupOperationState.PROCESSING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = NaturalPrimary
                            )
                        }
                    }

                    Text(
                        text = "Safely export your reading streaks, books metadata, highlights, bookmarks, and sessions to a versioned JSON file on device, or restore from a previous backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    if (backupInfo.lastBackupTimestamp > 0L) {
                        val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(backupInfo.lastBackupTimestamp))
                        Text(
                            text = "Last Exported: $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalSageAccent
                        )
                    }

                    if (backupInfo.lastRestoreTimestamp > 0L) {
                        val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(backupInfo.lastRestoreTimestamp))
                        Text(
                            text = "Last Restored: $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                exportBackupLauncher.launch("ahex_streak_backup_$timestamp.json")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalPrimary)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Backup", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Application Language Switcher (EN, AR, FR)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = NaturalPrimary)
                            Text(
                                text = AppStrings.get("language_title", currentLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = NaturalPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${currentLanguage.flag} ${currentLanguage.nativeName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = AppStrings.get("language_desc", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = currentLanguage == lang
                            FilledTonalButton(
                                onClick = { viewModel.setLanguage(lang) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSelected) NaturalPrimary else NaturalDarkBackground
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${lang.flag} ${lang.nativeName}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) NaturalOnPrimary else NaturalDarkText
                                        )
                                    )
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = if (isSelected) NaturalOnPrimary.copy(alpha = 0.8f) else NaturalDarkTextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Storage & Library Statistics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Storage & Database",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Installed Books in Library", style = MaterialTheme.typography.bodyMedium, color = NaturalDarkTextMuted)
                        Text("${books.size} Books", fontWeight = FontWeight.SemiBold, color = NaturalPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Saved Highlights", style = MaterialTheme.typography.bodyMedium, color = NaturalDarkTextMuted)
                        Text("${highlights.size} Highlights", fontWeight = FontWeight.SemiBold, color = NaturalPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Offline Local Cache", style = MaterialTheme.typography.bodyMedium, color = NaturalDarkTextMuted)
                        Text("Optimized SQLite DB", fontWeight = FontWeight.SemiBold, color = NaturalDarkText)
                    }
                }
            }
        }

        // Features Checklist
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Included Engine Features",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    FeatureRow(icon = Icons.Default.Check, text = "EPUB, PDF, and TXT parsing & rendering")
                    FeatureRow(icon = Icons.Default.Check, text = "Customizable themes (Obsidian, AMOLED, Sepia, Nordic)")
                    FeatureRow(icon = Icons.Default.Check, text = "Multi-color highlights & personal annotation notes")
                    FeatureRow(icon = Icons.Default.Check, text = "A-Hex Streak Engine with daily reading goals & badges")
                    FeatureRow(icon = Icons.Default.Check, text = "Text-to-Speech (TTS) natural audio reader")
                    FeatureRow(icon = Icons.Default.Check, text = "Offline access with local database export and restore")
                }
            }
        }
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NaturalSageAccent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

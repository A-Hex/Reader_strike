package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SyncState
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val syncInfo by viewModel.cloudSyncInfo.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Status Card in Natural Tones
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = if (syncInfo.syncState == SyncState.SYNCING) NaturalPrimary.copy(alpha = 0.15f) else NaturalSageBg,
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (syncInfo.syncState == SyncState.SYNCING) Icons.Default.Sync else Icons.Default.CloudDone,
                            contentDescription = "Sync Status",
                            tint = if (syncInfo.syncState == SyncState.SYNCING) NaturalPrimary else NaturalSageAccent,
                            modifier = Modifier
                                .size(32.dp)
                                .then(if (syncInfo.syncState == SyncState.SYNCING) Modifier.rotate(syncRotation) else Modifier)
                        )
                    }

                    Text(
                        text = if (syncInfo.syncState == SyncState.SYNCING) "Synchronizing with A-Hex Cloud..." else "Cloud Sync Connected",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    val lastSyncDate = SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault()).format(Date(syncInfo.lastSyncedAt))
                    Text(
                        text = "Last synced: $lastSyncDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { viewModel.triggerSync() },
                        enabled = syncInfo.syncState != SyncState.SYNCING,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NaturalPrimary,
                            contentColor = NaturalOnPrimary
                        )
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (syncInfo.syncState == SyncState.SYNCING) "Syncing..." else "Sync Now", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Sync Toggles Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Synchronization Preferences",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Automatic Cloud Sync", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(text = "Sync progress & highlights in background", style = MaterialTheme.typography.bodySmall, color = NaturalDarkTextMuted)
                        }
                        Switch(
                            checked = syncInfo.autoSyncEnabled,
                            onCheckedChange = { viewModel.cloudSyncRepository.toggleAutoSync(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaturalOnPrimary,
                                checkedTrackColor = NaturalPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = NaturalDarkBorder.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Sync on Wi-Fi Only", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(text = "Conserve mobile data when downloading", style = MaterialTheme.typography.bodySmall, color = NaturalDarkTextMuted)
                        }
                        Switch(
                            checked = syncInfo.syncOnWifiOnly,
                            onCheckedChange = { viewModel.cloudSyncRepository.toggleWifiOnly(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaturalOnPrimary,
                                checkedTrackColor = NaturalPrimary
                            )
                        )
                    }
                }
            }
        }

        // Backup & Restore Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Backup & Migration (JSON)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Export your entire library, reading streak records, and highlights to a portable JSON backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val json = viewModel.cloudSyncRepository.exportFullLibraryJson()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("A-Hex Library Backup", json)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = NaturalDarkText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Backup", fontSize = 12.sp, color = NaturalDarkText)
                        }

                        Button(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NaturalDarkSurfaceElevated,
                                contentColor = NaturalPrimary
                            )
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Sync Activity Logs
        item {
            Text(
                text = "Recent Cloud Sync Activity",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NaturalDarkTextMuted
                )
            )
        }

        items(syncInfo.syncLogs) { log ->
            val logDate = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(log.timestamp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (log.isSuccess) NaturalSageAccent else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = log.message, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(text = logDate, style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Library Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste your exported A-Hex JSON backup code below:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("{\n  \"appName\": \"A-Hex streak\"...\n}", fontSize = 12.sp) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(restoreJsonInput)
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = NaturalOnPrimary)
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel", color = NaturalDarkTextMuted)
                }
            }
        )
    }
}


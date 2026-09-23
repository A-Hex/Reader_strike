package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AccountAuthSheet
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

/**
 * Full-screen Cloud Account destination opened from Settings.
 * Shows account status and sync actions; hosts the auth bottom sheet for sign-in/sign-up.
 */
@Composable
fun AccountScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val accountState by viewModel.accountState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    var showAuthSheet by remember { mutableStateOf(!accountState.isSignedIn) }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NaturalPrimary)
            }
            Column {
                Text(
                    text = "Cloud Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sign in to sync your library across devices",
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted
                )
            }
        }

        if (!viewModel.cloudAccountsAvailable) {
            Surface(
                color = NaturalOchreAccent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NaturalOchreAccent, modifier = Modifier.size(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "One-time setup needed",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Cloud accounts turn on once google-services.json is added to the Android app. See CLOUD_ACCOUNT_SETUP.md for the ~5-minute steps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }
            }
        }

        if (accountState.isSignedIn) {
            // Signed-in summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NaturalPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NaturalOnPrimary, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(
                                text = accountState.displayName?.takeIf { it.isNotBlank() } ?: accountState.email ?: "Signed in",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (accountState.displayName?.isNotBlank() == true) {
                                Text(accountState.email ?: "", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                            }
                        }
                    }

                    if (!accountState.isEmailVerified) {
                        Surface(color = NaturalOchreAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = NaturalOchreAccent, modifier = Modifier.size(16.dp))
                                Text(
                                    "Check your inbox to verify your email",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalOchreAccent
                                )
                            }
                        }
                    }

                    val isSyncing = syncState == com.example.data.repository.BackupOperationState.PROCESSING
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.uploadLibraryToCloud() },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Back Up Now", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.downloadLibraryFromCloud() },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalPrimary)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore Cloud", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    if (syncMessage.isNotBlank()) {
                        Text(syncMessage, style = MaterialTheme.typography.labelSmall, color = NaturalPrimary)
                    }

                    OutlinedButton(
                        onClick = { viewModel.signOutAccount() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            // Signed-out call to action
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A12)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(38.dp))
                    Text(
                        "Take your library to the cloud",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        "Create a free account to back up books, highlights, bookmarks and streaks — then restore them on any device with the same account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkText
                    )
                    Button(
                        onClick = { showAuthSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign In or Create Account", color = Color(0xFF141C15), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAuthSheet) {
        AccountAuthSheet(
            viewModel = viewModel,
            onDismiss = { showAuthSheet = false }
        )
    }
}

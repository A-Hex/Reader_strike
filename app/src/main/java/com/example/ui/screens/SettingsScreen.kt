package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.BackupOperationState
import com.example.model.VoiceMode
import com.example.notification.ReadingNotificationManager
import com.example.ui.components.DailyGoalPickerDialog
import com.example.ui.components.VoiceNarratorStudioDialog
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
    var showLocalSpaceScreen by remember { mutableStateOf(false) }

    if (showLocalSpaceScreen) {
        LocalSpaceScreen(
            viewModel = viewModel,
            onBack = { showLocalSpaceScreen = false }
        )
        return
    }

    val context = LocalContext.current
    val books by viewModel.allBooks.collectAsState()
    val highlights by viewModel.allHighlights.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val backupInfo by viewModel.localBackupInfo.collectAsState()
    val cloudSyncInfo by viewModel.cloudSyncInfo.collectAsState()
    val isFaceAssistedEnabled by viewModel.isFaceAssistedEnabled.collectAsState()
    val customVoiceProfile by viewModel.voiceProfileRepository.voiceProfile.collectAsState()
    val activeVoiceMode by viewModel.voiceProfileRepository.voiceMode.collectAsState()

    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    val isGoalAlertsEnabled by viewModel.isGoalAlertsEnabled.collectAsState()
    val isStreakAlertsEnabled by viewModel.isStreakAlertsEnabled.collectAsState()

    var showGoalPickerDialog by remember { mutableStateOf(false) }
    var showVoiceStudioDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
        }
    }

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

    if (showVoiceStudioDialog) {
        VoiceNarratorStudioDialog(
            viewModel = viewModel,
            currentLanguage = currentLanguage,
            onDismiss = { showVoiceStudioDialog = false }
        )
    }

    if (showReminderTimeDialog) {
        ReminderTimePickerDialog(
            currentHour = reminderHour,
            currentMinute = reminderMinute,
            onTimeSelected = { hour, min ->
                viewModel.setReminderTime(hour, min)
                showReminderTimeDialog = false
            },
            onDismiss = { showReminderTimeDialog = false }
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

        // Dedicated Local Space Vault Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A12)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NaturalPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = NaturalOnPrimary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Local Space Vault",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Air-Gapped • 100% On-Device",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = NaturalPrimary
                                )
                            }
                        }

                        Surface(
                            color = NaturalPrimary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "AIR-GAPPED",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = NaturalPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Access your private on-device vault, local storage allocation, database optimization, and local encrypted backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkText
                    )

                    Button(
                        onClick = { showLocalSpaceScreen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF141C15), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Local Space", color = Color(0xFF141C15), fontWeight = FontWeight.Bold)
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
                            Icon(Icons.Default.Timer, contentDescription = null, tint = NaturalPrimary)
                            Text(
                                text = AppStrings.get("daily_goal_title", currentLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = NaturalPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$dailyGoalMinutes min/day",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = AppStrings.get("daily_goal_desc", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    Button(
                        onClick = { showGoalPickerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize Daily Goal", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Reading Notifications & Habit Reminders Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NaturalPrimary)
                            Text(
                                text = "Reading Habit Notifications",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = isNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReadingNotificationManager.hasNotificationPermission(context)) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setNotificationsEnabled(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaturalPrimary,
                                checkedTrackColor = NaturalPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Text(
                        text = "Receive gentle daily nudges, streak expiry warnings, and daily goal completion celebrations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    if (isNotificationsEnabled) {
                        // Reminder Time Selector Box
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showReminderTimeDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Daily Reminder Time",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val amPm = if (reminderHour >= 12) "PM" else "AM"
                                    val displayHour = when {
                                        reminderHour == 0 -> 12
                                        reminderHour > 12 -> reminderHour - 12
                                        else -> reminderHour
                                    }
                                    val formattedTime = "%d:%02d %s (%02d:%02d)".format(displayHour, reminderMinute, amPm, reminderHour, reminderMinute)
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalPrimary
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { showReminderTimeDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = NaturalPrimary.copy(alpha = 0.2f),
                                        contentColor = NaturalPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Sub-options: Goal Alerts and Streak Alerts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Goal Completed Celebrations",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Notify when you hit your daily reading target",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                            Switch(
                                checked = isGoalAlertsEnabled,
                                onCheckedChange = { viewModel.setGoalAlertsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NaturalPrimary,
                                    checkedTrackColor = NaturalPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Streak Expiry Warnings",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Warn before midnight if streak is at risk",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                            Switch(
                                checked = isStreakAlertsEnabled,
                                onCheckedChange = { viewModel.setStreakAlertsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NaturalPrimary,
                                    checkedTrackColor = NaturalPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        // Test Notification Button
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReadingNotificationManager.hasNotificationPermission(context)) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.sendTestNotification()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalPrimary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Test Notification Now", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Custom AI Voice Narrator Studio (TTS Custom Voice Generation)
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
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NaturalPrimary)
                            Text(
                                text = AppStrings.get("voice_narrator_title", currentLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (customVoiceProfile != null) {
                            Switch(
                                checked = activeVoiceMode == VoiceMode.USER_CLONED_VOICE,
                                onCheckedChange = { checked ->
                                    viewModel.setVoiceMode(if (checked) VoiceMode.USER_CLONED_VOICE else VoiceMode.SYSTEM_DEFAULT)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NaturalPrimary,
                                    checkedTrackColor = NaturalPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Text(
                        text = AppStrings.get("voice_narrator_desc", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    if (customVoiceProfile == null) {
                        Button(
                            onClick = { showVoiceStudioDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("voice_narrator_train_btn", currentLanguage), fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        val profile = customVoiceProfile!!
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Timbre: ${profile.timbreDescriptor} • Pitch: ${String.format("%.2fx", profile.estimatedPitch)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) NaturalForestAccent.copy(alpha = 0.25f) else NaturalDarkSurfaceVariant
                                ) {
                                    Text(
                                        text = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) "Active" else "Standby",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) NaturalForestAccent else NaturalDarkTextMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showVoiceStudioDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Voice Studio", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { viewModel.testVoiceNarration() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalPrimary)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Sample", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Smart Face Presence Counter
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
                            Icon(Icons.Default.Face, contentDescription = null, tint = NaturalPrimary)
                            Text(
                                text = AppStrings.get("face_presence_title", currentLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = isFaceAssistedEnabled,
                            onCheckedChange = { viewModel.setFaceAssistedEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaturalPrimary,
                                checkedTrackColor = NaturalPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Text(
                        text = AppStrings.get("face_presence_desc", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    Surface(
                        color = NaturalPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = AppStrings.get("face_privacy_badge", currentLanguage),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NaturalPrimary
                            )
                        }
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

        // Google Drive Cloud Sync & Multi-Device Synchronization
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
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = NaturalPrimary)
                            Column {
                                Text(
                                    text = "Google Drive Cloud Sync",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Multi-device backup & sync manifest",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                        }

                        Surface(
                            color = when (cloudSyncInfo.syncState) {
                                com.example.model.SyncState.SYNCING -> NaturalOchreAccent.copy(alpha = 0.2f)
                                com.example.model.SyncState.SUCCESS -> NaturalSageBg
                                com.example.model.SyncState.ERROR -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                else -> NaturalPrimary.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (cloudSyncInfo.syncState == com.example.model.SyncState.SYNCING) {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 2.dp, color = NaturalOchreAccent)
                                }
                                Text(
                                    text = when (cloudSyncInfo.syncState) {
                                        com.example.model.SyncState.SYNCING -> "Syncing..."
                                        com.example.model.SyncState.SUCCESS -> "Up to date"
                                        com.example.model.SyncState.ERROR -> "Sync Error"
                                        com.example.model.SyncState.OFFLINE -> "Offline"
                                        else -> "Connected"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when (cloudSyncInfo.syncState) {
                                        com.example.model.SyncState.SYNCING -> NaturalOchreAccent
                                        com.example.model.SyncState.SUCCESS -> NaturalSageAccent
                                        com.example.model.SyncState.ERROR -> Color(0xFFEF4444)
                                        else -> NaturalPrimary
                                    }
                                )
                            }
                        }
                    }

                    // Account row
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Google Account",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted
                                )
                                Text(
                                    text = cloudSyncInfo.cloudAccountName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = cloudSyncInfo.cloudStorageUsed,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalPrimary
                                )
                            }

                            FilledTonalButton(
                                onClick = { viewModel.performGoogleDriveSync() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = NaturalPrimary,
                                    contentColor = NaturalOnPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Granular toggles
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Automatic Background Sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = cloudSyncInfo.autoSyncEnabled,
                                onCheckedChange = { viewModel.setDriveAutoSync(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NaturalPrimary, checkedTrackColor = NaturalPrimary.copy(alpha = 0.5f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sync on Wi-Fi Only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = cloudSyncInfo.syncOnWifiOnly,
                                onCheckedChange = { viewModel.setDriveSyncWifiOnly(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NaturalPrimary, checkedTrackColor = NaturalPrimary.copy(alpha = 0.5f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sync Highlights & Annotations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = cloudSyncInfo.syncHighlights,
                                onCheckedChange = { viewModel.setDriveSyncHighlights(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NaturalPrimary, checkedTrackColor = NaturalPrimary.copy(alpha = 0.5f))
                            )
                        }
                    }

                    // Connected devices badge
                    Text("Linked Devices (${cloudSyncInfo.connectedDevices.size})", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NaturalDarkTextMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cloudSyncInfo.connectedDevices.forEach { dev ->
                            Surface(
                                color = if (dev.isCurrentDevice) NaturalPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = if (dev.platform.contains("Tab", ignoreCase = true)) Icons.Default.TabletAndroid else if (dev.platform.contains("Web", ignoreCase = true)) Icons.Default.Laptop else Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = if (dev.isCurrentDevice) NaturalPrimary else NaturalDarkTextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (dev.isCurrentDevice) "This Device" else dev.deviceName.take(12),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                            color = if (dev.isCurrentDevice) NaturalPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                    containerColor = if (isSelected) NaturalPrimary else NaturalDarkSurfaceVariant,
                                    contentColor = if (isSelected) NaturalOnPrimary else NaturalDarkText
                                )
                            ) {
                                Text(
                                    text = "${lang.flag} ${lang.displayName}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Storage & Library Statistics Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = AppStrings.get("storage_title", currentLanguage),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStrings.get("installed_books", currentLanguage), style = MaterialTheme.typography.bodySmall, color = NaturalDarkTextMuted)
                        Text("${books.size}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStrings.get("saved_highlights", currentLanguage), style = MaterialTheme.typography.bodySmall, color = NaturalDarkTextMuted)
                        Text("${highlights.size}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStrings.get("offline_cache", currentLanguage), style = MaterialTheme.typography.bodySmall, color = NaturalDarkTextMuted)
                        Text("Active (SQLite + Room)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = NaturalForestAccent)
                    }
                }
            }
        }

        // Interactive Tour & Onboarding Replay Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = NaturalPrimary)
                        Text(
                            text = "Walkthrough & Interactive Tour",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Revisit the interactive feature walkthrough or replay the introductory onboarding setup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.showTutorial() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("settings_replay_tutorial", currentLanguage), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetOnboarding() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("settings_replay_onboarding", currentLanguage), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderTimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(currentHour) }
    var selectedMinute by remember { mutableIntStateOf(currentMinute) }

    val presets = listOf(
        Triple("Morning", 7, 0),
        Triple("Noon", 12, 30),
        Triple("Evening", 18, 0),
        Triple("Night (Recommended)", 20, 0),
        Triple("Bedtime", 21, 30)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = NaturalPrimary)
                Text(
                    text = "Daily Reminder Time",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose when you want A-Hex streak to nudge you for your daily reading habit:",
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted
                )

                // Large Time Display
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val amPm = if (selectedHour >= 12) "PM" else "AM"
                        val displayHour = when {
                            selectedHour == 0 -> 12
                            selectedHour > 12 -> selectedHour - 12
                            else -> selectedHour
                        }
                        Text(
                            text = "%d:%02d %s".format(displayHour, selectedMinute, amPm),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary
                        )
                        Text(
                            text = "24-Hour: %02d:%02d".format(selectedHour, selectedMinute),
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Hour & Minute Steppers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour controls
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hour", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { selectedHour = if (selectedHour == 0) 23 else selectedHour - 1 }
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease hour", tint = NaturalPrimary)
                                    }
                                    Text(
                                        text = "%02d".format(selectedHour),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { selectedHour = (selectedHour + 1) % 24 }
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase hour", tint = NaturalPrimary)
                                    }
                                }
                            }

                            Divider(
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(1.dp),
                                color = NaturalDarkBorder
                            )

                            // Minute controls
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Minute", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { selectedMinute = if (selectedMinute < 15) 45 else selectedMinute - 15 }
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease minute", tint = NaturalPrimary)
                                    }
                                    Text(
                                        text = "%02d".format(selectedMinute),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { selectedMinute = (selectedMinute + 15) % 60 }
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase minute", tint = NaturalPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Presets
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { (label, h, m) ->
                        val isSelected = selectedHour == h && selectedMinute == m
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NaturalPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, NaturalPrimary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedHour = h
                                    selectedMinute = m
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isSelected) NaturalPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "%02d:%02d".format(h, m),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) NaturalPrimary else NaturalDarkTextMuted
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onTimeSelected(selectedHour, selectedMinute) },
                colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Reminder Time", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NaturalDarkTextMuted)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

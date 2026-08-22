package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CustomVoiceProfile
import com.example.model.VoiceMode
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceNarratorStudioDialog(
    viewModel: MainViewModel,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentProfile by viewModel.voiceProfileRepository.voiceProfile.collectAsState()
    val activeVoiceMode by viewModel.voiceProfileRepository.voiceMode.collectAsState()

    var isRecording by remember { mutableStateOf(false) }
    var recordingProgress by remember { mutableStateOf(0f) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isTestingPlayback by remember { mutableStateOf(false) }

    var fineTunePitch by remember(currentProfile) {
        mutableStateOf(currentProfile?.estimatedPitch ?: 1.0f)
    }
    var fineTuneSpeed by remember(currentProfile) {
        mutableStateOf(currentProfile?.preferredSpeed ?: 1.0f)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Surface(
                            shape = CircleShape,
                            color = NaturalPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = NaturalPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Custom Voice Narrator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AI Voice Narration Profile Studio",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalPrimary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                // Scope & Privacy Notice
                Surface(
                    color = NaturalPrimary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Audio recordings are strictly used for custom book narration synthesis. 100% private and on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Profile status card
                if (currentProfile != null && !isAnalyzing) {
                    val profile = currentProfile!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.4f))
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
                                Column {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Timbre: ${profile.timbreDescriptor}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) NaturalPrimary else NaturalDarkSurfaceVariant
                                ) {
                                    Text(
                                        text = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) "Active in Reader" else "Standby",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (activeVoiceMode == VoiceMode.USER_CLONED_VOICE) NaturalOnPrimary else NaturalDarkTextMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Sliders for Fine-Tuning Pitch and Speed
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Narrator Pitch Factor", style = MaterialTheme.typography.labelMedium, color = NaturalDarkTextMuted)
                                    Text(String.format("%.2fx", fineTunePitch), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                                }
                                Slider(
                                    value = fineTunePitch,
                                    onValueChange = {
                                        fineTunePitch = it
                                        viewModel.voiceProfileRepository.updatePitch(it)
                                    },
                                    valueRange = 0.70f..1.40f,
                                    colors = SliderDefaults.colors(thumbColor = NaturalPrimary, activeTrackColor = NaturalPrimary)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cadence / Speed", style = MaterialTheme.typography.labelMedium, color = NaturalDarkTextMuted)
                                    Text(String.format("%.2fx", fineTuneSpeed), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NaturalPrimary)
                                }
                                Slider(
                                    value = fineTuneSpeed,
                                    onValueChange = {
                                        fineTuneSpeed = it
                                        viewModel.voiceProfileRepository.updateSpeed(it)
                                    },
                                    valueRange = 0.75f..1.75f,
                                    colors = SliderDefaults.colors(thumbColor = NaturalPrimary, activeTrackColor = NaturalPrimary)
                                )
                            }

                            // Test narration button
                            Button(
                                onClick = {
                                    isTestingPlayback = true
                                    viewModel.testVoiceNarration(
                                        pitch = fineTunePitch,
                                        speed = fineTuneSpeed,
                                        onComplete = { isTestingPlayback = false }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isTestingPlayback) NaturalForestAccent else NaturalPrimary)
                            ) {
                                Icon(
                                    imageVector = if (isTestingPlayback) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isTestingPlayback) "Playing Voice Preview..." else "Test Voice Narration Sample",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Recording Section (Train / Re-Train Profile)
                Text(
                    text = if (currentProfile == null) "Hold the button below and read the prompt aloud for 3 seconds to generate your narrator voice profile."
                    else "Want to update your voice sample? Hold the microphone to re-record.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted,
                    textAlign = TextAlign.Center
                )

                // Prompt Card
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Sample Reading Passage:",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"Books are the quietest and most constant of friends; they are the most accessible and wisest of counselors, and the most patient of teachers.\"",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Mic Action Button
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isRecording -> Color(0xFFEF4444)
                                isAnalyzing -> NaturalForestAccent
                                currentProfile != null -> NaturalPrimary.copy(alpha = 0.85f)
                                else -> NaturalPrimary
                            }
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isRecording = true
                                    recordingProgress = 0f
                                    statusMessage = "Recording voice sample..."
                                    val progressJob = coroutineScope.launch {
                                        for (i in 1..30) {
                                            delay(100)
                                            recordingProgress = i / 30f
                                        }
                                    }
                                    val job = coroutineScope.launch {
                                        val audioSamples = viewModel.audioRecorder.recordAudioClip(3)
                                        isRecording = false
                                        progressJob.cancel()

                                        if (audioSamples.isNotEmpty()) {
                                            isAnalyzing = true
                                            statusMessage = "Analyzing vocal timbre, pitch & harmonic profile..."
                                            delay(1200)
                                            val created = viewModel.voiceProfileRepository.trainAndSaveProfile(
                                                audioSamples = audioSamples,
                                                name = "My Voice Narrator"
                                            )
                                            isAnalyzing = false
                                            if (created != null) {
                                                statusMessage = "Voice profile generated successfully! Ready for narration."
                                                fineTunePitch = created.estimatedPitch
                                                fineTuneSpeed = created.preferredSpeed
                                                viewModel.setVoiceMode(VoiceMode.USER_CLONED_VOICE)
                                            } else {
                                                statusMessage = "Failed to process audio. Please try again."
                                            }
                                        } else {
                                            statusMessage = "No audio captured. Please check microphone permission."
                                        }
                                    }
                                    tryAwaitRelease()
                                    if (isRecording) {
                                        viewModel.audioRecorder.stop()
                                        isRecording = false
                                        progressJob.cancel()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = NaturalOnPrimary, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "Record Voice",
                            tint = NaturalOnPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                if (isRecording) {
                    LinearProgressIndicator(
                        progress = { recordingProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFEF4444),
                        trackColor = NaturalDarkSurfaceVariant
                    )
                }

                Text(
                    text = if (isRecording) "Recording... Hold still & speak naturally"
                    else if (isAnalyzing) "Synthesizing custom narrator profile..."
                    else "Press & Hold to Record Sample (3s)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                )

                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.contains("Failed") || msg.contains("No audio")) Color(0xFFEF4444) else NaturalPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentProfile != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.voiceProfileRepository.deleteProfile()
                                viewModel.setVoiceMode(VoiceMode.SYSTEM_DEFAULT)
                                statusMessage = "Voice profile removed."
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Profile", color = Color(0xFFEF4444))
                        }
                    }

                    Button(
                        onClick = {
                            if (currentProfile != null) {
                                viewModel.setVoiceMode(VoiceMode.USER_CLONED_VOICE)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                    ) {
                        Text(
                            text = if (currentProfile != null) "Apply to Reader" else "Done",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

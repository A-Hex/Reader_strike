package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.reader.AudioRecorder
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class VoiceAuthMode {
    ENROLL,
    VERIFY
}

@Composable
fun VoiceprintDialog(
    viewModel: MainViewModel,
    mode: VoiceAuthMode,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var matchScore by remember { mutableStateOf<Float?>(null) }
    var isSuccessState by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = NaturalPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = AppStrings.get("voice_auth_title", currentLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalDarkText
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                Text(
                    text = if (mode == VoiceAuthMode.ENROLL) {
                        AppStrings.get("voice_auth_enroll_prompt", currentLanguage)
                    } else {
                        AppStrings.get("voice_auth_verify_prompt", currentLanguage)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted,
                    textAlign = TextAlign.Center
                )

                // Big Mic Action Button
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color(0xFFEF4444)
                            else if (isSuccessState) NaturalPrimary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isRecording = true
                                    statusMessage = AppStrings.get("voice_auth_recording", currentLanguage)
                                    val job = coroutineScope.launch {
                                        val audioSamples = viewModel.audioRecorder.recordAudioClip(3)
                                        isRecording = false
                                        if (audioSamples.isNotEmpty()) {
                                            if (mode == VoiceAuthMode.ENROLL) {
                                                val enrolled = viewModel.voiceAuthRepository.enrollUser(audioSamples)
                                                if (enrolled) {
                                                    isSuccessState = true
                                                    statusMessage = AppStrings.get("voice_auth_enrolled", currentLanguage)
                                                    onSuccess()
                                                }
                                            } else {
                                                val (verified, score) = viewModel.voiceAuthRepository.verifyUser(audioSamples)
                                                matchScore = score
                                                if (verified) {
                                                    isSuccessState = true
                                                    statusMessage = "Voice Verified (${(score * 100).toInt()}% Match)"
                                                    onSuccess()
                                                } else {
                                                    statusMessage = "Voice Mismatch (${(score * 100).toInt()}% Match). Speak clearly."
                                                }
                                            }
                                        } else {
                                            statusMessage = "No audio captured. Check microphone permission."
                                        }
                                    }
                                    tryAwaitRelease()
                                    viewModel.audioRecorder.stop()
                                    isRecording = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccessState) Icons.Default.Check else Icons.Default.Mic,
                        contentDescription = "Hold to record",
                        tint = if (isRecording || isSuccessState) Color.White else NaturalPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = if (isRecording) "Listening (Speak for 3s)..." else "Press & Hold Mic Button",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isRecording) Color(0xFFEF4444) else NaturalPrimary
                )

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuccessState) NaturalPrimary else NaturalDarkTextMuted,
                        textAlign = TextAlign.Center
                    )
                }

                // Privacy notice footer
                Surface(
                    color = Color(0xFF131A14),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NaturalForestAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "100% Offline & Encrypted On-Device",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }
            }
        }
    }
}

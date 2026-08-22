package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.viewmodel.MainViewModel

data class TutorialStep(
    val stepNumber: Int,
    val titleKey: String,
    val descKey: String,
    val icon: ImageVector,
    val badgeLabel: String,
    val tipHighlight: String
)

@Composable
fun InteractiveTutorialOverlay(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    var currentStepIndex by remember { mutableStateOf(0) }

    val steps = remember {
        listOf(
            TutorialStep(
                stepNumber = 1,
                titleKey = "tutorial_step1_title",
                descKey = "tutorial_step1_desc",
                icon = Icons.Default.AutoStories,
                badgeLabel = "Library & Formats",
                tipHighlight = "Tip: Tap 'Import Document' to add any PDF, EPUB, or TXT file directly from your device!"
            ),
            TutorialStep(
                stepNumber = 2,
                titleKey = "tutorial_step2_title",
                descKey = "tutorial_step2_desc",
                icon = Icons.Default.LocalFireDepartment,
                badgeLabel = "Streak & Timer",
                tipHighlight = "Tip: Reading timer tracks active seconds attentively. Daily reading builds streak fire and earns shields!"
            ),
            TutorialStep(
                stepNumber = 3,
                titleKey = "tutorial_step3_title",
                descKey = "tutorial_step3_desc",
                icon = Icons.Default.Bolt,
                badgeLabel = "RSVP & Audio",
                tipHighlight = "Tip: Inside any book, tap the Flash icon for rapid RSVP speed reading or Headphone icon for TTS!"
            ),
            TutorialStep(
                stepNumber = 4,
                titleKey = "tutorial_step4_title",
                descKey = "tutorial_step4_desc",
                icon = Icons.Default.Mic,
                badgeLabel = "Voice Studio & Privacy",
                tipHighlight = "Tip: Head to Settings to train a custom Voice Profile that sounds like you—processed 100% on device."
            )
        )
    }

    val activeStep = steps[currentStepIndex]

    // Pulsing Beacon Animation
    val infiniteTransition = rememberInfiniteTransition(label = "beaconPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Full screen modal overlay
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* keep inside */ }
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pulsing Spotlight Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(NaturalPrimary.copy(alpha = pulseAlpha))
                )

                // Inner Main Circle
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NaturalPrimary, NaturalDarkSurfaceVariant)
                            )
                        )
                        .border(2.dp, NaturalPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        activeStep.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Interactive Dialog Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Step Pill & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = NaturalPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Step ${currentStepIndex + 1} of ${steps.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalPrimary
                                )
                                Text("•", color = NaturalDarkTextMuted)
                                Text(
                                    text = activeStep.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = NaturalDarkTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { (currentStepIndex + 1).toFloat() / steps.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = NaturalPrimary,
                        trackColor = NaturalDarkSurfaceVariant
                    )

                    // Title & Description
                    Text(
                        text = AppStrings.get(activeStep.titleKey, currentLanguage),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = AppStrings.get(activeStep.descKey, currentLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaturalDarkTextMuted,
                        lineHeight = 20.sp
                    )

                    // Pro-Tip Highlight Box
                    Surface(
                        color = NaturalForestAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NaturalForestAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = NaturalPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = activeStep.tipHighlight,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = NaturalSageAccent,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Bottom Navigation Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = { currentStepIndex-- },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NaturalDarkBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Prev", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("Skip Tour", color = NaturalDarkTextMuted, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Button(
                            onClick = {
                                if (currentStepIndex < steps.size - 1) {
                                    currentStepIndex++
                                } else {
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                        ) {
                            Text(
                                text = if (currentStepIndex < steps.size - 1) "Next Step" else "Finish & Explore",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            if (currentStepIndex < steps.size - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val books by viewModel.allBooks.collectAsState()
    val highlights by viewModel.allHighlights.collectAsState()

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

        // Instagram Creator Card in Natural Tones
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalSageBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NaturalPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Instagram Logo",
                                tint = NaturalOnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Instagram: @ahex0_01",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NaturalSageAccent)
                            )
                            Text(
                                text = "Official Creator & Developer Profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalSageMuted
                            )
                        }
                    }

                    Text(
                        text = "Connect on Instagram @ahex0_01 for feature requests, book recommendations, streak updates, and community reading challenges.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalSageMuted,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { viewModel.openInstagramProfile() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NaturalPrimary,
                            contentColor = NaturalOnPrimary
                        )
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open @ahex0_01 on Instagram", fontWeight = FontWeight.SemiBold)
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
                        Text("12.4 MB (Encrypted)", fontWeight = FontWeight.SemiBold, color = NaturalDarkText)
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
                    FeatureRow(icon = Icons.Default.Check, text = "Offline access with cloud backup synchronization")
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


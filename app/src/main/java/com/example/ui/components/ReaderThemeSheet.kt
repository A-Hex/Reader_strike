package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FontFamilyPreference
import com.example.model.ReaderPreferences
import com.example.model.ReaderTheme
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderThemeSheet(
    preferences: ReaderPreferences,
    onUpdateTheme: (String) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateLineSpacing: (Float) -> Unit,
    onUpdateFontFamily: (FontFamilyPreference) -> Unit,
    onUpdateMargins: (Float) -> Unit,
    onToggleNightLight: (Boolean) -> Unit,
    onUpdateBrightness: (Float) -> Unit,
    onTogglePagedMode: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Reader Customization",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Theme Color Palettes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Reading Theme",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = NaturalDarkTextMuted)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ReaderTheme.ALL_THEMES) { theme ->
                        val isSelected = preferences.themeId == theme.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onUpdateTheme(theme.id) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(theme.backgroundColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) theme.accentColor else NaturalDarkBorder,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = theme.accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Aa",
                                        color = theme.textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.name.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) NaturalPrimary else NaturalDarkTextMuted
                            )
                        }
                    }
                }
            }

            // Font Family
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Typography Font",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = NaturalDarkTextMuted)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontFamilyPreference.entries.forEach { font ->
                        val isSelected = preferences.fontFamily == font
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdateFontFamily(font) },
                            label = {
                                Text(
                                    text = font.displayName.substringBefore(" ("),
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaturalPrimary,
                                selectedLabelColor = NaturalOnPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = NaturalDarkText
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) NaturalPrimary else NaturalDarkBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Font Size Stepper & Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.FormatSize, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Font Size (${preferences.fontSizeSp.toInt()} sp)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onUpdateFontSize(preferences.fontSizeSp - 1f) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalDarkText)
                        }
                        OutlinedButton(
                            onClick = { onUpdateFontSize(preferences.fontSizeSp + 1f) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("A+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalDarkText)
                        }
                    }
                }
                Slider(
                    value = preferences.fontSizeSp,
                    onValueChange = onUpdateFontSize,
                    valueRange = 12f..30f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = NaturalPrimary,
                        activeTrackColor = NaturalPrimary,
                        inactiveTrackColor = NaturalDarkBorder
                    )
                )
            }

            // Line Spacing
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Line Height Spacing",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = NaturalDarkTextMuted)
                    )
                    Text(
                        text = String.format("%.1fx", preferences.lineSpacingMultiplier),
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalPrimary
                    )
                }
                Slider(
                    value = preferences.lineSpacingMultiplier,
                    onValueChange = onUpdateLineSpacing,
                    valueRange = 1.2f..2.4f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = NaturalPrimary,
                        activeTrackColor = NaturalPrimary,
                        inactiveTrackColor = NaturalDarkBorder
                    )
                )
            }

            // Page Mode & Night Light
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTogglePagedMode() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Navigation",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                            Text(
                                text = if (preferences.isPagedMode) "Page Turn" else "Continuous",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalDarkText
                            )
                        }
                        Switch(
                            checked = preferences.isPagedMode,
                            onCheckedChange = { onTogglePagedMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaturalOnPrimary,
                                checkedTrackColor = NaturalPrimary
                            )
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleNightLight(!preferences.isNightLightFilter) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Warm Filter",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                            Text(
                                text = if (preferences.isNightLightFilter) "Amber On" else "Off",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalDarkText
                            )
                        }
                        Switch(
                            checked = preferences.isNightLightFilter,
                            onCheckedChange = onToggleNightLight,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaturalOnPrimary,
                                checkedTrackColor = NaturalPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}


package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.LocalLlmBackend
import com.example.ai.LocalLlmEngine
import com.example.ai.LocalLlmModel
import com.example.ui.theme.NaturalDarkBorder
import com.example.ui.theme.NaturalDarkTextMuted
import com.example.ui.theme.NaturalOchreAccent
import com.example.ui.theme.NaturalOnPrimary
import com.example.ui.theme.NaturalPrimary
import com.example.ui.theme.NaturalSageAccent
import com.example.ui.theme.NaturalSageBg

/**
 * Settings entry point for the on-device LLM.
 *
 * A `.litertlm` bundle is 0.5-2 GB, so the app never ships one: this card reports whether a model
 * is installed, imports one the user picks, and controls how the assistant prefers it. All state
 * comes from [LocalLlmModel], so nothing here can claim a model that is not actually on disk.
 */
@Composable
fun OnDeviceModelCard(
    isImporting: Boolean,
    modelRevision: Int,
    onRequestImport: () -> Unit,
    onModelRemoved: () -> Unit
) {
    val context = LocalContext.current
    var backend by remember { mutableStateOf(LocalLlmModel.backend(context)) }
    var preferLocal by remember { mutableStateOf(LocalLlmModel.preferLocal(context)) }

    // Re-read from disk whenever a model is installed or removed.
    val installedName = remember(modelRevision) { LocalLlmModel.displayName(context) }
    val installedSize = remember(modelRevision) { LocalLlmModel.sizeLabel(context) }
    val hasModel = installedName.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            1.dp,
            if (hasModel) NaturalSageAccent.copy(alpha = 0.6f) else NaturalPrimary.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = NaturalOnPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Reading AI",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "On-device model (LiteRT-LM)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalPrimary
                        )
                    }
                }

                Surface(
                    color = if (hasModel) NaturalSageBg else NaturalOchreAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (hasModel) "INSTALLED" else "NOT INSTALLED",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (hasModel) NaturalSageAccent else NaturalOchreAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Run the reading assistant on a language model that lives on this phone: no API key, " +
                    "no network, and no passage ever leaves the device. The app ships no model of its own, so " +
                    "install a bundle you choose and it backs summaries, analysis, character maps and Ask.",
                style = MaterialTheme.typography.bodySmall,
                color = NaturalDarkTextMuted
            )

            if (hasModel) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ACTIVE MODEL",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = NaturalDarkTextMuted
                        )
                        Text(
                            text = installedName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$installedSize \u2022 ${backend.label} backend" +
                                if (LocalLlmEngine.isLoaded()) " \u2022 loaded in memory" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalPrimary
                        )
                    }
                }
            }

            if (isImporting) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = NaturalPrimary,
                        trackColor = NaturalDarkBorder
                    )
                    Text(
                        text = "Copying the model into app storage. Keep the app open.",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRequestImport,
                    enabled = !isImporting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = NaturalOnPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasModel) "Replace" else "Install model",
                        color = NaturalOnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        LocalLlmModel.deleteInstalledModels(context)
                        onModelRemoved()
                        Toast.makeText(context, "On-device model removed.", Toast.LENGTH_SHORT).show()
                    },
                    enabled = hasModel && !isImporting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NaturalDarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "COMPUTE BACKEND",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = NaturalDarkTextMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocalLlmBackend.entries.forEach { option ->
                        FilterChip(
                            selected = backend == option,
                            onClick = {
                                backend = option
                                LocalLlmModel.setBackend(context, option)
                                Toast.makeText(
                                    context,
                                    "Backend set to ${option.label}. The model reloads on the next request.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            label = { Text(option.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaturalPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = NaturalPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = backend == option,
                                borderColor = if (backend == option) NaturalPrimary else NaturalDarkBorder
                            )
                        )
                    }
                }
                Text(
                    text = "CPU works on every supported device. GPU is faster where the vendor exposes OpenCL.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaturalDarkTextMuted
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prefer the on-device model",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "When a model is installed, use it instead of the Gemini key.",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }
                Switch(
                    checked = preferLocal,
                    onCheckedChange = { checked ->
                        preferLocal = checked
                        LocalLlmModel.setPreferLocal(context, checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NaturalOnPrimary,
                        checkedTrackColor = NaturalPrimary
                    )
                )
            }

            Text(
                text = "Install a .litertlm bundle (for example Gemma 3 1B IT from the LiteRT community on " +
                    "Hugging Face), or push one for development with " +
                    "adb push model.litertlm /data/local/tmp/llm/. Model files stay on this device and are " +
                    "excluded from library backups and cloud sync.",
                style = MaterialTheme.typography.labelSmall,
                color = NaturalDarkTextMuted
            )
        }
    }
}

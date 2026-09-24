package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionLoop
import com.example.model.ActionLoopStage
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

/**
 * The "Read -> Build" habit, as a real workflow instead of a slogan:
 *
 *   1. Read    - note what you read.
 *   2. Compress- shrink a real problem in your life to a sentence or two.
 *   3. Apply   - name the idea in the book that speaks to that problem.
 *   4. Build   - commit to one action in the next 24 hours.
 *
 * Everything is typed by the reader and stored only on this device.
 */
@Composable
fun ActionLoopScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    initialBookTitle: String? = null
) {
    val loops by viewModel.actionLoops.collectAsState()
    val books by viewModel.allBooks.collectAsState()

    // Opened from a book: start a fresh loop with that book already named, so the reader only
    // has to write the problem, the idea and the action.
    val seedText = initialBookTitle?.trim().orEmpty()
    var showEditor by remember { mutableStateOf(seedText.isNotBlank()) }
    var editingLoop by remember {
        mutableStateOf(if (seedText.isBlank()) null else ActionLoop(id = "", bookTitle = seedText))
    }
    var pendingDelete by remember { mutableStateOf<ActionLoop?>(null) }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NaturalPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Read \u2192 Build",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Read it, compress the problem, apply the idea, build it",
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalDarkTextMuted
                )
            }
            FilledTonalButton(
                onClick = {
                    editingLoop = null
                    showEditor = true
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = NaturalPrimary,
                    contentColor = NaturalOnPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { LoopExplainerCard() }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile("Loops", loops.size.toString(), NaturalPrimary, Modifier.weight(1f))
                    StatTile("Built", loops.count { it.isBuilt }.toString(), NaturalSageAccent, Modifier.weight(1f))
                    StatTile(
                        "In progress",
                        loops.count { !it.isBuilt }.toString(),
                        NaturalOchreAccent,
                        Modifier.weight(1f)
                    )
                }
            }

            if (loops.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No loops yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Start with the book you are reading now. Next time you hit a real problem, " +
                                    "the loop gives you a place to turn a page into a decision.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalDarkTextMuted
                            )
                        }
                    }
                }
            }

            items(loops, key = { it.id }) { loop ->
                LoopCard(
                    loop = loop,
                    onEdit = {
                        editingLoop = loop
                        showEditor = true
                    },
                    onBuild = { viewModel.markActionLoopBuilt(loop.id) },
                    onDelete = { pendingDelete = loop }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showEditor) {
        ActionLoopEditorDialog(
            existing = editingLoop,
            libraryTitles = books.map { it.title }.distinct().take(20),
            onDismiss = { showEditor = false },
            onSave = { draft ->
                viewModel.saveActionLoop(draft)
                showEditor = false
            }
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this loop?") },
            text = { Text("The four steps you wrote for this loop will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteActionLoop(target.id)
                    pendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun LoopExplainerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "The habit",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            ActionLoopStage.entries.forEach { stage ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(NaturalPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stage.stepNumber.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stage.title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stage.prompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalDarkTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = NaturalDarkTextMuted
            )
        }
    }
}

@Composable
private fun LoopCard(
    loop: ActionLoop,
    onEdit: () -> Unit,
    onBuild: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (loop.isBuilt) NaturalSageAccent.copy(alpha = 0.6f) else NaturalDarkBorder.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loop.bookTitle.ifBlank { "Personal loop" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${loop.completedSteps}/4 steps written",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }
                Surface(
                    color = if (loop.isBuilt) NaturalSageBg else NaturalOchreAccent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (loop.isBuilt) "BUILT" else "IN PROGRESS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (loop.isBuilt) NaturalSageAccent else NaturalOchreAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { loop.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (loop.isBuilt) NaturalSageAccent else NaturalPrimary,
                trackColor = NaturalDarkBorder
            )

            StageRow(ActionLoopStage.READ, loop.readStep, loop)
            StageRow(ActionLoopStage.COMPRESS, loop.problem, loop)
            StageRow(ActionLoopStage.APPLY, loop.bookIdea, loop)
            StageRow(ActionLoopStage.BUILD, loop.action, loop)

            if (!loop.isBuilt) {
                Button(
                    onClick = onBuild,
                    enabled = loop.isReadyToBuild,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalSageAccent)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (loop.isReadyToBuild) {
                            "Build it - " + loop.action.take(40)
                        } else {
                            "Write step " + (loop.nextStage?.stepNumber ?: 4) + " to build"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NaturalOnPrimary,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalPrimary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StageRow(stage: ActionLoopStage, value: String, loop: ActionLoop) {
    val filled = value.isNotBlank()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = if (filled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (filled) NaturalSageAccent else NaturalDarkTextMuted,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${stage.stepNumber}. ${stage.title}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (filled) value else stage.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = if (filled) NaturalDarkText else NaturalDarkTextMuted
            )
            if (!filled && loop.nextStage == stage) {
                Text(
                    text = "Next step",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = NaturalPrimary
                )
            }
        }
    }
}

@Composable
private fun ActionLoopEditorDialog(
    existing: ActionLoop?,
    libraryTitles: List<String>,
    onDismiss: () -> Unit,
    onSave: (ActionLoop) -> Unit
) {
    var bookTitle by remember { mutableStateOf(existing?.bookTitle.orEmpty()) }
    var readNote by remember { mutableStateOf(existing?.readNote.orEmpty()) }
    var problem by remember { mutableStateOf(existing?.problem.orEmpty()) }
    var bookIdea by remember { mutableStateOf(existing?.bookIdea.orEmpty()) }
    var action by remember { mutableStateOf(existing?.action.orEmpty()) }

    val canSave = problem.isNotBlank() || bookIdea.isNotBlank() || action.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null || existing.id.isBlank()) "New loop" else "Edit loop",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Write it in your own words. Nothing is generated for you.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaturalDarkTextMuted
                )

                LoopField(
                    label = "1. Read - which book or passage?",
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    minLines = 1
                )

                if (libraryTitles.isNotEmpty()) {
                    Text(
                        text = "From your library",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = NaturalDarkTextMuted
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(libraryTitles) { title ->
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { bookTitle = title }
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                LoopField(
                    label = "1b. Notes on what you read (optional)",
                    value = readNote,
                    onValueChange = { readNote = it },
                    minLines = 2
                )

                LoopField(
                    label = "2. Compress the problem",
                    value = problem,
                    onValueChange = { problem = it },
                    minLines = 2
                )

                LoopField(
                    label = "3. Apply the idea",
                    value = bookIdea,
                    onValueChange = { bookIdea = it },
                    minLines = 2
                )

                LoopField(
                    label = "4. Build it - next 24 hours",
                    value = action,
                    onValueChange = { action = it },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        (existing ?: ActionLoop(id = "")).copy(
                            bookTitle = bookTitle.trim(),
                            readNote = readNote.trim(),
                            problem = problem.trim(),
                            bookIdea = bookIdea.trim(),
                            action = action.trim()
                        )
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
            ) {
                Text("Save", color = NaturalOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LoopField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) },
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NaturalPrimary,
            unfocusedBorderColor = NaturalDarkBorder,
            focusedLabelColor = NaturalPrimary,
            cursorColor = NaturalPrimary
        )
    )
}

package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.MindMapProvider
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class MindMapTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    NETWORK("Character Network", Icons.Default.AccountTree),
    PLOT_ARC("Plot Arc & Story", Icons.Default.Timeline),
    RELATION_MATRIX("Relations & Evidence", Icons.Default.Hub)
}

@Composable
fun CharacterMindMapDialog(
    book: Book,
    bookContentText: String = "",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mindMap by remember(book) {
        mutableStateOf(MindMapProvider.getMindMapForBook(book, bookContentText))
    }

    var selectedTab by remember { mutableStateOf(MindMapTab.NETWORK) }
    var selectedCharacter by remember { mutableStateOf<CharacterNode?>(mindMap.nodes.firstOrNull()) }
    var selectedPlotPoint by remember { mutableStateOf<PlotNode?>(mindMap.plotPoints.firstOrNull()) }
    var selectedFilterRelation by remember { mutableStateOf<RelationType?>(null) }
    var isRunningLocalAi by remember { mutableStateOf(false) }
    var showAddCharacterDialog by remember { mutableStateOf(false) }
    var showAddRelationDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val isArabic = book.languageCode == "ar" || book.title.any { it in '\u0600'..'\u06FF' }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides (if (isArabic) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr)
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
                border = BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.6f))
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Bar
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF8E24AA), NaturalPrimary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = NaturalOnPrimary, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Character & Plot Mind Map",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = NaturalPrimary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Text Parser",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = NaturalPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${book.title} • ${mindMap.nodes.size} Characters • ${mindMap.edges.size} Relations",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isRunningLocalAi = true
                                    delay(400)
                                    val updated = MindMapProvider.getMindMapForBook(book, bookContentText, forceAiDetection = true)
                                    mindMap = updated
                                    selectedCharacter = updated.nodes.firstOrNull()
                                    selectedPlotPoint = updated.plotPoints.firstOrNull()
                                    isRunningLocalAi = false
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isRunningLocalAi) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NaturalPrimary)
                            } else {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = "Re-analyze Text Structure", tint = NaturalPrimary)
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                        }
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    MindMapTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Text(tab.label, fontSize = 11.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            selectedContentColor = NaturalPrimary,
                            unselectedContentColor = NaturalDarkTextMuted
                        )
                    }
                }

                // Main Content Body based on tab
                when (selectedTab) {
                    MindMapTab.NETWORK -> {
                        CharacterNetworkTab(
                            mindMap = mindMap,
                            selectedCharacter = selectedCharacter,
                            selectedFilterRelation = selectedFilterRelation,
                            onSelectCharacter = { selectedCharacter = it },
                            onFilterRelation = { selectedFilterRelation = if (selectedFilterRelation == it) null else it },
                            onAddCharacter = { showAddCharacterDialog = true },
                            onAddRelation = { showAddRelationDialog = true }
                        )
                    }
                    MindMapTab.PLOT_ARC -> {
                        PlotArcTab(
                            plotPoints = mindMap.plotPoints,
                            selectedPlotPoint = selectedPlotPoint,
                            characters = mindMap.nodes,
                            onSelectPlotPoint = { selectedPlotPoint = it }
                        )
                    }
                    MindMapTab.RELATION_MATRIX -> {
                        RelationsEvidenceTab(
                            mindMap = mindMap,
                            onSelectNode = { nodeId ->
                                selectedCharacter = mindMap.nodes.find { it.id == nodeId }
                                selectedTab = MindMapTab.NETWORK
                            }
                        )
                    }
                }
            }
        }
    }
}

    // Dialog for adding custom character
    if (showAddCharacterDialog) {
        AddCustomCharacterDialog(
            onDismiss = { showAddCharacterDialog = false },
            onAdd = { name, role, faction, desc, quote, emoji ->
                val newNode = CharacterNode(
                    id = "custom_${System.currentTimeMillis()}",
                    name = name,
                    role = role,
                    faction = faction,
                    description = desc,
                    keyQuote = quote,
                    avatarEmoji = emoji,
                    xPercent = 0.5f,
                    yPercent = 0.5f,
                    significance = 1.1f,
                    isUserCustom = true
                )
                mindMap = MindMapProvider.addCustomNode(book.id, newNode)
                selectedCharacter = newNode
                showAddCharacterDialog = false
            }
        )
    }

    // Dialog for adding custom relation
    if (showAddRelationDialog) {
        AddCustomRelationDialog(
            nodes = mindMap.nodes,
            onDismiss = { showAddRelationDialog = false },
            onAdd = { fromId, toId, label, relType, quote ->
                val newEdge = RelationshipEdge(
                    fromNodeId = fromId,
                    toNodeId = toId,
                    label = label,
                    relationType = relType,
                    evidenceQuotes = if (quote.isNotBlank()) listOf(quote) else emptyList(),
                    isAiDetected = false
                )
                mindMap = MindMapProvider.addCustomEdge(book.id, newEdge)
                showAddRelationDialog = false
            }
        )
    }
}

@Composable
private fun CharacterNetworkTab(
    mindMap: BookMindMap,
    selectedCharacter: CharacterNode?,
    selectedFilterRelation: RelationType?,
    onSelectCharacter: (CharacterNode) -> Unit,
    onFilterRelation: (RelationType) -> Unit,
    onAddCharacter: () -> Unit,
    onAddRelation: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Relation filter chips & Add Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(RelationType.entries) { rel ->
                    val isFiltered = selectedFilterRelation == rel
                    Surface(
                        color = if (isFiltered) Color(rel.colorHex) else Color(rel.colorHex).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(rel.colorHex).copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onFilterRelation(rel) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isFiltered) Color.White else Color(rel.colorHex)))
                            Text(
                                rel.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = if (isFiltered) FontWeight.Bold else FontWeight.Normal),
                                color = if (isFiltered) Color.White else Color(rel.colorHex)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onAddCharacter, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Character", tint = NaturalPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onAddRelation, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.AddLink, contentDescription = "Add Relationship", tint = NaturalOchreAccent, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Interactive 2D Graph Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B100C))
                .border(1.dp, NaturalDarkBorder, RoundedCornerShape(16.dp))
        ) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight

            // Relationship Lines Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Grid background
                val gridSize = 36.dp.toPx()
                var x = 0f
                while (x < w) {
                    drawLine(Color(0x0CFFFFFF), Offset(x, 0f), Offset(x, h), 1f)
                    x += gridSize
                }
                var y = 0f
                while (y < h) {
                    drawLine(Color(0x0CFFFFFF), Offset(0f, y), Offset(w, y), 1f)
                    y += gridSize
                }

                // Render Edges
                val filteredEdges = mindMap.edges.filter {
                    selectedFilterRelation == null || it.relationType == selectedFilterRelation
                }

                for (edge in filteredEdges) {
                    val fromNode = mindMap.nodes.find { it.id == edge.fromNodeId }
                    val toNode = mindMap.nodes.find { it.id == edge.toNodeId }
                    if (fromNode != null && toNode != null) {
                        val isHighlighted = selectedCharacter != null && (fromNode.id == selectedCharacter.id || toNode.id == selectedCharacter.id)
                        val start = Offset(fromNode.xPercent * w, fromNode.yPercent * h)
                        val end = Offset(toNode.xPercent * w, toNode.yPercent * h)
                        val edgeColor = Color(edge.relationType.colorHex)

                        drawLine(
                            color = if (isHighlighted) edgeColor else edgeColor.copy(alpha = 0.35f),
                            start = start,
                            end = end,
                            strokeWidth = if (isHighlighted) 3.5.dp.toPx() else 1.8.dp.toPx(),
                            pathEffect = if (edge.relationType == RelationType.RIVAL || edge.relationType == RelationType.ANTAGONISTIC) {
                                PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            } else null
                        )
                    }
                }
            }

            // Node overlay items
            mindMap.nodes.forEach { node ->
                val isSelected = selectedCharacter?.id == node.id
                val nodeSize = (44 * node.significance).dp

                Box(
                    modifier = Modifier
                        .offset(
                            x = (canvasWidth * node.xPercent - (nodeSize / 2)).coerceIn(2.dp, canvasWidth - nodeSize - 2.dp),
                            y = (canvasHeight * node.yPercent - (nodeSize / 2)).coerceIn(2.dp, canvasHeight - nodeSize - 2.dp)
                        )
                        .size(nodeSize)
                        .clip(CircleShape)
                        .background(if (isSelected) NaturalPrimary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            2.dp,
                            if (isSelected) NaturalOchreAccent else NaturalPrimary.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .clickable { onSelectCharacter(node) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = node.avatarEmoji, fontSize = (16 * node.significance).sp)
                }

                // Name label under node
                Box(
                    modifier = Modifier
                        .offset(
                            x = (canvasWidth * node.xPercent - 45.dp).coerceIn(2.dp, canvasWidth - 92.dp),
                            y = (canvasHeight * node.yPercent + (nodeSize / 2) + 2.dp).coerceIn(2.dp, canvasHeight - 24.dp)
                        )
                        .width(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = NaturalDarkBackground.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, if (isSelected) NaturalPrimary else Color(0x30FFFFFF))
                    ) {
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) NaturalPrimary else Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // Character Detail Inspector
        if (selectedCharacter != null) {
            CharacterDetailCard(character = selectedCharacter, edges = mindMap.edges, allNodes = mindMap.nodes)
        }
    }
}

@Composable
private fun CharacterDetailCard(
    character: CharacterNode,
    edges: List<RelationshipEdge>,
    allNodes: List<CharacterNode>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(character.avatarEmoji, fontSize = 22.sp)
                    Column {
                        Text(character.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text("${character.role} • ${character.faction}", style = MaterialTheme.typography.labelSmall, color = NaturalPrimary)
                    }
                }

                Surface(
                    color = NaturalDarkBackground,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${character.mentionCount} mentions",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = NaturalDarkTextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = character.description,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = NaturalDarkText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (character.keyQuote.isNotBlank()) {
                Surface(
                    color = NaturalDarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${character.keyQuote}\"",
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic, color = NaturalOchreAccent, fontSize = 10.sp),
                        modifier = Modifier.padding(6.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Relations of this character
            val nodeEdges = edges.filter { it.fromNodeId == character.id || it.toNodeId == character.id }
            if (nodeEdges.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    items(nodeEdges) { edge ->
                        val otherId = if (edge.fromNodeId == character.id) edge.toNodeId else edge.fromNodeId
                        val otherName = allNodes.find { it.id == otherId }?.name ?: "Unknown"
                        Surface(
                            color = Color(edge.relationType.colorHex).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(edge.relationType.colorHex).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "→ $otherName (${edge.relationType.displayName})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color(edge.relationType.colorHex),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlotArcTab(
    plotPoints: List<PlotNode>,
    selectedPlotPoint: PlotNode?,
    characters: List<CharacterNode>,
    onSelectPlotPoint: (PlotNode) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Story Arc & Turning Points",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(plotPoints) { point ->
                val isSelected = selectedPlotPoint?.id == point.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPlotPoint(point) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF141C15)
                    ),
                    border = BorderStroke(1.dp, if (isSelected) Color(point.stage.colorHex) else NaturalDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = Color(point.stage.colorHex).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = point.stage.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = Color(point.stage.colorHex),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = point.chapter,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(point.conflictType.iconEmoji, fontSize = 12.sp)
                                Text(
                                    text = "Tension: ${(point.tensionLevel * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (point.tensionLevel > 0.7f) Color(0xFFEF5350) else NaturalPrimary
                                )
                            }
                        }

                        Text(
                            text = point.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )

                        Text(
                            text = point.summary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                            color = NaturalDarkText
                        )

                        if (point.keyEventQuote.isNotBlank()) {
                            Surface(
                                color = NaturalDarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "\"${point.keyEventQuote}\"",
                                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic, fontSize = 9.5.sp, color = NaturalOchreAccent),
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationsEvidenceTab(
    mindMap: BookMindMap,
    onSelectNode: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Extracted Relationship Evidence & Quotes",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mindMap.edges) { edge ->
                val from = mindMap.nodes.find { it.id == edge.fromNodeId }
                val to = mindMap.nodes.find { it.id == edge.toNodeId }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, Color(edge.relationType.colorHex).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${from?.avatarEmoji ?: "👤"} ${from?.name ?: "Unknown"}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("↔", color = NaturalDarkTextMuted)
                                Text("${to?.avatarEmoji ?: "👤"} ${to?.name ?: "Unknown"}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }

                            Surface(
                                color = Color(edge.relationType.colorHex).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = edge.relationType.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = Color(edge.relationType.colorHex),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = edge.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = NaturalDarkText
                        )

                        if (edge.evidenceQuotes.isNotEmpty()) {
                            Surface(
                                color = NaturalDarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Verbatim Evidence Quote:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = NaturalDarkTextMuted))
                                    Text(
                                        text = "\"${edge.evidenceQuotes.first()}\"",
                                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic, fontSize = 9.5.sp, color = NaturalOchreAccent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomCharacterDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, role: String, faction: String, desc: String, quote: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Supporting Figure") }
    var faction by remember { mutableStateOf("Independent") }
    var desc by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("👤") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Character", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Character Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role / Archetype") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = faction,
                    onValueChange = { faction = it },
                    label = { Text("Faction / Allegiance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description & Role") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, role, faction, desc.ifBlank { "Custom added character." }, quote, emoji)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add Node")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddCustomRelationDialog(
    nodes: List<CharacterNode>,
    onDismiss: () -> Unit,
    onAdd: (fromId: String, toId: String, label: String, relType: RelationType, quote: String) -> Unit
) {
    var fromNodeId by remember { mutableStateOf(nodes.firstOrNull()?.id ?: "") }
    var toNodeId by remember { mutableStateOf(nodes.getOrNull(1)?.id ?: nodes.firstOrNull()?.id ?: "") }
    var label by remember { mutableStateOf("Narrative dynamic") }
    var relType by remember { mutableStateOf(RelationType.ALLY) }
    var quote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Relationship Edge", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Relationship Type", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(RelationType.entries) { type ->
                        FilterChip(
                            selected = relType == type,
                            onClick = { relType = type },
                            label = { Text(type.displayName, fontSize = 9.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Relationship Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quote,
                    onValueChange = { quote = it },
                    label = { Text("Evidence Quote (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fromNodeId.isNotBlank() && toNodeId.isNotBlank()) {
                        onAdd(fromNodeId, toNodeId, label, relType, quote)
                    }
                }
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

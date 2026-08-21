package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.MindMapProvider
import com.example.model.Book
import com.example.model.BookMindMap
import com.example.model.CharacterNode
import com.example.model.RelationType
import com.example.ui.theme.*

@Composable
fun CharacterMindMapDialog(
    book: Book,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mindMap = remember(book) {
        MindMapProvider.getMindMapForBook(book)
    }

    var selectedCharacter by remember {
        mutableStateOf<CharacterNode?>(mindMap.nodes.firstOrNull())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
            border = BorderStroke(1.5.dp, NaturalPrimary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF8E24AA), NaturalPrimary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, tint = NaturalOnPrimary, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = "Character & Plot Map",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${book.title} • Interactive Network",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalDarkTextMuted)
                    }
                }

                // Legend chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val relations = listOf(
                        "Ally" to RelationType.ALLY,
                        "Rival" to RelationType.RIVAL,
                        "Mentor" to RelationType.MENTOR,
                        "Investigation" to RelationType.INVESTIGATION,
                        "Creation" to RelationType.CREATION,
                        "Kinship" to RelationType.KINSHIP
                    )
                    items(relations) { (name, rel) ->
                        Surface(
                            color = Color(rel.colorHex).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(rel.colorHex).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(rel.colorHex)))
                                Text(name, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color(rel.colorHex))
                            }
                        }
                    }
                }

                // Interactive Mind Map Canvas
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0C110D))
                        .border(1.dp, NaturalDarkBorder, RoundedCornerShape(18.dp))
                ) {
                    val canvasWidth = maxWidth
                    val canvasHeight = maxHeight

                    // Canvas to draw relationship lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw subtle grid pattern
                        val gridSize = 40.dp.toPx()
                        var x = 0f
                        while (x < w) {
                            drawLine(
                                color = Color(0x10FFFFFF),
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1f
                            )
                            x += gridSize
                        }
                        var y = 0f
                        while (y < h) {
                            drawLine(
                                color = Color(0x10FFFFFF),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                            y += gridSize
                        }

                        // Draw Relationship Edges
                        for (edge in mindMap.edges) {
                            val fromNode = mindMap.nodes.find { it.id == edge.fromNodeId }
                            val toNode = mindMap.nodes.find { it.id == edge.toNodeId }
                            if (fromNode != null && toNode != null) {
                                val start = Offset(fromNode.xPercent * w, fromNode.yPercent * h)
                                val end = Offset(toNode.xPercent * w, toNode.yPercent * h)
                                val edgeColor = Color(edge.relationType.colorHex)

                                drawLine(
                                    color = edgeColor.copy(alpha = 0.7f),
                                    start = start,
                                    end = end,
                                    strokeWidth = 3.dp.toPx(),
                                    pathEffect = if (edge.relationType == RelationType.RIVAL) PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) else null
                                )
                            }
                        }
                    }

                    // Render Interactive Node Composables directly over canvas
                    mindMap.nodes.forEach { node ->
                        val isSelected = selectedCharacter?.id == node.id
                        val nodeSize = (48 * node.significance).dp

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = canvasWidth * node.xPercent - (nodeSize / 2),
                                    y = canvasHeight * node.yPercent - (nodeSize / 2)
                                )
                                .size(nodeSize)
                                .clip(CircleShape)
                                .background(if (isSelected) NaturalPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    2.dp,
                                    if (isSelected) NaturalOchreAccent else NaturalPrimary.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .clickable { selectedCharacter = node },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = node.avatarEmoji,
                                fontSize = (18 * node.significance).sp
                            )
                        }

                        // Character Name Tag below node
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (canvasWidth * node.xPercent - 50.dp).coerceAtLeast(4.dp),
                                    y = canvasHeight * node.yPercent + (nodeSize / 2) + 2.dp
                                )
                                .width(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = NaturalDarkBackground.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = node.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) NaturalPrimary else Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Selected Character Detail Inspector
                val char = selectedCharacter
                if (char != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, NaturalPrimary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(char.avatarEmoji, fontSize = 20.sp)
                                    Column {
                                        Text(char.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                        Text("${char.role} • ${char.faction}", style = MaterialTheme.typography.labelSmall, color = NaturalPrimary)
                                    }
                                }
                            }

                            Text(
                                text = char.description,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                color = NaturalDarkText
                            )

                            if (char.keyQuote.isNotBlank()) {
                                Surface(
                                    color = NaturalDarkBackground,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "\"${char.keyQuote}\"",
                                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic, color = NaturalOchreAccent),
                                        modifier = Modifier.padding(8.dp)
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

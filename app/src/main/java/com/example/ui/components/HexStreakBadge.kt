package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BadgeTier
import com.example.model.StreakBadge
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HexagonFlameBadge(
    streakDays: Int,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hex_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val centerX = w / 2f
            val centerY = h / 2f
            val radius = (minOf(w, h) / 2f) - 6f

            // Create Hexagon Path
            val hexPath = Path()
            for (i in 0 until 6) {
                val angle = Math.toRadians((60.0 * i) - 30.0)
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()

            // Fill Hexagon with dark gradient
            drawPath(
                path = hexPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )

            // Draw glowing stroke
            drawPath(
                path = hexPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF59E0B).copy(alpha = glowAlpha),
                        Color(0xFFEF4444).copy(alpha = glowAlpha),
                        Color(0xFF38BDF8).copy(alpha = glowAlpha)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(width = 5.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak Flame",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(size * 0.35f)
            )
            Text(
                text = "$streakDays",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.22f).sp,
                    color = Color.White
                )
            )
            Text(
                text = "DAYS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF38BDF8)
                )
            )
        }
    }
}

@Composable
fun StreakBadgeCard(
    badge: StreakBadge,
    modifier: Modifier = Modifier
) {
    val tierColor = when (badge.tier) {
        BadgeTier.BRONZE -> Color(0xFFCD7F32)
        BadgeTier.SILVER -> Color(0xFFC0C0C0)
        BadgeTier.GOLD -> Color(0xFFFFD700)
        BadgeTier.DIAMOND -> Color(0xFF00E5FF)
    }

    val iconVector: ImageVector = when (badge.iconName) {
        "flag" -> Icons.Default.Flag
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "whatshot" -> Icons.Default.Whatshot
        "auto_stories" -> Icons.Default.AutoStories
        "edit_note" -> Icons.Default.EditNote
        else -> Icons.Default.WorkspacePremium
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (badge.isUnlocked) {
            androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = 0.6f))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (badge.isUnlocked) tierColor.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (badge.isUnlocked) tierColor else Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = badge.title,
                    tint = if (badge.isUnlocked) tierColor else Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = badge.tier.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = tierColor
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                if (!badge.isUnlocked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { badge.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp),
                        color = tierColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

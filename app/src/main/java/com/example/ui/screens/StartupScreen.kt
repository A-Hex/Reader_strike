package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NaturalDarkBackground
import com.example.ui.theme.NaturalDarkText
import com.example.ui.theme.NaturalDarkTextMuted
import com.example.ui.theme.NaturalPrimary
import com.example.ui.theme.NaturalTertiary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The SecureMind launch visual.
 *
 * A one-shot transformation rather than a static splash: orbiting rings lock on, particles fall
 * inward, and the shield mark draws itself segment by segment before the amber core ignites. When
 * the sequence (plus a short hold) finishes, [onFinished] hands control to the sign-in gate.
 */
@Composable
fun SecureMindStartupScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawProgress = remember { Animatable(0f) }
    val ambient = rememberInfiniteTransition(label = "securemind-ambient")
    val rotation by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring-rotation"
    )
    val pulse by ambient.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "core-pulse"
    )

    LaunchedEffect(Unit) {
        drawProgress.animateTo(1f, tween(durationMillis = 2_050, easing = FastOutSlowInEasing))
        delay(700L)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NaturalDarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(232.dp)) {
                drawSecureMindMark(
                    progress = drawProgress.value,
                    rotation = rotation,
                    pulse = pulse
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = "SecureMind",
                color = NaturalDarkText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your private reading mind",
                color = NaturalPrimary,
                fontSize = 13.sp,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "On-device AI  ·  Encrypted backup  ·  No tracking",
                color = NaturalDarkTextMuted,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Mark rendering
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawSecureMindMark(progress: Float, rotation: Float, pulse: Float) {
    val c = center
    val radius = size.minDimension / 2f
    val p = progress.coerceIn(0f, 1f)

    // Ambient glow that grows with the sequence.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(NaturalPrimary.copy(alpha = 0.20f * p), Color.Transparent),
            center = c,
            radius = radius * 1.3f
        ),
        radius = radius * 1.3f,
        center = c
    )

    // Three counter-rotating hex rings that "lock on".
    repeat(3) { index ->
        val ringRadius = radius * (0.56f + index * 0.17f)
        val direction = if (index % 2 == 0) 1f else -1f
        val alpha = (0.20f - index * 0.05f) * p
        if (alpha > 0.01f) {
            drawHexRing(
                center = c,
                radius = ringRadius,
                degrees = rotation * direction + index * 22f,
                color = NaturalPrimary.copy(alpha = alpha)
            )
        }
    }

    val shield = shieldVertices(c, radius * 0.64f)

    // Particles falling inward onto each vertex, then fading as the outline takes over.
    shield.forEachIndexed { index, vertex ->
        val offset = index * 0.055f
        val local = ((p - offset) / (0.82f - offset)).coerceIn(0f, 1f)
        if (local in 0.001f..0.999f) {
            val start = c + (vertex - c) * 2.2f
            val position = lerpOffset(start, vertex, easeOutCubic(local))
            val alpha = sin(PI * local).toFloat().coerceIn(0f, 1f)
            drawCircle(
                color = NaturalPrimary.copy(alpha = alpha * 0.9f),
                radius = 3.2f * (1f - local * 0.45f),
                center = position
            )
        }
    }

    // The shield outline draws itself clockwise.
    val total = shield.size
    val revealed = (p * total).coerceIn(0f, total.toFloat())
    for (i in 0 until total) {
        val fraction = (revealed - i).coerceIn(0f, 1f)
        if (fraction <= 0f) continue
        val from = shield[i]
        val to = shield[(i + 1) % total]
        drawLine(
            color = NaturalPrimary,
            start = from,
            end = lerpOffset(from, to, fraction),
            strokeWidth = 3.4f,
            cap = StrokeCap.Round
        )
    }

    // Amber core ignites once the outline is nearly complete.
    if (p > 0.55f) {
        val ignite = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
        val coreRadius = radius * (0.055f + 0.018f * pulse)
        drawCircle(
            color = NaturalTertiary.copy(alpha = ignite * 0.22f),
            radius = coreRadius * 3f,
            center = c
        )
        drawCircle(
            color = NaturalTertiary.copy(alpha = ignite),
            radius = coreRadius,
            center = c
        )
    }
}

private fun DrawScope.drawHexRing(center: Offset, radius: Float, degrees: Float, color: Color) {
    rotate(degrees = degrees, pivot = center) {
        val path = Path()
        for (i in 0..6) {
            val angle = (PI / 3.0) * i - PI / 2.0
            val x = center.x + radius * cos(angle).toFloat()
            val y = center.y + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = color, style = Stroke(width = 1.6f))
    }
}

/** The seven perimeter points of the SecureMind shield, in draw order. */
private fun shieldVertices(center: Offset, radius: Float): List<Offset> {
    val nx = radius
    val ny = radius * 1.18f
    fun point(x: Float, y: Float) = Offset(center.x + x * nx, center.y + y * ny)
    return listOf(
        point(-0.86f, -0.98f),
        point(0.86f, -0.98f),
        point(0.86f, -0.06f),
        point(0.62f, 0.52f),
        point(0.0f, 0.98f),
        point(-0.62f, 0.52f),
        point(-0.86f, -0.06f)
    )
}

private fun lerpOffset(start: Offset, stop: Offset, fraction: Float): Offset = Offset(
    x = start.x + (stop.x - start.x) * fraction,
    y = start.y + (stop.y - start.y) * fraction
)

private fun easeOutCubic(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    val inv = 1f - clamped
    return 1f - inv * inv * inv
}

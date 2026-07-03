package ru.akarakuts.tictactoe.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import ru.akarakuts.tictactoe.theme.BoardPalette
import kotlin.random.Random

/**
 * Lightweight confetti burst overlay shown on a win. No third-party deps: particles are simulated
 * on a single [Canvas] driven by an infinite transition. The overlay is purely decorative and
 * ignores pointer input so the board underneath stays tappable.
 */
@Composable
fun ConfettiOverlay(palette: BoardPalette) {
    val transition = rememberInfiniteTransition(label = "confetti")
    // A normalized time value cycling 0→1; particles use it plus their phase for motion.
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confettiT",
    )
    val colors = remember(palette) {
        listOf(
            palette.crossMark,
            palette.noughtMark,
            palette.winLine,
            palette.statusText,
            palette.newGameStroke,
        )
    }
    val particles = remember {
        List(70) { i ->
            ConfettiParticle(
                seed = i,
                xRatio = Random(i.toLong() * 2654435761L).nextFloat(),
                startYRatio = Random(i.toLong() * 40503L + 7L).nextFloat() * -0.3f,
                fallSpeed = 0.35f + Random(i.toLong() * 1103515245L).nextFloat() * 0.5f,
                swayAmp = 10f + Random(i.toLong() * 2147483647L).nextFloat() * 40f,
                swayFreq = 1f + Random(i.toLong() * 65537L).nextFloat() * 2f,
                rotationSpeed = Random(i.toLong() * 1299721L).nextFloat() * 540f,
                sizePx = 6f + Random(i.toLong() * 97L).nextFloat() * 8f,
                colorIndex = i,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            particles.forEach { p ->
                val cycle = (t + p.startYRatio) % 1f
                val cx = p.xRatio * w + (kotlin.math.sin((t * p.swayFreq + p.xRatio) * Math.PI.toFloat()) * p.swayAmp)
                val cy = cycle * (h * p.fallSpeed + 60f) - 30f
                if (cy < -40f || cy > h + 40f) return@forEach
                val color = colors[p.colorIndex % colors.size]
                val angle = (t * p.rotationSpeed + p.xRatio * 360f) % 360f
                drawConfetti(
                    center = Offset(cx, cy),
                    sizePx = p.sizePx,
                    angleDeg = angle,
                    color = color,
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val seed: Int,
    val xRatio: Float,
    val startYRatio: Float,
    val fallSpeed: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val rotationSpeed: Float,
    val sizePx: Float,
    val colorIndex: Int,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConfetti(
    center: Offset,
    sizePx: Float,
    angleDeg: Float,
    color: Color,
) {
    val rect = Size(sizePx, sizePx * 0.55f)
    rotate(degrees = angleDeg, pivot = center) {
        drawRect(
            brush = SolidColor(color),
            topLeft = Offset(center.x - rect.width / 2f, center.y - rect.height / 2f),
            size = rect,
        )
    }
}

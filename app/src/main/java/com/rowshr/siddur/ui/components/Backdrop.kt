package com.rowshr.siddur.ui.components

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.rowshr.siddur.ui.theme.Appearance
import com.rowshr.siddur.ui.theme.LocalAppearance
import kotlin.math.cos
import kotlin.math.sin

enum class BackdropStyle { HOME, READER }

/**
 * Soft animated gradient the translucent surfaces float over. Calm enough to read
 * against; rich enough that the glass has something to refract.
 */
@Composable
fun Backdrop(style: BackdropStyle, modifier: Modifier = Modifier) {
    val appearance = LocalAppearance.current
    // A gentle drift, redrawn twelve times a second rather than every frame: calm, and cheap on battery.
    var drift by remember { mutableFloatStateOf(0f) }
    if (style == BackdropStyle.HOME) {
        LaunchedEffect(Unit) {
            val start = System.nanoTime()
            while (true) {
                drift = ((System.nanoTime() - start) / 1e9f / 24f) % 1f
                delay(83)
            }
        }
    }

    val (base, blobs) = palette(style, appearance)
    Canvas(modifier.fillMaxSize()) {
        drawRect(base)
        if (style == BackdropStyle.READER) return@Canvas
        val w = size.width
        val h = size.height
        val t = drift * 2f * Math.PI.toFloat()
        blobs.forEachIndexed { i, (color, cx, cy) ->
            val dx = 0.05f * sin(t + i) * w
            val dy = 0.05f * cos(t * 0.8f + i) * h
            val center = Offset(cx * w + dx, cy * h + dy)
            drawCircle(
                brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = center, radius = w * 0.75f),
                radius = w * 0.75f,
                center = center,
            )
        }
    }
}

private data class Blob(val color: Color, val cx: Float, val cy: Float)

private fun palette(style: BackdropStyle, appearance: Appearance): Pair<Color, List<Blob>> = when (appearance) {
    Appearance.DARK -> when (style) {
        BackdropStyle.HOME -> Color(0xFF100E1F) to listOf(
            Blob(Color(0xFF2A2152).copy(alpha = 0.9f), 0.85f, 0.05f),
            Blob(Color(0xFF1B2A4A).copy(alpha = 0.8f), 0.1f, 0.5f),
            Blob(Color(0xFF3A2A5E).copy(alpha = 0.6f), 0.6f, 0.95f),
        )
        BackdropStyle.READER -> Color(0xFF121219) to emptyList()
    }
    Appearance.SEPIA -> when (style) {
        BackdropStyle.HOME -> Color(0xFFF8EFDD) to listOf(
            Blob(Color(0xFFFFE6B8), 0.85f, 0.05f),
            Blob(Color(0xFFFFF4E0), 0.15f, 0.5f),
            Blob(Color(0xFFF2DDB8), 0.6f, 0.95f),
        )
        BackdropStyle.READER -> Color(0xFFFAF3E2) to emptyList()
    }
    Appearance.LIGHT -> when (style) {
        BackdropStyle.HOME -> Color(0xFFF3F4FB) to listOf(
            Blob(Color(0xFFFFE1B0).copy(alpha = 0.85f), 0.85f, 0.05f),
            Blob(Color(0xFFDCE6FF), 0.1f, 0.45f),
            Blob(Color(0xFFF6E3F7), 0.65f, 0.95f),
        )
        BackdropStyle.READER -> Color(0xFFFBFAF7) to emptyList()
    }
}

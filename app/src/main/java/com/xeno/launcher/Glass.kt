package com.xeno.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    tint: Color = Color.White,
    strength: Float = 1f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    var m = modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    tint.copy(alpha = 0.22f * strength),
                    tint.copy(alpha = 0.06f * strength)
                )
            )
        )
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.30f)
                )
            ),
            shape
        )
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m, content = content)
}

@Composable
fun GlassBackdrop(content: @Composable BoxScope.() -> Unit) {
    val bg = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize().background(bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.9f
            val c1 = Offset(size.width * 0.12f, size.height * 0.15f)
            val c2 = Offset(size.width * 0.88f, size.height * 0.85f)
            val c3 = Offset(size.width * 0.55f, size.height * 0.45f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.50f), Color.Transparent),
                    center = c1, radius = r
                ),
                radius = r, center = c1
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.35f), Color.Transparent),
                    center = c2, radius = r
                ),
                radius = r, center = c2
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                    center = c3, radius = r * 0.7f
                ),
                radius = r * 0.7f, center = c3
            )
        }
        content()
    }
}

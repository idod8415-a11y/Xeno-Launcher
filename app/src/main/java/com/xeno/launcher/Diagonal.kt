package com.xeno.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class SlantShape(private val slantPx: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val s = slantPx.coerceAtMost(size.width / 3f)
        val p = Path().apply {
            moveTo(s, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - s, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(p)
    }
}

@Composable
fun DiagonalHeader(title: String, subtitle: String) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Box(Modifier.fillMaxWidth().height(100.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val body = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.55f)
                lineTo(0f, h)
                close()
            }
            drawPath(body, surface)
            val stripe = Path().apply {
                moveTo(0f, h * 0.86f)
                lineTo(w, h * 0.41f)
                lineTo(w, h * 0.55f)
                lineTo(0f, h)
                close()
            }
            drawPath(stripe, primary)
        }
        Column(Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = primary
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

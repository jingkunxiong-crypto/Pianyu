package com.example.newandroidapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PaperCutBackdrop(modifier: Modifier = Modifier) {
    val sun = MaterialTheme.colorScheme.primaryContainer
    val hills = MaterialTheme.colorScheme.secondaryContainer
    val grain = MaterialTheme.colorScheme.onBackground
    Canvas(modifier = modifier) {
        drawCircle(
            color = sun.copy(alpha = 0.30f),
            radius = size.minDimension * 0.13f,
            center = Offset(size.width * 0.94f, size.height * 0.12f),
        )
        val lowerWave = Path().apply {
            moveTo(0f, size.height * 0.84f)
            cubicTo(
                size.width * 0.24f,
                size.height * 0.78f,
                size.width * 0.55f,
                size.height * 0.94f,
                size.width,
                size.height * 0.84f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(lowerWave, hills.copy(alpha = 0.22f))
        repeat(76) { index ->
            val x = ((index * 79 + 17) % 991) / 991f * size.width
            val y = ((index * 137 + 31) % 983) / 983f * size.height
            drawCircle(
                color = grain.copy(alpha = 0.018f),
                radius = if (index % 4 == 0) 1.1f else 0.7f,
                center = Offset(x, y),
            )
        }
    }
}

@Composable
fun PaperCutMark(
    modifier: Modifier = Modifier,
    contentDescription: String = "片屿纸雕相片标志",
) {
    val frame = MaterialTheme.colorScheme.surface
    val frameEdge = MaterialTheme.colorScheme.outlineVariant
    val hill = MaterialTheme.colorScheme.secondaryContainer
    val foreground = MaterialTheme.colorScheme.surfaceVariant
    val sun = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(28.dp),
        color = frame,
        shadowElevation = 8.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx().coerceAtMost(size.minDimension * 0.07f)
                drawRoundRect(
                    color = frameEdge.copy(alpha = 0.72f),
                    cornerRadius = CornerRadius(size.minDimension * 0.17f),
                    style = Stroke(stroke),
                )
                drawCircle(
                    color = sun,
                    radius = size.minDimension * 0.09f,
                    center = Offset(size.width * 0.69f, size.height * 0.34f),
                )
                val rearHill = Path().apply {
                    moveTo(0f, size.height * 0.67f)
                    cubicTo(
                        size.width * 0.20f,
                        size.height * 0.42f,
                        size.width * 0.34f,
                        size.height * 0.42f,
                        size.width * 0.52f,
                        size.height * 0.62f,
                    )
                    cubicTo(
                        size.width * 0.68f,
                        size.height * 0.78f,
                        size.width * 0.82f,
                        size.height * 0.58f,
                        size.width,
                        size.height * 0.70f,
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(rearHill, hill)
                val frontWave = Path().apply {
                    moveTo(0f, size.height * 0.70f)
                    cubicTo(
                        size.width * 0.28f,
                        size.height * 0.64f,
                        size.width * 0.42f,
                        size.height * 0.88f,
                        size.width * 0.64f,
                        size.height * 0.86f,
                    )
                    cubicTo(
                        size.width * 0.80f,
                        size.height * 0.84f,
                        size.width * 0.86f,
                        size.height * 0.68f,
                        size.width,
                        size.height * 0.72f,
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(frontWave, foreground)
            }
        }
    }
}

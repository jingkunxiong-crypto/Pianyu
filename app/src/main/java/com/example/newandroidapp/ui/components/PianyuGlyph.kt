package com.example.newandroidapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

enum class PianyuGlyphType {
    Albums,
    Timeline,
    Favorite,
    Add,
    Search,
    Back,
    Tag,
    Qr,
    Info,
    Edit,
    Delete,
    Check,
}

@Composable
fun PianyuGlyph(
    type: PianyuGlyphType,
    selected: Boolean,
    modifier: Modifier = Modifier,
    colorOverride: Color? = null,
) {
    val color = colorOverride ?: if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 1.8.dp.toPx()
        val width = size.width
        val height = size.height
        when (type) {
            PianyuGlyphType.Albums -> {
                drawRoundRect(
                    color = color.copy(alpha = 0.52f),
                    topLeft = Offset(width * 0.11f, height * 0.18f),
                    size = Size(width * 0.61f, height * 0.61f),
                    cornerRadius = CornerRadius(width * 0.12f),
                    style = Stroke(stroke),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(width * 0.28f, height * 0.27f),
                    size = Size(width * 0.61f, height * 0.61f),
                    cornerRadius = CornerRadius(width * 0.12f),
                    style = Stroke(stroke),
                )
                drawCircle(
                    color = color,
                    radius = width * 0.07f,
                    center = Offset(width * 0.64f, height * 0.48f),
                )
            }

            PianyuGlyphType.Timeline -> {
                drawLine(
                    color = color.copy(alpha = 0.58f),
                    start = Offset(width * 0.32f, height * 0.19f),
                    end = Offset(width * 0.32f, height * 0.81f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                listOf(0.24f, 0.5f, 0.76f).forEachIndexed { index, y ->
                    drawCircle(
                        color = color,
                        radius = if (index == 1) width * 0.085f else width * 0.06f,
                        center = Offset(width * 0.32f, height * y),
                    )
                    drawLine(
                        color = color,
                        start = Offset(width * 0.49f, height * y),
                        end = Offset(width * 0.82f, height * y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }

            PianyuGlyphType.Favorite -> {
                val path = Path().apply {
                    moveTo(width * 0.5f, height * 0.84f)
                    cubicTo(
                        width * 0.14f,
                        height * 0.61f,
                        width * 0.11f,
                        height * 0.31f,
                        width * 0.31f,
                        height * 0.24f,
                    )
                    cubicTo(
                        width * 0.42f,
                        height * 0.20f,
                        width * 0.49f,
                        height * 0.29f,
                        width * 0.5f,
                        height * 0.36f,
                    )
                    cubicTo(
                        width * 0.53f,
                        height * 0.27f,
                        width * 0.61f,
                        height * 0.20f,
                        width * 0.72f,
                        height * 0.24f,
                    )
                    cubicTo(
                        width * 0.93f,
                        height * 0.31f,
                        width * 0.86f,
                        height * 0.63f,
                        width * 0.5f,
                        height * 0.84f,
                    )
                    close()
                }
                drawPath(
                    path = path,
                    color = color,
                    style = if (selected) {
                        androidx.compose.ui.graphics.drawscope.Fill
                    } else {
                        Stroke(stroke)
                    },
                )
            }

            PianyuGlyphType.Add -> {
                drawCircle(
                    color = color.copy(alpha = 0.13f),
                    radius = width * 0.45f,
                    center = center,
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.29f, height * 0.5f),
                    end = Offset(width * 0.71f, height * 0.5f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.5f, height * 0.29f),
                    end = Offset(width * 0.5f, height * 0.71f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            PianyuGlyphType.Search -> {
                drawCircle(
                    color = color,
                    radius = width * 0.27f,
                    center = Offset(width * 0.43f, height * 0.42f),
                    style = Stroke(stroke),
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.63f, height * 0.63f),
                    end = Offset(width * 0.84f, height * 0.84f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            PianyuGlyphType.Back -> {
                drawLine(
                    color = color,
                    start = Offset(width * 0.72f, height * 0.18f),
                    end = Offset(width * 0.31f, height * 0.5f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.31f, height * 0.5f),
                    end = Offset(width * 0.72f, height * 0.82f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            PianyuGlyphType.Tag -> {
                val path = Path().apply {
                    moveTo(width * 0.16f, height * 0.25f)
                    lineTo(width * 0.53f, height * 0.18f)
                    lineTo(width * 0.86f, height * 0.51f)
                    lineTo(width * 0.51f, height * 0.86f)
                    lineTo(width * 0.18f, height * 0.53f)
                    close()
                }
                drawPath(path, color, style = Stroke(stroke))
                drawCircle(
                    color = color,
                    radius = width * 0.06f,
                    center = Offset(width * 0.39f, height * 0.37f),
                )
            }

            PianyuGlyphType.Qr -> {
                fun finder(left: Float, top: Float) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(width * left, height * top),
                        size = Size(width * 0.27f, height * 0.27f),
                        cornerRadius = CornerRadius(width * 0.025f),
                        style = Stroke(stroke),
                    )
                    drawRect(
                        color = color,
                        topLeft = Offset(width * (left + 0.085f), height * (top + 0.085f)),
                        size = Size(width * 0.10f, height * 0.10f),
                    )
                }
                finder(0.13f, 0.13f)
                finder(0.60f, 0.13f)
                finder(0.13f, 0.60f)
                drawRect(
                    color = color,
                    topLeft = Offset(width * 0.60f, height * 0.60f),
                    size = Size(width * 0.11f, height * 0.11f),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(width * 0.76f, height * 0.60f),
                    size = Size(width * 0.11f, height * 0.27f),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(width * 0.60f, height * 0.76f),
                    size = Size(width * 0.11f, height * 0.11f),
                )
            }

            PianyuGlyphType.Info -> {
                drawCircle(
                    color = color,
                    radius = width * 0.36f,
                    center = center,
                    style = Stroke(stroke),
                )
                drawCircle(
                    color = color,
                    radius = width * 0.045f,
                    center = Offset(width * 0.5f, height * 0.34f),
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.5f, height * 0.47f),
                    end = Offset(width * 0.5f, height * 0.70f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            PianyuGlyphType.Edit -> {
                // A compact three-slider glyph reads as "adjust" at small sizes
                // and keeps the same rounded stroke language as the other tools.
                drawLine(
                    color = color,
                    start = Offset(width * 0.18f, height * 0.28f),
                    end = Offset(width * 0.82f, height * 0.28f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.18f, height * 0.50f),
                    end = Offset(width * 0.82f, height * 0.50f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.18f, height * 0.72f),
                    end = Offset(width * 0.82f, height * 0.72f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = color, radius = width * 0.085f, center = Offset(width * 0.37f, height * 0.28f))
                drawCircle(color = color, radius = width * 0.085f, center = Offset(width * 0.66f, height * 0.50f))
                drawCircle(color = color, radius = width * 0.085f, center = Offset(width * 0.46f, height * 0.72f))
            }

            PianyuGlyphType.Delete -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(width * 0.28f, height * 0.30f),
                    size = Size(width * 0.44f, height * 0.54f),
                    cornerRadius = CornerRadius(width * 0.07f),
                    style = Stroke(stroke),
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.22f, height * 0.25f),
                    end = Offset(width * 0.78f, height * 0.25f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.40f, height * 0.17f),
                    end = Offset(width * 0.60f, height * 0.17f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            PianyuGlyphType.Check -> {
                drawLine(
                    color = color,
                    start = Offset(width * 0.20f, height * 0.52f),
                    end = Offset(width * 0.42f, height * 0.73f),
                    strokeWidth = stroke * 1.2f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.42f, height * 0.73f),
                    end = Offset(width * 0.82f, height * 0.28f),
                    strokeWidth = stroke * 1.2f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

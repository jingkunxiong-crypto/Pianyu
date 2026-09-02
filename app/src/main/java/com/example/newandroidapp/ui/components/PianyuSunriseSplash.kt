package com.example.newandroidapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private val DawnCream = Color(0xFFF5ECDF)
private val DaylightIvory = Color(0xFFFFFCF5)
private val HorizonBlush = Color(0xFFF6DFCF)
private val FarMountain = Color(0xFFE2DED0)
private val NearMountain = Color(0xFFD2D6C7)
private val ForegroundMilk = Color(0xFFF1E8DA)
private val SunriseCoral = Color(0xFFE49A71)
private val WordmarkMist = Color(0xFFD4C5B7)
private val WordmarkInk = Color(0xFF4B4037)

@Composable
fun PianyuSunriseSplash(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sunrise = remember { Animatable(0f) }
    val wordmark = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                sunrise.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1_900,
                        easing = CubicBezierEasing(0.18f, 0.72f, 0.22f, 1f),
                    ),
                )
            }
            launch {
                delay(140)
                wordmark.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1_180,
                        easing = CubicBezierEasing(0.16f, 0.88f, 0.24f, 1f),
                    ),
                )
            }
        }
        delay(180)
        onFinished()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = sunrise.value
            val width = size.width
            val height = size.height
            val shortEdge = min(width, height)

            val skyTop = lerp(DawnCream, DaylightIvory, progress)
            val skyBottom = lerp(Color(0xFFF1E4D5), HorizonBlush, progress)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(skyTop, DaylightIvory, skyBottom),
                    startY = 0f,
                    endY = height,
                ),
            )

            val sunCenter = Offset(
                x = width * 0.53f,
                y = height * (0.72f - 0.33f * progress),
            )
            val sunRadius = shortEdge * 0.075f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SunriseCoral.copy(alpha = 0.20f + progress * 0.16f),
                        SunriseCoral.copy(alpha = 0f),
                    ),
                    center = sunCenter,
                    radius = shortEdge * (0.24f + progress * 0.08f),
                ),
                radius = shortEdge * (0.24f + progress * 0.08f),
                center = sunCenter,
            )
            drawCircle(
                color = lerp(Color(0xFFD98963), SunriseCoral, progress),
                radius = sunRadius,
                center = sunCenter,
            )

            val farRidge = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.66f)
                cubicTo(
                    width * 0.10f, height * 0.61f,
                    width * 0.18f, height * 0.47f,
                    width * 0.29f, height * 0.50f,
                )
                cubicTo(
                    width * 0.39f, height * 0.53f,
                    width * 0.43f, height * 0.64f,
                    width * 0.55f, height * 0.62f,
                )
                cubicTo(
                    width * 0.66f, height * 0.60f,
                    width * 0.72f, height * 0.48f,
                    width * 0.82f, height * 0.50f,
                )
                cubicTo(
                    width * 0.90f, height * 0.52f,
                    width * 0.95f, height * 0.62f,
                    width, height * 0.64f,
                )
                lineTo(width, height)
                close()
            }
            drawPath(path = farRidge, color = FarMountain)

            val nearRidge = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.72f)
                cubicTo(
                    width * 0.12f, height * 0.67f,
                    width * 0.20f, height * 0.55f,
                    width * 0.31f, height * 0.58f,
                )
                cubicTo(
                    width * 0.41f, height * 0.61f,
                    width * 0.46f, height * 0.72f,
                    width * 0.58f, height * 0.71f,
                )
                cubicTo(
                    width * 0.72f, height * 0.70f,
                    width * 0.80f, height * 0.59f,
                    width * 0.91f, height * 0.63f,
                )
                cubicTo(
                    width * 0.96f, height * 0.65f,
                    width * 0.98f, height * 0.69f,
                    width, height * 0.70f,
                )
                lineTo(width, height)
                close()
            }
            drawPath(path = nearRidge, color = NearMountain)

            val foreground = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.82f)
                cubicTo(
                    width * 0.22f, height * 0.77f,
                    width * 0.36f, height * 0.86f,
                    width * 0.55f, height * 0.81f,
                )
                cubicTo(
                    width * 0.72f, height * 0.77f,
                    width * 0.87f, height * 0.83f,
                    width, height * 0.79f,
                )
                lineTo(width, height)
                close()
            }
            drawPath(path = foreground, color = ForegroundMilk)
        }

        PianyuWordmark(
            reveal = wordmark.value,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 30.dp, top = 30.dp),
        )
    }
}

@Composable
private fun PianyuWordmark(
    reveal: Float,
    modifier: Modifier = Modifier,
) {
    val riseDistance = with(LocalDensity.current) { 18.dp.toPx() }
    val firstReveal = ((reveal - 0.02f) / 0.72f).coerceIn(0f, 1f)
    val secondReveal = ((reveal - 0.16f) / 0.72f).coerceIn(0f, 1f)
    val signatureReveal = ((reveal - 0.42f) / 0.58f).coerceIn(0f, 1f)

    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "片屿"
        },
    ) {
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(72.dp),
        ) {
            Text(
                text = "片",
                color = lerp(WordmarkMist, WordmarkInk, firstReveal),
                fontFamily = FontFamily.Serif,
                fontSize = 42.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-1).sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        alpha = firstReveal
                        translationY = riseDistance * (1f - firstReveal)
                    },
            )
            Text(
                text = "屿",
                color = lerp(WordmarkMist, WordmarkInk, secondReveal),
                fontFamily = FontFamily.Serif,
                fontSize = 42.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-1).sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer {
                        alpha = secondReveal
                        translationY = riseDistance * 1.15f * (1f - secondReveal)
                    },
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier
                    .width(22.dp)
                    .height(1.dp)
                    .graphicsLayer {
                        alpha = signatureReveal
                        scaleX = signatureReveal
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
            ) {
                drawLine(
                    color = SunriseCoral,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = size.height,
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "PIAN / YU",
                color = WordmarkInk.copy(alpha = 0.72f),
                fontFamily = FontFamily.SansSerif,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.8.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = signatureReveal
                    translationY = riseDistance * 0.45f * (1f - signatureReveal)
                },
            )
        }
    }
}

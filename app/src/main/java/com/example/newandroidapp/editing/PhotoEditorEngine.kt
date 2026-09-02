package com.example.newandroidapp.editing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.BitmapShader
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.example.newandroidapp.R
import com.example.newandroidapp.data.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

data class CropBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

data class WatermarkBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

fun computeCenterCropBounds(
    width: Int,
    height: Int,
    aspect: CropAspect,
): CropBounds {
    val targetRatio = aspect.ratio
    if (targetRatio == null || width <= 0 || height <= 0) {
        return CropBounds(0, 0, width.coerceAtLeast(1), height.coerceAtLeast(1))
    }
    val currentRatio = width.toFloat() / height.toFloat()
    return if (currentRatio > targetRatio) {
        val targetWidth = (height * targetRatio).roundToInt().coerceIn(1, width)
        CropBounds((width - targetWidth) / 2, 0, targetWidth, height)
    } else {
        val targetHeight = (width / targetRatio).roundToInt().coerceIn(1, height)
        CropBounds(0, (height - targetHeight) / 2, width, targetHeight)
    }
}

fun computeWatermarkBounds(
    canvasWidth: Int,
    canvasHeight: Int,
    watermarkWidth: Int,
    watermarkHeight: Int,
    position: WatermarkPosition,
): WatermarkBounds {
    if (canvasWidth <= 0 || canvasHeight <= 0 || watermarkWidth <= 0 || watermarkHeight <= 0) {
        return WatermarkBounds(0f, 0f, 1f, 1f)
    }
    val width = canvasWidth.toFloat()
    val height = canvasHeight.toFloat()
    val aspect = watermarkWidth.toFloat() / watermarkHeight.toFloat()
    val margin = (min(width, height) * 0.035f).coerceAtLeast(4f)
    val targetWidth = min(width * 0.34f, height * 0.18f * aspect)
        .coerceAtLeast(1f)
    val targetHeight = targetWidth / aspect
    val left = when (position) {
        WatermarkPosition.BottomLeft -> margin
        WatermarkPosition.BottomCenter -> (width - targetWidth) / 2f
        WatermarkPosition.BottomRight -> width - margin - targetWidth
    }.coerceIn(0f, (width - targetWidth).coerceAtLeast(0f))
    val bottom = (height - margin).coerceAtLeast(targetHeight)
    return WatermarkBounds(
        left = left,
        top = bottom - targetHeight,
        right = left + targetWidth,
        bottom = bottom,
    )
}

object PhotoEditorEngine {
    suspend fun loadPreview(
        context: Context,
        photo: PhotoItem,
        maxDimension: Int = 2_048,
    ): Bitmap = withContext(Dispatchers.IO) {
        decodeBitmap(context, photo, maxDimension)
    }

    suspend fun renderForExport(
        context: Context,
        photo: PhotoItem,
        settings: EditSettings,
        maxDimension: Int = 4_096,
    ): Bitmap = withContext(Dispatchers.Default) {
        val source = decodeBitmap(context, photo, maxDimension)
        val watermark = decodeWatermark(context, settings.watermarkStyle)
        try {
            applyEdits(source, settings, watermark)
        } finally {
            watermark?.recycle()
        }
    }

    fun colorFilter(settings: EditSettings): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(buildColorMatrix(settings))
    }

    fun colorMatrixValues(settings: EditSettings): FloatArray {
        return buildColorMatrix(settings).array.clone()
    }

    fun applyEdits(
        source: Bitmap,
        rawSettings: EditSettings,
        watermark: Bitmap? = null,
    ): Bitmap {
        val settings = rawSettings.normalized()
        val rotated = rotateBitmap(source, settings.rotationDegrees)
        val crop = computeCenterCropBounds(
            width = rotated.width,
            height = rotated.height,
            aspect = settings.cropAspect,
        )
        val cropped = if (
            crop.left == 0 && crop.top == 0 &&
            crop.width == rotated.width && crop.height == rotated.height
        ) {
            rotated
        } else {
            Bitmap.createBitmap(rotated, crop.left, crop.top, crop.width, crop.height)
        }
        val output = createBitmap(cropped.width, cropped.height)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            colorFilter = colorFilter(settings)
        }
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        if (settings.grain > 0) {
            drawFilmGrain(canvas, output.width, output.height, settings.grain)
        }
        if (settings.vignette > 0) {
            val strength = (settings.vignette / 100f * 190f).roundToInt().coerceIn(0, 190)
            val radius = hypot(output.width.toDouble(), output.height.toDouble()).toFloat() / 2f
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    output.width / 2f,
                    output.height / 2f,
                    radius,
                    intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(strength, 0, 0, 0)),
                    floatArrayOf(0f, 0.58f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), vignettePaint)
        }
        if (settings.watermarkStyle != WatermarkStyle.None && watermark != null) {
            drawWatermark(
                canvas = canvas,
                width = output.width,
                height = output.height,
                watermark = watermark,
                position = settings.watermarkPosition,
            )
        }
        return output
    }

    private fun drawWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        watermark: Bitmap,
        position: WatermarkPosition,
    ) {
        val bounds = computeWatermarkBounds(
            canvasWidth = width,
            canvasHeight = height,
            watermarkWidth = watermark.width,
            watermarkHeight = watermark.height,
            position = position,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 235
        }
        canvas.drawBitmap(
            watermark,
            null,
            RectF(bounds.left, bounds.top, bounds.right, bounds.bottom),
            paint,
        )
    }

    private fun decodeWatermark(context: Context, style: WatermarkStyle): Bitmap? {
        val resourceId = when (style) {
            WatermarkStyle.None -> return null
            WatermarkStyle.White -> R.drawable.pianyu_watermark_white
            WatermarkStyle.Black -> R.drawable.pianyu_watermark_black
        }
        return BitmapFactory.decodeResource(context.resources, resourceId)
            ?: error("无法读取水印素材")
    }

    private fun buildColorMatrix(rawSettings: EditSettings): ColorMatrix {
        val settings = rawSettings.normalized()
        val saturation = (1f + settings.saturation / 100f).coerceIn(0f, 2f)
        val fadeFactor = settings.fade / 100f
        val exposureFactor = 2.0.pow(settings.exposure / 100.0).toFloat()
        val clarityBoost = settings.sharpness / 100f * 0.16f
        val highlightCompression = settings.highlights / 100f * 0.20f
        val shadowLift = settings.shadows / 100f * 30f
        val contrast = (1f + settings.contrast / 100f * 0.75f + clarityBoost - highlightCompression) *
            (1f - fadeFactor * 0.22f) * exposureFactor
        val brightnessShift = settings.brightness * 1.27f + fadeFactor * 18f + shadowLift
        val centerShift = (1f - contrast) * 128f + brightnessShift
        val warmth = settings.warmth / 100f
        val tint = settings.tint / 100f

        val tone = ColorMatrix(
            floatArrayOf(
                contrast * (1f + warmth * 0.16f + tint * 0.05f), 0f, 0f, 0f, centerShift + settings.warmth * 0.12f + settings.tint * 0.05f,
                0f, contrast * (1f - tint * 0.10f), 0f, 0f, centerShift - settings.tint * 0.09f,
                0f, 0f, contrast * (1f - warmth * 0.16f + tint * 0.05f), 0f, centerShift - settings.warmth * 0.12f + settings.tint * 0.05f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        val saturationMatrix = ColorMatrix().apply { setSaturation(saturation) }
        saturationMatrix.postConcat(tone)
        return saturationMatrix
    }

    private fun drawFilmGrain(canvas: Canvas, width: Int, height: Int, amount: Int) {
        val tileSize = 96
        val pixels = IntArray(tileSize * tileSize)
        val random = Random(0x5049414E + amount)
        val alpha = (amount / 100f * 42f).roundToInt().coerceIn(0, 42)
        pixels.indices.forEach { index ->
            val gray = random.nextInt(72, 184)
            pixels[index] = Color.argb(alpha, gray, gray, gray)
        }
        val tile = Bitmap.createBitmap(pixels, tileSize, tileSize, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        tile.recycle()
    }

    private fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return source
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun decodeBitmap(
        context: Context,
        photo: PhotoItem,
        maxDimension: Int,
    ): Bitmap {
        val resolver = context.contentResolver
        val uri = photo.uri.toUri()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val largest = maxOf(width, height)
                if (largest > maxDimension) {
                    val scale = maxDimension.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        (width * scale).roundToInt().coerceAtLeast(1),
                        (height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) {
                sampleSize *= 2
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize },
                )
            } ?: error("无法读取这张照片")
            rotateBitmap(decoded, photo.orientationDegrees)
        }
    }
}

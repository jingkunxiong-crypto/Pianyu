package com.example.newandroidapp.editing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

fun generateFilterQrBitmap(
    preset: FilterPreset,
    sizePx: Int = 1_024,
): Bitmap {
    val matrix = QRCodeWriter().encode(
        FilterQrCodec.encode(preset),
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
        ),
    )
    return createBitmap(sizePx, sizePx).apply {
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                pixels[y * sizePx + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    }
}

suspend fun writeFilterQrToUri(
    context: Context,
    preset: FilterPreset,
    uri: Uri,
) = withContext(Dispatchers.IO) {
    val bitmap = generateFilterQrBitmap(preset)
    try {
        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        } ?: error("无法写入二维码文件")
    } finally {
        bitmap.recycle()
    }
}

suspend fun shareFilterQr(
    context: Context,
    preset: FilterPreset,
) = withContext(Dispatchers.IO) {
    val directory = File(context.cacheDir, "shared").apply { mkdirs() }
    directory.listFiles { file -> file.name.startsWith("pianyu-filter-") }
        ?.forEach(File::delete)
    val file = File(directory, "pianyu-filter-${System.currentTimeMillis()}.png")
    val bitmap = generateFilterQrBitmap(preset)
    try {
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    } finally {
        bitmap.recycle()
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "片屿滤镜：${preset.name}")
        clipData = ClipData.newUri(context.contentResolver, preset.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    withContext(Dispatchers.Main) {
        context.startActivity(Intent.createChooser(intent, "分享滤镜二维码"))
    }
}

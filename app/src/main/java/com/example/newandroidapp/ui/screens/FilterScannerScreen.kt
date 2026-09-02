package com.example.newandroidapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.newandroidapp.editing.FilterQrCodec
import com.example.newandroidapp.editing.ScannedFilterCode
import com.example.newandroidapp.ui.components.PaperCutMark
import com.example.newandroidapp.ui.components.PianyuGlyph
import com.example.newandroidapp.ui.components.PianyuGlyphType
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

@Composable
fun FilterScannerScreen(
    onDismiss: () -> Unit,
    onResult: (ScannedFilterCode) -> Unit,
    onFailure: (String) -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var requestedOnce by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission && !requestedOnce) {
            requestedOnce = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasPermission) Color.Black else MaterialTheme.colorScheme.background,
    ) {
        if (hasPermission) {
            CameraScannerContent(
                onDismiss = onDismiss,
                onResult = onResult,
                onFailure = onFailure,
            )
        } else {
            CameraPermissionContent(
                onDismiss = onDismiss,
                onRequestPermission = {
                    requestedOnce = true
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
            )
        }
    }
}

@Composable
private fun CameraScannerContent(
    onDismiss: () -> Unit,
    onResult: (ScannedFilterCode) -> Unit,
    onFailure: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, previewView) {
        var disposed = false
        var provider: ProcessCameraProvider? = null
        if (lifecycleOwner == null) {
            cameraError = "当前页面无法连接相机生命周期。"
        } else {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener(
                {
                    if (disposed) return@addListener
                    runCatching {
                        provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    analysisExecutor,
                                    FilterQrAnalyzer(mainExecutor, onResult),
                                )
                            }
                        provider?.unbindAll()
                        provider?.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }.onFailure {
                        cameraError = "相机暂时无法启动，请检查权限或稍后重试。"
                    }
                },
                mainExecutor,
            )
        }
        onDispose {
            disposed = true
            runCatching { provider?.unbindAll() }
            analysisExecutor.shutdownNow()
        }
    }

    LaunchedEffect(cameraError) {
        cameraError?.let(onFailure)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        ScannerFrame()

        Surface(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 18.dp, top = 12.dp)
                .size(48.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.42f),
            contentColor = Color.White,
        ) {
            Box(contentAlignment = Alignment.Center) {
                PianyuGlyph(
                    type = PianyuGlyphType.Back,
                    selected = true,
                    colorOverride = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("扫描滤镜二维码", style = MaterialTheme.typography.titleMedium)
                Text(
                    "对准方框即可自动识别 · 画面只在本机处理",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ScannerFrame() {
    val accent = MaterialTheme.colorScheme.primary
    val scanTransition = rememberInfiniteTransition(label = "qr-scan-line")
    val scanProgress by scanTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "qr-scan-progress",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val frame = min(size.width * 0.78f, size.height * 0.40f)
        val left = (size.width - frame) / 2f
        val top = (size.height - frame) * 0.42f
        val right = left + frame
        val bottom = top + frame
        val shade = Color.Black.copy(alpha = 0.48f)
        drawRect(shade, size = androidx.compose.ui.geometry.Size(size.width, top))
        drawRect(shade, topLeft = Offset(0f, bottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - bottom))
        drawRect(shade, topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(left, frame))
        drawRect(shade, topLeft = Offset(right, top), size = androidx.compose.ui.geometry.Size(size.width - right, frame))

        val corner = 32.dp.toPx()
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        listOf(
            Offset(left, top) to Offset(left + corner, top),
            Offset(left, top) to Offset(left, top + corner),
            Offset(right, top) to Offset(right - corner, top),
            Offset(right, top) to Offset(right, top + corner),
            Offset(left, bottom) to Offset(left + corner, bottom),
            Offset(left, bottom) to Offset(left, bottom - corner),
            Offset(right, bottom) to Offset(right - corner, bottom),
            Offset(right, bottom) to Offset(right, bottom - corner),
        ).forEach { (start, end) -> drawLine(accent, start, end, strokeWidth = stroke.width, cap = StrokeCap.Round) }
        val scanY = top + frame * (0.10f + scanProgress * 0.80f)
        drawLine(
            color = accent.copy(alpha = 0.22f),
            start = Offset(left + 18.dp.toPx(), scanY),
            end = Offset(right - 18.dp.toPx(), scanY),
            strokeWidth = 8.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = accent.copy(alpha = 0.92f),
            start = Offset(left + 22.dp.toPx(), scanY),
            end = Offset(right - 22.dp.toPx(), scanY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CameraPermissionContent(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(10.dp),
        ) { Text("返回") }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PaperCutMark(modifier = Modifier.size(116.dp))
            Text("允许相机后即可在片屿内扫码", style = MaterialTheme.typography.titleLarge)
            Text(
                "相机画面不会上传，也不会保存；只用于在本机识别滤镜二维码。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("允许使用相机") }
        }
    }
}

private class FilterQrAnalyzer(
    private val mainExecutor: Executor,
    private val onResult: (ScannedFilterCode) -> Unit,
) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val delivered = AtomicBoolean(false)
    private val hints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8",
    )

    override fun analyze(image: ImageProxy) {
        if (delivered.get()) {
            image.close()
            return
        }
        try {
            val raw = decode(image) ?: return
            if (delivered.compareAndSet(false, true)) {
                mainExecutor.execute { onResult(FilterQrCodec.classify(raw)) }
            }
        } finally {
            image.close()
        }
    }

    private fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        val buffer = plane.buffer
        val y = ByteArray(width * height)
        for (row in 0 until height) {
            val rowOffset = row * plane.rowStride
            for (column in 0 until width) {
                val sourceIndex = rowOffset + column * plane.pixelStride
                if (sourceIndex < buffer.limit()) {
                    y[row * width + column] = buffer.get(sourceIndex)
                }
            }
        }
        var source: LuminanceSource = PlanarYUVLuminanceSource(
            y,
            width,
            height,
            0,
            0,
            width,
            height,
            false,
        )
        val counterClockwiseTurns = ((360 - image.imageInfo.rotationDegrees) % 360) / 90
        repeat(counterClockwiseTurns) {
            if (source.isRotateSupported) source = source.rotateCounterClockwise()
        }
        return runCatching {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        }.also { reader.reset() }.getOrNull()
    }
}

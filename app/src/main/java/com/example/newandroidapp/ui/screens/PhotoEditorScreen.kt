package com.example.newandroidapp.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.data.PhotoItem
import com.example.newandroidapp.R
import com.example.newandroidapp.editing.Adjustment
import com.example.newandroidapp.editing.AdjustmentGroup
import com.example.newandroidapp.editing.BuiltInFilters
import com.example.newandroidapp.editing.CropAspect
import com.example.newandroidapp.editing.EditSettings
import com.example.newandroidapp.editing.ExportedPhoto
import com.example.newandroidapp.editing.FilterPreset
import com.example.newandroidapp.editing.PhotoEditorEngine
import com.example.newandroidapp.editing.ScannedFilterCode
import com.example.newandroidapp.editing.WatermarkPosition
import com.example.newandroidapp.editing.WatermarkStyle
import com.example.newandroidapp.editing.valueFor
import com.example.newandroidapp.editing.withValue
import com.example.newandroidapp.ui.components.FilterQrDialog
import com.example.newandroidapp.ui.components.PianyuGlyph
import com.example.newandroidapp.ui.components.PianyuGlyphType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private sealed interface EditorPreviewState {
    data object Loading : EditorPreviewState
    data class Ready(val bitmap: Bitmap) : EditorPreviewState
    data class Error(val message: String) : EditorPreviewState
}

private enum class FilterNameAction {
    Save,
    Export,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    photo: PhotoItem,
    presets: List<FilterPreset>,
    onBack: () -> Unit,
    onExport: suspend (EditSettings) -> ExportedPhoto,
    onExported: (ExportedPhoto) -> Unit,
    onOverwrite: (EditSettings) -> Unit,
    writeInProgress: Boolean,
    onScanCode: (
        onResult: (ScannedFilterCode) -> Unit,
        onFailure: (String) -> Unit,
    ) -> Unit,
    onSaveImportedPreset: (FilterPreset) -> FilterPreset,
    onOpenSnapseedPhoto: () -> Boolean,
    onOpenSnapseedQr: (String) -> Boolean,
    onInstallSnapseed: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var settings by remember { mutableStateOf(EditSettings()) }
    var activeAdjustmentName by rememberSaveable { mutableStateOf(Adjustment.Brightness.name) }
    val activeAdjustment = Adjustment.valueOf(activeAdjustmentName)
    var showOriginal by rememberSaveable { mutableStateOf(false) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    var showOverwriteConfirmation by rememberSaveable { mutableStateOf(false) }
    var filterNameAction by remember { mutableStateOf<FilterNameAction?>(null) }
    var activeGroupName by rememberSaveable { mutableStateOf(AdjustmentGroup.Light.name) }
    val activeGroup = AdjustmentGroup.valueOf(activeGroupName)
    var qrPreset by remember { mutableStateOf<FilterPreset?>(null) }
    var snapseedQrUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showSnapseedInstall by rememberSaveable { mutableStateOf(false) }
    val previewState by produceState<EditorPreviewState>(
        initialValue = EditorPreviewState.Loading,
        key1 = photo.uri,
    ) {
        value = runCatching { PhotoEditorEngine.loadPreview(context, photo) }
            .fold(
                onSuccess = { EditorPreviewState.Ready(it) },
                onFailure = {
                    EditorPreviewState.Error(it.message ?: "这张照片暂时无法打开")
                },
            )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("编辑照片") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "返回照片详情" },
                    ) {
                        PianyuGlyph(PianyuGlyphType.Back, selected = true)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            settings = EditSettings()
                            showOriginal = false
                        },
                    ) {
                        Text("重置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                exporting = true
                                runCatching { onExport(settings) }
                                    .onSuccess(onExported)
                                    .onFailure {
                                        snackbarHostState.showSnackbar(
                                            "保存失败：${it.message ?: "请重试"}",
                                        )
                                    }
                                exporting = false
                            }
                        },
                        enabled = !exporting && !writeInProgress && previewState is EditorPreviewState.Ready,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("另存副本")
                        }
                    }
                    Button(
                        onClick = { showOverwriteConfirmation = true },
                        enabled = !exporting && !writeInProgress && previewState is EditorPreviewState.Ready,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (writeInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("覆盖原图")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                EditorPreview(
                    state = previewState,
                    photo = photo,
                    settings = settings,
                    showOriginal = showOriginal,
                )
            }
            item {
                OutlinedButton(
                    onClick = { showOriginal = !showOriginal },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(if (showOriginal) "查看编辑效果" else "对比原图")
                }
            }
            item {
                EditorSection(title = "滤镜") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presets.forEach { preset ->
                            FilterPreviewTile(
                                state = previewState,
                                preset = preset,
                                selected = settings.filterId == preset.id,
                                onClick = {
                                    settings = settings.applyPreset(preset)
                                    showOriginal = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                EditorSection(title = "专业调整") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AdjustmentGroup.entries.forEach { group ->
                            FilterChip(
                                selected = group == activeGroup,
                                onClick = {
                                    activeGroupName = group.name
                                    activeAdjustmentName = Adjustment.entries
                                        .first { it.group == group }
                                        .name
                                },
                                label = { Text(group.label) },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Adjustment.entries.filter { it.group == activeGroup }.forEach { adjustment ->
                            FilterChip(
                                selected = adjustment == activeAdjustment,
                                onClick = { activeAdjustmentName = adjustment.name },
                                label = { Text(adjustment.label) },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(activeAdjustment.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = settings.valueFor(activeAdjustment).toString(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Slider(
                        value = settings.valueFor(activeAdjustment).toFloat(),
                        onValueChange = {
                            settings = settings.withValue(activeAdjustment, it.roundToInt())
                            showOriginal = false
                        },
                        valueRange = activeAdjustment.minimum.toFloat()..activeAdjustment.maximum.toFloat(),
                    )
                }
            }
            item {
                EditorSection(title = "裁剪与旋转") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CropAspect.entries.forEach { crop ->
                            FilterChip(
                                selected = settings.cropAspect == crop,
                                onClick = { settings = settings.copy(cropAspect = crop) },
                                label = { Text(crop.label) },
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            settings = settings.copy(
                                rotationDegrees = (settings.rotationDegrees + 90) % 360,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text("向右旋转 90°")
                    }
                }
            }
            item {
                EditorSection(title = "签名水印") {
                    Text(
                        text = "使用你提供的签名水印；默认放在底部中央。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WatermarkStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.watermarkStyle == style,
                                onClick = {
                                    settings = settings.copy(watermarkStyle = style)
                                    showOriginal = false
                                },
                                label = { Text(style.label) },
                            )
                        }
                    }
                    Text(
                        text = "位置",
                        color = if (settings.watermarkStyle == WatermarkStyle.None) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WatermarkPosition.entries.forEach { position ->
                            FilterChip(
                                selected = settings.watermarkPosition == position,
                                onClick = {
                                    settings = settings.copy(watermarkPosition = position)
                                    showOriginal = false
                                },
                                enabled = settings.watermarkStyle != WatermarkStyle.None,
                                label = { Text(position.label) },
                            )
                        }
                    }
                    Text(
                        text = "水印会写入另存副本或覆盖后的照片；保存滤镜时不会包含水印。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                EditorSection(title = "滤镜码与 Snapseed") {
                    Button(
                        onClick = {
                            onScanCode(
                                { result ->
                                    when (result) {
                                        is ScannedFilterCode.Pianyu -> {
                                            val saved = onSaveImportedPreset(result.preset)
                                            settings = settings.applyPreset(saved)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("已导入并应用“${saved.name}”")
                                            }
                                        }

                                        is ScannedFilterCode.Snapseed -> snapseedQrUrl = result.url
                                        is ScannedFilterCode.Unsupported -> scope.launch {
                                            snackbarHostState.showSnackbar(result.reason)
                                        }
                                    }
                                },
                                { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text("扫描滤镜二维码")
                    }
                    Button(
                        onClick = { filterNameAction = FilterNameAction.Save },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text("保存为新滤镜")
                    }
                    OutlinedButton(
                        onClick = { filterNameAction = FilterNameAction.Export },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text("导出当前滤镜")
                    }
                    OutlinedButton(
                        onClick = {
                            if (!onOpenSnapseedPhoto()) showSnapseedInstall = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text("在 Snapseed 中继续编辑")
                    }
                    Text(
                        text = "Snapseed 滤镜属于 Snapseed；片屿只负责识别官方 QR Look 链接并交给 Snapseed。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    filterNameAction?.let { action ->
        FilterNameDialog(
            initialName = presets.firstOrNull { it.id == settings.filterId }?.name ?: "我的滤镜",
            title = if (action == FilterNameAction.Save) "保存为新滤镜" else "导出当前滤镜",
            confirmLabel = if (action == FilterNameAction.Save) "保存" else "生成滤镜码",
            onDismiss = { filterNameAction = null },
            onConfirm = { name ->
                val preset = settings.asPreset(name)
                if (action == FilterNameAction.Save) {
                    val saved = onSaveImportedPreset(preset)
                    settings = settings.applyPreset(saved)
                    scope.launch { snackbarHostState.showSnackbar("已保存“${saved.name}”") }
                } else {
                    qrPreset = preset
                }
                filterNameAction = null
            },
        )
    }
    if (showOverwriteConfirmation) {
        AlertDialog(
            onDismissRequest = { showOverwriteConfirmation = false },
            title = { Text("覆盖原始照片？") },
            text = {
                Text("片屿会先在应用私有空间备份原图，再写回这张照片。以后可在照片工具页点“复原原图”。系统相册自己的复原按钮不一定会出现。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverwriteConfirmation = false
                        onOverwrite(settings)
                    },
                ) { Text("备份并覆盖") }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteConfirmation = false }) { Text("取消") }
            },
        )
    }
    qrPreset?.let { preset ->
        FilterQrDialog(preset = preset, onDismiss = { qrPreset = null })
    }
    snapseedQrUrl?.let { url ->
        SnapseedQrDialog(
            onDismiss = { snapseedQrUrl = null },
            onOpen = {
                if (!onOpenSnapseedQr(url)) showSnapseedInstall = true
                snapseedQrUrl = null
            },
        )
    }
    if (showSnapseedInstall) {
        AlertDialog(
            onDismissRequest = { showSnapseedInstall = false },
            title = { Text("需要 Snapseed") },
            text = { Text("当前设备没有可处理该请求的 Snapseed 版本。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSnapseedInstall = false
                        onInstallSnapseed()
                    },
                ) { Text("打开应用商店") }
            },
            dismissButton = {
                TextButton(onClick = { showSnapseedInstall = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun FilterPreviewTile(
    state: EditorPreviewState,
    preset: FilterPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(84.dp)
            .semantics {
                contentDescription = "应用${preset.name}滤镜"
                this.selected = selected
            },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
            ) {
                if (state is EditorPreviewState.Ready) {
                    Image(
                        bitmap = state.bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(
                            ColorMatrix(PhotoEditorEngine.colorMatrixValues(preset.settings)),
                        ),
                    )
                    if (preset.settings.vignette > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color.Black.copy(
                                                alpha = preset.settings.vignette / 100f * 0.72f,
                                            ),
                                        ),
                                    ),
                                ),
                        )
                    }
                }
            }
            Text(
                text = preset.name,
                maxLines = 1,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun EditorPreview(
    state: EditorPreviewState,
    photo: PhotoItem,
    settings: EditSettings,
    showOriginal: Boolean,
) {
    val rotated = settings.rotationDegrees % 180 != 0
    val originalAspect = if (rotated) 1f / photo.aspectRatio.coerceAtLeast(0.01f) else photo.aspectRatio
    val frameAspect = settings.cropAspect.ratio ?: originalAspect
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val frameHeight = (maxWidth / frameAspect.coerceIn(0.55f, 2.2f)).coerceIn(240.dp, 430.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(frameHeight),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black,
        ) {
            when (state) {
                EditorPreviewState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                is EditorPreviewState.Error -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                is EditorPreviewState.Ready -> Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = state.bitmap.asImageBitmap(),
                        contentDescription = "${photo.displayName}编辑预览",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(rotationZ = settings.rotationDegrees.toFloat()),
                        contentScale = if (settings.cropAspect == CropAspect.Original) {
                            ContentScale.Fit
                        } else {
                            ContentScale.Crop
                        },
                        colorFilter = if (showOriginal) {
                            null
                        } else {
                            ColorFilter.colorMatrix(
                                ColorMatrix(PhotoEditorEngine.colorMatrixValues(settings)),
                            )
                        },
                    )
                    if (!showOriginal && settings.vignette > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color.Black.copy(alpha = settings.vignette / 100f * 0.72f),
                                        ),
                                    ),
                                ),
                        )
                    }
                    if (!showOriginal) {
                        GrainPreviewOverlay(settings.grain)
                        WatermarkPreview(
                            style = settings.watermarkStyle,
                            position = settings.watermarkPosition,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatermarkPreview(
    style: WatermarkStyle,
    position: WatermarkPosition,
) {
    if (style == WatermarkStyle.None) return
    val resourceId = when (style) {
        WatermarkStyle.None -> return
        WatermarkStyle.White -> R.drawable.pianyu_watermark_white
        WatermarkStyle.Black -> R.drawable.pianyu_watermark_black
    }
    val alignment = when (position) {
        WatermarkPosition.BottomLeft -> Alignment.BottomStart
        WatermarkPosition.BottomCenter -> Alignment.BottomCenter
        WatermarkPosition.BottomRight -> Alignment.BottomEnd
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = alignment,
    ) {
        val watermarkAspect = 1299f / 519f
        val margin = minOf(maxWidth, maxHeight) * 0.035f
        val targetWidth = minOf(maxWidth * 0.34f, maxHeight * 0.18f * watermarkAspect)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = margin, end = margin, bottom = margin),
            contentAlignment = alignment,
        ) {
            Image(
                painter = painterResource(resourceId),
                contentDescription = "${style.label}签名水印，${position.label}",
                modifier = Modifier
                    .width(targetWidth)
                    .aspectRatio(watermarkAspect),
                alpha = 235f / 255f,
            )
        }
    }
}

@Composable
private fun GrainPreviewOverlay(amount: Int) {
    if (amount <= 0) return
    val opacity = amount.coerceIn(0, 100) / 100f * 0.18f
    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(220) { index ->
            val x = ((index * 73 + 19) % 997) / 997f * size.width
            val y = ((index * 151 + 41) % 991) / 991f * size.height
            drawCircle(
                color = if (index % 3 == 0) {
                    Color.Black.copy(alpha = opacity)
                } else {
                    Color.White.copy(alpha = opacity * 0.72f)
                },
                radius = if (index % 5 == 0) 1.15f else 0.72f,
                center = Offset(x, y),
            )
        }
    }
}

@Composable
private fun EditorSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun FilterNameDialog(
    initialName: String,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("滤镜名称") },
                supportingText = { Text("会保存全部光线、色彩与质感参数") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun SnapseedQrDialog(
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("识别到 Snapseed QR Look") },
        text = {
            Text("片屿无法在内部复制 Snapseed 的私有编辑栈，但可以把官方链接交给 Snapseed 导入。")
        },
        confirmButton = {
            Button(onClick = onOpen) { Text("交给 Snapseed") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

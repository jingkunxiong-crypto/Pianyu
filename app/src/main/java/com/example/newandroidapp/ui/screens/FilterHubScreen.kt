package com.example.newandroidapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.editing.FilterPreset
import com.example.newandroidapp.editing.ScannedFilterCode
import com.example.newandroidapp.ui.components.FilterQrDialog
import com.example.newandroidapp.ui.components.PianyuGlyph
import com.example.newandroidapp.ui.components.PianyuGlyphType
import com.example.newandroidapp.ui.components.PaperCutBackdrop
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterHubScreen(
    presets: List<FilterPreset>,
    onBack: () -> Unit,
    onScanCode: (
        onResult: (ScannedFilterCode) -> Unit,
        onFailure: (String) -> Unit,
    ) -> Unit,
    onSaveImportedPreset: (FilterPreset) -> FilterPreset,
    onDeletePreset: (String) -> Boolean,
    onOpenSnapseedQr: (String) -> Boolean,
    onInstallSnapseed: () -> Unit,
) {
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var qrPreset by remember { mutableStateOf<FilterPreset?>(null) }
    var snapseedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showInstallPrompt by rememberSaveable { mutableStateOf(false) }
    var pendingDeletePreset by remember { mutableStateOf<FilterPreset?>(null) }
    val builtIns = remember(presets) { presets.filter(FilterPreset::builtIn) }
    val custom = remember(presets) { presets.filterNot(FilterPreset::builtIn) }

    fun scan() {
        onScanCode(
            { result ->
                when (result) {
                    is ScannedFilterCode.Pianyu -> {
                        val saved = onSaveImportedPreset(result.preset)
                        qrPreset = saved
                        message = "已收藏“${saved.name}”，编辑照片时可以直接使用。"
                    }

                    is ScannedFilterCode.Snapseed -> snapseedUrl = result.url
                    is ScannedFilterCode.Unsupported -> message = result.reason
                }
            },
            { error -> message = error },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("滤镜码") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "返回照片库" },
                    ) {
                        PianyuGlyph(PianyuGlyphType.Back, selected = true)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                ) {
                    Box {
                        PaperCutBackdrop(modifier = Modifier.fillMaxSize())
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("扫码收藏滤镜", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "把喜欢的色调收进片屿；Snapseed QR Look 仍会交给 Snapseed 处理。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(
                                onClick = ::scan,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            ) {
                                PianyuGlyph(
                                    type = PianyuGlyphType.Qr,
                                    selected = true,
                                    colorOverride = MaterialTheme.colorScheme.onPrimary,
                                )
                                Text("  扫描滤镜二维码")
                            }
                        }
                    }
                }
            }
            message?.let { status ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = status,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = { message = null }) { Text("知道了") }
                        }
                    }
                }
            }
            item {
                Text("片屿内置滤镜", style = MaterialTheme.typography.titleLarge)
            }
            items(builtIns, key = FilterPreset::id) { preset ->
                FilterPresetRow(preset = preset, onShowCode = { qrPreset = preset })
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("我收藏的滤镜", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "向左滑动滤镜，可显示删除按钮",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (custom.isEmpty()) {
                item {
                    Text(
                        "还没有收藏的滤镜。扫描一张片屿滤镜码后，它会出现在这里。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(custom, key = FilterPreset::id) { preset ->
                    SwipeDeletableFilterPresetRow(
                        preset = preset,
                        onShowCode = { qrPreset = preset },
                        onRequestDelete = { pendingDeletePreset = preset },
                    )
                }
            }
            item {
                Text(
                    "说明：片屿不读取、仿制或逆向 Snapseed 的私有滤镜参数。两种滤镜码互不转换。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    qrPreset?.let { preset ->
        FilterQrDialog(preset = preset, onDismiss = { qrPreset = null })
    }
    pendingDeletePreset?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDeletePreset = null },
            title = { Text("删除“${preset.name}”？") },
            text = { Text("只会删除这条保存在片屿里的滤镜，不会影响任何照片。") },
            confirmButton = {
                Button(
                    onClick = {
                        val deleted = onDeletePreset(preset.id)
                        message = if (deleted) "已删除“${preset.name}”。" else "这条滤镜已经不存在。"
                        pendingDeletePreset = null
                    },
                ) { Text("删除滤镜") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePreset = null }) { Text("取消") }
            },
        )
    }
    snapseedUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { snapseedUrl = null },
            title = { Text("识别到 Snapseed QR Look") },
            text = { Text("这个滤镜需要在 Snapseed 中导入。片屿会把官方链接交给它打开。") },
            confirmButton = {
                Button(
                    onClick = {
                        if (!onOpenSnapseedQr(url)) showInstallPrompt = true
                        snapseedUrl = null
                    },
                ) { Text("交给 Snapseed") }
            },
            dismissButton = {
                TextButton(onClick = { snapseedUrl = null }) { Text("取消") }
            },
        )
    }
    if (showInstallPrompt) {
        AlertDialog(
            onDismissRequest = { showInstallPrompt = false },
            title = { Text("需要 Snapseed") },
            text = { Text("当前设备没有可处理此链接的 Snapseed 版本。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInstallPrompt = false
                        onInstallSnapseed()
                    },
                ) { Text("打开应用商店") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPrompt = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun FilterPresetRow(
    preset: FilterPreset,
    onShowCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(preset.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (preset.builtIn) "片屿内置" else "保存在本机",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onShowCode) { Text("查看码") }
        }
    }
}

@Composable
private fun SwipeDeletableFilterPresetRow(
    preset: FilterPreset,
    onShowCode: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val revealWidth = 104.dp
    val revealWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        revealWidth.toPx()
    }
    var targetOffsetPx by remember(preset.id) { mutableFloatStateOf(0f) }
    var isDragging by remember(preset.id) { mutableStateOf(false) }
    val renderedOffsetPx by animateFloatAsState(
        targetValue = targetOffsetPx,
        animationSpec = if (isDragging) snap() else spring(stiffness = 650f),
        label = "filterDeleteReveal",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Surface(
            modifier = Modifier.matchParentSize(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        targetOffsetPx = 0f
                        onRequestDelete()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "删除${preset.name}"
                    },
                ) {
                    PianyuGlyph(
                        type = PianyuGlyphType.Delete,
                        selected = true,
                        colorOverride = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text("  删除")
                }
            }
        }
        FilterPresetRow(
            preset = preset,
            onShowCode = onShowCode,
            modifier = Modifier
                .offset { IntOffset(renderedOffsetPx.roundToInt(), 0) }
                .pointerInput(revealWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            targetOffsetPx = (targetOffsetPx + dragAmount)
                                .coerceIn(-revealWidthPx, 0f)
                        },
                        onDragEnd = {
                            isDragging = false
                            targetOffsetPx = if (targetOffsetPx <= -revealWidthPx * 0.35f) {
                                -revealWidthPx
                            } else {
                                0f
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            targetOffsetPx = 0f
                        },
                    )
                },
        )
    }
}

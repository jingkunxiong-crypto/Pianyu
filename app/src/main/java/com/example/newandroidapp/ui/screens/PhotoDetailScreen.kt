package com.example.newandroidapp.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.newandroidapp.data.PhotoAlbum
import com.example.newandroidapp.data.PhotoItem
import com.example.newandroidapp.data.PhotoOrganizerState
import com.example.newandroidapp.ui.components.MediaPhoto
import com.example.newandroidapp.ui.components.PianyuGlyph
import com.example.newandroidapp.ui.components.PianyuGlyphType
import com.example.newandroidapp.ui.formatFileSize
import com.example.newandroidapp.ui.formatPhotoDateTime
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun PhotoDetailScreen(
    photo: PhotoItem,
    browsingPhotos: List<PhotoItem>,
    organizerState: PhotoOrganizerState,
    onPhotoChanged: (PhotoItem) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    canRestoreOriginal: Boolean,
    onRestoreOriginal: () -> Unit,
    onDelete: () -> Unit,
    onOpenSnapseed: () -> Boolean,
    onInstallSnapseed: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleAlbum: (String) -> Unit,
    onSetTags: (String) -> Unit,
) {
    var showAlbumPicker by rememberSaveable { mutableStateOf(false) }
    var showTagEditor by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showRestoreConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSnapseedInstall by rememberSaveable { mutableStateOf(false) }
    val viewerPhotos = browsingPhotos.ifEmpty { listOf(photo) }
    val initialPage = viewerPhotos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { viewerPhotos.size }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isFavorite = photo.id in organizerState.favoriteIds
    val tags = organizerState.tagsByPhotoId[photo.id].orEmpty()
    val showLightSystemBars by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 280
        }
    }

    ImmersiveSystemBars(showLightSystemBars)

    LaunchedEffect(pagerState, viewerPhotos) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                viewerPhotos.getOrNull(page)?.let(onPhotoChanged)
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) {
        item(key = "immersive-viewer") {
            Box(
                modifier = Modifier.fillMaxWidth().fillParentMaxHeight(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    key = { viewerPhotos[it].id },
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    MediaPhoto(
                        photo = viewerPhotos[page],
                        modifier = Modifier.fillMaxSize(),
                        targetSizePx = 2_560,
                        contentScale = ContentScale.Fit,
                        highQuality = true,
                        backgroundColor = Color.Black,
                    )
                }

                Surface(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 10.dp)
                        .size(48.dp)
                        .semantics { contentDescription = "返回时间线" },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.48f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PianyuGlyph(
                            type = PianyuGlyphType.Back,
                            selected = false,
                            colorOverride = Color.White,
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(end = 16.dp, top = 14.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.48f),
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${viewerPhotos.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                Surface(
                    onClick = { scope.launch { listState.animateScrollToItem(1) } },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 14.dp)
                        .semantics { contentDescription = "上滑或点击查看编辑与照片信息" },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.50f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(width = 32.dp, height = 3.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.78f),
                        ) {}
                        Text(
                            text = "上滑查看工具",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        item(key = "detail-tools") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = photo.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "左右滑动切换照片",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DetailAction("编辑", PianyuGlyphType.Edit, onEdit, Modifier.weight(1f))
                        DetailAction(
                            if (isFavorite) "已收藏" else "收藏",
                            PianyuGlyphType.Favorite,
                            onToggleFavorite,
                            Modifier.weight(1f),
                            selected = isFavorite,
                        )
                        DetailAction(
                            "光册",
                            PianyuGlyphType.Albums,
                            { showAlbumPicker = true },
                            Modifier.weight(1f),
                        )
                        DetailAction(
                            "标签",
                            PianyuGlyphType.Tag,
                            { showTagEditor = true },
                            Modifier.weight(1f),
                        )
                        DetailAction(
                            "删除",
                            PianyuGlyphType.Delete,
                            { showDeleteConfirmation = true },
                            Modifier.weight(1f),
                            destructive = true,
                        )
                    }

                    if (canRestoreOriginal) {
                        Button(
                            onClick = { showRestoreConfirmation = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) { Text("复原原图") }
                    }

                    OutlinedButton(
                        onClick = { if (!onOpenSnapseed()) showSnapseedInstall = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("在 Snapseed 中继续编辑") }

                    if (tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tags.forEach { tag ->
                                AssistChip(onClick = { showTagEditor = true }, label = { Text(tag) })
                            }
                        }
                    }
                    PhotoInformationCard(photo)
                }
            }
        }
    }

    if (showAlbumPicker) {
        AlbumPickerDialog(
            albums = organizerState.albums,
            photoId = photo.id,
            onToggleAlbum = onToggleAlbum,
            onDismiss = { showAlbumPicker = false },
        )
    }
    if (showTagEditor) {
        TagEditorDialog(
            initialTags = tags,
            onDismiss = { showTagEditor = false },
            onSave = { onSetTags(it); showTagEditor = false },
        )
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("删除这张照片？") },
            text = { Text("照片会从手机系统相册中删除。安卓系统还会再请你确认一次。") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; onDelete() }) {
                    Text("继续删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("取消") }
            },
        )
    }
    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false },
            title = { Text("复原到最初版本？") },
            text = { Text("当前编辑效果会被移除，照片将恢复为片屿第一次覆盖前备份的原始文件。") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmation = false
                        onRestoreOriginal()
                    },
                ) { Text("复原原图") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = false }) { Text("取消") }
            },
        )
    }
    if (showSnapseedInstall) {
        AlertDialog(
            onDismissRequest = { showSnapseedInstall = false },
            title = { Text("需要 Snapseed") },
            text = { Text("当前设备没有可处理这张照片的 Snapseed 版本。") },
            confirmButton = {
                TextButton(onClick = { showSnapseedInstall = false; onInstallSnapseed() }) {
                    Text("打开应用商店")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnapseedInstall = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ImmersiveSystemBars(showLightBars: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (context as? Activity)?.window ?: return
    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(window, view, darkTheme) {
        val controller = WindowCompat.getInsetsController(window, view)
        val oldStatus = controller.isAppearanceLightStatusBars
        val oldNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = oldStatus
            controller.isAppearanceLightNavigationBars = oldNavigation
        }
    }
    LaunchedEffect(window, view, showLightBars, darkTheme) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = showLightBars && !darkTheme
        controller.isAppearanceLightNavigationBars = showLightBars && !darkTheme
    }
}

@Composable
private fun DetailAction(
    label: String,
    glyph: PianyuGlyphType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    destructive: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(48.dp).semantics { contentDescription = label },
            shape = CircleShape,
            color = when {
                destructive -> MaterialTheme.colorScheme.errorContainer
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                PianyuGlyph(
                    type = glyph,
                    selected = selected,
                    colorOverride = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = label,
            color = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhotoInformationCard(photo: PhotoItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("照片信息", style = MaterialTheme.typography.titleMedium)
            DetailRow("拍摄时间", formatPhotoDateTime(photo.dateTakenMillis))
            DetailRow("尺寸", "${photo.width} × ${photo.height}")
            DetailRow("大小", formatFileSize(photo.sizeBytes))
            DetailRow("类型", photo.mimeType)
            DetailRow("来自", photo.bucketName)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            modifier = Modifier.widthIn(min = 72.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AlbumPickerDialog(
    albums: List<PhotoAlbum>,
    photoId: Long,
    onToggleAlbum: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("收进光册") },
        text = {
            if (albums.isEmpty()) {
                Text("还没有光册。请先回到“光册”页创建一本。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    albums.forEach { album ->
                        val checked = photoId in album.photoIds
                        Row(
                            modifier = Modifier.fillMaxWidth().toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { onToggleAlbum(album.id) },
                            ).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(album.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun TagEditorDialog(
    initialTags: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable(initialTags) { mutableStateOf(initialTags.joinToString("，")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑标签") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(240) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("照片标签") },
                supportingText = { Text("用逗号分隔，最多保留 12 个标签") },
                minLines = 2,
                maxLines = 4,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

package com.example.newandroidapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.newandroidapp.data.PhotoAlbum
import com.example.newandroidapp.data.PhotoItem
import com.example.newandroidapp.data.PhotoLibraryUiState
import com.example.newandroidapp.domain.formatDayLabel
import com.example.newandroidapp.domain.groupPhotosByDay
import com.example.newandroidapp.ui.components.EmptyStateCard
import com.example.newandroidapp.ui.components.PartialPhotoAccessBanner
import com.example.newandroidapp.ui.components.PhotoGridItem
import com.example.newandroidapp.ui.components.PhotoLibraryError
import com.example.newandroidapp.ui.components.PhotoLibraryLoading
import com.example.newandroidapp.ui.components.PianyuGlyphType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun TimelineScreen(
    contentPadding: PaddingValues,
    libraryState: PhotoLibraryUiState,
    favoriteIds: Set<Long>,
    albums: List<PhotoAlbum>,
    showPartialAccessBanner: Boolean,
    onManagePhotoAccess: () -> Unit,
    onRetry: () -> Unit,
    onPhotoClick: (PhotoItem) -> Unit,
    onAddPhotosToAlbum: (String, Set<Long>) -> Unit,
) {
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var showAlbumPicker by remember { mutableStateOf(false) }
    BackHandler(enabled = selectedIds.isNotEmpty()) { selectedIds = emptySet() }

    when (libraryState) {
        PhotoLibraryUiState.Loading -> StateColumn(contentPadding) { PhotoLibraryLoading() }
        is PhotoLibraryUiState.Error -> StateColumn(contentPadding) {
            PhotoLibraryError(
                title = libraryState.title,
                message = libraryState.message,
                showManagePermission = libraryState.permissionMayHaveChanged,
                onRetry = onRetry,
                onManagePermission = onManagePhotoAccess,
            )
        }
        is PhotoLibraryUiState.Ready -> {
            val photos = libraryState.photos
            if (photos.isEmpty()) {
                StateColumn(contentPadding) {
                    if (showPartialAccessBanner) {
                        PartialPhotoAccessBanner(onManagePhotoAccess = onManagePhotoAccess)
                    }
                    EmptyStateCard(
                        title = "还没有可显示的照片",
                        message = if (showPartialAccessBanner) {
                            "你可以点击上方“管理权限”，选择更多照片。"
                        } else {
                            "拍下第一张照片后，它会自动出现在这里。"
                        },
                        glyph = PianyuGlyphType.Timeline,
                    )
                }
            } else {
                val groups = groupPhotosByDay(photos)
                val gridState = rememberLazyGridState()
                val scope = rememberCoroutineScope()
                val latestSelectedIds by rememberUpdatedState(selectedIds)
                var edgeScrollJob by remember { mutableStateOf<Job?>(null) }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 104.dp),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(photos) {
                                var anchorIndex = -1
                                var baseSelection = emptySet<Long>()
                                var selecting = true

                                fun photoIdAt(x: Float, y: Float): Long? {
                                    val layoutInfo = gridState.layoutInfo
                                    val contentY = y + layoutInfo.viewportStartOffset
                                    return layoutInfo.visibleItemsInfo
                                    .firstOrNull { info ->
                                        x >= info.offset.x &&
                                            x <= info.offset.x + info.size.width &&
                                            contentY >= info.offset.y &&
                                            contentY <= info.offset.y + info.size.height
                                    }
                                    ?.key as? Long
                                }

                                fun updateRange(targetId: Long) {
                                    val targetIndex = photos.indexOfFirst { it.id == targetId }
                                    if (anchorIndex < 0 || targetIndex < 0) return
                                    val rangeIds = photos
                                        .subList(min(anchorIndex, targetIndex), max(anchorIndex, targetIndex) + 1)
                                        .mapTo(mutableSetOf(), PhotoItem::id)
                                    selectedIds = if (selecting) {
                                        baseSelection + rangeIds
                                    } else {
                                        baseSelection - rangeIds
                                    }
                                }

                                detectDragGesturesAfterLongPress(
                                    onDragStart = { position ->
                                        photoIdAt(position.x, position.y)?.let { photoId ->
                                            anchorIndex = photos.indexOfFirst { it.id == photoId }
                                            baseSelection = latestSelectedIds
                                            selecting = photoId !in baseSelection
                                            updateRange(photoId)
                                        }
                                    },
                                    onDrag = drag@{ change, _ ->
                                        if (anchorIndex < 0) return@drag
                                        change.consume()
                                        photoIdAt(change.position.x, change.position.y)?.let(::updateRange)

                                        val edge = 92.dp.toPx()
                                        val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                                        val scrollBy = when {
                                            change.position.y < edge -> -30.dp.toPx()
                                            change.position.y > viewportHeight - edge -> 30.dp.toPx()
                                            else -> 0f
                                        }
                                        edgeScrollJob?.cancel()
                                        if (scrollBy != 0f) {
                                            edgeScrollJob = scope.launch { gridState.scrollBy(scrollBy) }
                                        }
                                    },
                                    onDragEnd = {
                                        anchorIndex = -1
                                        edgeScrollJob?.cancel()
                                    },
                                    onDragCancel = {
                                        anchorIndex = -1
                                        edgeScrollJob?.cancel()
                                    },
                                )
                            },
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = contentPadding.calculateTopPadding() + 8.dp,
                            end = 16.dp,
                            bottom = contentPadding.calculateBottomPadding() + 24.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (showPartialAccessBanner) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PartialPhotoAccessBanner(onManagePhotoAccess = onManagePhotoAccess)
                            }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shadowElevation = 1.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        modifier = Modifier.size(12.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                    ) {}
                                    Text(
                                        text = "${photos.size} 张照片 · 长按后滑动可连续多选",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                        groups.forEach { group ->
                            item(key = "day-${group.dayStartMillis}", span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = formatDayLabel(group.dayStartMillis),
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
                            items(items = group.photos, key = PhotoItem::id) { photo ->
                                val isSelected = photo.id in selectedIds
                                PhotoGridItem(
                                    photo = photo,
                                    isFavorite = photo.id in favoriteIds,
                                    isSelected = isSelected,
                                    onLongClick = null,
                                    onClick = {
                                        if (selectedIds.isEmpty()) {
                                            onPhotoClick(photo)
                                        } else {
                                            selectedIds = if (isSelected) {
                                                selectedIds - photo.id
                                            } else {
                                                selectedIds + photo.id
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = selectedIds.isNotEmpty(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                start = 16.dp,
                                top = contentPadding.calculateTopPadding() + 8.dp,
                                end = 16.dp,
                            )
                            .zIndex(2f),
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 },
                    ) {
                        SelectionBar(
                            count = selectedIds.size,
                            onCancel = { selectedIds = emptySet() },
                            onAddToAlbum = { showAlbumPicker = true },
                        )
                    }
                }
            }
        }
    }

    if (showAlbumPicker) {
        BatchAlbumPickerDialog(
            albums = albums,
            photoCount = selectedIds.size,
            onDismiss = { showAlbumPicker = false },
            onSelect = { albumId ->
                onAddPhotosToAlbum(albumId, selectedIds)
                selectedIds = emptySet()
                showAlbumPicker = false
            },
        )
    }
}

@Composable
private fun SelectionBar(count: Int, onCancel: () -> Unit, onAddToAlbum: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("已选 $count 张", modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("取消") }
            Button(onClick = onAddToAlbum) { Text("收进光册") }
        }
    }
}

@Composable
private fun BatchAlbumPickerDialog(
    albums: List<PhotoAlbum>,
    photoCount: Int,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("把 $photoCount 张照片收进") },
        text = {
            if (albums.isEmpty()) {
                Text("还没有光册。请先在“光册”页创建一本。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    albums.forEach { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onSelect(album.id) }
                                .padding(horizontal = 4.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(album.name, modifier = Modifier.weight(1f))
                            Text(
                                "${album.photoIds.size} 张",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun StateColumn(contentPadding: PaddingValues, content: @Composable () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { content() }
    }
}

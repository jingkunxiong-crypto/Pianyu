package com.example.newandroidapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.data.PhotoAlbum
import com.example.newandroidapp.data.PhotoItem
import com.example.newandroidapp.ui.components.EmptyStateCard
import com.example.newandroidapp.ui.components.PartialPhotoAccessBanner
import com.example.newandroidapp.ui.components.PhotoGridItem
import com.example.newandroidapp.ui.components.PianyuGlyph
import com.example.newandroidapp.ui.components.PianyuGlyphType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: PhotoAlbum,
    photos: List<PhotoItem>,
    favoriteIds: Set<Long>,
    showPartialAccessBanner: Boolean,
    onManagePhotoAccess: () -> Unit,
    onBack: () -> Unit,
    onAddPhotos: () -> Unit,
    onRemovePhotos: (Set<Long>) -> Unit,
    onDeleteAlbum: () -> Unit,
    onPhotoClick: (PhotoItem) -> Unit,
) {
    val visiblePhotos = photos.filter { it.id in album.photoIds }
    var selectedIds by remember(album.id) { mutableStateOf(emptySet<Long>()) }
    var showDeleteAlbumConfirmation by rememberSaveable(album.id) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = selectedIds.isNotEmpty()) { selectedIds = emptySet() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(album.name) },
                navigationIcon = {
                    Row {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.semantics { contentDescription = "返回" },
                        ) {
                            PianyuGlyph(PianyuGlyphType.Back, selected = true)
                        }
                        IconButton(
                            onClick = onAddPhotos,
                            modifier = Modifier.semantics { contentDescription = "批量导入照片" },
                        ) {
                            PianyuGlyph(PianyuGlyphType.Add, selected = true)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteAlbumConfirmation = true },
                        modifier = Modifier.semantics { contentDescription = "删除光册" },
                    ) {
                        PianyuGlyph(
                            type = PianyuGlyphType.Delete,
                            selected = false,
                            colorOverride = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { contentPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp,
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
                if (selectedIds.isEmpty()) {
                    Text(
                        text = if (album.photoIds.size == visiblePhotos.size) {
                            "${visiblePhotos.size} 张照片 · 长按可多选移出光册"
                        } else {
                            "${visiblePhotos.size} 张可见照片，${album.photoIds.size - visiblePhotos.size} 张当前未授权"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    AlbumSelectionBar(
                        count = selectedIds.size,
                        onCancel = { selectedIds = emptySet() },
                        onRemove = {
                            val removedCount = selectedIds.size
                            onRemovePhotos(selectedIds)
                            selectedIds = emptySet()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "已移出 $removedCount 张照片，手机文件仍然保留",
                                )
                            }
                        },
                    )
                }
            }
            if (visiblePhotos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyStateCard(
                        title = "光册还是空的",
                        message = "点击左上角加号，可以一次选择多张照片。",
                        glyph = PianyuGlyphType.Albums,
                    )
                }
            } else {
                items(visiblePhotos, key = PhotoItem::id) { photo ->
                    val isSelected = photo.id in selectedIds
                    PhotoGridItem(
                        photo = photo,
                        isFavorite = photo.id in favoriteIds,
                        isSelected = isSelected,
                        onLongClick = { selectedIds = selectedIds + photo.id },
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
    }

    if (showDeleteAlbumConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAlbumConfirmation = false },
            title = { Text("删除“${album.name}”？") },
            text = { Text("只会删除这本光册，里面的照片仍会保留在手机和时间线中。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAlbumConfirmation = false
                        onDeleteAlbum()
                    },
                ) { Text("删除光册", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlbumConfirmation = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun AlbumSelectionBar(
    count: Int,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("已选 $count 张", modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("取消") }
            Button(onClick = onRemove) { Text("移出光册") }
        }
    }
}

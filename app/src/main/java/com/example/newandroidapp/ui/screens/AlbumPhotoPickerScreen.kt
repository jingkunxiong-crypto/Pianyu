package com.example.newandroidapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.data.PhotoAlbum
import com.example.newandroidapp.data.PhotoItem
import com.example.newandroidapp.ui.components.EmptyStateCard
import com.example.newandroidapp.ui.components.PhotoGridItem
import com.example.newandroidapp.ui.components.PianyuGlyph
import com.example.newandroidapp.ui.components.PianyuGlyphType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPhotoPickerScreen(
    album: PhotoAlbum,
    photos: List<PhotoItem>,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onAddPhotos: (Set<Long>) -> Unit,
) {
    var selectedIds by remember(album.id) { mutableStateOf(emptySet<Long>()) }
    val availablePhotos = remember(photos, album.photoIds) {
        photos.filterNot { it.id in album.photoIds }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (selectedIds.isEmpty()) "导入到${album.name}" else "已选 ${selectedIds.size} 张")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "取消导入" },
                    ) {
                        PianyuGlyph(PianyuGlyphType.Back, selected = true)
                    }
                },
                actions = {
                    TextButton(
                        enabled = selectedIds.isNotEmpty(),
                        onClick = { onAddPhotos(selectedIds) },
                    ) {
                        Text("加入")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { contentPadding ->
        if (availablePhotos.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        top = contentPadding.calculateTopPadding() + 12.dp,
                        end = 20.dp,
                    ),
            ) {
                EmptyStateCard(
                    title = "照片都在光册里了",
                    message = "当前授权的照片已经全部收入这本光册。",
                    glyph = PianyuGlyphType.Albums,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 28.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(availablePhotos, key = PhotoItem::id) { photo ->
                    val selected = photo.id in selectedIds
                    PhotoGridItem(
                        photo = photo,
                        isFavorite = photo.id in favoriteIds,
                        isSelected = selected,
                        onClick = {
                            selectedIds = if (selected) selectedIds - photo.id else selectedIds + photo.id
                        },
                    )
                }
            }
        }
    }
}

package com.example.newandroidapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.data.PhotoItem
import com.example.newandroidapp.data.PhotoLibraryUiState
import com.example.newandroidapp.ui.components.EmptyStateCard
import com.example.newandroidapp.ui.components.PartialPhotoAccessBanner
import com.example.newandroidapp.ui.components.PhotoGridItem
import com.example.newandroidapp.ui.components.PhotoLibraryError
import com.example.newandroidapp.ui.components.PhotoLibraryLoading
import com.example.newandroidapp.ui.components.PianyuGlyphType

@Composable
fun FavoritesScreen(
    contentPadding: PaddingValues,
    libraryState: PhotoLibraryUiState,
    favoriteIds: Set<Long>,
    showPartialAccessBanner: Boolean,
    onManagePhotoAccess: () -> Unit,
    onRetry: () -> Unit,
    onPhotoClick: (PhotoItem) -> Unit,
) {
    when (libraryState) {
        PhotoLibraryUiState.Loading -> FavoritesStateColumn(contentPadding) {
            PhotoLibraryLoading()
        }

        is PhotoLibraryUiState.Error -> FavoritesStateColumn(contentPadding) {
            PhotoLibraryError(
                title = libraryState.title,
                message = libraryState.message,
                showManagePermission = libraryState.permissionMayHaveChanged,
                onRetry = onRetry,
                onManagePermission = onManagePhotoAccess,
            )
        }

        is PhotoLibraryUiState.Ready -> {
            val favorites = libraryState.photos.filter { it.id in favoriteIds }
            if (favorites.isEmpty()) {
                FavoritesStateColumn(contentPadding) {
                    if (showPartialAccessBanner) {
                        PartialPhotoAccessBanner(onManagePhotoAccess = onManagePhotoAccess)
                    }
                    Text(
                        text = "把最喜欢的瞬间留在手边。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    EmptyStateCard(
                        title = "收藏还是空的",
                        message = "打开一张照片并点击“收藏”，它就会出现在这里。",
                        glyph = PianyuGlyphType.Favorite,
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
                                    text = "${favorites.size} 个喜欢的瞬间",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    items(favorites, key = PhotoItem::id) { photo ->
                        PhotoGridItem(
                            photo = photo,
                            isFavorite = true,
                            onClick = { onPhotoClick(photo) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesStateColumn(
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { content() }
    }
}

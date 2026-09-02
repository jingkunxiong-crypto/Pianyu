package com.example.newandroidapp.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.data.PhotoItem
import com.example.newandroidapp.data.PhotoLibraryUiState
import com.example.newandroidapp.data.PhotoOrganizerState
import com.example.newandroidapp.domain.searchPhotos
import com.example.newandroidapp.ui.components.EmptyStateCard
import com.example.newandroidapp.ui.components.PartialPhotoAccessBanner
import com.example.newandroidapp.ui.components.PhotoGridItem
import com.example.newandroidapp.ui.components.PhotoLibraryError
import com.example.newandroidapp.ui.components.PhotoLibraryLoading
import com.example.newandroidapp.ui.components.PianyuGlyph
import com.example.newandroidapp.ui.components.PianyuGlyphType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    libraryState: PhotoLibraryUiState,
    organizerState: PhotoOrganizerState,
    showPartialAccessBanner: Boolean,
    onManagePhotoAccess: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onPhotoClick: (PhotoItem) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("搜索照片") },
                        placeholder = { Text("文件名、相册或标签") },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                TextButton(onClick = { query = "" }) { Text("清除") }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "返回" },
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
        when (libraryState) {
            PhotoLibraryUiState.Loading -> SearchStateColumn(contentPadding) {
                PhotoLibraryLoading()
            }

            is PhotoLibraryUiState.Error -> SearchStateColumn(contentPadding) {
                PhotoLibraryError(
                    title = libraryState.title,
                    message = libraryState.message,
                    showManagePermission = libraryState.permissionMayHaveChanged,
                    onRetry = onRetry,
                    onManagePermission = onManagePhotoAccess,
                )
            }

            is PhotoLibraryUiState.Ready -> {
                val results = searchPhotos(
                    photos = libraryState.photos,
                    tagsByPhotoId = organizerState.tagsByPhotoId,
                    query = query,
                )
                if (query.isBlank()) {
                    SearchLanding(
                        contentPadding = contentPadding,
                        tags = organizerState.tagsByPhotoId.values.flatten().distinct().sorted(),
                        showPartialAccessBanner = showPartialAccessBanner,
                        onManagePhotoAccess = onManagePhotoAccess,
                        onTagClick = { query = it },
                    )
                } else {
                    SearchResults(
                        contentPadding = contentPadding,
                        query = query,
                        results = results,
                        favoriteIds = organizerState.favoriteIds,
                        showPartialAccessBanner = showPartialAccessBanner,
                        onManagePhotoAccess = onManagePhotoAccess,
                        onPhotoClick = onPhotoClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchLanding(
    contentPadding: PaddingValues,
    tags: List<String>,
    showPartialAccessBanner: Boolean,
    onManagePhotoAccess: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (showPartialAccessBanner) {
            item { PartialPhotoAccessBanner(onManagePhotoAccess = onManagePhotoAccess) }
        }
        item {
            Text(
                text = "搜索文件名、系统相册名，或你给照片添加的标签。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (tags.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("已用标签", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tags.forEach { tag ->
                            AssistChip(onClick = { onTagClick(tag) }, label = { Text(tag) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    contentPadding: PaddingValues,
    query: String,
    results: List<PhotoItem>,
    favoriteIds: Set<Long>,
    showPartialAccessBanner: Boolean,
    onManagePhotoAccess: () -> Unit,
    onPhotoClick: (PhotoItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
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
            Text(
                text = "找到 ${results.size} 张与“${query.trim()}”相关的照片",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (results.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyStateCard(
                    title = "没有找到照片",
                    message = "试试更短的关键词，或者先给照片添加标签。",
                    glyph = PianyuGlyphType.Search,
                )
            }
        } else {
            items(results, key = PhotoItem::id) { photo ->
                PhotoGridItem(
                    photo = photo,
                    isFavorite = photo.id in favoriteIds,
                    onClick = { onPhotoClick(photo) },
                )
            }
        }
    }
}

@Composable
private fun SearchStateColumn(
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { content() }
    }
}

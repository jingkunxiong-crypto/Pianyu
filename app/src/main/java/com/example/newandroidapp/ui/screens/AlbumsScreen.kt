package com.example.newandroidapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.data.PhotoAlbum
import com.example.newandroidapp.data.PhotoItem
import com.example.newandroidapp.data.PhotoLibraryUiState
import com.example.newandroidapp.data.PhotoOrganizerState
import com.example.newandroidapp.ui.components.EmptyStateCard
import com.example.newandroidapp.ui.components.MediaPhoto
import com.example.newandroidapp.ui.components.PaperCutBackdrop
import com.example.newandroidapp.ui.components.PaperCutMark
import com.example.newandroidapp.ui.components.PartialPhotoAccessBanner
import com.example.newandroidapp.ui.components.PhotoLibraryError
import com.example.newandroidapp.ui.components.PhotoLibraryLoading
import com.example.newandroidapp.ui.components.PianyuGlyphType
import com.example.newandroidapp.ui.components.PianyuGlyph
import kotlin.random.Random

@Composable
fun AlbumsScreen(
    contentPadding: PaddingValues,
    libraryState: PhotoLibraryUiState,
    organizerState: PhotoOrganizerState,
    showPartialAccessBanner: Boolean,
    onManagePhotoAccess: () -> Unit,
    onRetry: () -> Unit,
    onCreateAlbum: (String) -> Boolean,
    onAlbumClick: (PhotoAlbum) -> Unit,
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    val heroSelectionSeed = rememberSaveable { Random.nextInt() }
    val photos = (libraryState as? PhotoLibraryUiState.Ready)?.photos.orEmpty()
    val photosById = remember(photos) { photos.associateBy(PhotoItem::id) }
    val heroPhotos = remember(photos, organizerState.favoriteIds, heroSelectionSeed) {
        selectHeroPhotoIds(
            orderedPhotoIds = photos.map(PhotoItem::id),
            favoriteIds = organizerState.favoriteIds,
            seed = heroSelectionSeed,
        ).mapNotNull(photosById::get)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PaperCutBackdrop(modifier = Modifier.fillMaxSize())
        LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showPartialAccessBanner) {
            item {
                PartialPhotoAccessBanner(onManagePhotoAccess = onManagePhotoAccess)
            }
        }
        item {
            HomeHero(
                photos = photos,
                heroPhotos = heroPhotos,
                albumCount = organizerState.albums.size,
                libraryReady = libraryState is PhotoLibraryUiState.Ready,
            )
        }

        when (libraryState) {
            PhotoLibraryUiState.Loading -> item { PhotoLibraryLoading() }
            is PhotoLibraryUiState.Error -> item {
                PhotoLibraryError(
                    title = libraryState.title,
                    message = libraryState.message,
                    showManagePermission = libraryState.permissionMayHaveChanged,
                    onRetry = onRetry,
                    onManagePermission = onManagePhotoAccess,
                )
            }

            is PhotoLibraryUiState.Ready -> {
                item {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        PianyuGlyph(
                            type = PianyuGlyphType.Add,
                            selected = true,
                            colorOverride = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("新建一本光册")
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "我的光册",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "${organizerState.albums.size} 本",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                if (organizerState.albums.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "还没有光册",
                            message = "先创建一本，再在照片详情页把喜欢的画面收进来。",
                            glyph = PianyuGlyphType.Albums,
                        )
                    }
                } else {
                    organizerState.albums.forEach { album ->
                        item(key = album.id) {
                            AlbumRow(
                                album = album,
                                cover = album.photoIds.firstNotNullOfOrNull(photosById::get),
                                visiblePhotoCount = album.photoIds.count(photosById::containsKey),
                                onClick = { onAlbumClick(album) },
                            )
                        }
                    }
                }
            }
        }

    }
    }

    if (showCreateDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                if (onCreateAlbum(name)) {
                    showCreateDialog = false
                    true
                } else {
                    false
                }
            },
        )
    }
}

@Composable
private fun HomeHero(
    photos: List<PhotoItem>,
    heroPhotos: List<PhotoItem>,
    albumCount: Int,
    libraryReady: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            PaperCutBackdrop(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "片屿 · 私人影集",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "把散落的光，\n收成册。",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    text = if (libraryReady) {
                        "${photos.size} 张照片 · $albumCount 本光册"
                    } else {
                        "正在从系统相册寻找你的照片"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                PhotoPaperStack(photos = heroPhotos)
            }
        }
    }
}

internal fun selectHeroPhotoIds(
    orderedPhotoIds: List<Long>,
    favoriteIds: Set<Long>,
    seed: Int,
): List<Long> {
    if (orderedPhotoIds.isEmpty()) return emptyList()
    val favoriteCandidates = orderedPhotoIds.filter(favoriteIds::contains)
    if (favoriteCandidates.isEmpty()) return orderedPhotoIds.take(3)

    val selected = favoriteCandidates.shuffled(Random(seed)).take(3).toMutableList()
    if (selected.size < 3) {
        orderedPhotoIds.forEach { photoId ->
            if (photoId !in selected && selected.size < 3) selected += photoId
        }
    }
    return selected
}

@Composable
private fun PhotoPaperStack(photos: List<PhotoItem>) {
    Box(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (photos.isEmpty()) {
            PaperCutMark(modifier = Modifier.size(164.dp))
        } else {
            photos.getOrNull(1)?.let { photo ->
                PaperPhoto(
                    photo = photo,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 14.dp, y = 8.dp)
                        .graphicsLayer(rotationZ = -7f)
                        .width(126.dp)
                        .height(148.dp),
                )
            }
            photos.getOrNull(2)?.let { photo ->
                PaperPhoto(
                    photo = photo,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-14).dp, y = 8.dp)
                        .graphicsLayer(rotationZ = 7f)
                        .width(126.dp)
                        .height(148.dp),
                )
            }
            PaperPhoto(
                photo = photos.first(),
                modifier = Modifier.width(154.dp).height(178.dp),
            )
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 8.dp).size(32.dp),
                shape = RoundedCornerShape(99.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 3.dp,
            ) {}
        }
    }
}

@Composable
private fun PaperPhoto(photo: PhotoItem, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 7.dp,
    ) {
        Box(modifier = Modifier.padding(5.dp).clip(RoundedCornerShape(16.dp))) {
            MediaPhoto(photo = photo, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun AlbumRow(
    album: PhotoAlbum,
    cover: PhotoItem?,
    visiblePhotoCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) {
                    MediaPhoto(photo = cover, modifier = Modifier.fillMaxSize())
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {}
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = album.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "$visiblePhotoCount 张可见照片",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "打开",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun CreateAlbumDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Boolean,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val canCreate = name.trim().isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "新建光册",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(40)
                    showError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入光册名称") },
                supportingText = {
                    Text(if (showError) "请输入一个未使用的名称" else "最多 40 个字")
                },
                isError = showError,
                singleLine = true,
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { showError = !onCreate(name) },
                    enabled = canCreate,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                    ),
                ) {
                    Text("创建")
                }
            }
        },
    )
}

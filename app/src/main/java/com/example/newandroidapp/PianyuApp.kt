package com.example.newandroidapp

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.data.GalleryController
import com.example.newandroidapp.data.PhotoAlbum
import com.example.newandroidapp.data.PhotoDeleteManager
import com.example.newandroidapp.data.PhotoDeleteResult
import com.example.newandroidapp.data.PhotoItem
import com.example.newandroidapp.data.PhotoLibraryUiState
import com.example.newandroidapp.editing.FilterLibraryController
import com.example.newandroidapp.editing.EditSettings
import com.example.newandroidapp.editing.PhotoExporter
import com.example.newandroidapp.editing.PhotoOriginalManager
import com.example.newandroidapp.editing.PhotoWriteResult
import com.example.newandroidapp.editing.ScannedFilterCode
import com.example.newandroidapp.editing.SnapseedIntegration
import com.example.newandroidapp.permissions.PhotoAccessLevel
import com.example.newandroidapp.ui.components.PianyuGlyph
import com.example.newandroidapp.ui.components.PianyuGlyphType
import com.example.newandroidapp.ui.components.PaperCutMark
import com.example.newandroidapp.ui.screens.AlbumDetailScreen
import com.example.newandroidapp.ui.screens.AlbumPhotoPickerScreen
import com.example.newandroidapp.ui.screens.AlbumsScreen
import com.example.newandroidapp.ui.screens.FavoritesScreen
import com.example.newandroidapp.ui.screens.FilterHubScreen
import com.example.newandroidapp.ui.screens.FilterScannerScreen
import com.example.newandroidapp.ui.screens.PhotoDetailScreen
import com.example.newandroidapp.ui.screens.PhotoEditorScreen
import com.example.newandroidapp.ui.screens.PrivacyScreen
import com.example.newandroidapp.ui.screens.SearchScreen
import com.example.newandroidapp.ui.screens.TimelineScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TopLevelDestination(
    val title: String,
    val label: String,
    val glyph: PianyuGlyphType,
) {
    Albums(title = "片屿", label = "光册", glyph = PianyuGlyphType.Albums),
    Timeline(title = "时间线", label = "时间线", glyph = PianyuGlyphType.Timeline),
    Favorites(title = "收藏", label = "收藏", glyph = PianyuGlyphType.Favorite),
}

private sealed interface AppRoute {
    data object Library : AppRoute
    data object Search : AppRoute
    data object FilterHub : AppRoute
    data object Privacy : AppRoute
    data class Album(val albumId: String) : AppRoute
    data class AlbumPhotoPicker(val albumId: String) : AppRoute
    data class Photo(val photoId: Long, val origin: PhotoOrigin) : AppRoute
    data class Editor(val photoId: Long, val origin: PhotoOrigin) : AppRoute
}

private sealed interface PhotoOrigin {
    data object Library : PhotoOrigin
    data object Search : PhotoOrigin
    data class Album(val albumId: String) : PhotoOrigin
}

private data class SnapseedWatch(
    val launchedAtMillis: Long,
    val existingPhotoIds: Set<Long>,
)

private data class PendingFilterScan(
    val onResult: (ScannedFilterCode) -> Unit,
    val onFailure: (String) -> Unit,
)

private sealed interface PendingPhotoWrite {
    val photoId: Long
    val origin: PhotoOrigin

    data class Overwrite(
        override val photoId: Long,
        override val origin: PhotoOrigin,
        val settings: EditSettings,
    ) : PendingPhotoWrite

    data class Restore(
        override val photoId: Long,
        override val origin: PhotoOrigin,
    ) : PendingPhotoWrite
}

@Composable
fun PianyuApp(
    photoAccessLevel: PhotoAccessLevel,
    onManagePhotoAccess: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(context) { GalleryController(context.applicationContext) }
    val filterLibrary = remember(context) { FilterLibraryController(context.applicationContext) }
    val exporter = remember(context) { PhotoExporter(context.applicationContext) }
    val originalManager = remember(context) { PhotoOriginalManager(context.applicationContext) }
    val deleteManager = remember(context) { PhotoDeleteManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var route: AppRoute by remember { mutableStateOf(AppRoute.Library) }
    var destinationName by rememberSaveable { mutableStateOf(TopLevelDestination.Albums.name) }
    var snapseedWatch by remember { mutableStateOf<SnapseedWatch?>(null) }
    var detectedSnapseedPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<Pair<Long, PhotoOrigin>?>(null) }
    var deleteError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoWrite by remember { mutableStateOf<PendingPhotoWrite?>(null) }
    var photoWriteMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var photoWriteError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFilterScan by remember { mutableStateOf<PendingFilterScan?>(null) }
    val destination = remember(destinationName) { TopLevelDestination.valueOf(destinationName) }
    val libraryState = controller.libraryState
    val organizerState = controller.organizerState
    val photos = (libraryState as? PhotoLibraryUiState.Ready)?.photos.orEmpty()
    val showPartialAccessBanner = photoAccessLevel == PhotoAccessLevel.Partial

    fun retryPhotoLoad() {
        scope.launch { controller.refresh(showLoading = true) }
    }

    fun returnFromPhoto(origin: PhotoOrigin): AppRoute = when (origin) {
        PhotoOrigin.Library -> AppRoute.Library
        PhotoOrigin.Search -> AppRoute.Search
        is PhotoOrigin.Album -> AppRoute.Album(origin.albumId)
    }

    fun goBack() {
        route = when (val current = route) {
            AppRoute.Library -> AppRoute.Library
            AppRoute.Search,
            AppRoute.FilterHub,
            AppRoute.Privacy,
            is AppRoute.Album,
            -> AppRoute.Library

            is AppRoute.AlbumPhotoPicker -> AppRoute.Album(current.albumId)

            is AppRoute.Photo -> returnFromPhoto(current.origin)
            is AppRoute.Editor -> AppRoute.Photo(current.photoId, current.origin)
        }
    }

    val deleteConfirmationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val deletion = pendingDelete
        pendingDelete = null
        if (result.resultCode == Activity.RESULT_OK && deletion != null) {
            controller.forgetPhotos(setOf(deletion.first))
            route = returnFromPhoto(deletion.second)
            scope.launch { controller.refresh() }
        }
    }

    val writeConfirmationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val operation = pendingPhotoWrite
        if (result.resultCode != Activity.RESULT_OK || operation == null) {
            pendingPhotoWrite = null
        } else {
            scope.launch {
                val source = photos.firstOrNull { it.id == operation.photoId }
                if (source == null) {
                    pendingPhotoWrite = null
                    photoWriteError = "照片已经不在当前媒体库中。"
                    return@launch
                }
                val writeResult = when (operation) {
                    is PendingPhotoWrite.Overwrite -> originalManager.overwrite(
                        source,
                        operation.settings,
                        authorized = true,
                    )
                    is PendingPhotoWrite.Restore -> originalManager.restore(
                        source,
                        authorized = true,
                    )
                }
                when (writeResult) {
                    PhotoWriteResult.Done -> {
                        pendingPhotoWrite = null
                        controller.refresh()
                        route = AppRoute.Photo(source.id, operation.origin)
                        photoWriteMessage = if (operation is PendingPhotoWrite.Overwrite) {
                            "已覆盖原图，并保留了可复原备份。"
                        } else {
                            "已复原到最初版本。"
                        }
                    }
                    is PhotoWriteResult.Failed -> {
                        pendingPhotoWrite = null
                        photoWriteError = writeResult.message
                    }
                    is PhotoWriteResult.NeedsSystemConfirmation -> {
                        pendingPhotoWrite = null
                        photoWriteError = "系统没有完成照片写入授权，请重试。"
                    }
                }
            }
        }
    }

    fun overwritePhoto(photo: PhotoItem, origin: PhotoOrigin, settings: EditSettings) {
        val operation = PendingPhotoWrite.Overwrite(photo.id, origin, settings)
        pendingPhotoWrite = operation
        scope.launch {
            when (val result = originalManager.overwrite(photo, settings)) {
                PhotoWriteResult.Done -> {
                    pendingPhotoWrite = null
                    controller.refresh()
                    route = AppRoute.Photo(photo.id, origin)
                    photoWriteMessage = "已覆盖原图，并保留了可复原备份。"
                }
                is PhotoWriteResult.NeedsSystemConfirmation -> {
                    writeConfirmationLauncher.launch(result.request)
                }
                is PhotoWriteResult.Failed -> {
                    pendingPhotoWrite = null
                    photoWriteError = result.message
                }
            }
        }
    }

    fun restorePhoto(photo: PhotoItem, origin: PhotoOrigin) {
        val operation = PendingPhotoWrite.Restore(photo.id, origin)
        pendingPhotoWrite = operation
        scope.launch {
            when (val result = originalManager.restore(photo)) {
                PhotoWriteResult.Done -> {
                    pendingPhotoWrite = null
                    controller.refresh()
                    route = AppRoute.Photo(photo.id, origin)
                    photoWriteMessage = "已复原到最初版本。"
                }
                is PhotoWriteResult.NeedsSystemConfirmation -> {
                    writeConfirmationLauncher.launch(result.request)
                }
                is PhotoWriteResult.Failed -> {
                    pendingPhotoWrite = null
                    photoWriteError = result.message
                }
            }
        }
    }

    fun deletePhoto(photo: PhotoItem, origin: PhotoOrigin) {
        scope.launch {
            when (val result = deleteManager.prepare(photo)) {
                PhotoDeleteResult.Deleted -> {
                    controller.forgetPhotos(setOf(photo.id))
                    route = returnFromPhoto(origin)
                    controller.refresh()
                }
                is PhotoDeleteResult.NeedsSystemConfirmation -> {
                    pendingDelete = photo.id to origin
                    deleteConfirmationLauncher.launch(result.request)
                }
                is PhotoDeleteResult.Failed -> deleteError = result.message
            }
        }
    }

    fun scanFilter(
        onResult: (ScannedFilterCode) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        pendingFilterScan = PendingFilterScan(onResult = onResult, onFailure = onFailure)
    }

    fun openPhotoInSnapseed(photo: PhotoItem): Boolean {
        val opened = SnapseedIntegration.openPhoto(context, photo)
        if (opened) {
            snapseedWatch = SnapseedWatch(
                launchedAtMillis = System.currentTimeMillis(),
                existingPhotoIds = photos.mapTo(mutableSetOf(), PhotoItem::id),
            )
        }
        return opened
    }

    BackHandler(enabled = pendingFilterScan != null) { pendingFilterScan = null }
    BackHandler(enabled = pendingFilterScan == null && route != AppRoute.Library) { goBack() }

    LaunchedEffect(photoAccessLevel) {
        controller.refresh(showLoading = true)
    }

    DisposableEffect(controller, scope) {
        var refreshJob: Job? = null
        val observer = controller.registerMediaObserver {
            refreshJob?.cancel()
            refreshJob = scope.launch {
                delay(250)
                controller.refresh()
            }
        }
        onDispose {
            refreshJob?.cancel()
            controller.unregisterMediaObserver(observer)
        }
    }

    LaunchedEffect(route, libraryState) {
        when (val current = route) {
            is AppRoute.Photo,
            is AppRoute.Editor,
            -> {
                val photoId = when (current) {
                    is AppRoute.Photo -> current.photoId
                    is AppRoute.Editor -> current.photoId
                    else -> error("unreachable")
                }
                if (libraryState is PhotoLibraryUiState.Ready &&
                    libraryState.photos.none { it.id == photoId }
                ) {
                    route = when (current) {
                        is AppRoute.Photo -> returnFromPhoto(current.origin)
                        is AppRoute.Editor -> returnFromPhoto(current.origin)
                        else -> AppRoute.Library
                    }
                }
            }

            is AppRoute.Album,
            is AppRoute.AlbumPhotoPicker,
            -> {
                val albumId = when (current) {
                    is AppRoute.Album -> current.albumId
                    is AppRoute.AlbumPhotoPicker -> current.albumId
                    else -> error("unreachable")
                }
                if (organizerState.albums.none { it.id == albumId }) {
                    route = AppRoute.Library
                }
            }

            else -> Unit
        }
    }

    LaunchedEffect(libraryState, snapseedWatch) {
        val watch = snapseedWatch ?: return@LaunchedEffect
        val ready = libraryState as? PhotoLibraryUiState.Ready ?: return@LaunchedEffect
        val newest = ready.photos
            .asSequence()
            .filterNot { it.id in watch.existingPhotoIds }
            .filter { it.dateAddedSeconds * 1_000L >= watch.launchedAtMillis - 5_000L }
            .maxByOrNull(PhotoItem::dateAddedSeconds)
        if (newest != null) {
            detectedSnapseedPhotoId = newest.id
            snapseedWatch = null
        }
    }

    LaunchedEffect(snapseedWatch) {
        val current = snapseedWatch ?: return@LaunchedEffect
        delay(30 * 60 * 1_000L)
        if (snapseedWatch == current) snapseedWatch = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentRoute = route) {
        AppRoute.Library -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    PianyuTopBar(
                        destination = destination,
                        onPrivacyClick = { route = AppRoute.Privacy },
                        onFilterClick = { route = AppRoute.FilterHub },
                        onSearchClick = { route = AppRoute.Search },
                    )
                },
                bottomBar = {
                    PianyuNavigationBar(
                        currentDestination = destination,
                        onDestinationSelected = { destinationName = it.name },
                    )
                },
            ) { contentPadding ->
                AnimatedContent(
                    targetState = destination,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        }
                        (slideIntoContainer(direction, tween(260)) + fadeIn(tween(180))) togetherWith
                            (slideOutOfContainer(direction, tween(220)) + fadeOut(tween(140)))
                    },
                    label = "top-level-navigation",
                ) { currentDestination ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentDestination) {
                            TopLevelDestination.Albums -> AlbumsScreen(
                                contentPadding = contentPadding,
                                libraryState = libraryState,
                                organizerState = organizerState,
                                showPartialAccessBanner = showPartialAccessBanner,
                                onManagePhotoAccess = onManagePhotoAccess,
                                onRetry = ::retryPhotoLoad,
                                onCreateAlbum = controller::createAlbum,
                                onAlbumClick = { route = AppRoute.Album(it.id) },
                            )

                            TopLevelDestination.Timeline -> TimelineScreen(
                                contentPadding = contentPadding,
                                libraryState = libraryState,
                                favoriteIds = organizerState.favoriteIds,
                                albums = organizerState.albums,
                                showPartialAccessBanner = showPartialAccessBanner,
                                onManagePhotoAccess = onManagePhotoAccess,
                                onRetry = ::retryPhotoLoad,
                                onAddPhotosToAlbum = controller::addPhotosToAlbum,
                                onPhotoClick = {
                                    route = AppRoute.Photo(it.id, PhotoOrigin.Library)
                                },
                            )

                            TopLevelDestination.Favorites -> FavoritesScreen(
                                contentPadding = contentPadding,
                                libraryState = libraryState,
                                favoriteIds = organizerState.favoriteIds,
                                showPartialAccessBanner = showPartialAccessBanner,
                                onManagePhotoAccess = onManagePhotoAccess,
                                onRetry = ::retryPhotoLoad,
                                onPhotoClick = {
                                    route = AppRoute.Photo(it.id, PhotoOrigin.Library)
                                },
                            )
                        }
                    }
                }
            }
        }

        AppRoute.Search -> SearchScreen(
            libraryState = libraryState,
            organizerState = organizerState,
            showPartialAccessBanner = showPartialAccessBanner,
            onManagePhotoAccess = onManagePhotoAccess,
            onRetry = ::retryPhotoLoad,
            onBack = ::goBack,
            onPhotoClick = { route = AppRoute.Photo(it.id, PhotoOrigin.Search) },
        )

        AppRoute.FilterHub -> FilterHubScreen(
            presets = filterLibrary.allPresets,
            onBack = ::goBack,
            onScanCode = ::scanFilter,
            onSaveImportedPreset = filterLibrary::saveImported,
            onDeletePreset = filterLibrary::deleteCustomPreset,
            onOpenSnapseedQr = { SnapseedIntegration.openQrLook(context, it) },
            onInstallSnapseed = { SnapseedIntegration.openStore(context) },
        )

        AppRoute.Privacy -> PrivacyScreen(onBack = ::goBack)

        is AppRoute.Album -> {
            val album: PhotoAlbum? = organizerState.albums.firstOrNull {
                it.id == currentRoute.albumId
            }
            if (album != null) {
                AlbumDetailScreen(
                    album = album,
                    photos = photos,
                    favoriteIds = organizerState.favoriteIds,
                    showPartialAccessBanner = showPartialAccessBanner,
                    onManagePhotoAccess = onManagePhotoAccess,
                    onBack = ::goBack,
                    onAddPhotos = { route = AppRoute.AlbumPhotoPicker(album.id) },
                    onRemovePhotos = { photoIds ->
                        controller.removePhotosFromAlbum(album.id, photoIds)
                    },
                    onDeleteAlbum = {
                        controller.deleteAlbum(album.id)
                        route = AppRoute.Library
                    },
                    onPhotoClick = {
                        route = AppRoute.Photo(
                            photoId = it.id,
                            origin = PhotoOrigin.Album(album.id),
                        )
                    },
                )
            }
        }

        is AppRoute.AlbumPhotoPicker -> {
            val album = organizerState.albums.firstOrNull { it.id == currentRoute.albumId }
            if (album != null) {
                AlbumPhotoPickerScreen(
                    album = album,
                    photos = photos,
                    favoriteIds = organizerState.favoriteIds,
                    onBack = ::goBack,
                    onAddPhotos = { photoIds ->
                        controller.addPhotosToAlbum(album.id, photoIds)
                        route = AppRoute.Album(album.id)
                    },
                )
            }
        }

        is AppRoute.Photo -> {
            val photo = photos.firstOrNull { it.id == currentRoute.photoId }
            if (photo != null) {
                val browsingPhotos = when (val origin = currentRoute.origin) {
                    PhotoOrigin.Library -> photos
                    PhotoOrigin.Search -> listOf(photo)
                    is PhotoOrigin.Album -> {
                        val photoIds = organizerState.albums
                            .firstOrNull { it.id == origin.albumId }
                            ?.photoIds
                            .orEmpty()
                        photos.filter { it.id in photoIds }
                    }
                }
                PhotoDetailScreen(
                    photo = photo,
                    browsingPhotos = browsingPhotos,
                    organizerState = organizerState,
                    onPhotoChanged = { nextPhoto ->
                        route = AppRoute.Photo(nextPhoto.id, currentRoute.origin)
                    },
                    onBack = ::goBack,
                    onEdit = { route = AppRoute.Editor(photo.id, currentRoute.origin) },
                    canRestoreOriginal = originalManager.hasBackup(photo),
                    onRestoreOriginal = { restorePhoto(photo, currentRoute.origin) },
                    onDelete = { deletePhoto(photo, currentRoute.origin) },
                    onOpenSnapseed = { openPhotoInSnapseed(photo) },
                    onInstallSnapseed = { SnapseedIntegration.openStore(context) },
                    onToggleFavorite = { controller.toggleFavorite(photo.id) },
                    onToggleAlbum = { albumId ->
                        controller.togglePhotoInAlbum(albumId, photo.id)
                    },
                    onSetTags = { controller.setTags(photo.id, it) },
                )
            }
        }

        is AppRoute.Editor -> {
            val photo = photos.firstOrNull { it.id == currentRoute.photoId }
            if (photo != null) {
                PhotoEditorScreen(
                    photo = photo,
                    presets = filterLibrary.allPresets,
                    onBack = ::goBack,
                    onExport = { settings -> exporter.export(photo, settings) },
                    onExported = { exported ->
                        scope.launch {
                            controller.refresh()
                            route = AppRoute.Photo(exported.id, PhotoOrigin.Library)
                        }
                    },
                    onOverwrite = { settings ->
                        overwritePhoto(photo, currentRoute.origin, settings)
                    },
                    writeInProgress = pendingPhotoWrite != null,
                    onScanCode = ::scanFilter,
                    onSaveImportedPreset = filterLibrary::saveImported,
                    onOpenSnapseedPhoto = { openPhotoInSnapseed(photo) },
                    onOpenSnapseedQr = { SnapseedIntegration.openQrLook(context, it) },
                    onInstallSnapseed = { SnapseedIntegration.openStore(context) },
                )
            }
        }
        }

        pendingFilterScan?.let { scan ->
            FilterScannerScreen(
                onDismiss = { pendingFilterScan = null },
                onResult = { result ->
                    pendingFilterScan = null
                    scan.onResult(result)
                },
                onFailure = { message ->
                    pendingFilterScan = null
                    scan.onFailure(message)
                },
            )
        }
    }

    detectedSnapseedPhotoId?.let { photoId ->
        val detectedPhoto = photos.firstOrNull { it.id == photoId }
        AlertDialog(
            onDismissRequest = { detectedSnapseedPhotoId = null },
            title = { Text("检测到新照片") },
            text = {
                Text(
                    if (detectedPhoto != null) {
                        "系统相册新增了“${detectedPhoto.displayName}”，可能是从 Snapseed 保存的结果。"
                    } else {
                        "系统相册新增了一张照片，可能是从 Snapseed 保存的结果。"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        detectedSnapseedPhotoId = null
                        if (detectedPhoto != null) {
                            route = AppRoute.Photo(detectedPhoto.id, PhotoOrigin.Library)
                        }
                    },
                    enabled = detectedPhoto != null,
                ) { Text("查看照片") }
            },
            dismissButton = {
                TextButton(onClick = { detectedSnapseedPhotoId = null }) { Text("稍后") }
            },
        )
    }

    deleteError?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = { Text("无法删除照片") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { deleteError = null }) { Text("知道了") }
            },
        )
    }

    photoWriteMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { photoWriteMessage = null },
            title = { Text("照片已更新") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { photoWriteMessage = null }) { Text("完成") }
            },
        )
    }

    photoWriteError?.let { message ->
        AlertDialog(
            onDismissRequest = { photoWriteError = null },
            title = { Text("无法修改照片") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { photoWriteError = null }) { Text("知道了") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PianyuTopBar(
    destination: TopLevelDestination,
    onPrivacyClick: () -> Unit,
    onFilterClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (destination == TopLevelDestination.Albums) {
                    Surface(
                        onClick = onPrivacyClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "隐私与数据" },
                        shape = CircleShape,
                        color = Color.Transparent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PaperCutMark(
                                modifier = Modifier.size(40.dp),
                                contentDescription = "隐私与数据入口",
                            )
                        }
                    }
                }
                Text(
                    text = destination.title,
                    style = if (destination == TopLevelDestination.Albums) {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                )
            }
        },
        actions = {
            Surface(
                onClick = onFilterClick,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(44.dp)
                    .semantics { contentDescription = "滤镜二维码" },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PianyuGlyph(type = PianyuGlyphType.Qr, selected = true)
                }
            }
            Surface(
                onClick = onSearchClick,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(44.dp)
                    .semantics { contentDescription = "搜索照片" },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PianyuGlyph(type = PianyuGlyphType.Search, selected = true)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun PianyuNavigationBar(
    currentDestination: TopLevelDestination,
    onDestinationSelected: (TopLevelDestination) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = destination == currentDestination
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onDestinationSelected(destination) },
                        icon = { PianyuGlyph(type = destination.glyph, selected = selected) },
                        label = { Text(destination.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

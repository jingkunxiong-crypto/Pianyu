package com.example.newandroidapp.data

import android.content.Context
import android.database.ContentObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface PhotoLibraryUiState {
    data object Loading : PhotoLibraryUiState

    data class Ready(val photos: List<PhotoItem>) : PhotoLibraryUiState

    data class Error(
        val title: String,
        val message: String,
        val permissionMayHaveChanged: Boolean,
    ) : PhotoLibraryUiState
}

@Stable
class GalleryController(context: Context) {
    private val repository = PhotoRepository(context)
    private val organizerStore = PhotoOrganizerStore(context)

    var libraryState: PhotoLibraryUiState by mutableStateOf(PhotoLibraryUiState.Loading)
        private set

    var organizerState: PhotoOrganizerState by mutableStateOf(organizerStore.load())
        private set

    suspend fun refresh(showLoading: Boolean = false) {
        if (showLoading || libraryState !is PhotoLibraryUiState.Ready) {
            libraryState = PhotoLibraryUiState.Loading
        }
        libraryState = try {
            PhotoLibraryUiState.Ready(repository.loadPhotos())
        } catch (_: SecurityException) {
            PhotoLibraryUiState.Error(
                title = "照片权限已变化",
                message = "请重新选择片屿可以访问的照片，然后再试一次。",
                permissionMayHaveChanged = true,
            )
        } catch (_: Exception) {
            PhotoLibraryUiState.Error(
                title = "暂时无法读取照片",
                message = "系统相册暂时没有响应，请稍后重试。",
                permissionMayHaveChanged = false,
            )
        }
    }

    fun registerMediaObserver(onMediaChanged: () -> Unit): ContentObserver {
        return repository.registerObserver(onMediaChanged)
    }

    fun unregisterMediaObserver(observer: ContentObserver) {
        repository.unregisterObserver(observer)
    }

    fun createAlbum(name: String): Boolean {
        val updated = organizerStore.createAlbum(organizerState, name) ?: return false
        persist(updated)
        return true
    }

    fun toggleFavorite(photoId: Long) {
        persist(organizerStore.toggleFavorite(organizerState, photoId))
    }

    fun togglePhotoInAlbum(albumId: String, photoId: Long) {
        persist(organizerStore.togglePhotoInAlbum(organizerState, albumId, photoId))
    }

    fun addPhotosToAlbum(albumId: String, photoIds: Set<Long>) {
        persist(organizerStore.addPhotosToAlbum(organizerState, albumId, photoIds))
    }

    fun removePhotosFromAlbum(albumId: String, photoIds: Set<Long>) {
        persist(organizerStore.removePhotosFromAlbum(organizerState, albumId, photoIds))
    }

    fun deleteAlbum(albumId: String) {
        persist(organizerStore.deleteAlbum(organizerState, albumId))
    }

    fun forgetPhotos(photoIds: Set<Long>) {
        persist(organizerStore.forgetPhotos(organizerState, photoIds))
    }

    fun setTags(photoId: Long, tags: String) {
        persist(organizerStore.setTags(organizerState, photoId, tags))
    }

    private fun persist(updated: PhotoOrganizerState) {
        organizerState = updated
        organizerStore.save(updated)
    }
}

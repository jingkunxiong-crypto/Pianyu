package com.example.newandroidapp.data

import android.content.Context
import androidx.core.content.edit
import com.example.newandroidapp.domain.normalizeTags
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PhotoAlbum(
    val id: String,
    val name: String,
    val photoIds: Set<Long>,
    val createdAtMillis: Long,
)

data class PhotoOrganizerState(
    val favoriteIds: Set<Long> = emptySet(),
    val albums: List<PhotoAlbum> = emptyList(),
    val tagsByPhotoId: Map<Long, Set<String>> = emptyMap(),
)

internal fun PhotoOrganizerState.withPhotosRemovedFromAlbum(
    albumId: String,
    photoIds: Set<Long>,
): PhotoOrganizerState {
    if (photoIds.isEmpty()) return this
    return copy(
        albums = albums.map { album ->
            if (album.id == albumId) album.copy(photoIds = album.photoIds - photoIds) else album
        },
    )
}

internal fun PhotoOrganizerState.withAlbumDeleted(albumId: String): PhotoOrganizerState {
    return copy(albums = albums.filterNot { it.id == albumId })
}

class PhotoOrganizerStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "photo_organizer",
        Context.MODE_PRIVATE,
    )

    fun load(): PhotoOrganizerState {
        val raw = preferences.getString(StateKey, null) ?: return PhotoOrganizerState()
        return runCatching { decode(JSONObject(raw)) }.getOrDefault(PhotoOrganizerState())
    }

    fun save(state: PhotoOrganizerState) {
        preferences.edit { putString(StateKey, encode(state).toString()) }
    }

    fun createAlbum(state: PhotoOrganizerState, name: String): PhotoOrganizerState? {
        val cleanName = name.trim().take(MaxAlbumNameLength)
        if (cleanName.isEmpty() || state.albums.any { it.name.equals(cleanName, ignoreCase = true) }) {
            return null
        }
        return state.copy(
            albums = listOf(PhotoAlbum(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                photoIds = emptySet(),
                createdAtMillis = System.currentTimeMillis(),
            )) + state.albums,
        )
    }

    fun toggleFavorite(state: PhotoOrganizerState, photoId: Long): PhotoOrganizerState {
        val updated = if (photoId in state.favoriteIds) {
            state.favoriteIds - photoId
        } else {
            state.favoriteIds + photoId
        }
        return state.copy(favoriteIds = updated)
    }

    fun togglePhotoInAlbum(
        state: PhotoOrganizerState,
        albumId: String,
        photoId: Long,
    ): PhotoOrganizerState {
        return state.copy(
            albums = state.albums.map { album ->
                if (album.id != albumId) {
                    album
                } else {
                    album.copy(
                        photoIds = if (photoId in album.photoIds) {
                            album.photoIds - photoId
                        } else {
                            album.photoIds + photoId
                        },
                    )
                }
            },
        )
    }

    fun addPhotosToAlbum(
        state: PhotoOrganizerState,
        albumId: String,
        photoIds: Set<Long>,
    ): PhotoOrganizerState {
        if (photoIds.isEmpty()) return state
        return state.copy(
            albums = state.albums.map { album ->
                if (album.id == albumId) album.copy(photoIds = album.photoIds + photoIds) else album
            },
        )
    }

    fun removePhotosFromAlbum(
        state: PhotoOrganizerState,
        albumId: String,
        photoIds: Set<Long>,
    ): PhotoOrganizerState = state.withPhotosRemovedFromAlbum(albumId, photoIds)

    fun deleteAlbum(
        state: PhotoOrganizerState,
        albumId: String,
    ): PhotoOrganizerState = state.withAlbumDeleted(albumId)

    fun forgetPhotos(
        state: PhotoOrganizerState,
        photoIds: Set<Long>,
    ): PhotoOrganizerState {
        if (photoIds.isEmpty()) return state
        return state.copy(
            favoriteIds = state.favoriteIds - photoIds,
            albums = state.albums.map { album ->
                album.copy(photoIds = album.photoIds - photoIds)
            },
            tagsByPhotoId = state.tagsByPhotoId - photoIds,
        )
    }

    fun setTags(
        state: PhotoOrganizerState,
        photoId: Long,
        tags: String,
    ): PhotoOrganizerState {
        val normalizedTags = normalizeTags(tags)
        val updated = state.tagsByPhotoId.toMutableMap().apply {
            if (normalizedTags.isEmpty()) remove(photoId) else put(photoId, normalizedTags)
        }
        return state.copy(tagsByPhotoId = updated)
    }

    private fun encode(state: PhotoOrganizerState): JSONObject {
        return JSONObject().apply {
            put("favorites", JSONArray().apply {
                state.favoriteIds.forEach(::put)
            })
            put("albums", JSONArray().apply {
                state.albums.forEach { album ->
                    put(JSONObject().apply {
                        put("id", album.id)
                        put("name", album.name)
                        put("createdAt", album.createdAtMillis)
                        put("photos", JSONArray().apply { album.photoIds.forEach(::put) })
                    })
                }
            })
            put("tags", JSONObject().apply {
                state.tagsByPhotoId.forEach { (photoId, tags) ->
                    put(photoId.toString(), JSONArray().apply { tags.forEach(::put) })
                }
            })
        }
    }

    private fun decode(json: JSONObject): PhotoOrganizerState {
        val favorites = json.optJSONArray("favorites").toLongSet()
        val albums = buildList {
            val array = json.optJSONArray("albums") ?: JSONArray()
            repeat(array.length()) { index ->
                val album = array.optJSONObject(index) ?: return@repeat
                val id = album.optString("id")
                val name = album.optString("name")
                if (id.isNotBlank() && name.isNotBlank()) {
                    add(
                        PhotoAlbum(
                            id = id,
                            name = name,
                            createdAtMillis = album.optLong("createdAt"),
                            photoIds = album.optJSONArray("photos").toLongSet(),
                        ),
                    )
                }
            }
        }
        val tags = buildMap {
            val tagsObject = json.optJSONObject("tags") ?: JSONObject()
            val keys = tagsObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                key.toLongOrNull()?.let { photoId ->
                    val values = tagsObject.optJSONArray(key).toStringSet()
                    if (values.isNotEmpty()) put(photoId, values)
                }
            }
        }
        return PhotoOrganizerState(
            favoriteIds = favorites,
            albums = albums.sortedByDescending(PhotoAlbum::createdAtMillis),
            tagsByPhotoId = tags,
        )
    }

    private fun JSONArray?.toLongSet(): Set<Long> {
        if (this == null) return emptySet()
        return buildSet {
            repeat(length()) { index -> add(optLong(index)) }
        }
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return buildSet {
            repeat(length()) { index ->
                optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }

    private companion object {
        const val StateKey = "organizer_state_v1"
        const val MaxAlbumNameLength = 40
    }
}

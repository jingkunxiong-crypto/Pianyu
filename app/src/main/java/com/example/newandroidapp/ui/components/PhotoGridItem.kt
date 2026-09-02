package com.example.newandroidapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.data.PhotoItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGridItem(
    photo: PhotoItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics {
                contentDescription = "打开${photo.displayName}"
                selected = isSelected
                stateDescription = when {
                    isSelected -> "已选择"
                    isFavorite -> "已收藏"
                    else -> "未收藏"
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaPhoto(
                photo = photo,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(30.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PianyuGlyph(
                            type = PianyuGlyphType.Check,
                            selected = true,
                            colorOverride = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
            if (isFavorite) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PianyuGlyph(
                            type = PianyuGlyphType.Favorite,
                            selected = true,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

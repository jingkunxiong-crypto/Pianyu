package com.example.newandroidapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.ui.theme.ContactBlue
import com.example.newandroidapp.ui.theme.SafeLight
import com.example.newandroidapp.ui.theme.SilverGrain

private val framePalettes = listOf(
    listOf(Color(0xFF33475B), Color(0xFF91A7B7)),
    listOf(Color(0xFF62776B), Color(0xFFC6D0C8)),
    listOf(Color(0xFF8B625A), Color(0xFFE0B8A2)),
    listOf(Color(0xFF6B6781), Color(0xFFB8B4CE)),
    listOf(Color(0xFF9A7959), Color(0xFFE2C69E)),
)

@Composable
fun ContactSheet(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            framePalettes.forEachIndexed { index, colors ->
                Box(
                    modifier = Modifier
                        .width(78.dp)
                        .aspectRatio(0.78f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(colors))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(10.dp),
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = (index + 1).toString().padStart(2, '0'),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(ContactBlue, SilverGrain, SafeLight).forEach { color ->
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .aspectRatio(3.8f)
                        .clip(RoundedCornerShape(99.dp))
                        .background(color),
                )
            }
            Text(
                text = "联系印样 · 你的照片会在这里显影",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

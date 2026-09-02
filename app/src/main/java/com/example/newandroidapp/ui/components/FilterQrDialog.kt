package com.example.newandroidapp.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.editing.FilterPreset
import com.example.newandroidapp.editing.generateFilterQrBitmap
import com.example.newandroidapp.editing.shareFilterQr
import com.example.newandroidapp.editing.writeFilterQrToUri
import kotlinx.coroutines.launch

@Composable
fun FilterQrDialog(
    preset: FilterPreset,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap: Bitmap = remember(preset) { generateFilterQrBitmap(preset, 768) }
    var status by remember { mutableStateOf<String?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                status = runCatching {
                    writeFilterQrToUri(context, preset, uri)
                    "二维码已保存"
                }.getOrElse { "保存失败：${it.message ?: "请重试"}" }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(preset.name) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "${preset.name}的片屿滤镜二维码",
                    modifier = Modifier.size(248.dp),
                )
                Text(
                    text = "这是片屿自有滤镜码，包含完整调色参数，不包含照片内容。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                status?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            saveLauncher.launch("${preset.name}-片屿滤镜.png")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("保存")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching { shareFilterQr(context, preset) }
                                    .onFailure { status = "分享失败：${it.message ?: "请重试"}" }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("分享")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}

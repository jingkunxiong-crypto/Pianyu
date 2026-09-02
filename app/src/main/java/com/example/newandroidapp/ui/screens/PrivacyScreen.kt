package com.example.newandroidapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.ui.components.PianyuGlyph
import com.example.newandroidapp.ui.components.PianyuGlyphType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("隐私与数据") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "返回照片库" },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "你的照片，仍然属于你。",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            item {
                PrivacyCard(
                    title = "照片权限",
                    body = "片屿只读取你授权的系统照片，用来展示、整理和编辑。Android 14 及以上可以只授权部分照片，也可以随时在系统设置中更改。",
                )
            }
            item {
                PrivacyCard(
                    title = "本地处理",
                    body = "光册、收藏、标签和片屿滤镜保存在本机。你可以另存副本，也可以在明确确认后覆盖原照片；覆盖前的原始文件会备份在片屿的应用私有空间，供“复原原图”使用。",
                )
            }
            item {
                PrivacyCard(
                    title = "二维码扫描",
                    body = "扫码界面由 Google Play 服务提供，片屿不申请相机权限。识别结果只用于判断片屿滤镜码或 Snapseed 官方链接。",
                )
            }
            item {
                PrivacyCard(
                    title = "Snapseed",
                    body = "只有你主动选择时，片屿才会把照片或官方 QR Look 链接交给 Snapseed。Snapseed 的后续处理受其自身隐私政策约束。",
                )
            }
            item {
                PrivacyCard(
                    title = "我们没有的东西",
                    body = "当前版本没有账号、广告、统计分析、云同步或片屿自建服务器，也不会出售个人数据。卸载应用会删除光册、标签、滤镜和原图备份；已覆盖的系统照片不会自动复原，请在卸载前先使用“复原原图”。",
                )
            }
            item {
                Text(
                    "隐私说明版本：2026-08-31",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PrivacyCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

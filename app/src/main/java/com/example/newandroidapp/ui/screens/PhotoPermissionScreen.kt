package com.example.newandroidapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newandroidapp.ui.components.ContactSheet

@Composable
fun PhotoPermissionScreen(
    previouslyRequested: Boolean,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "片屿",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = if (previouslyRequested) {
                    "需要照片访问权限"
                } else {
                    "让照片在片屿里显影"
                },
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = if (previouslyRequested) {
                    "片屿尚未获得照片权限，所以暂时无法生成时间线和光册。"
                } else {
                    "片屿需要照片访问权限，才能在时间线与光册中整理你的回忆。"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        ContactSheet()

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PermissionPromise(
                    index = "01",
                    title = "由你决定允许哪些照片",
                    message = "Android 14 及以上可选择全部照片，也可仅允许部分照片。",
                )
                PermissionPromise(
                    index = "02",
                    title = "整理与编辑留在本机",
                    message = "基础照片管理与处理不需要上传云端。",
                )
                PermissionPromise(
                    index = "03",
                    title = "修改原图前一定询问你",
                    message = "默认可另存副本；只有你确认时才会备份并覆盖原照片。",
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onRequestAccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (previouslyRequested) "再次申请" else "继续设置照片权限")
            }
            if (previouslyRequested) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("打开系统设置")
                }
            }
            Text(
                text = "想整理手机中的全部图片，请在系统弹窗中选择“允许全部照片”。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun PermissionPromise(
    index: String,
    title: String,
    message: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = index,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 9.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

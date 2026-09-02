package com.example.newandroidapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.newandroidapp.editing.BuiltInFilters
import com.example.newandroidapp.ui.screens.FilterHubScreen
import com.example.newandroidapp.ui.screens.PhotoPermissionScreen
import com.example.newandroidapp.ui.theme.PianyuTheme
import org.junit.Rule
import org.junit.Test

class PianyuComposeSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permissionScreenExplainsFullLibraryAccess() {
        composeRule.setContent {
            PianyuTheme {
                PhotoPermissionScreen(
                    previouslyRequested = false,
                    onRequestAccess = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("让照片在片屿里显影").assertIsDisplayed()
    }

    @Test
    fun filterHubShowsScannerAndBuiltInFilters() {
        composeRule.setContent {
            PianyuTheme {
                FilterHubScreen(
                    presets = BuiltInFilters.presets,
                    onBack = {},
                    onScanCode = { _, _ -> },
                    onSaveImportedPreset = { it },
                    onOpenSnapseedQr = { false },
                    onInstallSnapseed = {},
                )
            }
        }

        composeRule.onNodeWithText("扫描滤镜二维码").assertIsDisplayed()
        composeRule.onNodeWithText("片屿内置滤镜").assertIsDisplayed()
        composeRule.onNodeWithText("原始").assertIsDisplayed()
        composeRule.onNodeWithText("旧日森光").assertIsDisplayed()
    }
}

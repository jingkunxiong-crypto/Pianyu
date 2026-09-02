package com.example.newandroidapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val PianyuShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkContactBlue,
    onPrimary = Darkroom,
    primaryContainer = Color(0xFF5A301F),
    onPrimaryContainer = MoonPaper,
    secondary = DarkSafeLight,
    onSecondary = Darkroom,
    secondaryContainer = Color(0xFF464131),
    onSecondaryContainer = MoonPaper,
    background = Darkroom,
    onBackground = MoonPaper,
    surface = DarkroomSurface,
    onSurface = MoonPaper,
    surfaceVariant = DarkroomRaised,
    onSurfaceVariant = Color(0xFFD5C8BC),
    outline = Color(0xFFA89A8E),
    outlineVariant = Color(0xFF493F37),
)

private val LightColorScheme = lightColorScheme(
    primary = ContactBlue,
    onPrimary = PhotoPaper,
    primaryContainer = Color(0xFFF0CFBC),
    onPrimaryContainer = DeveloperInk,
    secondary = SafeLight,
    onSecondary = PhotoPaper,
    secondaryContainer = Color(0xFFE6E0CB),
    onSecondaryContainer = DeveloperInk,
    background = PhotoPaper,
    onBackground = DeveloperInk,
    surface = PressedPaper,
    onSurface = DeveloperInk,
    surfaceVariant = PaperMist,
    onSurfaceVariant = Color(0xFF6F655C),
    outline = Color(0xFF8A7C70),
    outlineVariant = SilverGrain,
)

@Composable
fun PianyuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = PianyuTypography,
        shapes = PianyuShapes,
        content = content,
    )
}

package com.classity.fasttool.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FastToolDarkScheme = darkColorScheme(
    primary = Color(0xFFE4E4E4),
    onPrimary = Color(0xFF161616),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF161616),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF767676)
)

@Composable
fun FastToolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FastToolDarkScheme,
        content = content
    )
}

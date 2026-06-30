package com.copyplay.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CopyplayColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF33D6A6),
    onPrimary = Color(0xFF062019),
    secondary = Color(0xFFFFC857),
    onSecondary = Color(0xFF251A00),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFF7F9FB),
    surface = Color(0xFF121923),
    onSurface = Color(0xFFF7F9FB),
    onSurfaceVariant = Color(0xFFB7C1CC),
    error = Color(0xFFFF7A7A),
)

@Composable
fun CopyplayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CopyplayColors,
        content = content,
    )
}

package com.copyplay.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val CopyplayColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF4FE0B4),
    onPrimary = Color(0xFF052019),
    secondary = Color(0xFFFFC95A),
    onSecondary = Color(0xFF261A00),
    tertiary = Color(0xFF8DB6FF),
    onTertiary = Color(0xFF071833),
    background = Color(0xFF070A0E),
    onBackground = Color(0xFFF6F8FB),
    surface = Color(0xFF10161D),
    onSurface = Color(0xFFF6F8FB),
    surfaceVariant = Color(0xFF19222C),
    onSurfaceVariant = Color(0xFFC1CAD4),
    outline = Color(0xFF3A4652),
    outlineVariant = Color(0xFF242E39),
    error = Color(0xFFFF8B8B),
)

private val BaseTypography = Typography()

private val CopyplayTypography = BaseTypography.copy(
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

private val CopyplayShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun CopyplayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CopyplayColors,
        typography = CopyplayTypography,
        shapes = CopyplayShapes,
        content = content,
    )
}

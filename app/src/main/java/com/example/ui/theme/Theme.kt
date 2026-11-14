package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DataPilotDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = CyberCyan,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = NeuralViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3B1F7A),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = SlateElevated,
    outline = GlassBorder,
    outlineVariant = SubtleDivider,
    error = CoralError,
    onError = Color.White,
)

private val DataPilotShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DataPilotTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DataPilotDarkColorScheme,
        typography = DataPilotTypography,
        shapes = DataPilotShapes,
        content = content,
    )
}

package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val UwesToDarkColorScheme = darkColorScheme(
    primary = BrainHealthyEmerald,
    onPrimary = Color(0xFF052417),
    primaryContainer = Color(0xFF103828),
    onPrimaryContainer = BrainHealthyEmerald,
    secondary = BrainTiredAmber,
    onSecondary = Color(0xFF281900),
    secondaryContainer = Color(0xFF3A2A10),
    onSecondaryContainer = BrainTiredAmber,
    tertiary = BrainCrackedCoral,
    onTertiary = Color(0xFF2D0A04),
    tertiaryContainer = Color(0xFF3E1912),
    onTertiaryContainer = BrainCrackedCoral,
    background = ObsidianBackground,
    onBackground = CrispOffWhite,
    surface = CharcoalSurface,
    onSurface = CrispOffWhite,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = CoolSlateText,
    outline = SurfaceStroke,
    outlineVariant = Color(0xFF222938),
    error = BrainLostCrimson,
    onError = Color.White,
    errorContainer = Color(0xFF3E1218),
    onErrorContainer = Color(0xFFFFB3BA)
)

val UwesToShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun UwesToTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UwesToDarkColorScheme,
        typography = UwesToTypography,
        shapes = UwesToShapes,
        content = content
    )
}

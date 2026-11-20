package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkGrowthTwinColors.lime,
    onPrimary = DarkGrowthTwinColors.onLime,
    primaryContainer = DarkGrowthTwinColors.limeSoft,
    onPrimaryContainer = DarkGrowthTwinColors.limeBright,
    secondary = DarkGrowthTwinColors.mint,
    onSecondary = DarkGrowthTwinColors.bg,
    secondaryContainer = DarkGrowthTwinColors.mintSoft,
    onSecondaryContainer = DarkGrowthTwinColors.mint,
    tertiary = DarkGrowthTwinColors.warn,
    onTertiary = DarkGrowthTwinColors.bg,
    background = DarkGrowthTwinColors.bg,
    onBackground = DarkGrowthTwinColors.ink,
    surface = DarkGrowthTwinColors.surface,
    onSurface = DarkGrowthTwinColors.ink,
    surfaceVariant = DarkGrowthTwinColors.surfaceCard,
    onSurfaceVariant = DarkGrowthTwinColors.mute,
    outline = DarkGrowthTwinColors.line,
    outlineVariant = DarkGrowthTwinColors.lineBright,
    error = DarkGrowthTwinColors.bad,
    onError = DarkGrowthTwinColors.bg
)

private val LightColorScheme = lightColorScheme(
    primary = LightGrowthTwinColors.lime,
    onPrimary = LightGrowthTwinColors.onLime,
    primaryContainer = LightGrowthTwinColors.limeSoft,
    onPrimaryContainer = LightGrowthTwinColors.limeBright,
    secondary = LightGrowthTwinColors.mint,
    onSecondary = LightGrowthTwinColors.surface,
    secondaryContainer = LightGrowthTwinColors.mintSoft,
    onSecondaryContainer = LightGrowthTwinColors.mint,
    tertiary = LightGrowthTwinColors.warn,
    onTertiary = LightGrowthTwinColors.surface,
    background = LightGrowthTwinColors.bg,
    onBackground = LightGrowthTwinColors.ink,
    surface = LightGrowthTwinColors.surface,
    onSurface = LightGrowthTwinColors.ink,
    surfaceVariant = LightGrowthTwinColors.surfaceElevated,
    onSurfaceVariant = LightGrowthTwinColors.mute,
    outline = LightGrowthTwinColors.line,
    outlineVariant = LightGrowthTwinColors.lineBright,
    error = LightGrowthTwinColors.bad,
    onError = LightGrowthTwinColors.surface
)

val GrowthTwinShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object GTTheme {
    val colors: GrowthTwinColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGrowthTwinColors.current
}

@Composable
fun GrowthTwinTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val gtColors = if (darkTheme) DarkGrowthTwinColors else LightGrowthTwinColors
    val m3Colors = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalGrowthTwinColors provides gtColors) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = GrowthTwinTypography,
            shapes = GrowthTwinShapes,
            content = content
        )
    }
}

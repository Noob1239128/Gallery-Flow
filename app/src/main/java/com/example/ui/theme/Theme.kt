package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val LuminaDarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = OnAmberDark,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = AmberGoldSoft,
    secondary = WarmTeal,
    onSecondary = ObsidianBg,
    secondaryContainer = WarmTealContainerDark,
    onSecondaryContainer = WarmTeal,
    tertiary = CoralRose,
    onTertiary = ObsidianBg,
    tertiaryContainer = CoralRoseContainerDark,
    onTertiaryContainer = CoralRose,
    background = ObsidianBg,
    onBackground = CreamText,
    surface = ObsidianSurface,
    onSurface = CreamText,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = MutedSlateText,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline.copy(alpha = 0.6f)
)

private val LuminaLightColorScheme = lightColorScheme(
    primary = AmberLight,
    onPrimary = OnAmberLight,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = AmberLight,
    secondary = TealLight,
    onSecondary = OnAmberLight,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = TealLight,
    tertiary = RoseLight,
    onTertiary = OnAmberLight,
    tertiaryContainer = RoseContainerLight,
    onTertiaryContainer = RoseLight,
    background = AlabasterBg,
    onBackground = InkText,
    surface = AlabasterSurface,
    onSurface = InkText,
    surfaceVariant = AlabasterSurfaceHigh,
    onSurfaceVariant = WarmGrayText,
    surfaceContainerLowest = AlabasterSurface,
    surfaceContainerLow = AlabasterBg,
    surfaceContainer = AlabasterSurfaceHigh,
    surfaceContainerHigh = AlabasterSurfaceHighest,
    surfaceContainerHighest = AlabasterSurfaceHighest,
    outline = AlabasterOutline,
    outlineVariant = AlabasterOutline.copy(alpha = 0.6f)
)

val LuminaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LuminaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) LuminaDarkColorScheme else LuminaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LuminaTypography,
        shapes = LuminaShapes,
        content = content
    )
}

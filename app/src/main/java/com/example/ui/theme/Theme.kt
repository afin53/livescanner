package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val ScannerDarkColorScheme =
  darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = CyanDeep,
    onPrimaryContainer = TextPrimary,
    secondary = SkyBlue,
    onSecondary = ObsidianBg,
    tertiary = EmeraldMint,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = CoralAlert,
    onError = ObsidianBg,
  )

private val ScannerShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
  )

@Composable
fun LensScannerTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = ScannerDarkColorScheme,
    typography = AppTypography,
    shapes = ScannerShapes,
    content = content,
  )
}

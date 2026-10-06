package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SwissLightColorScheme = lightColorScheme(
  primary = SwissCrimson,
  onPrimary = Color.White,
  primaryContainer = SwissCrimsonLight,
  onPrimaryContainer = SwissCrimsonDark,
  secondary = SwissDark,
  onSecondary = Color.White,
  secondaryContainer = SwissSurfaceVariant,
  onSecondaryContainer = SwissDark,
  tertiary = SwissTextSecondary,
  onTertiary = Color.White,
  background = SwissBackground,
  onBackground = SwissDark,
  surface = SwissSurface,
  onSurface = SwissDark,
  surfaceVariant = SwissSurfaceVariant,
  onSurfaceVariant = SwissTextSecondary,
  outline = SwissBorder,
  outlineVariant = SwissBorderLight
)

private val SwissDarkColorScheme = darkColorScheme(
  primary = SwissDarkCrimson,
  onPrimary = Color.Black,
  primaryContainer = SwissDarkCrimsonLight,
  onPrimaryContainer = SwissDarkCrimson,
  secondary = SwissDarkText,
  onSecondary = Color.Black,
  secondaryContainer = SwissDarkSurfaceVariant,
  onSecondaryContainer = SwissDarkText,
  tertiary = SwissDarkTextSecondary,
  onTertiary = Color.Black,
  background = SwissDarkBackground,
  onBackground = SwissDarkText,
  surface = SwissDarkSurface,
  onSurface = SwissDarkText,
  surfaceVariant = SwissDarkSurfaceVariant,
  onSurfaceVariant = SwissDarkTextSecondary,
  outline = SwissDarkBorder,
  outlineVariant = SwissDarkBorder
)

@Composable
fun SanchayTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) SwissDarkColorScheme else SwissLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

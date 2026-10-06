package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassCanvas
import com.example.ui.theme.GlassDarkBorderBottom
import com.example.ui.theme.GlassDarkBorderTop
import com.example.ui.theme.GlassDarkSurface
import com.example.ui.theme.GlassIceBlueGlow
import com.example.ui.theme.GlassRoseGlow
import com.example.ui.theme.GlassSurfaceMilky

/**
 * Ambient chromatic mesh gradient background:
 * - Off-white canvas (#F8FAFC)
 * - Soft crimson/rose glow (#FFE4E6) at top right
 * - Cool ice blue glow (#E0F2FE) at bottom left
 */
fun Modifier.ambientMeshBackground(isDark: Boolean = false): Modifier = this.drawBehind {
  if (isDark) {
    drawRect(Color(0xFF0F172A))
    // Soft dark mesh accents
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0x25EF4444), Color.Transparent),
        center = Offset(size.width * 0.95f, size.height * 0.05f),
        radius = size.width * 0.8f
      )
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0x2038BDF8), Color.Transparent),
        center = Offset(size.width * 0.05f, size.height * 0.95f),
        radius = size.width * 0.8f
      )
    )
  } else {
    // 1. Off-white canvas background
    drawRect(GlassCanvas)

    // 2. Diffused soft crimson/rose glow (#FFE4E6) at top right
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          GlassRoseGlow.copy(alpha = 0.85f),
          GlassRoseGlow.copy(alpha = 0.35f),
          Color.Transparent
        ),
        center = Offset(size.width * 0.90f, size.height * 0.06f),
        radius = size.width * 0.75f
      )
    )

    // 3. Diffused cool ice blue glow (#E0F2FE) at bottom left
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          GlassIceBlueGlow.copy(alpha = 0.90f),
          GlassIceBlueGlow.copy(alpha = 0.40f),
          Color.Transparent
        ),
        center = Offset(size.width * 0.10f, size.height * 0.85f),
        radius = size.width * 0.85f
      )
    )
  }
}

/**
 * Frosted Glass Card Modifier:
 * - Semi-transparent milky white surface (rgba(255, 255, 255, 0.72))
 * - 1px inner hairline stroke with gradient highlights (rgba(255, 255, 255, 0.8) fading to rgba(226, 232, 240, 0.4))
 * - Continuous squircle curves (24.dp for hero, 16.dp for secondary)
 * - Soft subtle drop shadow
 */
fun Modifier.frostedGlass(
  shape: Shape = RoundedCornerShape(24.dp),
  elevation: Dp = 4.dp,
  isDark: Boolean = false
): Modifier = this
  .shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = Color(0x11111827),
    spotColor = Color(0x0A111827)
  )
  .clip(shape)
  .background(if (isDark) GlassDarkSurface else GlassSurfaceMilky)
  .border(
    width = 1.dp,
    brush = if (isDark) {
      Brush.verticalGradient(listOf(GlassDarkBorderTop, GlassDarkBorderBottom))
    } else {
      Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom))
    },
    shape = shape
  )

/**
 * Reusable Frosted Glass Card Composable
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(24.dp),
  elevation: Dp = 4.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val baseModifier = modifier.frostedGlass(shape = shape, elevation = elevation, isDark = isDark)
  val finalModifier = if (onClick != null) {
    baseModifier.clickable { onClick() }
  } else {
    baseModifier
  }

  Box(modifier = finalModifier, content = content)
}

/**
 * Reusable Frosted Glass Chip Composable (16.dp squircle curves)
 */
@Composable
fun GlassChip(
  modifier: Modifier = Modifier,
  isSelected: Boolean = false,
  onClick: () -> Unit = {},
  shape: Shape = RoundedCornerShape(16.dp),
  content: @Composable BoxScope.() -> Unit
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val backgroundBrush = if (isSelected) {
    Brush.verticalGradient(
      listOf(
        Color(0xFF1E293B),
        Color(0xFF0F172A)
      )
    )
  } else {
    Brush.verticalGradient(
      listOf(
        if (isDark) Color(0x40334155) else Color(0xD8FFFFFF),
        if (isDark) Color(0x201E293B) else Color(0x99F8FAFC)
      )
    )
  }

  val borderBrush = if (isSelected) {
    Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF1E293B)))
  } else {
    if (isDark) {
      Brush.verticalGradient(listOf(GlassDarkBorderTop, GlassDarkBorderBottom))
    } else {
      Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom))
    }
  }

  Box(
    modifier = modifier
      .clip(shape)
      .background(brush = backgroundBrush)
      .border(width = 1.dp, brush = borderBrush, shape = shape)
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 8.dp),
    content = content
  )
}

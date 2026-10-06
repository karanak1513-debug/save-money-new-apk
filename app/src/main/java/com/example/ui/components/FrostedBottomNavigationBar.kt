package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SlateHeader
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissDark
import com.example.ui.viewmodel.NavigationTab

/**
 * High-End Frosted Glass Bottom Navigation Bar:
 * - Floating dock capsule architecture (26.dp continuous squircle curves)
 * - Semi-transparent milky acrylic glass with subtle chromatic mesh refraction
 * - Directional 1.25px hairline glass border (bright specular reflection at top, soft shadow at bottom)
 * - Animated active capsule indicator with micro-interactions
 * - Safe area handling respecting system navigation bar insets
 */
@Composable
fun FrostedBottomNavigationBar(
  currentTab: NavigationTab,
  onTabSelected: (NavigationTab) -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f

  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    // Floating Frosted Glass Dock Capsule
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = 16.dp,
          shape = RoundedCornerShape(26.dp),
          ambientColor = Color(0x180F172A),
          spotColor = Color(0x140F172A)
        )
        .clip(RoundedCornerShape(26.dp))
        .background(
          brush = Brush.verticalGradient(
            colors = if (isDark) {
              listOf(
                Color(0xEE1E293B),
                Color(0xD80F172A)
              )
            } else {
              listOf(
                Color(0xF0FFFFFF), // top specular light reflection
                Color(0xE0F8FAFC), // subtle ambient milky body
                Color(0xD0F1F5F9)  // cool ice blue floor refraction
              )
            }
          )
        )
        // Subtle ambient chromatic mesh shimmer on top rim
        .drawBehind {
          if (!isDark) {
            // Soft top crimson/rose refraction glow
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(Color(0x35FFE4E6), Color.Transparent),
                center = Offset(size.width * 0.25f, 0f),
                radius = size.width * 0.45f
              )
            )
            // Soft bottom ice blue refraction glow
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(Color(0x30E0F2FE), Color.Transparent),
                center = Offset(size.width * 0.75f, size.height),
                radius = size.width * 0.45f
              )
            )
          }
        }
        .border(
          width = 1.25.dp,
          brush = Brush.verticalGradient(
            colors = if (isDark) {
              listOf(
                Color(0x35FFFFFF),
                Color(0x10FFFFFF)
              )
            } else {
              listOf(
                Color(0xF5FFFFFF), // crisp specular bevel highlight on top
                Color(0x60CBD5E1)  // subtle gradient fade on bottom
              )
            }
          ),
          shape = RoundedCornerShape(26.dp)
        )
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .testTag("frosted_bottom_navigation_bar")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Tab 1: Dashboard
        FrostedNavItem(
          title = "Dashboard",
          isSelected = currentTab == NavigationTab.DASHBOARD,
          icon = Icons.Default.Home,
          onClick = { onTabSelected(NavigationTab.DASHBOARD) },
          testTag = "nav_tab_dashboard",
          modifier = Modifier.weight(1f)
        )

        // Tab 2: Goals
        FrostedNavItem(
          title = "Goals",
          isSelected = currentTab == NavigationTab.GOALS,
          icon = Icons.Default.Flag,
          onClick = { onTabSelected(NavigationTab.GOALS) },
          testTag = "nav_tab_goals",
          modifier = Modifier.weight(1f)
        )

        // Tab 3: Ledger
        FrostedNavItem(
          title = "Ledger",
          isSelected = currentTab == NavigationTab.LEDGER,
          icon = Icons.AutoMirrored.Filled.ListAlt,
          onClick = { onTabSelected(NavigationTab.LEDGER) },
          testTag = "nav_tab_ledger",
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

/**
 * Individual Frosted Navigation Item with dynamic animated active state
 */
@Composable
private fun FrostedNavItem(
  title: String,
  isSelected: Boolean,
  icon: ImageVector,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f

  val iconScale by animateFloatAsState(
    targetValue = if (isSelected) 1.08f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "nav_icon_scale"
  )

  val iconColor by animateColorAsState(
    targetValue = when {
      isSelected && isDark -> Color.White
      isSelected -> Color.White
      isDark -> Color(0xFF94A3B8)
      else -> SlateHeader
    },
    animationSpec = tween(durationMillis = 200),
    label = "nav_icon_color"
  )

  val textColor by animateColorAsState(
    targetValue = when {
      isSelected && isDark -> Color.White
      isSelected -> Color.White
      isDark -> Color(0xFF94A3B8)
      else -> SlateHeader
    },
    animationSpec = tween(durationMillis = 200),
    label = "nav_text_color"
  )

  Box(
    modifier = modifier
      .height(52.dp)
      .clip(RoundedCornerShape(20.dp))
      .clickable(
        role = Role.Tab,
        onClick = onClick
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    // Animated Active Capsule Pill
    if (isSelected) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .clip(RoundedCornerShape(20.dp))
          .background(
            brush = Brush.verticalGradient(
              colors = if (isDark) {
                listOf(
                  Color(0xFF334155),
                  Color(0xFF1E293B)
                )
              } else {
                listOf(
                  Color(0xFF1E293B),
                  SwissDark
                )
              }
            )
          )
          .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
              colors = listOf(
                Color(0x40FFFFFF),
                Color(0x10FFFFFF)
              )
            ),
            shape = RoundedCornerShape(20.dp)
          )
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = iconColor,
        modifier = Modifier
          .size(20.dp)
          .scale(iconScale)
      )

      Spacer(modifier = Modifier.width(6.dp))

      Text(
        text = title,
        fontFamily = PlusJakartaSans,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        color = textColor,
        fontSize = 12.sp,
        letterSpacing = (-0.1).sp
      )

      // Micro crimson accent dot on active tab
      if (isSelected) {
        Spacer(modifier = Modifier.width(5.dp))
        Box(
          modifier = Modifier
            .size(4.dp)
            .background(SwissCrimson, CircleShape)
        )
      }
    }
  }
}

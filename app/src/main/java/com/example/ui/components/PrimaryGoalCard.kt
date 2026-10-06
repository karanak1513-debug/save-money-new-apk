package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.Goal
import com.example.data.model.PacingInfo
import com.example.ui.theme.MonospaceBody
import com.example.ui.theme.MonospaceDisplay
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonBorder
import com.example.ui.theme.SwissCrimsonDark
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary

@Composable
fun PrimaryGoalCard(
  goal: Goal?,
  pacingInfo: PacingInfo,
  onSwitchGoalClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val title = goal?.title ?: "Reserve Target 2026"
  val savedAmount = goal?.savedAmount ?: 60000.0
  val targetAmount = goal?.targetAmount ?: 150000.0
  val progress = goal?.progressFraction ?: (60000f / 150000f)

  val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 800),
    label = "progress_bar_anim"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
      // Frosted white border, flat surface (border with subtle translucent white/gray)
      .border(1.5.dp, SwissBorder, RoundedCornerShape(12.dp))
      .padding(20.dp)
      .testTag("primary_goal_card")
  ) {
    // Top Row: Goal Title & Switcher
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(SwissCrimson, CircleShape)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          letterSpacing = (-0.2).sp
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clickable { onSwitchGoalClick() }
          .padding(4.dp)
          .testTag("switch_goal_button")
      ) {
        Text(
          text = "${pacingInfo.daysRemaining}d left",
          style = MaterialTheme.typography.labelSmall,
          color = SwissTextSecondary,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.Default.SwapHoriz,
          contentDescription = "Switch Goal",
          tint = SwissTextSecondary,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Total Saved vs Target: "₹60,000 / ₹1,50,000" in bold monospaced typography
    Column {
      Text(
        text = "TOTAL SAVED / TARGET",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "${CurrencyFormatter.formatRupee(savedAmount)} / ${CurrencyFormatter.formatRupee(targetAmount)}",
          style = MonospaceDisplay,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 22.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4px flat progress line (Crimson Red fill on Light Gray track)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
        .background(Color(0xFFE5E7EB), RoundedCornerShape(2.dp))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(animatedProgress)
          .height(4.dp)
          .background(SwissCrimson, RoundedCornerShape(2.dp))
          .testTag("goal_progress_indicator")
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // "Suggested Pace" Hero Badge (Highlighted in subtle red-tint container)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(SwissCrimsonLight, RoundedCornerShape(8.dp))
        .border(1.dp, SwissCrimsonBorder, RoundedCornerShape(8.dp))
        .padding(horizontal = 14.dp, vertical = 12.dp)
        .testTag("suggested_pace_badge")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .background(SwissCrimson, RoundedCornerShape(6.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = "Pace indicator",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }

        Column {
          Text(
            text = "SUGGESTED PACE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissCrimsonDark,
            letterSpacing = 1.sp,
            fontSize = 9.sp
          )
          Text(
            text = "Save ${CurrencyFormatter.formatRupee(pacingInfo.dailyPace)} Today to hit your goal on schedule",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = SwissDark
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Sub-metrics row: "₹3,150 / week" and "₹13,500 / month"
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
        .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "WEEKLY:",
          style = MaterialTheme.typography.labelSmall,
          color = SwissTextTertiary,
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp
        )
        Text(
          text = "${CurrencyFormatter.formatRupee(pacingInfo.weeklyPace)} / week",
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // Hairline divider
      Box(
        modifier = Modifier
          .width(1.dp)
          .height(14.dp)
          .background(Color(0xFFE5E7EB))
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "MONTHLY:",
          style = MaterialTheme.typography.labelSmall,
          color = SwissTextTertiary,
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp
        )
        Text(
          text = "${CurrencyFormatter.formatRupee(pacingInfo.monthlyPace)} / month",
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

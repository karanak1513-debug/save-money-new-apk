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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.Goal
import com.example.data.model.PacingInfo
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
  val title = goal?.title ?: "No Goal Created"
  val savedAmount = goal?.savedAmount ?: 0.0
  val targetAmount = goal?.targetAmount ?: 0.0
  val progress = goal?.progressFraction ?: 0f

  val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 600),
    label = "progress_bar_anim"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
      .border(1.5.dp, SwissBorder, RoundedCornerShape(12.dp))
      .padding(18.dp)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(if (pacingInfo.isExpired) SwissDark else SwissCrimson, CircleShape)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          letterSpacing = (-0.2).sp,
          maxLines = 1
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
          text = when {
            goal == null -> "Set Goal"
            pacingInfo.isExpired -> "Expired"
            pacingInfo.isCompleted -> "Achieved"
            else -> "${pacingInfo.daysRemaining}d left"
          },
          style = MaterialTheme.typography.labelSmall,
          color = if (pacingInfo.isExpired) SwissCrimson else SwissTextSecondary,
          fontWeight = FontWeight.Bold
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

    Spacer(modifier = Modifier.height(14.dp))

    // Total Saved vs Target: Monospaced typography
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
      Text(
        text = "${CurrencyFormatter.formatRupee(savedAmount)} / ${CurrencyFormatter.formatRupee(targetAmount)}",
        style = MonospaceDisplay,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 20.sp
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

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
          .background(if (pacingInfo.isCompleted) Color(0xFF10B981) else SwissCrimson, RoundedCornerShape(2.dp))
          .testTag("goal_progress_indicator")
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Suggested Pace Hero Badge
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(SwissCrimsonLight, RoundedCornerShape(8.dp))
        .border(1.dp, SwissCrimsonBorder, RoundedCornerShape(8.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp)
        .testTag("suggested_pace_badge")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .background(if (pacingInfo.isExpired) SwissDark else SwissCrimson, RoundedCornerShape(6.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when {
              pacingInfo.isCompleted -> Icons.Default.CheckCircle
              pacingInfo.isExpired -> Icons.Default.HourglassBottom
              else -> Icons.AutoMirrored.Filled.TrendingUp
            },
            contentDescription = "Pace indicator",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }

        Column {
          Text(
            text = "DYNAMIC PACING",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissCrimsonDark,
            letterSpacing = 1.sp,
            fontSize = 9.sp
          )
          Text(
            text = when {
              goal == null -> "Add a savings target to compute daily pace"
              pacingInfo.isCompleted -> "Target reached! Excellent savings discipline"
              pacingInfo.isExpired -> "Target deadline has elapsed. Update goal deadline."
              pacingInfo.dailyPace > 0 -> "Save ${CurrencyFormatter.formatRupee(pacingInfo.dailyPace)} Today to hit your goal on schedule"
              else -> "Goal on track with zero pending contributions needed"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = SwissDark
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // FIX FOR OVERLAPPING LAYOUT BUG:
    // Structured 2-column grid layout with clean padding and weight distribution so text never collides!
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
        .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Column 1: Weekly Pace
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = "WEEKLY PACE",
          style = MaterialTheme.typography.labelSmall,
          color = SwissTextTertiary,
          fontWeight = FontWeight.Bold,
          fontSize = 9.sp,
          letterSpacing = 0.5.sp
        )
        Text(
          text = when {
            pacingInfo.isExpired -> "Expired"
            pacingInfo.isCompleted -> "Done"
            else -> "${CurrencyFormatter.formatRupee(pacingInfo.weeklyPace)} / wk"
          },
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp
        )
      }

      // Hairline divider
      Box(
        modifier = Modifier
          .width(1.dp)
          .height(28.dp)
          .background(Color(0xFFE5E7EB))
      )

      Spacer(modifier = Modifier.width(14.dp))

      // Column 2: Monthly Pace
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = "MONTHLY PACE",
          style = MaterialTheme.typography.labelSmall,
          color = SwissTextTertiary,
          fontWeight = FontWeight.Bold,
          fontSize = 9.sp,
          letterSpacing = 0.5.sp
        )
        Text(
          text = when {
            pacingInfo.isExpired -> "Expired"
            pacingInfo.isCompleted -> "Done"
            else -> "${CurrencyFormatter.formatRupee(pacingInfo.monthlyPace)} / mo"
          },
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp
        )
      }
    }
  }
}

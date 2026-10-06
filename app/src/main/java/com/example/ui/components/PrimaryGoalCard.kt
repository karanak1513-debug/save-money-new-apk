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
import androidx.compose.material.icons.filled.ChevronRight
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
import com.example.ui.theme.MonospaceDisplay
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissAlpineGreen
import com.example.ui.theme.SwissAlpineGreenLight
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonDark
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissHairline
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import java.util.Locale

@Composable
fun PrimaryGoalCard(
  goal: Goal?,
  pacingInfo: PacingInfo,
  onSwitchGoalClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val title = goal?.title ?: "No Goal Configured"
  val savedAmount = goal?.savedAmount ?: 0.0
  val targetAmount = goal?.targetAmount ?: 0.0
  val progress = goal?.progressFraction ?: 0f

  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 600),
    label = "primary_goal_progress"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
      .border(1.dp, SwissBorder, RoundedCornerShape(10.dp))
      .padding(20.dp)
      .testTag("primary_goal_card")
  ) {
    // Top Row: Editorial Tag & Switch Goal CTA
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(if (pacingInfo.isExpired) SwissDark else SwissCrimson, CircleShape)
        )
        Text(
          text = "01 // ACTIVE ACCELERATION",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          letterSpacing = 1.2.sp
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .clickable { onSwitchGoalClick() }
          .padding(horizontal = 6.dp, vertical = 2.dp)
          .testTag("switch_goal_button")
      ) {
        Text(
          text = when {
            goal == null -> "SELECT GOAL"
            pacingInfo.isExpired -> "EXPIRED"
            pacingInfo.isCompleted -> "COMPLETED"
            else -> "${pacingInfo.daysRemaining}D REMAINING"
          },
          style = MonospaceMicro,
          color = if (pacingInfo.isExpired) SwissCrimson else SwissDark,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.Default.SwapHoriz,
          contentDescription = "Switch Goal",
          tint = SwissTextSecondary,
          modifier = Modifier.size(14.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Goal Title Heading
    Text(
      text = title,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      letterSpacing = (-0.3).sp,
      maxLines = 1
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Editorial Financial Scale: Saved Amount vs Target
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      Column {
        Text(
          text = "CURRENT ALLOCATED",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = CurrencyFormatter.formatRupee(savedAmount),
          style = MonospaceDisplay,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 24.sp
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "TARGET RATIO",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = String.format(Locale.US, "%.1f%%", progress * 100),
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = if (pacingInfo.isCompleted) SwissAlpineGreen else SwissCrimson
        )
        Text(
          text = "OF ${CurrencyFormatter.formatRupee(targetAmount)}",
          style = MonospaceMicro,
          color = SwissTextSecondary,
          fontSize = 10.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Precision Razor Progress Meter with Hairline Track
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(5.dp)
        .background(Color(0xFFEEEEEE), RoundedCornerShape(2.5.dp))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(animatedProgress)
          .height(5.dp)
          .background(
            if (pacingInfo.isCompleted) SwissAlpineGreen else SwissCrimson,
            RoundedCornerShape(2.5.dp)
          )
          .testTag("goal_progress_indicator")
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Pacing Grid: 3 Clean Architectural Columns with Hairline Dividers
    // Absolutely zero overlapping text! Each metric has ample dedicated space.
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFFF9F9F8), RoundedCornerShape(8.dp))
        .border(0.75.dp, SwissHairline, RoundedCornerShape(8.dp))
        .padding(vertical = 12.dp, horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Metric 1: Daily Pace
      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "DAILY",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = if (pacingInfo.isExpired) "—" else CurrencyFormatter.formatRupee(pacingInfo.dailyPace),
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.sp
        )
      }

      // Hairline Divider
      Box(
        modifier = Modifier
          .width(0.75.dp)
          .height(26.dp)
          .background(SwissHairline)
      )

      // Metric 2: Weekly Pace
      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "WEEKLY",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = if (pacingInfo.isExpired) "—" else CurrencyFormatter.formatRupee(pacingInfo.weeklyPace),
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.sp
        )
      }

      // Hairline Divider
      Box(
        modifier = Modifier
          .width(0.75.dp)
          .height(26.dp)
          .background(SwissHairline)
      )

      // Metric 3: Monthly Pace
      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "MONTHLY",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = if (pacingInfo.isExpired) "—" else CurrencyFormatter.formatRupee(pacingInfo.monthlyPace),
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Pacing Narrative Insight Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          if (pacingInfo.isExpired) SwissCrimsonLight else Color(0xFFF6F6F4),
          RoundedCornerShape(6.dp)
        )
        .padding(horizontal = 10.dp, vertical = 7.dp)
        .testTag("suggested_pace_badge"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(4.dp)
          .background(if (pacingInfo.isExpired) SwissCrimson else SwissDark, CircleShape)
      )
      Text(
        text = when {
          goal == null -> "Configure a target to compute real-time run rates."
          pacingInfo.isCompleted -> "Target achieved. Capital accumulation complete."
          pacingInfo.isExpired -> "Target deadline has elapsed. Update deadline in Goals."
          pacingInfo.dailyPace > 0 -> "Commit ${CurrencyFormatter.formatRupee(pacingInfo.dailyPace)} / day to satisfy target."
          else -> "Cadence satisfied. No immediate contribution required."
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (pacingInfo.isExpired) SwissCrimsonDark else SwissDark,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

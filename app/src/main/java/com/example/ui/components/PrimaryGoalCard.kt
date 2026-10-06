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
import com.example.data.model.GoalFeasibility
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

/**
 * Hero Goal Card:
 * - Monospaced balance (with privacy blur support)
 * - 3px razor crimson progress bar (#DC2626)
 * - Strictly padded 3-column pacing grid (DAILY, WEEKLY, MONTHLY) preventing text collision
 */
@Composable
fun PrimaryGoalCard(
  goal: Goal?,
  pacingInfo: PacingInfo,
  feasibility: GoalFeasibility? = null,
  isPrivacyMode: Boolean = false,
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
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
      .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
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

    // Dynamic Predictive Goal Feasibility (Run-Rate AI) Status Badge
    if (feasibility != null && goal != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            if (feasibility.isOnTrack) SwissAlpineGreenLight else SwissCrimsonLight,
            RoundedCornerShape(6.dp)
          )
          .border(
            0.75.dp,
            if (feasibility.isOnTrack) SwissAlpineGreen else SwissCrimson,
            RoundedCornerShape(6.dp)
          )
          .padding(horizontal = 10.dp, vertical = 7.dp)
          .testTag("feasibility_status_badge")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = feasibility.statusBadgeText,
            style = MonospaceMicro,
            color = if (feasibility.isOnTrack) SwissAlpineGreen else SwissCrimson,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
          Text(
            text = if (isPrivacyMode) "14D: ₹••••/D" else "14D: ${CurrencyFormatter.formatRupee(feasibility.trailing14dVelocity)}/D",
            style = MonospaceMicro,
            color = SwissDark,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Editorial Financial Scale: Saved Amount vs Target
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      Column {
        Text(
          text = "SAVED AMOUNT",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (isPrivacyMode) "₹••••••••" else CurrencyFormatter.formatRupee(savedAmount),
          style = MonospaceDisplay,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 26.sp,
          letterSpacing = (-0.5).sp
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "TARGET GOAL",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (isPrivacyMode) "₹••••••••" else CurrencyFormatter.formatRupee(targetAmount),
          style = MonospaceSmall,
          color = SwissTextSecondary,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 3px razor crimson progress bar (#DC2626)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(3.dp)
        .background(Color(0xFFF3F4F6), RoundedCornerShape(1.5.dp))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(animatedProgress)
          .height(3.dp)
          .background(
            if (pacingInfo.isCompleted) SwissAlpineGreen else SwissCrimson,
            RoundedCornerShape(1.5.dp)
          )
          .testTag("goal_progress_indicator")
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Pacing Grid: Strictly padded 3-column pacing grid preventing text collision
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
        .border(1.dp, SwissHairline, RoundedCornerShape(6.dp))
        .padding(vertical = 12.dp, horizontal = 12.dp),
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
          letterSpacing = 0.8.sp,
          fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = when {
            isPrivacyMode -> "₹•••"
            pacingInfo.isExpired -> "—"
            else -> CurrencyFormatter.formatRupee(pacingInfo.dailyPace)
          },
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.sp,
          maxLines = 1
        )
      }

      // Hairline Divider
      Box(
        modifier = Modifier
          .width(1.dp)
          .height(28.dp)
          .background(SwissBorder)
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
          letterSpacing = 0.8.sp,
          fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = when {
            isPrivacyMode -> "₹••••"
            pacingInfo.isExpired -> "—"
            else -> CurrencyFormatter.formatRupee(pacingInfo.weeklyPace)
          },
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.sp,
          maxLines = 1
        )
      }

      // Hairline Divider
      Box(
        modifier = Modifier
          .width(1.dp)
          .height(28.dp)
          .background(SwissBorder)
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
          letterSpacing = 0.8.sp,
          fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = when {
            isPrivacyMode -> "₹•••••"
            pacingInfo.isExpired -> "—"
            else -> CurrencyFormatter.formatRupee(pacingInfo.monthlyPace)
          },
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.sp,
          maxLines = 1
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Pacing Narrative Insight Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          if (pacingInfo.isExpired) SwissCrimsonLight else Color(0xFFF9FAFB),
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
          isPrivacyMode -> "Daily run-rate active to satisfy target."
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

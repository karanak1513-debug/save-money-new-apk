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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.DailyBarData
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissBorderLight
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import kotlin.math.max

@Composable
fun WeeklyBarChart(
  dailyData: List<DailyBarData>,
  modifier: Modifier = Modifier
) {
  var selectedIndex by remember { mutableStateOf<Int?>(null) }
  val maxAmount = max(1000.0, dailyData.maxOfOrNull { it.amount } ?: 1000.0)
  val weeklyTotal = dailyData.sumOf { it.amount }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
      .border(1.dp, SwissBorder, RoundedCornerShape(12.dp))
      .padding(16.dp)
      .testTag("weekly_bar_chart_container")
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Weekly Activity",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Daily manual additions",
          style = MaterialTheme.typography.bodySmall,
          color = SwissTextSecondary
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = CurrencyFormatter.formatRupee(weeklyTotal),
          style = MonospaceSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 14.sp
        )
        Text(
          text = "THIS WEEK",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = SwissTextTertiary,
          fontSize = 9.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Interactive tooltip/detail row
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(20.dp),
      contentAlignment = Alignment.Center
    ) {
      if (selectedIndex != null && selectedIndex!! in dailyData.indices) {
        val item = dailyData[selectedIndex!!]
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = item.dateStr,
            style = MaterialTheme.typography.labelSmall,
            color = SwissTextSecondary,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = SwissTextTertiary
          )
          Text(
            text = CurrencyFormatter.formatRupee(item.amount),
            style = MonospaceSmall,
            fontWeight = FontWeight.Bold,
            color = if (item.isToday) SwissCrimson else MaterialTheme.colorScheme.onSurface
          )
        }
      } else {
        Text(
          text = "Tap a bar to inspect day",
          style = MaterialTheme.typography.labelSmall,
          color = SwissTextTertiary
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Razor-Sharp Bars Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      dailyData.forEachIndexed { index, item ->
        val fraction = (item.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)
        val animFraction by animateFloatAsState(
          targetValue = fraction,
          animationSpec = tween(durationMillis = 600, delayMillis = index * 40),
          label = "bar_anim_$index"
        )
        val isSelected = selectedIndex == index

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Bottom,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable {
              selectedIndex = if (selectedIndex == index) null else index
            }
            .testTag("bar_day_$index")
        ) {
          // Bar container with track
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
          ) {
            // Full height light track
            Box(
              modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
                .background(Color(0xFFF3F4F6), RoundedCornerShape(2.dp))
            )

            // Active Razor-Sharp Bar
            Box(
              modifier = Modifier
                .width(8.dp)
                .fillMaxHeight(animFraction)
                .background(
                  color = when {
                    item.isToday -> SwissCrimson
                    isSelected -> SwissCrimson
                    item.amount > 0 -> SwissDark
                    else -> Color(0xFFE5E7EB)
                  },
                  shape = RoundedCornerShape(2.dp)
                )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Day Label
          Text(
            text = item.dayLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (item.isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (item.isToday) SwissCrimson else SwissTextSecondary
          )

          // Red dot indicator under Today
          if (item.isToday) {
            Box(
              modifier = Modifier
                .padding(top = 2.dp)
                .size(4.dp)
                .background(SwissCrimson, CircleShape)
            )
          } else {
            Spacer(modifier = Modifier.height(6.dp))
          }
        }
      }
    }

    // Baseline hairline
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(SwissBorderLight)
    )
  }
}

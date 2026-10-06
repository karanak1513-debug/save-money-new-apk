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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.DailyBarData
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SectionHeaderMedium
import com.example.ui.theme.SlateHeader
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissHairline
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
      .frostedGlass(shape = RoundedCornerShape(24.dp), elevation = 4.dp)
      .padding(20.dp)
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
          text = "WEEKLY CASHFLOW",
          style = SectionHeaderMedium,
          color = SlateHeader,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Activity & Savings",
          style = MaterialTheme.typography.titleMedium,
          fontFamily = PlusJakartaSans,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = CurrencyFormatter.formatRupee(weeklyTotal),
          style = MaterialTheme.typography.titleMedium,
          fontFamily = PlusJakartaSans,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 16.sp
        )
        Text(
          text = "7-DAY ROLLING",
          style = SectionHeaderMedium,
          fontSize = 10.sp,
          color = SlateHeader
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Interactive tooltip / Day inspector
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(18.dp),
      contentAlignment = Alignment.Center
    ) {
      if (selectedIndex != null && selectedIndex!! in dailyData.indices) {
        val item = dailyData[selectedIndex!!]
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = item.dateStr.uppercase(),
            style = MonospaceMicro,
            color = SwissTextSecondary
          )
          Text(text = "—", style = MonospaceMicro, color = SwissTextTertiary)
          Text(
            text = CurrencyFormatter.formatRupee(item.amount),
            style = MonospaceSmall,
            fontWeight = FontWeight.Bold,
            color = if (item.isToday) SwissCrimson else MaterialTheme.colorScheme.onSurface
          )
        }
      } else {
        Text(
          text = "INSPECT ANY DAY COLUMN",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          fontSize = 9.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Precision Vertical Column Bars
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(110.dp)
        .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      dailyData.forEachIndexed { index, item ->
        val fraction = (item.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)
        val animFraction by animateFloatAsState(
          targetValue = fraction,
          animationSpec = tween(durationMillis = 500, delayMillis = index * 30),
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
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
          ) {
            // Full height track
            Box(
              modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
                .background(Color(0x50E2E8F0), RoundedCornerShape(4.dp))
            )

            // Active Frosted Pillar
            Box(
              modifier = Modifier
                .width(8.dp)
                .fillMaxHeight(animFraction)
                .background(
                  brush = when {
                    item.isToday || isSelected -> Brush.verticalGradient(
                      listOf(Color(0xFFF87171), SwissCrimson)
                    )
                    item.amount > 0 -> Brush.verticalGradient(
                      listOf(Color(0xFF475569), SwissDark)
                    )
                    else -> Brush.verticalGradient(
                      listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))
                    )
                  },
                  shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Day Monospace Label
          Text(
            text = item.dayLabel.uppercase(),
            style = MonospaceMicro,
            fontWeight = if (item.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (item.isToday) SwissCrimson else SwissTextSecondary,
            fontSize = 9.sp
          )

          // Indicator under Today
          if (item.isToday) {
            Box(
              modifier = Modifier
                .padding(top = 2.dp)
                .size(3.dp)
                .background(SwissCrimson, CircleShape)
            )
          } else {
            Spacer(modifier = Modifier.height(5.dp))
          }
        }
      }
    }

    // Baseline Hairline
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(0.75.dp)
        .background(SwissHairline)
    )
  }
}

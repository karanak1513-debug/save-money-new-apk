package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.ChannelBreakdown
import com.example.data.model.CurrencyFormatter
import com.example.ui.theme.ChannelCash
import com.example.ui.theme.ChannelOther
import com.example.ui.theme.ChannelUpi
import com.example.ui.theme.MonospaceHeadline
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary

@Composable
fun DonutBreakdownChart(
  breakdown: List<ChannelBreakdown>,
  modifier: Modifier = Modifier,
  onChannelSelected: (Channel?) -> Unit = {}
) {
  var selectedChannel by remember { mutableStateOf<Channel?>(null) }
  val total = breakdown.sumOf { it.amount }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
      .border(1.dp, SwissBorder, RoundedCornerShape(10.dp))
      .padding(18.dp)
      .testTag("donut_chart_container")
  ) {
    // Editorial Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "02 // LIQUIDITY DISTRIBUTION",
        style = MonospaceMicro,
        color = SwissTextTertiary,
        letterSpacing = 1.2.sp
      )
      Text(
        text = "CHANNELS",
        style = MonospaceMicro,
        color = SwissDark,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Donut + Side Legend Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Donut Canvas
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(136.dp)
      ) {
        val animationProgress by animateFloatAsState(
          targetValue = 1f,
          animationSpec = tween(durationMillis = 800),
          label = "donut_anim"
        )

        Canvas(
          modifier = Modifier
            .size(130.dp)
            .testTag("donut_canvas")
        ) {
          val strokeWidth = 14.dp.toPx()
          val chartDiameter = size.minDimension - strokeWidth
          val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
          val chartSize = Size(chartDiameter, chartDiameter)

          // Background light ring
          drawArc(
            color = Color(0xFFEEEEEE),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = chartSize,
            style = Stroke(width = strokeWidth)
          )

          var currentAngle = -90f
          val gapDegrees = 3f

          breakdown.forEach { item ->
            val color = when (item.channel) {
              Channel.UPI -> ChannelUpi
              Channel.CASH -> ChannelCash
              Channel.OTHER -> ChannelOther
            }

            val rawSweep = (item.percentage * 360f) * animationProgress
            val sweep = (rawSweep - gapDegrees).coerceAtLeast(0f)

            if (sweep > 0f) {
              val isSelected = selectedChannel == null || selectedChannel == item.channel
              val arcAlpha = if (isSelected) 1f else 0.3f

              drawArc(
                color = color.copy(alpha = arcAlpha),
                startAngle = currentAngle + (gapDegrees / 2f),
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = chartSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
              )
            }
            currentAngle += rawSweep
          }
        }

        // Center Monospace Metric
        Column(
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          val activeAmount = if (selectedChannel != null) {
            breakdown.find { it.channel == selectedChannel }?.amount ?: total
          } else {
            total
          }
          val activeLabel = selectedChannel?.displayName ?: "PORTFOLIO"

          Text(
            text = activeLabel.uppercase(),
            style = MonospaceMicro,
            color = SwissTextTertiary,
            fontSize = 9.sp
          )
          Text(
            text = CurrencyFormatter.formatCompactRupee(activeAmount),
            style = MonospaceHeadline,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
          )
        }
      }

      // Legend List
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = 14.dp)
      ) {
        breakdown.forEach { item ->
          val color = when (item.channel) {
            Channel.UPI -> ChannelUpi
            Channel.CASH -> ChannelCash
            Channel.OTHER -> ChannelOther
          }
          val isSelected = selectedChannel == item.channel

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clickable {
                selectedChannel = if (selectedChannel == item.channel) null else item.channel
                onChannelSelected(selectedChannel)
              }
              .padding(vertical = 2.dp)
              .testTag("legend_row_${item.channel.name.lowercase()}")
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(color)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = item.channel.displayName,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${(item.percentage * 100).toInt()}%",
                  style = MonospaceMicro,
                  color = SwissTextSecondary,
                  fontSize = 10.sp
                )
              }
              Text(
                text = CurrencyFormatter.formatRupee(item.amount),
                style = MonospaceSmall,
                fontWeight = FontWeight.Bold,
                color = if (item.channel == Channel.CASH) ChannelCash else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
              )
            }
          }
        }
      }
    }
  }
}

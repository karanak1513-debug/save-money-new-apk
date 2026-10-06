package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.MicroLeakAlert
import com.example.data.model.WeeklyAuditSummary
import com.example.ui.theme.MonospaceHeadline
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissAlpineGreen
import com.example.ui.theme.SwissAlpineGreenLight
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissHairline
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary

@Composable
fun WeeklyAuditCard(
  auditSummary: WeeklyAuditSummary,
  microLeakAlert: MicroLeakAlert,
  modifier: Modifier = Modifier
) {
  var isExpanded by rememberSaveable { mutableStateOf(true) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
      .border(1.dp, SwissBorder, RoundedCornerShape(10.dp))
      .padding(18.dp)
      .testTag("weekly_audit_card")
  ) {
    // Header Row with Toggle
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { isExpanded = !isExpanded }
        .padding(bottom = if (isExpanded) 12.dp else 0.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .background(SwissCrimson, CircleShape)
        )
        Text(
          text = "04 // WEEKLY AUDIT BRIEF",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          letterSpacing = 1.3.sp
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = auditSummary.dateRangeLabel,
          style = MonospaceMicro,
          color = SwissDark,
          fontSize = 9.sp
        )
        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = if (isExpanded) "Collapse" else "Expand",
          tint = SwissTextSecondary,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    // Micro-Leak Anomaly Alert Banner (Rendered prominently when detected)
    if (microLeakAlert.isDetected) {
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(SwissCrimsonLight, RoundedCornerShape(6.dp))
          .border(0.75.dp, SwissCrimson, RoundedCornerShape(6.dp))
          .padding(horizontal = 12.dp, vertical = 9.dp)
          .testTag("micro_leak_alert_banner")
      ) {
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = SwissCrimson,
            modifier = Modifier
              .size(15.dp)
              .padding(top = 1.dp)
          )
          Column {
            Text(
              text = "MICRO-LEAK DETECTED",
              style = MonospaceMicro,
              color = SwissCrimson,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = microLeakAlert.badgeText,
              style = MaterialTheme.typography.bodySmall,
              color = SwissCrimson,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    // Expandable Detailed Metrics
    AnimatedVisibility(
      visible = isExpanded,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(0.75.dp)
            .background(SwissHairline)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // High-Contrast Inflow vs Outflow Ledger Box
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F8), RoundedCornerShape(6.dp))
            .border(0.75.dp, SwissHairline, RoundedCornerShape(6.dp))
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Total Inflow Column
          Column {
            Text(
              text = "TOTAL INFLOW (+)",
              style = MonospaceMicro,
              color = SwissTextTertiary,
              letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = CurrencyFormatter.formatRupee(auditSummary.totalInflow),
              style = MonospaceHeadline,
              color = SwissAlpineGreen,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Vertical divider
          Box(
            modifier = Modifier
              .width(0.75.dp)
              .height(30.dp)
              .background(SwissHairline)
          )

          // Total Outflow Column
          Column {
            Text(
              text = "TOTAL OUTFLOW (-)",
              style = MonospaceMicro,
              color = SwissTextTertiary,
              letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = CurrencyFormatter.formatRupee(auditSummary.totalOutflow),
              style = MonospaceHeadline,
              color = if (auditSummary.totalOutflow > 0) SwissCrimson else SwissDark,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Vertical divider
          Box(
            modifier = Modifier
              .width(0.75.dp)
              .height(30.dp)
              .background(SwissHairline)
          )

          // Net Savings Delta
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "NET VELOCITY",
              style = MonospaceMicro,
              color = SwissTextTertiary,
              letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = CurrencyFormatter.formatRupee(auditSummary.netSavings, showSign = true),
              style = MonospaceHeadline,
              color = if (auditSummary.netSavings >= 0) SwissAlpineGreen else SwissCrimson,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Key Category Expenditure Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "LARGEST OUTFLOW DRIVER",
            style = MonospaceMicro,
            color = SwissTextTertiary,
            letterSpacing = 1.sp
          )

          if (auditSummary.biggestCategory != null && auditSummary.biggestCategoryAmount > 0) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .background(Color(0xFFEEEEEC), RoundedCornerShape(4.dp))
                  .border(0.75.dp, SwissBorder, RoundedCornerShape(4.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = auditSummary.biggestCategory.shortTag,
                  style = MonospaceMicro,
                  color = SwissDark,
                  fontWeight = FontWeight.Bold
                )
              }
              Text(
                text = CurrencyFormatter.formatRupee(auditSummary.biggestCategoryAmount),
                style = MonospaceSmall,
                color = SwissDark,
                fontWeight = FontWeight.Bold
              )
            }
          } else {
            Text(
              text = "NONE RECORDED",
              style = MonospaceMicro,
              color = SwissTextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pace Adjustment Recommendation
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              if (auditSummary.recommendedPaceAdjustment > 0) SwissCrimsonLight else Color(0xFFF6F6F4),
              RoundedCornerShape(6.dp)
            )
            .border(
              0.75.dp,
              if (auditSummary.recommendedPaceAdjustment > 0) SwissCrimson else SwissBorder,
              RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("recommended_pace_adjustment")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(5.dp)
                .background(
                  if (auditSummary.recommendedPaceAdjustment > 0) SwissCrimson else SwissDark,
                  CircleShape
                )
            )
            Column {
              Text(
                text = "UPCOMING WEEK PACE ADJUSTMENT",
                style = MonospaceMicro,
                color = if (auditSummary.recommendedPaceAdjustment > 0) SwissCrimson else SwissTextTertiary,
                letterSpacing = 0.8.sp
              )
              Text(
                text = auditSummary.recommendationText,
                style = MaterialTheme.typography.bodySmall,
                color = if (auditSummary.recommendedPaceAdjustment > 0) SwissCrimson else SwissDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }
  }
}

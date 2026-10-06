package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.TransactionItem
import com.example.ui.theme.ChannelCash
import com.example.ui.theme.ChannelOther
import com.example.ui.theme.ChannelUpi
import com.example.ui.theme.MonospaceBody
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissBorderLight
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayUpiActivityCard(
  autoCapturedEntries: List<TransactionItem>,
  modifier: Modifier = Modifier
) {
  val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
      .border(1.dp, SwissBorder, RoundedCornerShape(12.dp))
      .padding(16.dp)
      .testTag("today_upi_activity_card")
  ) {
    // Header
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
          text = "Today's Detected UPI Activity",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Box(
        modifier = Modifier
          .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = SwissDark,
            modifier = Modifier.size(12.dp)
          )
          Text(
            text = "AUTO-SYNC",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissDark,
            letterSpacing = 0.5.sp,
            fontSize = 9.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (autoCapturedEntries.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
          .border(1.dp, SwissBorderLight, RoundedCornerShape(8.dp))
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "NO AUTO-CAPTURED UPI NOTIFICATIONS TODAY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissTextTertiary,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Background listener active. New payments from GPay, PhonePe, Paytm will appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = SwissTextSecondary,
            fontSize = 11.sp
          )
        }
      }
    } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        autoCapturedEntries.take(4).forEach { item ->
          val isCredit = item.amount > 0
          val appName = item.upiAppName ?: "UPI"
          val merchant = item.merchantOrSender ?: item.note
          val timeStr = timeFormat.format(Date(item.timestamp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
              .border(1.dp, SwissBorderLight, RoundedCornerShape(8.dp))
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Left: App Badge + Merchant + Time
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              // App Badge
              Box(
                modifier = Modifier
                  .background(
                    when (appName) {
                      "Google Pay" -> Color(0xFFE8F0FE)
                      "PhonePe" -> Color(0xFFF3E8FD)
                      "Paytm" -> Color(0xFFE1F5FE)
                      else -> Color(0xFFF3F4F6)
                    },
                    RoundedCornerShape(4.dp)
                  )
                  .padding(horizontal = 6.dp, vertical = 3.dp)
              ) {
                Text(
                  text = when (appName) {
                    "Google Pay" -> "GPAY"
                    "PhonePe" -> "PHONEPE"
                    "Paytm" -> "PAYTM"
                    else -> "UPI"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = SwissDark,
                  fontSize = 9.sp
                )
              }

              Column {
                Text(
                  text = merchant,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1
                )
                Text(
                  text = "$timeStr • Ref: ${item.upiRefId ?: "Auto"}",
                  style = MonospaceSmall,
                  color = SwissTextSecondary,
                  fontSize = 10.sp
                )
              }
            }

            // Right: Monospaced amount
            Text(
              text = CurrencyFormatter.formatRupee(item.amount, showSign = true),
              style = MonospaceBody,
              fontWeight = FontWeight.Bold,
              color = if (isCredit) SwissDark else SwissCrimson,
              fontSize = 14.sp
            )
          }
        }
      }
    }
  }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.TransactionType
import com.example.service.ParsedUpiNotification
import com.example.ui.theme.ChannelCash
import com.example.ui.theme.ChannelUpi
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary

@Composable
fun UpiDetectedBanner(
  pendingNotification: ParsedUpiNotification?,
  onConfirm: (ParsedUpiNotification) -> Unit,
  onCategorize: (ParsedUpiNotification) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = pendingNotification != null,
    enter = fadeIn() + slideInVertically(),
    exit = fadeOut() + slideOutVertically(),
    modifier = modifier
  ) {
    if (pendingNotification != null) {
      val isDebit = pendingNotification.type == TransactionType.DEBIT
      val actionVerb = if (isDebit) "paid to" else "received from"
      val formattedAmt = CurrencyFormatter.formatRupee(pendingNotification.amount)

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
          .border(1.5.dp, if (isDebit) SwissCrimson else SwissDark, RoundedCornerShape(12.dp))
          .padding(14.dp)
          .testTag("upi_detected_banner")
      ) {
        // Top status tag & App logo badge
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
                .background(if (isDebit) SwissCrimson else SwissDark, CircleShape)
            )

            Text(
              text = "UPI NOTIFICATION DETECTED",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = if (isDebit) SwissCrimson else SwissDark,
              fontSize = 10.sp
            )
          }

          // UPI App Badge
          Box(
            modifier = Modifier
              .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
              .border(1.dp, SwissBorder, RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = pendingNotification.appName.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = SwissDark,
              fontSize = 9.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main message
        Text(
          text = "Detected $formattedAmt $actionVerb ${pendingNotification.merchantOrPerson}.",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        if (!pendingNotification.upiRefId.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Ref: ${pendingNotification.upiRefId}",
            style = MonospaceSmall,
            color = SwissTextSecondary,
            fontSize = 11.sp
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions: Confirm | Categorize | Dismiss
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Confirm button (Primary)
          Button(
            onClick = { onConfirm(pendingNotification) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isDebit) SwissCrimson else SwissDark,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
              .testTag("banner_confirm_button")
          ) {
            Text(
              text = "Confirm",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
          }

          // Categorize button
          OutlinedButton(
            onClick = { onCategorize(pendingNotification) },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SwissDark),
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
              .testTag("banner_categorize_button")
          ) {
            Text(
              text = "Categorize",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Dismiss button
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(38.dp)
              .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
              .testTag("banner_dismiss_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Dismiss",
              tint = SwissTextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }
  }
}

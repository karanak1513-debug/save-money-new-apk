package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.ChannelBreakdown
import com.example.data.model.CurrencyFormatter
import com.example.data.model.DailyBarData
import com.example.data.model.Goal
import com.example.data.model.PacingInfo
import com.example.data.model.TransactionItem
import com.example.service.ParsedUpiNotification
import com.example.ui.components.DonutBreakdownChart
import com.example.ui.components.NotificationPermissionBanner
import com.example.ui.components.PrimaryGoalCard
import com.example.ui.components.TodayUpiActivityCard
import com.example.ui.components.UpiDetectedBanner
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.ChannelCash
import com.example.ui.theme.ChannelOther
import com.example.ui.theme.ChannelUpi
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary

@Composable
fun DashboardScreen(
  userName: String,
  primaryGoal: Goal?,
  pacingInfo: PacingInfo,
  channelBreakdown: List<ChannelBreakdown>,
  weeklyBars: List<DailyBarData>,
  autoCapturedTodayEntries: List<TransactionItem>,
  pendingUpiNotification: ParsedUpiNotification?,
  isNotificationListenerGranted: Boolean,
  onOpenListenerSettings: () -> Unit,
  onOpenSimulator: () -> Unit,
  onConfirmNotification: (ParsedUpiNotification) -> Unit,
  onCategorizeNotification: (ParsedUpiNotification) -> Unit,
  onDismissNotification: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenAddEntry: () -> Unit,
  onSwitchGoal: () -> Unit,
  onChannelFilterClick: (Channel) -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      // Primary CTA: Floating Bottom Pill Button: Solid Crimson Red with white text "+ Add Entry"
      ExtendedFloatingActionButton(
        onClick = onOpenAddEntry,
        containerColor = SwissCrimson,
        contentColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
          .padding(bottom = 12.dp)
          .testTag("floating_add_entry_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(horizontal = 8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Entry",
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "Add Entry",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("dashboard_scroll_container"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Header:
      // Left: Personalized greeting "Namaste, [User Name]" with a small avatar badge.
      // Right: Minimalist Settings icon & Simulator icon
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Small minimal avatar badge with initials
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(SwissDark, CircleShape)
                .border(1.dp, SwissBorder, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = userName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }

            Column {
              Text(
                text = "Namaste, $userName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.3).sp
              )
              Text(
                text = "Manual & Auto-UPI Savings Ledger",
                style = MaterialTheme.typography.bodySmall,
                color = SwissTextSecondary,
                letterSpacing = 0.2.sp
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Quick Test Simulator Button
            IconButton(
              onClick = onOpenSimulator,
              modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
                .testTag("dashboard_simulator_icon_button")
            ) {
              Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Simulate UPI Notification",
                tint = SwissCrimson,
                modifier = Modifier.size(20.dp)
              )
            }

            // Minimalist Settings Icon
            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
                .testTag("settings_icon_button")
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // Non-intrusive In-App Banner: "Detected ₹450 paid to XYZ. Categorize or Confirm?"
      if (pendingUpiNotification != null) {
        item {
          UpiDetectedBanner(
            pendingNotification = pendingUpiNotification,
            onConfirm = onConfirmNotification,
            onCategorize = onCategorizeNotification,
            onDismiss = onDismissNotification
          )
        }
      }

      // Notification Listener Status & Permission Banner
      item {
        NotificationPermissionBanner(
          isPermissionGranted = isNotificationListenerGranted,
          onOpenSettings = onOpenListenerSettings,
          onOpenSimulator = onOpenSimulator
        )
      }

      // 2. Primary Goal Card (Frosted White Border, Flat Surface):
      // Hero Target Card with dynamic run-rate (e.g., "Save ₹450 today to stay on track")
      item {
        PrimaryGoalCard(
          goal = primaryGoal,
          pacingInfo = pacingInfo,
          onSwitchGoalClick = onSwitchGoal
        )
      }

      // 4. Bucket Summary Chips (Horizontal):
      // [UPI: ₹35,000] | [Cash: ₹15,000] | [Other: ₹10,000]
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "SAVINGS BUCKETS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = SwissTextTertiary,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(channelBreakdown) { item ->
              val indicatorColor = when (item.channel) {
                Channel.UPI -> ChannelUpi
                Channel.CASH -> ChannelCash
                Channel.OTHER -> ChannelOther
              }

              Box(
                modifier = Modifier
                  .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                  .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
                  .clickable { onChannelFilterClick(item.channel) }
                  .padding(horizontal = 12.dp, vertical = 8.dp)
                  .testTag("bucket_chip_${item.channel.name.lowercase()}")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(indicatorColor, CircleShape)
                  )
                  Text(
                    text = "${item.channel.displayName}:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = SwissTextSecondary
                  )
                  Text(
                    text = CurrencyFormatter.formatRupee(item.amount),
                    style = MonospaceSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }
      }

      // Today's Detected UPI Activity feed
      item {
        TodayUpiActivityCard(
          autoCapturedEntries = autoCapturedTodayEntries
        )
      }

      // 3. Visual Analytics Section:
      // Donut Chart: "Channel Distribution" showing split between UPI, Cash, Other
      item {
        DonutBreakdownChart(
          breakdown = channelBreakdown,
          onChannelSelected = { ch ->
            if (ch != null) onChannelFilterClick(ch)
          }
        )
      }

      // Weekly Bar Chart: 7 clean vertical bars
      item {
        WeeklyBarChart(
          dailyData = weeklyBars
        )
      }

      // Bottom padding space for FAB
      item {
        Spacer(modifier = Modifier.height(72.dp))
      }
    }
  }
}

package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.ChannelBreakdown
import com.example.data.model.CurrencyFormatter
import com.example.data.model.DailyBarData
import com.example.data.model.Goal
import com.example.data.model.GoalFeasibility
import com.example.data.model.MicroLeakAlert
import com.example.data.model.PacingInfo
import com.example.data.model.TransactionItem
import com.example.data.model.WeeklyAuditSummary
import com.example.service.UpiNotificationService
import com.example.ui.components.DonutBreakdownChart
import com.example.ui.components.PrimaryGoalCard
import com.example.ui.components.WeeklyAuditCard
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.ChannelCash
import com.example.ui.theme.ChannelOther
import com.example.ui.theme.ChannelUpi
import com.example.ui.theme.MonospaceBody
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
import com.example.ui.theme.SwissSlate
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Redesigned Swiss Editorial Light Theme Dashboard:
 * - Palette: Pure White (#FFFFFF), Off-White canvas (#F9FAFB), Stark Black (#111827), Precision Crimson (#DC2626)
 * - Removed all fake manual "Quick Paste UPI / SMS" buttons or input boxes completely.
 * - Header: Greeting "Namaste, [User Name]" with sync status dot and a Privacy Blur toggle icon (blurs all balances on tap).
 * - Hero Goal Card: Monospaced balance, 3px razor crimson progress bar, and strictly padded 3-column pacing grid.
 * - Bucket Split: Segmented chips [ UPI: ₹... ] | [ Cash: ₹... ] | [ Other: ₹... ].
 * - Live Transaction Stream: Auto-captured stream showing source badge ([PhonePe], [GPay]), extracted merchant title,
 *   timestamp, and monospaced amounts (-₹450 in Crimson, +₹2,500 in Slate). Full swipe-to-delete.
 * - In-app check: If notification access is missing, shows clean, non-blocking banner linking to Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  primaryGoal: Goal?,
  pacingInfo: PacingInfo,
  channelBreakdown: List<ChannelBreakdown>,
  weeklyBars: List<DailyBarData>,
  recentTransactions: List<TransactionItem>,
  dailyStreak: Int,
  userName: String,
  isAuthenticated: Boolean = false,
  isPrivacyMode: Boolean = false,
  onTogglePrivacyMode: () -> Unit = {},
  goalFeasibility: GoalFeasibility? = null,
  microLeakAlert: MicroLeakAlert = MicroLeakAlert(false, 0, 0.0, 0, 0, "", 0.0),
  weeklyAuditSummary: WeeklyAuditSummary = WeeklyAuditSummary(0.0, 0.0, 0.0, null, 0.0, 0.0, "", "", 0),
  onDeleteTransaction: (TransactionItem) -> Unit = {},
  onSwitchGoal: () -> Unit,
  onChannelFilterClick: (Channel) -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var isNotificationEnabled by remember { mutableStateOf(false) }

  // Check system notification listener permission dynamically
  LaunchedEffect(Unit) {
    isNotificationEnabled = UpiNotificationService.isNotificationServiceEnabled(context)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFF9FAFB)) // Off-White canvas
      .testTag("dashboard_screen")
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Header Row: "Namaste, [User Name]", Sync Status Dot, Privacy Blur Toggle, Settings
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Editorial Brand Header
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
                text = "SANCHAY // CAPITAL TRACKER",
                style = MonospaceMicro,
                color = SwissTextTertiary,
                letterSpacing = 1.5.sp
              )
            }

            // Right Actions: Privacy Blur Toggle & Settings
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              // Privacy Blur Toggle Icon (blurs all balances on tap)
              IconButton(
                onClick = onTogglePrivacyMode,
                modifier = Modifier
                  .size(36.dp)
                  .background(Color(0xFFFFFFFF), RoundedCornerShape(6.dp))
                  .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
                  .testTag("privacy_blur_toggle_button")
              ) {
                Icon(
                  imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (isPrivacyMode) "Show Balances" else "Hide Balances",
                  tint = if (isPrivacyMode) SwissCrimson else SwissDark,
                  modifier = Modifier.size(17.dp)
                )
              }

              // Settings Action
              IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                  .size(36.dp)
                  .background(Color(0xFFFFFFFF), RoundedCornerShape(6.dp))
                  .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
                  .testTag("settings_icon_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Tune,
                  contentDescription = "Settings",
                  tint = SwissDark,
                  modifier = Modifier.size(17.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Greeting: "Namaste, [User Name]" with Sync Status Dot
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                // Sync status dot
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .background(
                      if (isAuthenticated) SwissAlpineGreen else Color(0xFF9CA3AF),
                      CircleShape
                    )
                )
                Text(
                  text = if (isAuthenticated) "CLOUD SYNCED" else "LOCAL LEDGER",
                  style = MonospaceMicro,
                  color = if (isAuthenticated) SwissAlpineGreen else SwissTextTertiary,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                )
              }

              Spacer(modifier = Modifier.height(2.dp))

              Text(
                text = "Namaste, ${userName.ifBlank { "Client" }}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = SwissDark,
                letterSpacing = (-0.5).sp,
                fontSize = 24.sp
              )
            }

            // Streak Counter Badge
            Box(
              modifier = Modifier
                .background(SwissCrimsonLight, RoundedCornerShape(6.dp))
                .border(1.dp, SwissCrimson.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
                .testTag("streak_badge")
            ) {
              Text(
                text = if (dailyStreak > 0) "🔥 ${dailyStreak}D STREAK" else "🔥 0D STREAK",
                style = MonospaceMicro,
                color = SwissCrimson,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(SwissHairline)
          )
        }
      }

      // 2. Non-Blocking Notification Listener Permission Banner (if missing)
      if (!isNotificationEnabled) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFFFFFFF), RoundedCornerShape(8.dp))
              .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
              .padding(14.dp)
              .testTag("notification_permission_banner")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .background(SwissCrimsonLight, RoundedCornerShape(6.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = SwissCrimson,
                    modifier = Modifier.size(18.dp)
                  )
                }

                Column {
                  Text(
                    text = "AUTOMATIC UPI TRACKER",
                    style = MonospaceMicro,
                    color = SwissDark,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                  )
                  Text(
                    text = "Grant notification access to intercept PhonePe, GPay, Paytm & bank SMS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SwissTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Box(
                modifier = Modifier
                  .background(SwissDark, RoundedCornerShape(4.dp))
                  .clickable {
                    UpiNotificationService.openNotificationListenerSettings(context)
                  }
                  .padding(horizontal = 10.dp, vertical = 7.dp)
                  .testTag("enable_upi_listener_button")
              ) {
                Text(
                  text = "ENABLE",
                  style = MonospaceMicro,
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // 3. Hero Goal Card with 3px razor progress bar & 3-column pacing grid
      item {
        PrimaryGoalCard(
          goal = primaryGoal,
          pacingInfo = pacingInfo,
          feasibility = goalFeasibility,
          isPrivacyMode = isPrivacyMode,
          onSwitchGoalClick = onSwitchGoal
        )
      }

      // 4. Bucket Split: Segmented chips [ UPI: ₹... ] | [ Cash: ₹... ] | [ Other: ₹... ]
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "02 // BUCKET SPLIT",
            style = MonospaceMicro,
            color = SwissTextTertiary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            channelBreakdown.forEach { item ->
              val indicatorColor = when (item.channel) {
                Channel.UPI -> ChannelUpi
                Channel.CASH -> ChannelCash
                Channel.OTHER -> ChannelOther
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(Color(0xFFFFFFFF), RoundedCornerShape(6.dp))
                  .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
                  .clickable { onChannelFilterClick(item.channel) }
                  .padding(horizontal = 10.dp, vertical = 10.dp)
                  .testTag("bucket_chip_${item.channel.name.lowercase()}"),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .background(indicatorColor, CircleShape)
                  )
                  Column {
                    Text(
                      text = item.channel.displayName.uppercase(),
                      style = MonospaceMicro,
                      color = SwissTextSecondary,
                      fontSize = 9.sp
                    )
                    Text(
                      text = if (isPrivacyMode) "₹••••" else CurrencyFormatter.formatRupee(item.amount),
                      style = MonospaceSmall,
                      fontWeight = FontWeight.Bold,
                      color = SwissDark,
                      fontSize = 11.sp,
                      maxLines = 1
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 5. Visual Analytics: Donut Breakdown & Weekly Bar Chart
      item {
        DonutBreakdownChart(
          breakdown = channelBreakdown,
          onChannelSelected = { ch ->
            if (ch != null) onChannelFilterClick(ch)
          }
        )
      }

      item {
        WeeklyBarChart(
          dailyData = weeklyBars
        )
      }

      // 6. Live Transaction Stream: Auto-captured stream with swipe-to-delete
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                  .background(SwissDark, CircleShape)
              )
              Text(
                text = "03 // LIVE TRANSACTION STREAM",
                style = MonospaceMicro,
                color = SwissTextTertiary,
                letterSpacing = 1.2.sp
              )
            }

            Text(
              text = "SWIPE TO DELETE",
              style = MonospaceMicro,
              color = SwissTextTertiary,
              fontSize = 9.sp
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (recentTransactions.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFFFF), RoundedCornerShape(8.dp))
                .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "NO TRANSACTIONS CAPTURED",
                  style = MonospaceMicro,
                  color = SwissTextTertiary,
                  letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Automatic listener will capture transactions from PhonePe, GPay, Paytm, and SMS.",
                  style = MaterialTheme.typography.bodySmall,
                  color = SwissTextSecondary,
                  fontSize = 12.sp,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }
            }
          } else {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFFFF), RoundedCornerShape(8.dp))
                .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
            ) {
              val displayList = recentTransactions.take(8)
              displayList.forEachIndexed { index, tx ->
                val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
                val timeString = timeFormat.format(Date(tx.timestamp))
                val isDebit = tx.amount < 0
                val sourceLabel = tx.upiAppName ?: tx.channel.displayName

                // Material 3 Swipe-To-Dismiss Box for full swipe-to-delete
                val dismissState = rememberSwipeToDismissBoxState(
                  confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                      onDeleteTransaction(tx)
                      true
                    } else {
                      false
                    }
                  }
                )

                SwipeToDismissBox(
                  state = dismissState,
                  backgroundContent = {
                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .background(SwissCrimson)
                        .padding(horizontal = 16.dp),
                      contentAlignment = Alignment.CenterEnd
                    ) {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                      )
                    }
                  }
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(Color(0xFFFFFFFF))
                      .padding(horizontal = 14.dp, vertical = 12.dp)
                      .testTag("stream_row_${tx.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Left: Source Badge + Extracted Merchant Title + Timestamp
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      // Source Badge: [PhonePe], [GPay], etc.
                      Box(
                        modifier = Modifier
                          .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                          .border(1.dp, SwissBorder, RoundedCornerShape(4.dp))
                          .padding(horizontal = 6.dp, vertical = 3.dp)
                      ) {
                        Text(
                          text = sourceLabel.uppercase(),
                          style = MonospaceMicro,
                          color = SwissDark,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold
                        )
                      }

                      Column {
                        Text(
                          text = tx.merchantOrSender?.ifBlank { tx.note } ?: tx.note,
                          style = MaterialTheme.typography.bodyMedium,
                          color = SwissDark,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 13.sp,
                          maxLines = 1,
                          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                          text = timeString,
                          style = MonospaceMicro,
                          color = SwissTextTertiary,
                          fontSize = 9.sp
                        )
                      }
                    }

                    // Right: Monospaced Amount (-₹450 in Crimson, +₹2,500 in Slate)
                    Text(
                      text = if (isPrivacyMode) {
                        if (isDebit) "-₹•••" else "+₹•••"
                      } else {
                        CurrencyFormatter.formatRupee(tx.amount, showSign = true)
                      },
                      style = MonospaceHeadline,
                      fontWeight = FontWeight.Bold,
                      color = if (isDebit) SwissCrimson else SwissSlate,
                      fontSize = 13.sp
                    )
                  }
                }

                if (index < displayList.lastIndex) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(1.dp)
                      .background(SwissHairline)
                  )
                }
              }
            }
          }
        }
      }

      // 7. Executive Weekly Audit Brief (7-day summary & micro-leak anomaly card)
      item {
        WeeklyAuditCard(
          auditSummary = weeklyAuditSummary,
          microLeakAlert = microLeakAlert
        )
      }

      item {
        Spacer(modifier = Modifier.height(72.dp))
      }
    }
  }
}

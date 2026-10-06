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
import androidx.compose.ui.draw.clip
import com.example.ui.components.ambientMeshBackground
import com.example.ui.components.frostedGlass
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SectionHeaderMedium
import com.example.ui.theme.SlateHeader
import com.example.ui.theme.GlassSurfaceMilky
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
      .ambientMeshBackground()
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
                text = "SANCHAY",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                color = SwissDark,
                fontSize = 13.sp,
                letterSpacing = 2.sp
              )
              Text(
                text = "• Wealth Management",
                style = SectionHeaderMedium,
                fontSize = 11.sp,
                color = SlateHeader
              )
            }

            // Right Actions: Privacy Blur Toggle & Settings
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Privacy Blur Toggle Icon (blurs all balances on tap)
              IconButton(
                onClick = onTogglePrivacyMode,
                modifier = Modifier
                  .size(38.dp)
                  .frostedGlass(RoundedCornerShape(12.dp), elevation = 2.dp)
                  .testTag("privacy_blur_toggle_button")
              ) {
                Icon(
                  imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (isPrivacyMode) "Show Balances" else "Hide Balances",
                  tint = if (isPrivacyMode) SwissCrimson else SwissDark,
                  modifier = Modifier.size(18.dp)
                )
              }

              // Settings Action
              IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                  .size(38.dp)
                  .frostedGlass(RoundedCornerShape(12.dp), elevation = 2.dp)
                  .testTag("settings_icon_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Tune,
                  contentDescription = "Settings",
                  tint = SwissDark,
                  modifier = Modifier.size(18.dp)
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
                    .size(7.dp)
                    .background(
                      if (isAuthenticated) SwissAlpineGreen else Color(0xFF9CA3AF),
                      CircleShape
                    )
                )
                Text(
                  text = if (isAuthenticated) "CLOUD SYNCED" else "LOCAL LEDGER",
                  style = SectionHeaderMedium,
                  color = if (isAuthenticated) SwissAlpineGreen else SlateHeader,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                )
              }

              Spacer(modifier = Modifier.height(2.dp))

              Text(
                text = "Namaste, ${userName.ifBlank { "Client" }}",
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                color = SwissDark,
                letterSpacing = (-0.6).sp,
                fontSize = 26.sp
              )
            }

            // Streak Counter Badge
            Box(
              modifier = Modifier
                .frostedGlass(RoundedCornerShape(14.dp), elevation = 2.dp)
                .background(SwissCrimsonLight.copy(alpha = 0.6f))
                .padding(horizontal = 11.dp, vertical = 6.dp)
                .testTag("streak_badge")
            ) {
              Text(
                text = if (dailyStreak > 0) "🔥 ${dailyStreak}D STREAK" else "🔥 0D STREAK",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = PlusJakartaSans,
                color = SwissCrimson,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              )
            }
          }
        }
      }

      // 2. Non-Blocking Notification Listener Permission Banner (if missing)
      if (!isNotificationEnabled) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .frostedGlass(shape = RoundedCornerShape(20.dp), elevation = 3.dp)
              .padding(16.dp)
              .testTag("notification_permission_banner")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SwissCrimsonLight),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = SwissCrimson,
                    modifier = Modifier.size(20.dp)
                  )
                }

                Column {
                  Text(
                    text = "AUTOMATIC UPI INTERCEPTION",
                    style = SectionHeaderMedium,
                    color = SlateHeader,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Grant notification access to intercept PhonePe, GPay, Paytm & bank SMS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SwissTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                  )
                }
              }

              Spacer(modifier = Modifier.width(10.dp))

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(SwissDark)
                  .clickable {
                    UpiNotificationService.openNotificationListenerSettings(context)
                  }
                  .padding(horizontal = 14.dp, vertical = 8.dp)
                  .testTag("enable_upi_listener_button")
              ) {
                Text(
                  text = "ENABLE",
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = PlusJakartaSans,
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
            text = "Capital by Channel",
            style = SectionHeaderMedium,
            color = SlateHeader,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                  .frostedGlass(shape = RoundedCornerShape(16.dp), elevation = 2.dp)
                  .clickable { onChannelFilterClick(item.channel) }
                  .padding(horizontal = 12.dp, vertical = 12.dp)
                  .testTag("bucket_chip_${item.channel.name.lowercase()}"),
                contentAlignment = Alignment.Center
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
                  Column {
                    Text(
                      text = item.channel.displayName.uppercase(),
                      style = SectionHeaderMedium,
                      fontSize = 10.sp,
                      color = SlateHeader,
                      fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (isPrivacyMode) "₹••••" else CurrencyFormatter.formatRupee(item.amount),
                      style = MaterialTheme.typography.titleSmall,
                      fontFamily = PlusJakartaSans,
                      fontWeight = FontWeight.Bold,
                      color = SwissDark,
                      fontSize = 13.sp,
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
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .background(SwissCrimson, CircleShape)
              )
              Text(
                text = "LIVE TRANSACTION STREAM",
                style = SectionHeaderMedium,
                color = SlateHeader,
                fontWeight = FontWeight.SemiBold
              )
            }

            Text(
              text = "SWIPE TO DELETE",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = PlusJakartaSans,
              color = SlateHeader,
              fontSize = 10.sp
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (recentTransactions.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .frostedGlass(shape = RoundedCornerShape(24.dp), elevation = 3.dp)
                .padding(28.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "NO TRANSACTIONS CAPTURED YET",
                  style = SectionHeaderMedium,
                  color = SlateHeader,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Automatic listener will capture transactions from PhonePe, GPay, Paytm, and bank SMS.",
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
                .frostedGlass(shape = RoundedCornerShape(24.dp), elevation = 4.dp)
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
                      .background(if (index % 2 == 0) Color(0x60FFFFFF) else Color(0x35FFFFFF))
                      .padding(horizontal = 16.dp, vertical = 14.dp)
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
                          .clip(RoundedCornerShape(8.dp))
                          .background(Color(0xFFF1F5F9))
                          .border(0.75.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                          .padding(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Text(
                          text = sourceLabel.uppercase(),
                          style = MaterialTheme.typography.labelSmall,
                          fontFamily = PlusJakartaSans,
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

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
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.DonutBreakdownChart
import com.example.ui.components.PrimaryGoalCard
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.ChannelCash
import com.example.ui.theme.ChannelOther
import com.example.ui.theme.ChannelUpi
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonBorder
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissHairline
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary

@Composable
fun DashboardScreen(
  userName: String,
  dailyStreak: Int,
  primaryGoal: Goal?,
  pacingInfo: PacingInfo,
  channelBreakdown: List<ChannelBreakdown>,
  weeklyBars: List<DailyBarData>,
  hasTransactions: Boolean,
  onQuickAddPreset: (Double) -> Unit,
  onOpenQuickPaste: () -> Unit,
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
      ExtendedFloatingActionButton(
        onClick = onOpenAddEntry,
        containerColor = SwissDark,
        contentColor = Color.White,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .padding(bottom = 12.dp)
          .testTag("floating_add_entry_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(horizontal = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Entry",
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "LOG ENTRY",
            style = MonospaceMicro,
            letterSpacing = 1.2.sp
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
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Editorial Masthead
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
        ) {
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
                text = "SANCHAY // CAPITAL TRACKER",
                style = MonospaceMicro,
                color = SwissTextTertiary,
                letterSpacing = 1.5.sp
              )
            }

            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                .border(0.75.dp, SwissBorder, RoundedCornerShape(6.dp))
                .testTag("settings_icon_button")
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "PORTFOLIO",
                style = MonospaceMicro,
                color = SwissTextTertiary,
                letterSpacing = 1.sp
              )
              Text(
                text = userName.ifBlank { "Client" },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
              )
            }

            // Streak Counter Badge
            Box(
              modifier = Modifier
                .background(SwissCrimsonLight, RoundedCornerShape(4.dp))
                .border(0.75.dp, SwissCrimsonBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("streak_badge")
            ) {
              Text(
                text = if (dailyStreak > 0) "🔥 ${dailyStreak}D STREAK" else "🔥 0D STREAK",
                style = MonospaceMicro,
                color = SwissCrimson,
                letterSpacing = 0.5.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(0.75.dp)
              .background(SwissHairline)
          )
        }
      }

      // Feature 1: Quick-Add Preset Chips ([+ ₹100], [+ ₹500], [+ ₹1,000])
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "QUICK ALLOCATION PRESETS",
            style = MonospaceMicro,
            color = SwissTextTertiary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val presets = listOf(
              Pair(100.0, "+ ₹100"),
              Pair(500.0, "+ ₹500"),
              Pair(1000.0, "+ ₹1,000")
            )

            presets.forEach { (amount, label) ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                  .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
                  .clickable { onQuickAddPreset(amount) }
                  .padding(vertical = 10.dp)
                  .testTag("quick_add_${amount.toInt()}"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  style = MonospaceSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }

      // Functional Quick Paste UPI / SMS Smart Bar
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F8), RoundedCornerShape(8.dp))
            .border(0.75.dp, SwissHairline, RoundedCornerShape(8.dp))
            .clickable { onOpenQuickPaste() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag("quick_paste_action_banner"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(SwissCrimsonLight, RoundedCornerShape(4.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = SwissCrimson,
                modifier = Modifier.size(16.dp)
              )
            }
            Column {
              Text(
                text = "QUICK PASTE UPI / SMS",
                style = MonospaceMicro,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Parse bank notifications & debits instantly",
                style = MaterialTheme.typography.bodySmall,
                color = SwissTextSecondary,
                fontSize = 11.sp
              )
            }
          }

          Box(
            modifier = Modifier
              .background(SwissDark, RoundedCornerShape(4.dp))
              .padding(horizontal = 9.dp, vertical = 5.dp)
          ) {
            Text(
              text = "PASTE",
              style = MonospaceMicro,
              color = Color.White
            )
          }
        }
      }

      // 2. Primary Goal Card
      item {
        PrimaryGoalCard(
          goal = primaryGoal,
          pacingInfo = pacingInfo,
          onSwitchGoalClick = onSwitchGoal
        )
      }

      // 4. Savings Bucket Chips
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "LIQUIDITY CHANNELS",
            style = MonospaceMicro,
            color = SwissTextTertiary,
            letterSpacing = 1.2.sp,
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
                  .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                  .border(0.75.dp, SwissBorder, RoundedCornerShape(6.dp))
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
                      .size(6.dp)
                      .background(indicatorColor, CircleShape)
                  )
                  Text(
                    text = "${item.channel.displayName.uppercase()}:",
                    style = MonospaceMicro,
                    color = SwissTextSecondary
                  )
                  Text(
                    text = CurrencyFormatter.formatRupee(item.amount),
                    style = MonospaceSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                  )
                }
              }
            }
          }
        }
      }

      // Empty State Notice
      if (!hasTransactions) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
              .border(0.75.dp, SwissBorder, RoundedCornerShape(8.dp))
              .padding(20.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "NO TRANSACTIONS RECORDED",
                style = MonospaceMicro,
                color = SwissTextTertiary,
                letterSpacing = 1.2.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Add your first entry or paste a UPI payment notification above.",
                style = MaterialTheme.typography.bodySmall,
                color = SwissTextSecondary,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // 3. Visual Analytics Section
      item {
        DonutBreakdownChart(
          breakdown = channelBreakdown,
          onChannelSelected = { ch ->
            if (ch != null) onChannelFilterClick(ch)
          }
        )
      }

      // Weekly Activity Bar Chart
      item {
        WeeklyBarChart(
          dailyData = weeklyBars
        )
      }

      item {
        Spacer(modifier = Modifier.height(72.dp))
      }
    }
  }
}

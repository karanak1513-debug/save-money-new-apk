package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.CurrencyFormatter
import com.example.data.model.Goal
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.theme.ChannelCash
import com.example.ui.theme.ChannelOther
import com.example.ui.theme.ChannelUpi
import com.example.ui.theme.MonospaceHeadline
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryBottomSheet(
  goals: List<Goal>,
  selectedGoalId: Long?,
  onDismiss: () -> Unit,
  onSaveEntry: (TransactionItem) -> Unit,
  initialType: TransactionType = TransactionType.CREDIT,
  initialAmount: Double? = null,
  initialChannel: Channel = Channel.UPI,
  initialNote: String = "",
  initialUpiAppName: String? = null,
  initialUpiRefId: String? = null,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  var transactionType by remember { mutableStateOf(initialType) }
  var amountText by remember {
    mutableStateOf(
      initialAmount?.let { if (it % 1 == 0.0) it.toLong().toString() else it.toString() } ?: ""
    )
  }
  var selectedChannel by remember { mutableStateOf(initialChannel) }
  var noteText by remember { mutableStateOf(initialNote) }
  var linkedGoalId by remember { mutableStateOf(selectedGoalId ?: goals.firstOrNull()?.id) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val quickAmounts = listOf(100.0, 500.0, 1000.0, 2500.0, 5000.0)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 20.dp)
        .testTag("add_entry_bottom_sheet")
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "MANUAL ENTRY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissCrimson,
            letterSpacing = 1.2.sp,
            fontSize = 10.sp
          )
          Text(
            text = "Record Savings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_add_entry_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = SwissTextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Type Selector Segment: Credit (+) vs Debit (-)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
          .padding(4.dp)
      ) {
        // Credit Button
        Box(
          modifier = Modifier
            .weight(1f)
            .background(
              color = if (transactionType == TransactionType.CREDIT) SwissDark else Color.Transparent,
              shape = RoundedCornerShape(6.dp)
            )
            .clickable { transactionType = TransactionType.CREDIT }
            .padding(vertical = 10.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "+ Add to Savings (Credit)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (transactionType == TransactionType.CREDIT) Color.White else SwissTextSecondary
          )
        }

        // Debit Button
        Box(
          modifier = Modifier
            .weight(1f)
            .background(
              color = if (transactionType == TransactionType.DEBIT) SwissCrimson else Color.Transparent,
              shape = RoundedCornerShape(6.dp)
            )
            .clickable { transactionType = TransactionType.DEBIT }
            .padding(vertical = 10.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "- Withdrawal (Debit)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (transactionType == TransactionType.DEBIT) Color.White else SwissTextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Amount Input Field
      Text(
        text = "AMOUNT (₹)",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = amountText,
        onValueChange = { input ->
          if (input.all { it.isDigit() || it == '.' }) {
            amountText = input
            errorMessage = null
          }
        },
        leadingIcon = {
          Text(
            text = "₹",
            style = MonospaceHeadline,
            color = if (transactionType == TransactionType.CREDIT) SwissDark else SwissCrimson,
            modifier = Modifier.padding(start = 12.dp)
          )
        },
        placeholder = {
          Text(
            text = "0",
            style = MonospaceHeadline,
            color = SwissTextTertiary
          )
        },
        textStyle = MonospaceHeadline.copy(
          color = if (transactionType == TransactionType.CREDIT) MaterialTheme.colorScheme.onSurface else SwissCrimson
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = if (transactionType == TransactionType.CREDIT) SwissDark else SwissCrimson,
          unfocusedBorderColor = SwissBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("entry_amount_input")
      )

      // Quick amount chips
      Spacer(modifier = Modifier.height(8.dp))
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(quickAmounts) { amt ->
          Box(
            modifier = Modifier
              .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
              .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
              .clickable {
                val current = amountText.toDoubleOrNull() ?: 0.0
                amountText = (current + amt).toLong().toString()
              }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "+${CurrencyFormatter.formatRupee(amt)}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = SwissDark
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Channel Selection: UPI, Cash, Other
      Text(
        text = "PAYMENT CHANNEL",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Channel.values().forEach { channel ->
          val isSelected = selectedChannel == channel
          val badgeColor = when (channel) {
            Channel.UPI -> ChannelUpi
            Channel.CASH -> ChannelCash
            Channel.OTHER -> ChannelOther
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .background(
                color = if (isSelected) Color(0xFFF3F4F6) else Color.White,
                shape = RoundedCornerShape(8.dp)
              )
              .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) badgeColor else SwissBorder,
                shape = RoundedCornerShape(8.dp)
              )
              .clickable { selectedChannel = channel }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(badgeColor, CircleShape)
              )
              Text(
                text = channel.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Target Goal Selector
      if (goals.isNotEmpty()) {
        Text(
          text = "ALLOCATE TO GOAL",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = SwissTextTertiary,
          fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(goals) { g ->
            val isSelected = linkedGoalId == g.id
            Box(
              modifier = Modifier
                .background(
                  color = if (isSelected) SwissDark else Color(0xFFF9FAFB),
                  shape = RoundedCornerShape(6.dp)
                )
                .border(1.dp, if (isSelected) SwissDark else SwissBorder, RoundedCornerShape(6.dp))
                .clickable { linkedGoalId = g.id }
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = g.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Note Field
      Text(
        text = "NOTE / REMARK",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        placeholder = { Text("e.g. Freelance bonus, Skipped dining out") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("entry_note_input")
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = errorMessage!!,
          style = MaterialTheme.typography.bodySmall,
          color = SwissCrimson
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Solid Crimson Red Save Button
      Button(
        onClick = {
          val parsedAmount = amountText.toDoubleOrNull()
          if (parsedAmount == null || parsedAmount <= 0) {
            errorMessage = "Please enter a valid amount"
            return@Button
          }

          val finalAmount = if (transactionType == TransactionType.CREDIT) parsedAmount else -parsedAmount
          val today = LocalDate.now()
          val newItem = TransactionItem(
            goalId = linkedGoalId,
            amount = finalAmount,
            channel = selectedChannel,
            note = noteText.ifBlank { if (transactionType == TransactionType.CREDIT) "Manual Savings" else "Withdrawal" },
            timestamp = System.currentTimeMillis(),
            dateEpochDay = today.toEpochDay(),
            isAutoCaptured = initialUpiAppName != null,
            upiAppName = initialUpiAppName,
            upiRefId = initialUpiRefId,
            isConfirmed = true
          )
          onSaveEntry(newItem)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = SwissCrimson,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_entry_button")
      ) {
        Text(
          text = "Save Entry",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}

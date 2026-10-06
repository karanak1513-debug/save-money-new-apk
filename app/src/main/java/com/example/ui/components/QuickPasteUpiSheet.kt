package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.CurrencyFormatter
import com.example.data.model.Goal
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.service.UpiParserEngine
import com.example.ui.theme.MonospaceHeadline
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPasteUpiSheet(
  goals: List<Goal>,
  selectedGoalId: Long?,
  onDismiss: () -> Unit,
  onSaveTransaction: (TransactionItem) -> Unit
) {
  val context = LocalContext.current
  var rawText by remember { mutableStateOf("") }
  var linkedGoalId by remember { mutableStateOf(selectedGoalId ?: goals.firstOrNull()?.id) }

  // Editable parsed fields populated live
  var parsedAmountText by remember { mutableStateOf("") }
  var parsedMerchantText by remember { mutableStateOf("") }
  var parsedType by remember { mutableStateOf(TransactionType.DEBIT) }
  var parsedRefId by remember { mutableStateOf<String?>(null) }
  var hasParsed by remember { mutableStateOf(false) }

  // Live real-time regex parsing on rawText!
  LaunchedEffect(rawText) {
    if (rawText.isNotBlank()) {
      val result = UpiParserEngine.parse("com.google.android.apps.nbu.paisa.user", null, rawText)
      if (result != null) {
        parsedAmountText = if (result.amount % 1 == 0.0) result.amount.toLong().toString() else result.amount.toString()
        parsedMerchantText = result.merchantOrPerson
        parsedType = result.type
        parsedRefId = result.upiRefId
        hasParsed = true
      }
    } else {
      hasParsed = false
    }
  }

  val sampleTexts = listOf(
    "Paid ₹450 to Swiggy on Google Pay",
    "Received Rs. 5,000 from Client via UPI",
    "Debited INR 1,200.00 for payment to Uber",
    "Paid ₹150 to Sharma General Store on Paytm"
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 20.dp)
        .testTag("quick_paste_upi_sheet")
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "SMART PARSER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissCrimson,
            letterSpacing = 1.2.sp,
            fontSize = 10.sp
          )
          Text(
            text = "Quick Paste UPI / SMS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_quick_paste")) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = SwissTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Paste any UPI SMS or notification text. Our Regex engine extracts the amount, merchant, and debit/credit type live.",
        style = MaterialTheme.typography.bodySmall,
        color = SwissTextSecondary
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Input field with Paste action
      OutlinedTextField(
        value = rawText,
        onValueChange = { rawText = it },
        placeholder = { Text("e.g. Paid ₹450 to Swiggy on Google Pay...") },
        trailingIcon = {
          IconButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
              if (!clip.isNullOrBlank()) {
                rawText = clip
              }
            },
            modifier = Modifier.testTag("clipboard_paste_button")
          ) {
            Icon(
              imageVector = Icons.Default.ContentPaste,
              contentDescription = "Paste from clipboard",
              tint = SwissDark
            )
          }
        },
        maxLines = 3,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quick_paste_input")
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Quick Sample Chips
      Text(
        text = "OR TRY A SAMPLE:",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 9.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sampleTexts) { sample ->
          Box(
            modifier = Modifier
              .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
              .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
              .clickable { rawText = sample }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = sample,
              style = MaterialTheme.typography.bodySmall,
              color = SwissDark,
              maxLines = 1,
              fontSize = 11.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Real-time Regex Parsed Card (Review Form Populated Automatically)
      AnimatedVisibility(
        visible = hasParsed,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        val isDebit = parsedType == TransactionType.DEBIT
        val parsedAmtDouble = parsedAmountText.toDoubleOrNull() ?: 0.0

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(if (isDebit) SwissCrimsonLight else Color(0xFFF3F4F6), RoundedCornerShape(10.dp))
            .border(1.dp, if (isDebit) SwissCrimson else SwissDark, RoundedCornerShape(10.dp))
            .padding(14.dp)
        ) {
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
                  .size(8.dp)
                  .background(if (isDebit) SwissCrimson else SwissDark, CircleShape)
              )
              Text(
                text = if (isDebit) "PARSED: DEDUCTION (-)" else "PARSED: DEPOSIT (+)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (isDebit) SwissCrimson else SwissDark,
                fontSize = 10.sp
              )
            }

            Box(
              modifier = Modifier
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, SwissBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "CHANNEL: UPI",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SwissDark,
                fontSize = 9.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Auto-populated editable fields for review
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Amount Field
            OutlinedTextField(
              value = parsedAmountText,
              onValueChange = { parsedAmountText = it },
              label = { Text("Amount (₹)", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SwissDark,
                unfocusedBorderColor = SwissBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f)
            )

            // Type Toggle
            Row(
              modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
                .padding(3.dp)
            ) {
              Box(
                modifier = Modifier
                  .background(
                    if (parsedType == TransactionType.DEBIT) SwissCrimson else Color.Transparent,
                    RoundedCornerShape(4.dp)
                  )
                  .clickable { parsedType = TransactionType.DEBIT }
                  .padding(horizontal = 10.dp, vertical = 8.dp)
              ) {
                Text(
                  text = "Debit",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (parsedType == TransactionType.DEBIT) Color.White else SwissDark
                )
              }
              Box(
                modifier = Modifier
                  .background(
                    if (parsedType == TransactionType.CREDIT) SwissDark else Color.Transparent,
                    RoundedCornerShape(4.dp)
                  )
                  .clickable { parsedType = TransactionType.CREDIT }
                  .padding(horizontal = 10.dp, vertical = 8.dp)
              ) {
                Text(
                  text = "Credit",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (parsedType == TransactionType.CREDIT) Color.White else SwissDark
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Merchant / Note Field
          OutlinedTextField(
            value = parsedMerchantText,
            onValueChange = { parsedMerchantText = it },
            label = { Text("Merchant / Sender Note", fontSize = 11.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SwissDark,
              unfocusedBorderColor = SwissBorder,
              focusedContainerColor = Color.White,
              unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
          )

          if (!parsedRefId.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "UPI Ref ID: $parsedRefId",
              style = MonospaceSmall,
              color = SwissTextSecondary,
              fontSize = 10.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Target Goal Selector
      if (goals.isNotEmpty()) {
        Text(
          text = "ASSIGN TO SAVINGS TARGET",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = SwissTextTertiary,
          fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(goals) { g ->
            val isSelected = linkedGoalId == g.id
            Box(
              modifier = Modifier
                .background(if (isSelected) SwissDark else Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
                .border(1.dp, if (isSelected) SwissDark else SwissBorder, RoundedCornerShape(6.dp))
                .clickable { linkedGoalId = g.id }
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = g.title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color.White else SwissDark,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // 1-Tap Save to Ledger Button
      val canSave = hasParsed && (parsedAmountText.toDoubleOrNull() ?: 0.0) > 0.0
      Button(
        onClick = {
          val amt = parsedAmountText.toDoubleOrNull()
          if (amt != null && amt > 0) {
            val signedAmount = if (parsedType == TransactionType.DEBIT) -amt else amt
            val today = LocalDate.now()
            val item = TransactionItem(
              goalId = linkedGoalId,
              amount = signedAmount,
              channel = Channel.UPI,
              note = "${parsedMerchantText.ifBlank { "UPI Transaction" }} (UPI)",
              timestamp = System.currentTimeMillis(),
              dateEpochDay = today.toEpochDay(),
              isAutoCaptured = true,
              upiAppName = "UPI",
              merchantOrSender = parsedMerchantText,
              upiRefId = parsedRefId,
              isConfirmed = true
            )
            onSaveTransaction(item)
            onDismiss()
          }
        },
        enabled = canSave,
        colors = ButtonDefaults.buttonColors(
          containerColor = SwissCrimson,
          contentColor = Color.White,
          disabledContainerColor = Color(0xFFE5E7EB),
          disabledContentColor = Color(0xFF9CA3AF)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_parsed_upi_button")
      ) {
        val amt = parsedAmountText.toDoubleOrNull()
        Text(
          text = if (canSave && amt != null) {
            "Save to Ledger (${CurrencyFormatter.formatRupee(amt)})"
          } else {
            "Paste Text to Parse & Save"
          },
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}

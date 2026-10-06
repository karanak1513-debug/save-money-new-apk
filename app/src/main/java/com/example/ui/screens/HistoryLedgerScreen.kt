package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionItem
import com.example.ui.theme.ChannelCash
import com.example.ui.theme.ChannelOther
import com.example.ui.theme.ChannelUpi
import com.example.ui.theme.MonospaceBody
import com.example.ui.theme.MonospaceHeadline
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.MonospaceSmall
import androidx.compose.ui.draw.clip
import com.example.ui.components.ambientMeshBackground
import com.example.ui.components.frostedGlass
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SectionHeaderMedium
import com.example.ui.theme.SlateHeader
import com.example.ui.theme.SwissAlpineGreen
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissHairline
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class LedgerFilter(val displayName: String) {
  ALL("ALL"),
  UPI("UPI"),
  CASH("CASH"),
  OTHER("OTHER"),
  FOOD("FOOD"),
  COMMUTE("TRANSIT"),
  BILLS("BILLS"),
  LEISURE("LEISURE"),
  CREDITS("CREDITS (+)"),
  DEBITS("DEBITS (-)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryLedgerScreen(
  transactions: List<TransactionItem>,
  onUpdateTransaction: (TransactionItem) -> Unit = {},
  onDeleteTransaction: (TransactionItem) -> Unit,
  onClearLedger: () -> Unit,
  initialChannelFilter: Channel? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val today = remember { LocalDate.now() }
  var currentMonth by remember { mutableStateOf(YearMonth.now()) }
  var selectedDayNumber by remember { mutableStateOf<Int?>(null) }
  var activeFilter by remember {
    mutableStateOf(
      when (initialChannelFilter) {
        Channel.UPI -> LedgerFilter.UPI
        Channel.CASH -> LedgerFilter.CASH
        Channel.OTHER -> LedgerFilter.OTHER
        else -> LedgerFilter.ALL
      }
    )
  }
  var showClearConfirmDialog by remember { mutableStateOf(false) }
  var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }
  var transactionToEdit by remember { mutableStateOf<TransactionItem?>(null) }

  // Dates in the current month that contain recorded entries
  val datesWithEntries = remember(transactions, currentMonth) {
    transactions.mapNotNull { tx ->
      val date = LocalDate.ofEpochDay(tx.dateEpochDay)
      if (date.year == currentMonth.year && date.monthValue == currentMonth.monthValue) {
        date.dayOfMonth
      } else null
    }.toSet()
  }

  // Filter transactions
  val filteredTransactions = remember(transactions, currentMonth, selectedDayNumber, activeFilter) {
    transactions.filter { tx ->
      val txDate = LocalDate.ofEpochDay(tx.dateEpochDay)

      val monthMatches = txDate.year == currentMonth.year && txDate.monthValue == currentMonth.monthValue
      if (!monthMatches) return@filter false

      if (selectedDayNumber != null && txDate.dayOfMonth != selectedDayNumber) {
        return@filter false
      }

      when (activeFilter) {
        LedgerFilter.ALL -> true
        LedgerFilter.UPI -> tx.channel == Channel.UPI
        LedgerFilter.CASH -> tx.channel == Channel.CASH
        LedgerFilter.OTHER -> tx.channel == Channel.OTHER
        LedgerFilter.FOOD -> tx.expenseCategory == ExpenseCategory.FOOD_ESSENTIALS
        LedgerFilter.COMMUTE -> tx.expenseCategory == ExpenseCategory.COMMUTE_FUEL
        LedgerFilter.BILLS -> tx.expenseCategory == ExpenseCategory.BILLS_UTILITIES
        LedgerFilter.LEISURE -> tx.expenseCategory == ExpenseCategory.DISCRETIONARY
        LedgerFilter.CREDITS -> tx.amount > 0
        LedgerFilter.DEBITS -> tx.amount < 0
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .border(0.75.dp, SwissBorder)
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("quick_export_bar")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = { exportToCsv(context, transactions) },
            modifier = Modifier.testTag("export_csv_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                tint = SwissDark,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = "EXPORT CSV",
                style = MonospaceMicro,
                color = SwissDark
              )
            }
          }

          Box(
            modifier = Modifier
              .width(0.75.dp)
              .height(16.dp)
              .background(SwissBorder)
          )

          TextButton(
            onClick = { showClearConfirmDialog = true },
            modifier = Modifier.testTag("clear_ledger_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = SwissCrimson,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = "PURGE LEDGER",
                style = MonospaceMicro,
                color = SwissCrimson
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("history_ledger_scroll"),
      contentPadding = PaddingValues(bottom = 24.dp)
    ) {
      // Masthead
      item {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
          Text(
            text = "TRANSACTION LEDGER",
            style = SectionHeaderMedium,
            color = SlateHeader,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "History Ledger",
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp
          )
        }
      }

      // Minimalist Calendar Strip
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .frostedGlass(shape = RoundedCornerShape(20.dp), elevation = 3.dp)
            .padding(vertical = 14.dp)
            .testTag("calendar_matrix_strip")
        ) {
          // Month Switcher Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)).uppercase(),
                style = MonospaceSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )

              if (selectedDayNumber != null) {
                Box(
                  modifier = Modifier
                    .background(SwissCrimsonLight, RoundedCornerShape(3.dp))
                    .clickable { selectedDayNumber = null }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "DAY $selectedDayNumber ✕",
                    style = MonospaceMicro,
                    color = SwissCrimson
                  )
                }
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(
                onClick = { currentMonth = currentMonth.minusMonths(1) },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ChevronLeft,
                  contentDescription = "Previous Month",
                  tint = SwissDark,
                  modifier = Modifier.size(18.dp)
                )
              }
              IconButton(
                onClick = { currentMonth = currentMonth.plusMonths(1) },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = "Next Month",
                  tint = SwissDark,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Horizontal Date Strip
          val daysInMonth = currentMonth.lengthOfMonth()
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(daysInMonth) { index ->
              val dayNum = index + 1
              val dayDate = currentMonth.atDay(dayNum)
              val dayOfWeekLabel = dayDate.dayOfWeek.name.take(1)
              val isSelected = selectedDayNumber == dayNum
              val isTodayDate = (dayDate == today)
              val hasEntries = datesWithEntries.contains(dayNum)

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .width(36.dp)
                  .background(
                    color = when {
                      isSelected -> SwissDark
                      isTodayDate -> Color(0xFFF3F3F1)
                      else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(6.dp)
                  )
                  .border(
                    width = 0.75.dp,
                    color = when {
                      isSelected -> SwissDark
                      isTodayDate -> SwissCrimson
                      else -> SwissBorder
                    },
                    shape = RoundedCornerShape(6.dp)
                  )
                  .clickable {
                    selectedDayNumber = if (selectedDayNumber == dayNum) null else dayNum
                  }
                  .padding(vertical = 7.dp)
                  .testTag("calendar_day_$dayNum")
              ) {
                Text(
                  text = dayOfWeekLabel,
                  style = MonospaceMicro,
                  color = if (isSelected) Color.White.copy(alpha = 0.7f) else SwissTextSecondary,
                  fontSize = 9.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = dayNum.toString().padStart(2, '0'),
                  style = MonospaceSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                  fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(3.dp))

                if (hasEntries || isTodayDate || isSelected) {
                  Box(
                    modifier = Modifier
                      .size(4.dp)
                      .background(SwissCrimson, CircleShape)
                  )
                } else {
                  Spacer(modifier = Modifier.size(4.dp))
                }
              }
            }
          }
        }
      }

      // Filter Segmented Control
      item {
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          contentPadding = PaddingValues(horizontal = 18.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(LedgerFilter.values()) { filter ->
            val isSelected = activeFilter == filter
            Box(
              modifier = Modifier
                .background(
                  color = if (isSelected) SwissDark else MaterialTheme.colorScheme.surface,
                  shape = RoundedCornerShape(4.dp)
                )
                .border(
                  width = 0.75.dp,
                  color = if (isSelected) SwissDark else SwissBorder,
                  shape = RoundedCornerShape(4.dp)
                )
                .clickable { activeFilter = filter }
                .padding(horizontal = 11.dp, vertical = 7.dp)
                .testTag("filter_segment_${filter.name.lowercase()}")
            ) {
              Text(
                text = filter.displayName,
                style = MonospaceMicro,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Transaction History Feed
      if (filteredTransactions.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp, vertical = 24.dp)
              .frostedGlass(shape = RoundedCornerShape(20.dp), elevation = 2.dp)
              .padding(28.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "NO TRANSACTIONS MATCHED",
                style = SectionHeaderMedium,
                color = SlateHeader,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Zero records match active filter criteria.",
                style = MaterialTheme.typography.bodySmall,
                color = SwissTextSecondary,
                fontSize = 12.sp
              )
            }
          }
        }
      } else {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp)
              .frostedGlass(shape = RoundedCornerShape(24.dp), elevation = 4.dp)
              .testTag("transaction_feed_table")
          ) {
            filteredTransactions.forEachIndexed { index, tx ->
              val txDate = LocalDate.ofEpochDay(tx.dateEpochDay)
              val dateTag = txDate.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)).uppercase()
              val isCredit = tx.amount > 0

              val channelColor = when (tx.channel) {
                Channel.UPI -> ChannelUpi
                Channel.CASH -> ChannelCash
                Channel.OTHER -> ChannelOther
              }

              // Tabular Row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { transactionToEdit = tx }
                  .padding(horizontal = 14.dp, vertical = 12.dp)
                  .testTag("ledger_row_${tx.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Left: Date, Channel badge, Memo
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text(
                    text = dateTag,
                    style = MonospaceMicro,
                    color = SwissDark
                  )

                  Box(
                    modifier = Modifier
                      .background(
                        color = when (tx.channel) {
                          Channel.UPI -> Color(0xFFF3F3F1)
                          Channel.CASH -> SwissCrimsonLight
                          Channel.OTHER -> Color(0xFFF3F3F1)
                        },
                        shape = RoundedCornerShape(3.dp)
                      )
                      .border(
                        0.75.dp,
                        when (tx.channel) {
                          Channel.UPI -> SwissBorder
                          Channel.CASH -> SwissCrimson
                          Channel.OTHER -> SwissBorder
                        },
                        RoundedCornerShape(3.dp)
                      )
                      .padding(horizontal = 4.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = tx.channel.displayName.uppercase(),
                      style = MonospaceMicro,
                      color = channelColor,
                      fontSize = 8.sp
                    )
                  }

                  // Smart Category Tag Pill
                  Box(
                    modifier = Modifier
                      .background(Color(0xFFEEEEEC), RoundedCornerShape(3.dp))
                      .border(0.75.dp, SwissBorder, RoundedCornerShape(3.dp))
                      .padding(horizontal = 4.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = tx.expenseCategory.shortTag,
                      style = MonospaceMicro,
                      color = SwissTextSecondary,
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  Text(
                    text = tx.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                }

                // Right: Amount in Monospace + Delete Trigger
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = CurrencyFormatter.formatRupee(tx.amount, showSign = true),
                    style = MonospaceBody,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) SwissAlpineGreen else SwissCrimson,
                    fontSize = 13.sp
                  )

                  IconButton(
                    onClick = { transactionToDelete = tx },
                    modifier = Modifier.size(26.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.DeleteOutline,
                      contentDescription = "Delete",
                      tint = SwissTextTertiary,
                      modifier = Modifier.size(15.dp)
                    )
                  }
                }
              }

              // Hairline separator
              if (index < filteredTransactions.lastIndex) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(0.75.dp)
                    .background(SwissHairline)
                )
              }
            }
          }
        }
      }
    }
  }

  // Edit Transaction Bottom Sheet
  if (transactionToEdit != null) {
    EditTransactionModal(
      item = transactionToEdit!!,
      onDismiss = { transactionToEdit = null },
      onSave = { updated ->
        onUpdateTransaction(updated)
        transactionToEdit = null
      }
    )
  }

  // Delete Single Transaction Dialog
  if (transactionToDelete != null) {
    AlertDialog(
      onDismissRequest = { transactionToDelete = null },
      title = {
        Text("Remove Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      },
      text = {
        Text("Are you sure you want to remove this record (${transactionToDelete?.note} - ${CurrencyFormatter.formatRupee(transactionToDelete?.amount ?: 0.0, showSign = true)})?")
      },
      confirmButton = {
        Button(
          onClick = {
            transactionToDelete?.let { onDeleteTransaction(it) }
            transactionToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = SwissCrimson),
          shape = RoundedCornerShape(4.dp)
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { transactionToDelete = null }) {
          Text("Cancel", color = SwissDark)
        }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(8.dp)
    )
  }

  // Clear Ledger Dialog
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = {
        Text("Purge Entire Ledger?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      },
      text = {
        Text("This action will permanently delete all transaction history entries. Your active savings targets will remain intact.")
      },
      confirmButton = {
        Button(
          onClick = {
            onClearLedger()
            showClearConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = SwissCrimson),
          shape = RoundedCornerShape(4.dp)
        ) {
          Text("Purge All", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Cancel", color = SwissDark)
        }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(8.dp)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionModal(
  item: TransactionItem,
  onDismiss: () -> Unit,
  onSave: (TransactionItem) -> Unit
) {
  var noteText by remember { mutableStateOf(item.note) }
  var amountText by remember {
    mutableStateOf(kotlin.math.abs(item.amount).let { if (it % 1 == 0.0) it.toLong().toString() else it.toString() })
  }

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
        .padding(22.dp)
        .testTag("edit_transaction_modal")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "RECORD MODIFICATION",
            style = MonospaceMicro,
            color = SwissCrimson,
            letterSpacing = 1.2.sp
          )
          Text(
            text = "Edit Transaction",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = SwissTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "AMOUNT (₹)",
        style = MonospaceMicro,
        color = SwissTextTertiary,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = amountText,
        onValueChange = { input ->
          if (input.all { it.isDigit() || it == '.' }) amountText = input
        },
        leadingIcon = {
          Text(
            text = "₹",
            style = MonospaceHeadline,
            color = SwissDark,
            modifier = Modifier.padding(start = 12.dp)
          )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "DESCRIPTION / MEMO",
        style = MonospaceMicro,
        color = SwissTextTertiary,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = {
          val parsedAmt = amountText.toDoubleOrNull() ?: kotlin.math.abs(item.amount)
          val signed = if (item.amount < 0) -parsedAmt else parsedAmt
          onSave(item.copy(note = noteText.trim(), amount = signed))
        },
        colors = ButtonDefaults.buttonColors(containerColor = SwissDark),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
      ) {
        Text("SAVE CHANGES", style = MonospaceMicro, letterSpacing = 1.2.sp)
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

private fun exportToCsv(context: Context, transactions: List<TransactionItem>) {
  val csvBuilder = StringBuilder()
  csvBuilder.append("ID,Date,Channel,Amount,Type,Source,RefID,Note\n")

  transactions.forEach { tx ->
    val dateStr = LocalDate.ofEpochDay(tx.dateEpochDay).toString()
    val typeStr = if (tx.amount > 0) "CREDIT" else "DEBIT"
    val sourceStr = if (tx.isAutoCaptured) tx.upiAppName ?: "AUTO_UPI" else "MANUAL"
    val safeNote = tx.note.replace(",", " ")
    val refStr = tx.upiRefId ?: ""
    csvBuilder.append("${tx.id},$dateStr,${tx.channel.name},${tx.amount},$typeStr,$sourceStr,$refStr,$safeNote\n")
  }

  val csvData = csvBuilder.toString()

  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText("Sanchay Ledger CSV", csvData)
  clipboard.setPrimaryClip(clip)

  val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, "Sanchay Savings Ledger Export")
    putExtra(Intent.EXTRA_TEXT, csvData)
  }

  context.startActivity(Intent.createChooser(shareIntent, "Share Sanchay Ledger CSV"))
  Toast.makeText(context, "CSV copied to clipboard & share ready", Toast.LENGTH_SHORT).show()
}

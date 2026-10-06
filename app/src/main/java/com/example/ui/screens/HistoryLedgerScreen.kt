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
import com.example.data.model.TransactionItem
import com.example.ui.theme.ChannelCash
import com.example.ui.theme.ChannelOther
import com.example.ui.theme.ChannelUpi
import com.example.ui.theme.MonospaceBody
import com.example.ui.theme.MonospaceHeadline
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class LedgerFilter(val displayName: String) {
  ALL("All"),
  UPI("UPI"),
  CASH("Cash"),
  OTHER("Other"),
  CREDITS("Credits (+)"),
  DEBITS("Debits (-)")
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

      // Month match
      val monthMatches = txDate.year == currentMonth.year && txDate.monthValue == currentMonth.monthValue
      if (!monthMatches) return@filter false

      // Optional Day match
      if (selectedDayNumber != null && txDate.dayOfMonth != selectedDayNumber) {
        return@filter false
      }

      // Filter Segment Match
      when (activeFilter) {
        LedgerFilter.ALL -> true
        LedgerFilter.UPI -> tx.channel == Channel.UPI
        LedgerFilter.CASH -> tx.channel == Channel.CASH
        LedgerFilter.OTHER -> tx.channel == Channel.OTHER
        LedgerFilter.CREDITS -> tx.amount > 0
        LedgerFilter.DEBITS -> tx.amount < 0
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      // Quick Export Bar: "Export to CSV" | "Clear Ledger"
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, SwissBorder)
          .padding(horizontal = 16.dp, vertical = 8.dp)
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
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Export to CSV",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SwissDark
              )
            }
          }

          Box(
            modifier = Modifier
              .width(1.dp)
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
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Clear Ledger",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
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
      // Title
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Text(
            text = "VERIFIED TIMELINE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissCrimson,
            letterSpacing = 1.2.sp,
            fontSize = 10.sp
          )
          Text(
            text = "History Ledger",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.3).sp
          )
        }
      }

      // Minimal calendar strip (Month switcher with active date highlighted)
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, SwissBorder)
            .padding(vertical = 12.dp)
            .testTag("calendar_matrix_strip")
        ) {
          // Month Switcher Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
              )

              if (selectedDayNumber != null) {
                Box(
                  modifier = Modifier
                    .background(SwissCrimsonLight, RoundedCornerShape(4.dp))
                    .clickable { selectedDayNumber = null }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "Day $selectedDayNumber ✕",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SwissCrimson,
                    fontSize = 10.sp
                  )
                }
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(
                onClick = { currentMonth = currentMonth.minusMonths(1) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ChevronLeft,
                  contentDescription = "Previous Month",
                  tint = SwissDark,
                  modifier = Modifier.size(20.dp)
                )
              }
              IconButton(
                onClick = { currentMonth = currentMonth.plusMonths(1) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = "Next Month",
                  tint = SwissDark,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Horizontal Date Strip
          val daysInMonth = currentMonth.lengthOfMonth()
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
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
                  .width(38.dp)
                  .background(
                    color = when {
                      isSelected -> SwissDark
                      isTodayDate -> Color(0xFFF3F4F6)
                      else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(8.dp)
                  )
                  .border(
                    width = 1.dp,
                    color = when {
                      isSelected -> SwissDark
                      isTodayDate -> SwissCrimson
                      else -> SwissBorder
                    },
                    shape = RoundedCornerShape(8.dp)
                  )
                  .clickable {
                    selectedDayNumber = if (selectedDayNumber == dayNum) null else dayNum
                  }
                  .padding(vertical = 8.dp)
                  .testTag("calendar_day_$dayNum")
              ) {
                Text(
                  text = dayOfWeekLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSelected) Color.White.copy(alpha = 0.7f) else SwissTextSecondary,
                  fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = dayNum.toString().padStart(2, '0'),
                  style = MonospaceSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                  fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Highlight dot for dates with transactions or today
                if (hasEntries || isTodayDate || isSelected) {
                  Box(
                    modifier = Modifier
                      .size(5.dp)
                      .background(SwissCrimson, CircleShape)
                  )
                } else {
                  Spacer(modifier = Modifier.size(5.dp))
                }
              }
            }
          }
        }
      }

      // Filter Segmented Control: [All] [UPI] [Cash] [Other] [Credits (+)] [Debits (-)]
      item {
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(horizontal = 16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(LedgerFilter.values()) { filter ->
            val isSelected = activeFilter == filter
            Box(
              modifier = Modifier
                .background(
                  color = if (isSelected) SwissDark else MaterialTheme.colorScheme.surface,
                  shape = RoundedCornerShape(6.dp)
                )
                .border(
                  width = 1.dp,
                  color = if (isSelected) SwissDark else SwissBorder,
                  shape = RoundedCornerShape(6.dp)
                )
                .clickable { activeFilter = filter }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("filter_segment_${filter.name.lowercase()}")
            ) {
              Text(
                text = filter.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Transaction History Feed
      if (filteredTransactions.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 32.dp)
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
              .border(1.dp, SwissBorder, RoundedCornerShape(12.dp))
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "NO TRANSACTIONS YET",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SwissTextTertiary,
                letterSpacing = 1.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "No records match the current filter. Add your first entry below.",
                style = MaterialTheme.typography.bodySmall,
                color = SwissTextSecondary
              )
            }
          }
        }
      } else {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
              .border(1.dp, SwissBorder, RoundedCornerShape(12.dp))
              .testTag("transaction_feed_table")
          ) {
            filteredTransactions.forEachIndexed { index, tx ->
              val txDate = LocalDate.ofEpochDay(tx.dateEpochDay)
              val dateTag = txDate.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))
              val isCredit = tx.amount > 0

              val channelColor = when (tx.channel) {
                Channel.UPI -> ChannelUpi
                Channel.CASH -> ChannelCash
                Channel.OTHER -> ChannelOther
              }

              // Tabular row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { transactionToEdit = tx }
                  .padding(horizontal = 14.dp, vertical = 12.dp)
                  .testTag("ledger_row_${tx.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Left: Date tag, Channel badge, Note
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text(
                    text = dateTag,
                    style = MonospaceSmall,
                    fontWeight = FontWeight.Bold,
                    color = SwissDark,
                    fontSize = 11.sp
                  )

                  Box(
                    modifier = Modifier
                      .background(
                        color = when (tx.channel) {
                          Channel.UPI -> Color(0xFFF3F4F6)
                          Channel.CASH -> SwissCrimsonLight
                          Channel.OTHER -> Color(0xFFF3F4F6)
                        },
                        shape = RoundedCornerShape(4.dp)
                      )
                      .border(
                        1.dp,
                        when (tx.channel) {
                          Channel.UPI -> SwissBorder
                          Channel.CASH -> SwissCrimson
                          Channel.OTHER -> SwissBorder
                        },
                        RoundedCornerShape(4.dp)
                      )
                      .padding(horizontal = 5.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = tx.channel.displayName.uppercase(),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = channelColor,
                      fontSize = 8.sp
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

                // Right: Amount & Delete icon
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = CurrencyFormatter.formatRupee(tx.amount, showSign = true),
                    style = MonospaceBody,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) SwissDark else SwissCrimson,
                    fontSize = 13.sp
                  )

                  IconButton(
                    onClick = { transactionToDelete = tx },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.DeleteOutline,
                      contentDescription = "Delete",
                      tint = SwissTextTertiary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }

              // 1px border separator
              if (index < filteredTransactions.lastIndex) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE5E7EB))
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
        Text("Delete Entry", fontWeight = FontWeight.Bold)
      },
      text = {
        Text("Are you sure you want to remove this entry (${transactionToDelete?.note} - ${CurrencyFormatter.formatRupee(transactionToDelete?.amount ?: 0.0, showSign = true)})?")
      },
      confirmButton = {
        Button(
          onClick = {
            transactionToDelete?.let { onDeleteTransaction(it) }
            transactionToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = SwissCrimson)
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { transactionToDelete = null }) {
          Text("Cancel", color = SwissDark)
        }
      },
      containerColor = MaterialTheme.colorScheme.surface
    )
  }

  // Clear Ledger Dialog
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = {
        Text("Clear Entire Ledger?", fontWeight = FontWeight.Bold)
      },
      text = {
        Text("This will permanently remove all transaction history entries. Your active savings targets will remain intact.")
      },
      confirmButton = {
        Button(
          onClick = {
            onClearLedger()
            showClearConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = SwissCrimson)
        ) {
          Text("Clear All", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Cancel", color = SwissDark)
        }
      },
      containerColor = MaterialTheme.colorScheme.surface
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
        .padding(20.dp)
        .testTag("edit_transaction_modal")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "EDIT TRANSACTION",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissCrimson,
            letterSpacing = 1.2.sp,
            fontSize = 10.sp
          )
          Text(
            text = "Modify Record",
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
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "NOTE / DESCRIPTION",
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
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          val parsedAmt = amountText.toDoubleOrNull() ?: kotlin.math.abs(item.amount)
          val signed = if (item.amount < 0) -parsedAmt else parsedAmt
          onSave(item.copy(note = noteText.trim(), amount = signed))
        },
        colors = ButtonDefaults.buttonColors(containerColor = SwissDark),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
      ) {
        Text("Save Changes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(10.dp))
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

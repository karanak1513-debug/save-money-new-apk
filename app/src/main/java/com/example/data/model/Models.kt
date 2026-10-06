package com.example.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class Channel(val displayName: String, val tag: String) {
  UPI("UPI", "UPI"),
  CASH("Cash", "CASH"),
  OTHER("Other", "OTHER")
}

enum class FrequencyPref(val displayName: String) {
  DAILY("Daily"),
  WEEKLY("Weekly"),
  MONTHLY("Monthly")
}

enum class TransactionType {
  CREDIT, // savings / deposit added (+)
  DEBIT   // spending / deduction (-)
}

data class Goal(
  val id: Long = 0,
  val title: String,
  val targetAmount: Double,
  val savedAmount: Double = 0.0,
  val deadlineEpochDay: Long, // LocalDate.toEpochDay()
  val frequencyPref: FrequencyPref = FrequencyPref.DAILY,
  val isPrimary: Boolean = false,
  val createdAtEpochDay: Long = 0
) {
  val remainingAmount: Double
    get() = (targetAmount - savedAmount).coerceAtLeast(0.0)

  val progressFraction: Float
    get() = if (targetAmount > 0) ((savedAmount / targetAmount).toFloat()).coerceIn(0f, 1f) else 0f
}

data class TransactionItem(
  val id: Long = 0,
  val goalId: Long? = null,
  val amount: Double, // positive for credit (+), negative for debit (-)
  val channel: Channel,
  val note: String,
  val timestamp: Long,
  val dateEpochDay: Long,
  val isAutoCaptured: Boolean = false,
  val upiAppName: String? = null,
  val merchantOrSender: String? = null,
  val upiRefId: String? = null,
  val isConfirmed: Boolean = true
)

data class ChannelBreakdown(
  val channel: Channel,
  val amount: Double,
  val percentage: Float
)

data class DailyBarData(
  val dayLabel: String, // "M", "T", "W", "T", "F", "S", "S"
  val dateStr: String,  // "05 Oct"
  val amount: Double,
  val isToday: Boolean
)

data class PacingInfo(
  val dailyPace: Double,
  val weeklyPace: Double,
  val monthlyPace: Double,
  val daysRemaining: Long,
  val isExpired: Boolean = false,
  val isCompleted: Boolean = false
)

object CurrencyFormatter {
  /**
   * Formats Indian Rupee values with Indian numbering system (e.g. ₹1,50,000).
   */
  fun formatRupee(amount: Double, includeSymbol: Boolean = true, showSign: Boolean = false): String {
    val isNegative = amount < 0
    val absAmount = kotlin.math.abs(amount).toLong()

    val formattedNumber = formatIndianNumber(absAmount)
    val symbol = if (includeSymbol) "₹" else ""
    return when {
      showSign && isNegative -> "-$symbol$formattedNumber"
      showSign && !isNegative -> "+$symbol$formattedNumber"
      isNegative -> "-$symbol$formattedNumber"
      else -> "$symbol$formattedNumber"
    }
  }

  fun formatCompactRupee(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    return when {
      absAmount >= 10000000 -> "₹${(absAmount / 10000000).toInt()}Cr"
      absAmount >= 100000 -> "₹${(absAmount / 100000).toInt()}L"
      absAmount >= 1000 -> "₹${(absAmount / 1000).toInt()}k"
      else -> "₹${absAmount.toInt()}"
    }
  }

  private fun formatIndianNumber(number: Long): String {
    if (number == 0L) return "0"
    val s = number.toString()
    if (s.length <= 3) return s

    val lastThree = s.substring(s.length - 3)
    var rest = s.substring(0, s.length - 3)
    val parts = mutableListOf<String>()

    while (rest.length > 2) {
      parts.add(0, rest.substring(rest.length - 2))
      rest = rest.substring(0, rest.length - 2)
    }
    if (rest.isNotEmpty()) {
      parts.add(0, rest)
    }

    return parts.joinToString(",") + "," + lastThree
  }
}

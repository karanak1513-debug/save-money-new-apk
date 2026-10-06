package com.example.service

import com.example.data.model.TransactionType

data class ParsedUpiNotification(
  val amount: Double,
  val type: TransactionType, // CREDIT or DEBIT
  val merchantOrPerson: String,
  val upiRefId: String?,
  val appName: String,
  val appPackage: String,
  val rawText: String,
  val timestamp: Long = System.currentTimeMillis()
)

object UpiParserEngine {

  const val PACKAGE_GPAY = "com.google.android.apps.nbu.paisa.user"
  const val PACKAGE_PHONEPE = "com.phonepe.app"
  const val PACKAGE_PAYTM = "net.one97.paytm"

  private val SUPPORTED_PACKAGES = setOf(
    PACKAGE_GPAY,
    PACKAGE_PHONEPE,
    PACKAGE_PAYTM,
    "com.google.android.apps.messaging",
    "com.samsung.android.messaging",
    "com.android.mms"
  )

  fun isSupportedPackage(packageName: String): Boolean {
    return packageName in SUPPORTED_PACKAGES ||
      packageName.contains("messaging", ignoreCase = true) ||
      packageName.contains("sms", ignoreCase = true) ||
      packageName.contains("upi", ignoreCase = true) ||
      packageName.contains("paisa", ignoreCase = true) ||
      packageName.contains("paytm", ignoreCase = true) ||
      packageName.contains("phonepe", ignoreCase = true)
  }

  fun getAppNameForPackage(packageName: String): String {
    return when {
      packageName == PACKAGE_GPAY || packageName.contains("paisa") -> "Google Pay"
      packageName == PACKAGE_PHONEPE || packageName.contains("phonepe") -> "PhonePe"
      packageName == PACKAGE_PAYTM || packageName.contains("paytm") -> "Paytm"
      else -> "Bank SMS"
    }
  }

  // Regex patterns
  private val amountPatternPrefix = Regex("""(?i)(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)""")
  private val amountPatternSuffix = Regex("""(?i)([\d,]+(?:\.\d{1,2})?)\s*(?:₹|Rs\.?|INR)""")

  private val debitKeywords = Regex("""(?i)\b(paid\s+to|sent\s+to|debited|transferred\s+to|spent|payment\s+to|withdrawn|deducted|debited\s+by)\b""")
  private val creditKeywords = Regex("""(?i)\b(received\s+from|received|credited|added\s+to|refund|cashback|deposit|credited\s+with)\b""")

  private val merchantPaidToPattern = Regex(
    """(?i)(?:paid\s+to|sent\s+to|payment\s+to|transferred\s+to|by\s+UPI\s+to|to)\s+([A-Za-z0-9\s&'.]{2,30}?)(?:\s+(?:using|on|via|ref|upi|utr|from|\.|successfully|,|$))"""
  )

  private val merchantReceivedFromPattern = Regex(
    """(?i)(?:received\s+from|credited\s+by|from|by\s+UPI/)\s*([A-Za-z0-9\s&'.]{2,30}?)(?:\s+(?:on|via|ref|upi|utr|\.|successfully|,|/|$))"""
  )

  private val refIdPattern = Regex(
    """(?i)(?:UPI\s+Ref(?:\s+no)?[:\s]+|UTR[:\s]+|Ref(?:\s+no)?[:\s/]+|(?:Transaction|Txn)\s+ID[:\s]+|Ref:\s*)([A-Za-z0-9]{8,24})"""
  )

  /**
   * Main parsing entry point. Combines title and body text and parses out UPI transaction components.
   */
  fun parse(packageName: String, title: String?, text: String?): ParsedUpiNotification? {
    val fullText = buildString {
      if (!title.isNullOrBlank()) append(title).append(" ")
      if (!text.isNullOrBlank()) append(text)
    }.trim()

    if (fullText.isBlank()) return null

    // 1. Amount Extraction
    val amount = extractAmount(fullText) ?: return null

    // 2. Transaction Nature (Credit vs Debit)
    val type = determineTransactionType(fullText)

    // 3. Merchant / Person extraction
    val merchant = extractMerchantOrPerson(fullText, type)

    // 4. UPI Ref ID / UTR extraction
    val refId = extractRefId(fullText)

    val appName = getAppNameForPackage(packageName)

    return ParsedUpiNotification(
      amount = amount,
      type = type,
      merchantOrPerson = merchant,
      upiRefId = refId,
      appName = appName,
      appPackage = packageName,
      rawText = fullText
    )
  }

  fun extractAmount(text: String): Double? {
    val matchPrefix = amountPatternPrefix.find(text)
    if (matchPrefix != null) {
      val raw = matchPrefix.groupValues[1].replace(",", "").trim()
      val parsed = raw.toDoubleOrNull()
      if (parsed != null && parsed > 0) return parsed
    }

    val matchSuffix = amountPatternSuffix.find(text)
    if (matchSuffix != null) {
      val raw = matchSuffix.groupValues[1].replace(",", "").trim()
      val parsed = raw.toDoubleOrNull()
      if (parsed != null && parsed > 0) return parsed
    }

    return null
  }

  fun determineTransactionType(text: String): TransactionType {
    val hasDebit = debitKeywords.containsMatchIn(text)
    val hasCredit = creditKeywords.containsMatchIn(text)

    return when {
      hasDebit && !hasCredit -> TransactionType.DEBIT
      hasCredit && !hasDebit -> TransactionType.CREDIT
      hasCredit -> TransactionType.CREDIT
      else -> TransactionType.DEBIT // Default to debit for UPI payments
    }
  }

  fun extractMerchantOrPerson(text: String, type: TransactionType): String {
    if (type == TransactionType.CREDIT) {
      val match = merchantReceivedFromPattern.find(text)
      if (match != null) {
        val candidate = match.groupValues[1].trim().removeSuffix(".")
        if (candidate.isNotBlank() && candidate.length > 1) return candidate
      }
    } else {
      val match = merchantPaidToPattern.find(text)
      if (match != null) {
        val candidate = match.groupValues[1].trim().removeSuffix(".")
        if (candidate.isNotBlank() && candidate.length > 1) return candidate
      }
    }

    // Secondary fallback heuristics
    return when (type) {
      TransactionType.DEBIT -> "UPI Merchant"
      TransactionType.CREDIT -> "UPI Sender"
    }
  }

  fun extractRefId(text: String): String? {
    val match = refIdPattern.find(text)
    return match?.groupValues?.get(1)?.trim()
  }
}

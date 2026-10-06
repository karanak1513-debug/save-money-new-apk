package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.local.SanchayDatabase
import com.example.data.local.TransactionEntity
import com.example.data.model.Channel
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.LocalDate
import java.util.Collections
import java.util.LinkedHashSet
import java.util.UUID

/**
 * Data model for captured and parsed UPI transactions.
 */
data class UpiTransaction(
  val id: String,
  val amount: Double,
  val type: TransactionType,
  val merchant: String,
  val sourceApp: String,
  val timestamp: Long
)

/**
 * Production-ready Android NotificationListenerService for real-time UPI transaction capture.
 */
class UpiNotificationService : NotificationListenerService() {

  companion object {
    private const val TAG = "UpiNotificationService"

    // Supported target application packages
    const val PACKAGE_PHONEPE = "com.phonepe.app"
    const val PACKAGE_GPAY = "com.google.android.apps.nbu.paisa.user"
    const val PACKAGE_PAYTM = "net.one97.paytm"
    const val PACKAGE_BHIM = "in.org.npci.upiapp"
    const val PACKAGE_GOOGLE_MESSAGING = "com.google.android.apps.messaging"
    const val PACKAGE_SAMSUNG_MESSAGING = "com.samsung.android.messaging"

    val TARGET_PACKAGES = setOf(
      PACKAGE_PHONEPE,
      PACKAGE_GPAY,
      PACKAGE_PAYTM,
      PACKAGE_BHIM,
      PACKAGE_GOOGLE_MESSAGING,
      PACKAGE_SAMSUNG_MESSAGING
    )

    // Global SharedFlow emitting parsed transactions across the app
    private val _transactionFlow = MutableSharedFlow<UpiTransaction>(extraBufferCapacity = 64)
    val transactionFlow: SharedFlow<UpiTransaction> = _transactionFlow.asSharedFlow()

    // Deduplication tracking: Set of computed transaction hashes (amount + merchant + minute)
    private val deduplicationCache = Collections.synchronizedSet(LinkedHashSet<String>())
    private const val MAX_DEDUPLICATION_CACHE_SIZE = 500

    /**
     * Regex for extracting amount formats:
     * Handles ₹500, Rs. 500, Rs 500.00, INR 1,500.50, 500 Rs, 1,500.50 INR
     */
    private val amountRegex = Regex(
      """(?i)(?:(?:rs|inr|₹)\.?\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:inr|rs|₹))"""
    )

    // Strict transaction nature keywords
    private val debitKeywords = Regex(
      """(?i)\b(paid|sent\s+to|debited|transferred\s+to|spent|purchase)\b"""
    )
    private val creditKeywords = Regex(
      """(?i)\b(received|credited|added|deposited|cashback)\b"""
    )

    // Merchant / recipient extraction regex
    private val merchantDebitRegex = Regex(
      """(?i)(?:paid\s+to|sent\s+to|transferred\s+to|payment\s+(?:of\s+[^\s]+\s+)?to|purchase\s+at|to)\s+([A-Za-z0-9\s&'.@_-]{2,32}?)(?:\s+(?:using|on|via|ref|upi|utr|from|with|\.|successfully|,|/|$))"""
    )
    private val merchantCreditRegex = Regex(
      """(?i)(?:received\s+from|credited\s+by|from|by\s+UPI/|vpa)\s+([A-Za-z0-9\s&'.@_-]{2,32}?)(?:\s+(?:on|via|ref|upi|utr|\.|successfully|,|/|$))"""
    )

    /**
     * Checks if the NotificationListenerService permission is granted by the user in system settings.
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
      val cn = ComponentName(context, UpiNotificationService::class.java)
      val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
      return flat != null && flat.contains(cn.flattenToString())
    }

    /**
     * Redirects the user directly to the Android Notification Listener Settings page.
     */
    fun openNotificationListenerSettings(context: Context) {
      val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }

    /**
     * Robust transaction parser parsing notification content against supported UPI app and SMS payloads.
     */
    fun parseTransaction(packageName: String, title: String?, text: String?): UpiTransaction? {
      val combinedText = buildString {
        if (!title.isNullOrBlank()) append(title).append(" ")
        if (!text.isNullOrBlank()) append(text)
      }.trim()

      if (combinedText.isBlank()) return null

      // 1. Amount Extraction
      val amountMatch = amountRegex.find(combinedText) ?: return null
      val rawAmountStr = amountMatch.groupValues[1].ifEmpty { amountMatch.groupValues[2] }.replace(",", "").trim()
      val amount = rawAmountStr.toDoubleOrNull() ?: return null
      if (amount <= 0.0) return null

      // 2. Transaction Type Classification
      val hasDebit = debitKeywords.containsMatchIn(combinedText)
      val hasCredit = creditKeywords.containsMatchIn(combinedText)
      val type = when {
        hasDebit && !hasCredit -> TransactionType.DEBIT
        hasCredit && !hasDebit -> TransactionType.CREDIT
        hasCredit -> TransactionType.CREDIT
        else -> TransactionType.DEBIT
      }

      // 3. Merchant / Recipient Extraction
      val merchant = extractMerchant(combinedText, type)

      // 4. Source App Display Name
      val sourceApp = when (packageName) {
        PACKAGE_PHONEPE -> "PhonePe"
        PACKAGE_GPAY -> "Google Pay"
        PACKAGE_PAYTM -> "Paytm"
        PACKAGE_BHIM -> "BHIM UPI"
        PACKAGE_GOOGLE_MESSAGING, PACKAGE_SAMSUNG_MESSAGING -> "Bank SMS"
        else -> "UPI"
      }

      val timestamp = System.currentTimeMillis()
      val id = UUID.randomUUID().toString()

      return UpiTransaction(
        id = id,
        amount = amount,
        type = type,
        merchant = merchant,
        sourceApp = sourceApp,
        timestamp = timestamp
      )
    }

    private fun extractMerchant(text: String, type: TransactionType): String {
      val pattern = if (type == TransactionType.CREDIT) {
        Regex("""(?i)\b(?:received\s+from|credited\s+by|from|by\s+upi/|vpa)\s+([A-Za-z0-9&'@_.-]+(?:\s+[A-Za-z0-9&'@_.-]+){0,3})""")
      } else {
        Regex("""(?i)\b(?:paid\s+to|sent\s+to|transferred\s+to|purchase\s+at|payment\s+to|to|at)\s+([A-Za-z0-9&'@_.-]+(?:\s+[A-Za-z0-9&'@_.-]+){0,3})""")
      }

      val match = pattern.find(text)
      if (match != null) {
        val rawCandidate = match.groupValues[1].trim().removeSuffix(".")
        val cleaned = rawCandidate.split(
          Regex("""(?i)\s+\b(on|using|via|ref|upi|utr|from|with|successfully|for|a/c)\b""")
        )[0].trim().removeSuffix(".")

        if (cleaned.length >= 2) {
          return cleaned
        }
      }

      return if (type == TransactionType.CREDIT) "UPI Sender" else "UPI Merchant"
    }

    /**
     * Generates a unique deduplication hash based on amount, merchant, and minute timestamp.
     */
    fun generateDeduplicationKey(amount: Double, merchant: String, timestamp: Long): String {
      val minuteBucket = timestamp / 60000L
      val rawKey = "${"%.2f".format(amount)}:${merchant.trim().lowercase()}:$minuteBucket"
      val digest = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray())
      return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Checks if transaction is duplicate within the minute window.
     */
    fun isDuplicate(transaction: UpiTransaction): Boolean {
      val key = generateDeduplicationKey(transaction.amount, transaction.merchant, transaction.timestamp)
      synchronized(deduplicationCache) {
        return deduplicationCache.contains(key)
      }
    }

    /**
     * Records transaction in deduplication cache.
     */
    fun markProcessed(transaction: UpiTransaction) {
      val key = generateDeduplicationKey(transaction.amount, transaction.merchant, transaction.timestamp)
      synchronized(deduplicationCache) {
        if (deduplicationCache.size >= MAX_DEDUPLICATION_CACHE_SIZE) {
          val iterator = deduplicationCache.iterator()
          if (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
          }
        }
        deduplicationCache.add(key)
      }
    }

    /**
     * Resets deduplication cache for testing or cleanup.
     */
    fun clearDeduplicationCache() {
      synchronized(deduplicationCache) {
        deduplicationCache.clear()
      }
    }
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    super.onNotificationPosted(sbn)
    if (sbn == null) return

    val packageName = sbn.packageName ?: return

    // Filter by target UPI and SMS applications
    if (packageName !in TARGET_PACKAGES) return

    val notification = sbn.notification ?: return
    val extras = notification.extras ?: return

    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
    val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

    val combinedContent = buildString {
      if (!text.isNullOrBlank()) append(text)
      if (!bigText.isNullOrBlank() && bigText != text) {
        if (isNotEmpty()) append(" ")
        append(bigText)
      }
      if (!subText.isNullOrBlank()) {
        if (isNotEmpty()) append(" ")
        append(subText)
      }
    }

    Log.d(TAG, "Notification from $packageName received: '$title' - '$combinedContent'")

    val transaction = parseTransaction(packageName, title, combinedContent) ?: return

    // Deduplication check
    if (isDuplicate(transaction)) {
      Log.i(TAG, "Duplicate detected and filtered for ${transaction.amount} to ${transaction.merchant}")
      return
    }

    markProcessed(transaction)
    Log.i(TAG, "Captured UPI Transaction: ${transaction.amount} [${transaction.type}] via ${transaction.sourceApp}")

    // 1. Emit via Kotlin SharedFlow
    _transactionFlow.tryEmit(transaction)

    // 2. Persist to Room Database so app ledger is updated automatically
    serviceScope.launch {
      try {
        val database = SanchayDatabase.getDatabase(applicationContext, this)
        val todayEpoch = LocalDate.now().toEpochDay()
        val signedAmount = if (transaction.type == TransactionType.DEBIT) -transaction.amount else transaction.amount
        val entity = TransactionEntity(
          goalId = null,
          amount = signedAmount,
          channel = Channel.UPI.name,
          note = "${transaction.merchant} (${transaction.sourceApp})",
          timestamp = transaction.timestamp,
          dateEpochDay = todayEpoch,
          isAutoCaptured = true,
          upiAppName = transaction.sourceApp,
          merchantOrSender = transaction.merchant,
          upiRefId = transaction.id,
          isConfirmed = true
        )
        database.sanchayDao().addTransactionWithGoalUpdate(entity)
      } catch (e: Exception) {
        Log.e(TAG, "Error persisting transaction to database", e)
      }
    }
  }
}

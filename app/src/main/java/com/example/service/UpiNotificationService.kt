package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.R
import com.example.data.auth.AuthManager
import com.example.data.local.SanchayDatabase
import com.example.data.local.TransactionEntity
import com.example.data.model.Channel
import com.example.data.model.TransactionType
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
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
import java.util.Locale
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
 * Production-ready Android NotificationListenerService for automatic UPI transaction interception.
 */
class UpiNotificationService : NotificationListenerService() {

  companion object {
    private const val TAG = "UpiNotificationService"

    // Target Packages
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

    // Deduplication tracking: Set of computed transaction hashes (amount + merchant + (timestamp / 60000))
    private val deduplicationCache = Collections.synchronizedSet(LinkedHashSet<String>())
    private const val MAX_DEDUPLICATION_CACHE_SIZE = 500

    /**
     * Strict Amount Regex required by Sanchay Parser Engine:
     * (?:(?:rs|inr|₹)\.?\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:inr|rs|₹))
     */
    private val amountRegex = Regex(
      """(?i)(?:(?:rs|inr|₹)\.?\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:inr|rs|₹))"""
    )

    // Debit classification: paid, debited, sent to, spent
    private val debitKeywords = Regex(
      """(?i)\b(paid|debited|sent\s+to|spent|purchase|transferred\s+to)\b"""
    )

    // Credit classification: received, credited, added, cashback
    private val creditKeywords = Regex(
      """(?i)\b(received|credited|added|cashback|deposited)\b"""
    )

    /**
     * Checks if notification listener access is enabled in Android system settings.
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
      return try {
        val cn = ComponentName(context, UpiNotificationService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        flat != null && flat.contains(cn.flattenToString())
      } catch (e: Throwable) {
        false
      }
    }

    /**
     * Redirects the user directly to Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS.
     */
    fun openNotificationListenerSettings(context: Context) {
      try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
      } catch (e: Throwable) {
        Log.e(TAG, "Failed to launch notification listener settings", e)
      }
    }

    /**
     * Robust parser engine inspecting notification text against supported UPI app and bank SMS payloads.
     */
    fun parseTransaction(packageName: String, title: String?, text: String?): UpiTransaction? {
      val combinedText = buildString {
        if (!title.isNullOrBlank()) append(title).append(" ")
        if (!text.isNullOrBlank()) append(text)
      }.trim()

      if (combinedText.isBlank()) return null

      // Check for Debit vs Credit
      val isDebit = debitKeywords.containsMatchIn(combinedText)
      val isCredit = creditKeywords.containsMatchIn(combinedText)

      if (!isDebit && !isCredit) {
        return null
      }

      val type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT

      // Extract amount using Strict Amount Regex
      val amountMatch = amountRegex.find(combinedText) ?: return null
      val rawAmountStr = (amountMatch.groups[1]?.value ?: amountMatch.groups[2]?.value)?.replace(",", "") ?: return null
      val amount = rawAmountStr.toDoubleOrNull() ?: return null
      if (amount <= 0.0) return null

      val sourceApp = when (packageName) {
        PACKAGE_PHONEPE -> "PhonePe"
        PACKAGE_GPAY -> "Google Pay"
        PACKAGE_PAYTM -> "Paytm"
        PACKAGE_BHIM -> "BHIM"
        PACKAGE_GOOGLE_MESSAGING, PACKAGE_SAMSUNG_MESSAGING -> "Bank SMS"
        else -> "UPI"
      }

      val merchant = extractMerchant(combinedText, type)
      val txId = UUID.randomUUID().toString()

      return UpiTransaction(
        id = txId,
        amount = amount,
        type = type,
        merchant = merchant,
        sourceApp = sourceApp,
        timestamp = System.currentTimeMillis()
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
     * Unique signature check: hash(amount + merchant + (timestamp / 60000))
     * Ensures dual SMS and UPI alerts do not create duplicate ledger entries.
     */
    fun generateDeduplicationKey(amount: Double, merchant: String, timestamp: Long): String {
      val minuteBucket = timestamp / 60000L
      val rawKey = "${"%.2f".format(Locale.US, amount)}:${merchant.trim().lowercase(Locale.ROOT)}:$minuteBucket"
      val digest = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray())
      return digest.joinToString("") { "%02x".format(it) }
    }

    fun isDuplicate(transaction: UpiTransaction): Boolean {
      val key = generateDeduplicationKey(transaction.amount, transaction.merchant, transaction.timestamp)
      synchronized(deduplicationCache) {
        return deduplicationCache.contains(key)
      }
    }

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

    fun clearDeduplicationCache() {
      synchronized(deduplicationCache) {
        deduplicationCache.clear()
      }
    }
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onListenerConnected() {
    try {
      super.onListenerConnected()
      Log.i(TAG, "UpiNotificationService connected and active")
    } catch (e: Throwable) {
      Log.e(TAG, "Error in onListenerConnected", e)
    }
  }

  override fun onListenerDisconnected() {
    try {
      super.onListenerDisconnected()
      Log.i(TAG, "UpiNotificationService disconnected safely")
    } catch (e: Throwable) {
      Log.e(TAG, "Error in onListenerDisconnected", e)
    }
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    try {
      super.onNotificationPosted(sbn)
      if (sbn == null) return

      val packageName = sbn.packageName ?: return
      if (packageName !in TARGET_PACKAGES) return

      val notification = sbn.notification ?: return
      val extras = notification.extras ?: return

      // Completely inspect extras safely: EXTRA_TITLE, EXTRA_TEXT, EXTRA_BIG_TEXT, EXTRA_SUB_TEXT
      val title = runCatching { extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() }.getOrNull()
      val text = runCatching { extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() }.getOrNull()
      val bigText = runCatching { extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() }.getOrNull()
      val subText = runCatching { extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() }.getOrNull()

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
      }.trim()

      val transaction = parseTransaction(packageName, title, combinedContent) ?: return

      // Deduplication check: hash(amount + merchant + (timestamp / 60000))
      if (isDuplicate(transaction)) {
        Log.i(TAG, "Duplicate filtered for ${transaction.amount} to ${transaction.merchant}")
        return
      }

      markProcessed(transaction)
      Log.i(TAG, "Captured UPI Transaction: ${transaction.amount} [${transaction.type}] via ${transaction.sourceApp}")

      _transactionFlow.tryEmit(transaction)

      // Persist to Room DB and sync directly with Firestore under users/{userId}/transactions
      serviceScope.launch {
        try {
          val database = SanchayDatabase.getDatabase(applicationContext)
          val todayEpoch = LocalDate.now().toEpochDay()
          val signedAmount = if (transaction.type == TransactionType.DEBIT) -transaction.amount else transaction.amount
          val detectedCategory = ExpenseClassifier.classify(transaction.merchant, transaction.sourceApp)

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
            isConfirmed = true,
            category = detectedCategory.name
          )
          val generatedId = database.sanchayDao().addTransactionWithGoalUpdate(entity)

          // Direct Firestore sync under users/{userId}/transactions with isAutoCaptured = true
          val user = AuthManager.currentUser
          if (user != null) {
            try {
              val fbApp = runCatching { FirebaseApp.getInstance() }.getOrNull()
              if (fbApp != null) {
                val dbId = runCatching { applicationContext.getString(R.string.firestore_database_id) }.getOrNull()
                val firestore = if (!dbId.isNullOrBlank()) FirebaseFirestore.getInstance(fbApp, dbId) else FirebaseFirestore.getInstance(fbApp)
                val docId = if (!entity.upiRefId.isNullOrBlank()) entity.upiRefId!! else generatedId.toString()

                val data = mapOf(
                  "userId" to user.uid,
                  "amount" to signedAmount,
                  "channel" to Channel.UPI.name,
                  "note" to entity.note,
                  "category" to detectedCategory.name,
                  "dateEpochDay" to todayEpoch,
                  "timestamp" to transaction.timestamp,
                  "isAutoCaptured" to true,
                  "upiAppName" to transaction.sourceApp,
                  "merchantOrSender" to transaction.merchant,
                  "upiRefId" to transaction.id,
                  "createdAt" to Timestamp.now()
                )

                firestore.collection("users")
                  .document(user.uid)
                  .collection("transactions")
                  .document(docId)
                  .set(data)
              }
            } catch (fsEx: Throwable) {
              Log.w(TAG, "Background Firestore sync skipped/failed: ${fsEx.message}")
            }
          }
        } catch (e: Throwable) {
          Log.e(TAG, "Error persisting transaction to database", e)
        }
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Unexpected error in onNotificationPosted", e)
    }
  }
}

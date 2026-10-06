package com.example.service

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object DeduplicationManager {

  // Map of UPI reference ID -> timestamp processed
  private val processedRefIds = ConcurrentHashMap<String, Long>()

  // List of recent transactions for fuzzy matching (amount + merchant + close timestamp)
  private val recentSignatures = ConcurrentHashMap<String, Long>()

  // 10-minute duplicate window in milliseconds
  private const val DUP_WINDOW_MILLIS = 10 * 60 * 1000L

  @Synchronized
  fun isDuplicate(notification: ParsedUpiNotification): Boolean {
    val now = System.currentTimeMillis()
    cleanupOldEntries(now)

    // 1. Exact UPI reference ID / UTR match
    if (!notification.upiRefId.isNullOrBlank()) {
      val existingTime = processedRefIds[notification.upiRefId]
      if (existingTime != null && now - existingTime < DUP_WINDOW_MILLIS) {
        return true
      }
    }

    // 2. Fuzzy signature match (Amount + normalized merchant + time window)
    // Avoids duplicate when both Bank SMS and GPay/PhonePe notify for the same payment
    val normMerchant = notification.merchantOrPerson.lowercase().replace(Regex("[^a-z0-9]"), "")
    val signatureKey = "${notification.amount}_${normMerchant.take(8)}"

    val lastSeen = recentSignatures[signatureKey]
    if (lastSeen != null && now - lastSeen < DUP_WINDOW_MILLIS) {
      return true
    }

    return false
  }

  @Synchronized
  fun record(notification: ParsedUpiNotification) {
    val now = System.currentTimeMillis()
    if (!notification.upiRefId.isNullOrBlank()) {
      processedRefIds[notification.upiRefId] = now
    }
    val normMerchant = notification.merchantOrPerson.lowercase().replace(Regex("[^a-z0-9]"), "")
    val signatureKey = "${notification.amount}_${normMerchant.take(8)}"
    recentSignatures[signatureKey] = now
  }

  @Synchronized
  fun clear() {
    processedRefIds.clear()
    recentSignatures.clear()
  }

  private fun cleanupOldEntries(now: Long) {
    val expiry = now - (24 * 60 * 60 * 1000L) // 24 hours
    processedRefIds.entries.removeIf { it.value < expiry }
    recentSignatures.entries.removeIf { it.value < expiry }
  }
}

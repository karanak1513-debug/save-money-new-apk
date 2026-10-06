package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class UpiNotificationListenerService : NotificationListenerService() {

  companion object {
    private const val TAG = "UpiListenerService"
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    super.onNotificationPosted(sbn)
    if (sbn == null) return

    val packageName = sbn.packageName ?: return

    // Quick filter check for supported UPI or SMS packages
    if (!UpiParserEngine.isSupportedPackage(packageName)) return

    val notification = sbn.notification ?: return
    val extras = notification.extras ?: return

    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

    val combinedContent = buildString {
      if (!text.isNullOrBlank()) append(text)
      if (!bigText.isNullOrBlank() && bigText != text) {
        if (isNotEmpty()) append(" ")
        append(bigText)
      }
    }

    Log.d(TAG, "Notification received from $packageName: Title='$title', Content='$combinedContent'")

    val parsed = UpiParserEngine.parse(packageName, title, combinedContent)
    if (parsed != null) {
      if (DeduplicationManager.isDuplicate(parsed)) {
        Log.i(TAG, "Ignored duplicate transaction for ref=${parsed.upiRefId} or amount=${parsed.amount}")
        return
      }

      DeduplicationManager.record(parsed)
      Log.i(TAG, "Successfully parsed UPI notification: amount=${parsed.amount}, merchant=${parsed.merchantOrPerson}, type=${parsed.type}")
      UpiNotificationBus.emit(parsed)
    }
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    super.onNotificationRemoved(sbn)
  }
}

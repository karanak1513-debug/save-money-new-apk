package com.example.service

import com.example.data.model.TransactionType

data class DemoNotification(
  val label: String,
  val packageName: String,
  val title: String,
  val text: String
)

object UpiSimulationHelper {

  val sampleNotifications = listOf(
    DemoNotification(
      label = "Google Pay: ₹450 to Swiggy",
      packageName = UpiParserEngine.PACKAGE_GPAY,
      title = "Google Pay",
      text = "You paid ₹450 to Swiggy. UPI Ref: 428194829102"
    ),
    DemoNotification(
      label = "PhonePe: ₹250 to Chai Point",
      packageName = UpiParserEngine.PACKAGE_PHONEPE,
      title = "Transaction Successful",
      text = "Paid ₹250.00 to Chai Point successfully. Transaction ID T241005123456"
    ),
    DemoNotification(
      label = "Paytm: ₹150 to Sharma Store",
      packageName = UpiParserEngine.PACKAGE_PAYTM,
      title = "Paytm Payment",
      text = "Paid Rs. 150 to Sharma General Store on Paytm. UPI Ref 382910291029"
    ),
    DemoNotification(
      label = "Google Pay: +₹1,200 from Rahul (Saving)",
      packageName = UpiParserEngine.PACKAGE_GPAY,
      title = "Money Received",
      text = "You received ₹1,200 from Rahul Sharma. UPI Ref: 981273918237"
    ),
    DemoNotification(
      label = "Bank SMS: Duplicate ₹450 to Swiggy",
      packageName = "com.google.android.apps.messaging",
      title = "HDFC Bank Alert",
      text = "A/c *1234 debited by Rs 450.00 on 05-Oct-26 by UPI to SWIGGY Ref 428194829102"
    )
  )

  fun simulate(demo: DemoNotification): String {
    val parsed = UpiParserEngine.parse(demo.packageName, demo.title, demo.text)
      ?: return "Parsing failed for ${demo.label}"

    if (DeduplicationManager.isDuplicate(parsed)) {
      return "Deduplication: Dropped duplicate entry for ref=${parsed.upiRefId ?: parsed.amount}"
    }

    DeduplicationManager.record(parsed)
    UpiNotificationBus.emit(parsed)
    return "Detected ₹${parsed.amount.toInt()} (${parsed.type}) via ${parsed.appName}!"
  }
}

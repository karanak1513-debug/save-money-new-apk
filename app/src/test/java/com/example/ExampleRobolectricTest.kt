package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CurrencyFormatter
import com.example.data.model.TransactionType
import com.example.service.DeduplicationManager
import com.example.service.ParsedUpiNotification
import com.example.service.UpiParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Before
  fun setUp() {
    DeduplicationManager.clear()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sanchay", appName)
  }

  @Test
  fun `currency formatter formats rupee with indian system`() {
    val formatted150k = CurrencyFormatter.formatRupee(150000.0)
    assertEquals("₹1,50,000", formatted150k)

    val formatted60k = CurrencyFormatter.formatRupee(60000.0)
    assertEquals("₹60,000", formatted60k)

    val formattedDebit = CurrencyFormatter.formatRupee(-200.0, showSign = true)
    assertEquals("-₹200", formattedDebit)

    val formattedCredit = CurrencyFormatter.formatRupee(500.0, showSign = true)
    assertEquals("+₹500", formattedCredit)
  }

  @Test
  fun `upi parser extracts google pay debit notification`() {
    val result = UpiParserEngine.parse(
      packageName = UpiParserEngine.PACKAGE_GPAY,
      title = "Google Pay",
      text = "You paid ₹450 to Swiggy. UPI Ref: 428194829102"
    )

    assertNotNull(result)
    assertEquals(450.0, result!!.amount, 0.01)
    assertEquals(TransactionType.DEBIT, result.type)
    assertEquals("Swiggy", result.merchantOrPerson)
    assertEquals("428194829102", result.upiRefId)
    assertEquals("Google Pay", result.appName)
  }

  @Test
  fun `upi parser extracts phonepe notification`() {
    val result = UpiParserEngine.parse(
      packageName = UpiParserEngine.PACKAGE_PHONEPE,
      title = "Transaction Successful",
      text = "Paid ₹250.00 to Chai Point successfully. Transaction ID T241005123456"
    )

    assertNotNull(result)
    assertEquals(250.0, result!!.amount, 0.01)
    assertEquals(TransactionType.DEBIT, result.type)
    assertEquals("Chai Point", result.merchantOrPerson)
    assertEquals("T241005123456", result.upiRefId)
    assertEquals("PhonePe", result.appName)
  }

  @Test
  fun `upi parser extracts paytm notification`() {
    val result = UpiParserEngine.parse(
      packageName = UpiParserEngine.PACKAGE_PAYTM,
      title = "Paytm Payment",
      text = "Paid Rs. 150 to Sharma General Store on Paytm. UPI Ref 382910291029"
    )

    assertNotNull(result)
    assertEquals(150.0, result!!.amount, 0.01)
    assertEquals(TransactionType.DEBIT, result.type)
    assertEquals("Sharma General Store", result.merchantOrPerson)
    assertEquals("Paytm", result.appName)
  }

  @Test
  fun `upi parser extracts money received credit notification`() {
    val result = UpiParserEngine.parse(
      packageName = UpiParserEngine.PACKAGE_GPAY,
      title = "Money Received",
      text = "You received ₹1,200 from Rahul Sharma. UPI Ref: 981273918237"
    )

    assertNotNull(result)
    assertEquals(1200.0, result!!.amount, 0.01)
    assertEquals(TransactionType.CREDIT, result.type)
    assertEquals("Rahul Sharma", result.merchantOrPerson)
    assertEquals("981273918237", result.upiRefId)
  }

  @Test
  fun `deduplication manager prevents duplicate bank sms and upi app notifications`() {
    val upiNotification = ParsedUpiNotification(
      amount = 450.0,
      type = TransactionType.DEBIT,
      merchantOrPerson = "Swiggy",
      upiRefId = "428194829102",
      appName = "Google Pay",
      appPackage = UpiParserEngine.PACKAGE_GPAY,
      rawText = "You paid ₹450 to Swiggy. UPI Ref: 428194829102"
    )

    val bankSmsNotification = ParsedUpiNotification(
      amount = 450.0,
      type = TransactionType.DEBIT,
      merchantOrPerson = "SWIGGY",
      upiRefId = "428194829102",
      appName = "Bank SMS",
      appPackage = "com.google.android.apps.messaging",
      rawText = "A/c *1234 debited by Rs 450.00 on 05-Oct-26 by UPI to SWIGGY Ref 428194829102"
    )

    assertFalse("First notification should not be duplicate", DeduplicationManager.isDuplicate(upiNotification))
    DeduplicationManager.record(upiNotification)

    assertTrue("Second notification with same ref and amount should be detected as duplicate", DeduplicationManager.isDuplicate(bankSmsNotification))
  }
}

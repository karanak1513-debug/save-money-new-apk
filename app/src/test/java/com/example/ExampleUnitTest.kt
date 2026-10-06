package com.example

import com.example.data.model.Channel
import com.example.data.model.Goal
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.service.UpiNotificationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {

  @Test
  fun `goal progress fraction calculates correctly`() {
    val goal = Goal(
      id = 1L,
      title = "Car Reserve",
      targetAmount = 100000.0,
      savedAmount = 25000.0,
      deadlineEpochDay = LocalDate.now().toEpochDay() + 100
    )

    assertEquals(0.25f, goal.progressFraction, 0.001f)
    assertEquals(75000.0, goal.remainingAmount, 0.01)
  }

  @Test
  fun `daily streak counts consecutive deposit days`() {
    val today = LocalDate.now().toEpochDay()
    val transactions = listOf(
      TransactionItem(
        amount = 100.0,
        channel = Channel.CASH,
        note = "Day 1",
        timestamp = 1,
        dateEpochDay = today
      ),
      TransactionItem(
        amount = 500.0,
        channel = Channel.CASH,
        note = "Day 2",
        timestamp = 2,
        dateEpochDay = today - 1
      ),
      TransactionItem(
        amount = 1000.0,
        channel = Channel.CASH,
        note = "Day 3",
        timestamp = 3,
        dateEpochDay = today - 2
      )
    )

    val depositDates = transactions.filter { it.amount > 0 }.map { it.dateEpochDay }.toSet()
    var streak = 0
    var checkDay = today
    while (depositDates.contains(checkDay)) {
      streak++
      checkDay--
    }

    assertEquals(3, streak)
  }

  @Test
  fun `upi notification service parses phonepe debit notification`() {
    val parsed = UpiNotificationService.parseTransaction(
      packageName = UpiNotificationService.PACKAGE_PHONEPE,
      title = "PhonePe",
      text = "Paid ₹500 to Swiggy on PhonePe successfully."
    )

    assertNotNull(parsed)
    assertEquals(500.0, parsed!!.amount, 0.01)
    assertEquals(TransactionType.DEBIT, parsed.type)
    assertEquals("Swiggy", parsed.merchant)
    assertEquals("PhonePe", parsed.sourceApp)
  }

  @Test
  fun `upi notification service parses gpay credit with Rs 500`() {
    val parsed = UpiNotificationService.parseTransaction(
      packageName = UpiNotificationService.PACKAGE_GPAY,
      title = "Google Pay",
      text = "Received Rs. 500 from Rajesh Kumar on Google Pay"
    )

    assertNotNull(parsed)
    assertEquals(500.0, parsed!!.amount, 0.01)
    assertEquals(TransactionType.CREDIT, parsed.type)
    assertEquals("Rajesh Kumar", parsed.merchant)
    assertEquals("Google Pay", parsed.sourceApp)
  }

  @Test
  fun `upi notification service parses inr 1500_50 debit`() {
    val parsed = UpiNotificationService.parseTransaction(
      packageName = UpiNotificationService.PACKAGE_GOOGLE_MESSAGING,
      title = "Bank SMS",
      text = "A/c debited INR 1,500.50 for purchase at Starbucks."
    )

    assertNotNull(parsed)
    assertEquals(1500.50, parsed!!.amount, 0.01)
    assertEquals(TransactionType.DEBIT, parsed.type)
    assertEquals("Starbucks", parsed.merchant)
    assertEquals("Bank SMS", parsed.sourceApp)
  }

  @Test
  fun `upi notification service deduplication works correctly`() {
    UpiNotificationService.clearDeduplicationCache()
    val now = System.currentTimeMillis()
    val tx1 = UpiNotificationService.parseTransaction(
      packageName = UpiNotificationService.PACKAGE_GPAY,
      title = "Google Pay",
      text = "Paid ₹500 to Swiggy on Google Pay"
    )!!

    assertFalse(UpiNotificationService.isDuplicate(tx1))
    UpiNotificationService.markProcessed(tx1)
    assertTrue(UpiNotificationService.isDuplicate(tx1))
  }
}

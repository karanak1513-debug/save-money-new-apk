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

  @Test
  fun `expense classifier accurately maps merchant entities to financial buckets`() {
    val catFood1 = com.example.service.ExpenseClassifier.classify("Swiggy", null)
    val catFood2 = com.example.service.ExpenseClassifier.classify("Zomato", null)
    val catFood3 = com.example.service.ExpenseClassifier.classify("Blinkit", null)
    val catFood4 = com.example.service.ExpenseClassifier.classify("Zepto", null)
    val catFood5 = com.example.service.ExpenseClassifier.classify(null, "Local grocery store")

    assertEquals(com.example.data.model.ExpenseCategory.FOOD_ESSENTIALS, catFood1)
    assertEquals(com.example.data.model.ExpenseCategory.FOOD_ESSENTIALS, catFood2)
    assertEquals(com.example.data.model.ExpenseCategory.FOOD_ESSENTIALS, catFood3)
    assertEquals(com.example.data.model.ExpenseCategory.FOOD_ESSENTIALS, catFood4)
    assertEquals(com.example.data.model.ExpenseCategory.FOOD_ESSENTIALS, catFood5)

    val catCommute1 = com.example.service.ExpenseClassifier.classify("Uber", null)
    val catCommute2 = com.example.service.ExpenseClassifier.classify("Ola", null)
    val catCommute3 = com.example.service.ExpenseClassifier.classify("DMRC", null)
    val catCommute4 = com.example.service.ExpenseClassifier.classify(null, "Petrol bunk")
    val catCommute5 = com.example.service.ExpenseClassifier.classify(null, "Fuel refill")

    assertEquals(com.example.data.model.ExpenseCategory.COMMUTE_FUEL, catCommute1)
    assertEquals(com.example.data.model.ExpenseCategory.COMMUTE_FUEL, catCommute2)
    assertEquals(com.example.data.model.ExpenseCategory.COMMUTE_FUEL, catCommute3)
    assertEquals(com.example.data.model.ExpenseCategory.COMMUTE_FUEL, catCommute4)
    assertEquals(com.example.data.model.ExpenseCategory.COMMUTE_FUEL, catCommute5)

    val catBills1 = com.example.service.ExpenseClassifier.classify("Jio", null)
    val catBills2 = com.example.service.ExpenseClassifier.classify("Airtel", null)
    val catBills3 = com.example.service.ExpenseClassifier.classify(null, "Electricity bill")
    val catBills4 = com.example.service.ExpenseClassifier.classify(null, "Broadband recharge")

    assertEquals(com.example.data.model.ExpenseCategory.BILLS_UTILITIES, catBills1)
    assertEquals(com.example.data.model.ExpenseCategory.BILLS_UTILITIES, catBills2)
    assertEquals(com.example.data.model.ExpenseCategory.BILLS_UTILITIES, catBills3)
    assertEquals(com.example.data.model.ExpenseCategory.BILLS_UTILITIES, catBills4)

    val catLeisure1 = com.example.service.ExpenseClassifier.classify("Movies", null)
    val catLeisure2 = com.example.service.ExpenseClassifier.classify(null, "Shopping spree")
    val catLeisure3 = com.example.service.ExpenseClassifier.classify("Zara", null)
    val catLeisure4 = com.example.service.ExpenseClassifier.classify("Amazon", null)

    assertEquals(com.example.data.model.ExpenseCategory.DISCRETIONARY, catLeisure1)
    assertEquals(com.example.data.model.ExpenseCategory.DISCRETIONARY, catLeisure2)
    assertEquals(com.example.data.model.ExpenseCategory.DISCRETIONARY, catLeisure3)
    assertEquals(com.example.data.model.ExpenseCategory.DISCRETIONARY, catLeisure4)
  }

  @Test
  fun `predictive goal feasibility calculates on-track and at-risk run rates`() {
    val goal = Goal(
      id = 1L,
      title = "Europe Trip",
      targetAmount = 50000.0,
      savedAmount = 10000.0,
      deadlineEpochDay = LocalDate.now().toEpochDay() + 100
    )
    val pacing = com.example.data.model.PacingInfo(
      dailyPace = 400.0,
      weeklyPace = 2800.0,
      monthlyPace = 12000.0,
      daysRemaining = 100
    )

    val today = LocalDate.now().toEpochDay()
    // 14 days of high deposits (e.g. 500/day = 7000 over 14d -> velocity = 500/day >= 400/day)
    val onTrackTxs = (0..13).map { i ->
      TransactionItem(
        id = i.toLong(),
        goalId = 1L,
        amount = 500.0,
        channel = Channel.UPI,
        note = "Save",
        timestamp = 1000L,
        dateEpochDay = today - i
      )
    }

    val onTrackResult = com.example.service.FinancialAnalyticsEngine.calculateGoalFeasibility(
      goal = goal,
      transactions = onTrackTxs,
      pacingInfo = pacing
    )
    assertTrue(onTrackResult.isOnTrack)
    assertTrue(onTrackResult.statusBadgeText.contains("On Track"))
    assertTrue(onTrackResult.probabilityPercent >= 85)

    // At risk test case: deposits of only 100/day -> velocity = 100/day < 400/day
    val atRiskTxs = (0..13).map { i ->
      TransactionItem(
        id = i.toLong(),
        goalId = 1L,
        amount = 100.0,
        channel = Channel.UPI,
        note = "Save",
        timestamp = 1000L,
        dateEpochDay = today - i
      )
    }

    val atRiskResult = com.example.service.FinancialAnalyticsEngine.calculateGoalFeasibility(
      goal = goal,
      transactions = atRiskTxs,
      pacingInfo = pacing
    )
    assertFalse(atRiskResult.isOnTrack)
    assertTrue(atRiskResult.statusBadgeText.contains("At Risk"))
    assertEquals(300.0, atRiskResult.shortfallPerDay, 0.01)
  }

  @Test
  fun `micro leak anomaly detection triggers when small spending exceeds 30 percent`() {
    val today = LocalDate.now().toEpochDay()
    val pacing = com.example.data.model.PacingInfo(
      dailyPace = 200.0,
      weeklyPace = 1400.0,
      monthlyPace = 6000.0,
      daysRemaining = 50
    )

    // Outflow of 1000 total: 400 in micro transactions (< 150) and 600 in big transactions
    val txs = listOf(
      TransactionItem(amount = -100.0, channel = Channel.UPI, note = "Chai", timestamp = 1, dateEpochDay = today),
      TransactionItem(amount = -100.0, channel = Channel.UPI, note = "Coffee", timestamp = 2, dateEpochDay = today),
      TransactionItem(amount = -100.0, channel = Channel.UPI, note = "Snacks", timestamp = 3, dateEpochDay = today),
      TransactionItem(amount = -100.0, channel = Channel.UPI, note = "Blinkit tip", timestamp = 4, dateEpochDay = today),
      TransactionItem(amount = -600.0, channel = Channel.UPI, note = "Electricity", timestamp = 5, dateEpochDay = today)
    )

    val alert = com.example.service.FinancialAnalyticsEngine.detectMicroLeaks(txs, pacing)
    assertTrue("Micro leak should be detected (40% >= 30%)", alert.isDetected)
    assertEquals(40, alert.microPercent)
    assertEquals(400.0, alert.microTotal, 0.01)
    assertTrue(alert.badgeText.contains("AI Audit: Micro-spending increased by 40%"))
  }

  @Test
  fun `weekly audit brief summarizes inflow outflow and pace adjustment`() {
    val today = LocalDate.now().toEpochDay()
    val goal = Goal(id = 1L, title = "Reserve", targetAmount = 50000.0, savedAmount = 5000.0, deadlineEpochDay = today + 30)
    val pacing = com.example.data.model.PacingInfo(dailyPace = 500.0, weeklyPace = 3500.0, monthlyPace = 15000.0, daysRemaining = 30)

    val txs = listOf(
      TransactionItem(amount = 2000.0, channel = Channel.UPI, note = "Salary bonus", timestamp = 1, dateEpochDay = today),
      TransactionItem(amount = -800.0, channel = Channel.UPI, note = "Swiggy order", timestamp = 2, dateEpochDay = today, category = com.example.data.model.ExpenseCategory.FOOD_ESSENTIALS.name),
      TransactionItem(amount = -300.0, channel = Channel.UPI, note = "Uber cab", timestamp = 3, dateEpochDay = today, category = com.example.data.model.ExpenseCategory.COMMUTE_FUEL.name)
    )

    val audit = com.example.service.FinancialAnalyticsEngine.generateWeeklyAudit(txs, goal, pacing)
    assertEquals(2000.0, audit.totalInflow, 0.01)
    assertEquals(1100.0, audit.totalOutflow, 0.01)
    assertEquals(900.0, audit.netSavings, 0.01)
    assertEquals(com.example.data.model.ExpenseCategory.FOOD_ESSENTIALS, audit.biggestCategory)
    assertEquals(800.0, audit.biggestCategoryAmount, 0.01)
  }
}

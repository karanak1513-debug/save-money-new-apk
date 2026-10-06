package com.example.service

import com.example.data.model.CurrencyFormatter
import com.example.data.model.ExpenseCategory
import com.example.data.model.Goal
import com.example.data.model.GoalFeasibility
import com.example.data.model.MicroLeakAlert
import com.example.data.model.PacingInfo
import com.example.data.model.TransactionItem
import com.example.data.model.WeeklyAuditSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Intelligent Financial Analytics Engine for Sanchay.
 * Calculates run-rate feasibility, micro-leak anomalies, and 7-day executive audits.
 */
object FinancialAnalyticsEngine {

  /**
   * 1. Dynamic Predictive Goal Feasibility (Run-Rate AI)
   * Calculates a dynamic "Feasibility Score" (0–100%) based on trailing 14-day net savings velocity.
   */
  fun calculateGoalFeasibility(
    goal: Goal?,
    transactions: List<TransactionItem>,
    pacingInfo: PacingInfo
  ): GoalFeasibility {
    if (goal == null) {
      return GoalFeasibility(
        scorePercent = 0,
        isOnTrack = false,
        statusBadgeText = "⚪ Inactive Target",
        trailing14dVelocity = 0.0,
        shortfallPerDay = 0.0,
        probabilityPercent = 0
      )
    }

    if (pacingInfo.isCompleted) {
      return GoalFeasibility(
        scorePercent = 100,
        isOnTrack = true,
        statusBadgeText = "🟢 Completed (100% Probability)",
        trailing14dVelocity = 0.0,
        shortfallPerDay = 0.0,
        probabilityPercent = 100,
        isCompleted = true
      )
    }

    if (pacingInfo.isExpired) {
      return GoalFeasibility(
        scorePercent = 0,
        isOnTrack = false,
        statusBadgeText = "🔴 Target Expired",
        trailing14dVelocity = 0.0,
        shortfallPerDay = pacingInfo.dailyPace,
        probabilityPercent = 0,
        isExpired = true
      )
    }

    val today = LocalDate.now().toEpochDay()
    val fourteenDaysAgo = today - 13

    // Trailing 14-day net deposits dedicated to this goal (or overall net savings if goalId is unassigned/matching)
    val trailing14dDeposits = transactions
      .filter { it.dateEpochDay in fourteenDaysAgo..today && (it.goalId == null || it.goalId == goal.id) && it.amount > 0 }
      .sumOf { it.amount }

    val trailing14dWithdrawals = transactions
      .filter { it.dateEpochDay in fourteenDaysAgo..today && (it.goalId == null || it.goalId == goal.id) && it.amount < 0 }
      .sumOf { abs(it.amount) }

    val net14dSavings = max(0.0, trailing14dDeposits - trailing14dWithdrawals)
    val dailyVelocity = net14dSavings / 14.0
    val requiredDailyPace = max(0.0, pacingInfo.dailyPace)

    if (requiredDailyPace <= 0.0) {
      return GoalFeasibility(
        scorePercent = 100,
        isOnTrack = true,
        statusBadgeText = "🟢 On Track (100% Probability)",
        trailing14dVelocity = dailyVelocity,
        shortfallPerDay = 0.0,
        probabilityPercent = 100
      )
    }

    val isOnTrack = dailyVelocity >= requiredDailyPace
    val ratio = if (requiredDailyPace > 0) dailyVelocity / requiredDailyPace else 1.0

    val probability = if (isOnTrack) {
      val bonus = ((ratio - 1.0) * 12.0).toInt()
      min(98, 88 + bonus)
    } else {
      val score = (ratio * 78.0).toInt()
      max(18, min(79, score))
    }

    val shortfall = max(0.0, requiredDailyPace - dailyVelocity)

    val badgeText = if (isOnTrack) {
      "🟢 On Track ($probability% Probability)"
    } else {
      val shortfallFormatted = CurrencyFormatter.formatRupee(shortfall, includeSymbol = true)
      "🔴 At Risk ($probability% Probability) — Shortfall of $shortfallFormatted/day"
    }

    return GoalFeasibility(
      scorePercent = probability,
      isOnTrack = isOnTrack,
      statusBadgeText = badgeText,
      trailing14dVelocity = dailyVelocity,
      shortfallPerDay = shortfall,
      probabilityPercent = probability
    )
  }

  /**
   * 2. Micro-Leak / Anomaly Detection Card
   * Detects high-frequency small transactions (< ₹150) exceeding 30% of weekly outflow.
   */
  fun detectMicroLeaks(
    transactions: List<TransactionItem>,
    pacingInfo: PacingInfo
  ): MicroLeakAlert {
    val today = LocalDate.now().toEpochDay()
    val sevenDaysAgo = today - 6

    val weeklyDebits = transactions.filter { it.dateEpochDay in sevenDaysAgo..today && it.amount < 0 }
    val totalWeeklyOutflow = weeklyDebits.sumOf { abs(it.amount) }

    val microDebits = weeklyDebits.filter { abs(it.amount) < 150.0 }
    val totalMicroOutflow = microDebits.sumOf { abs(it.amount) }
    val microCount = microDebits.size

    if (totalWeeklyOutflow <= 0.0 || totalMicroOutflow <= 0.0) {
      return MicroLeakAlert(
        isDetected = false,
        microPercent = 0,
        microTotal = 0.0,
        delayedDays = 0,
        transactionCount = 0,
        badgeText = "",
        weeklyOutflow = 0.0
      )
    }

    val microFraction = (totalMicroOutflow / totalWeeklyOutflow).toFloat()
    val microPercent = (microFraction * 100f).roundToInt()

    // Trigger condition: Micro-spending >= 30% of total weekly outflow and at least 2 transactions
    val isDetected = microPercent >= 30 && microCount >= 2

    val dailyPace = if (pacingInfo.dailyPace > 0) pacingInfo.dailyPace else 150.0
    val delayedDays = max(1, (totalMicroOutflow / dailyPace).roundToInt())

    val alertText = "AI Audit: Micro-spending increased by ${microPercent}% this week. Projected goal impact: +${delayedDays} days delay."

    return MicroLeakAlert(
      isDetected = isDetected,
      microPercent = microPercent,
      microTotal = totalMicroOutflow,
      delayedDays = delayedDays,
      transactionCount = microCount,
      badgeText = alertText,
      weeklyOutflow = totalWeeklyOutflow
    )
  }

  /**
   * 3. Weekly Audit Brief (Executive Summary)
   * High-contrast executive breakdown across the 7-day trailing window.
   */
  fun generateWeeklyAudit(
    transactions: List<TransactionItem>,
    primaryGoal: Goal?,
    pacingInfo: PacingInfo
  ): WeeklyAuditSummary {
    val today = LocalDate.now()
    val todayEpoch = today.toEpochDay()
    val sevenDaysAgoEpoch = todayEpoch - 6
    val startDate = today.minusDays(6)

    val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
    val dateRangeLabel = "${startDate.format(formatter).uppercase()} — ${today.format(formatter).uppercase()}"

    val weeklyTxs = transactions.filter { it.dateEpochDay in sevenDaysAgoEpoch..todayEpoch }

    val totalInflow = weeklyTxs.filter { it.amount > 0 }.sumOf { it.amount }
    val totalOutflow = weeklyTxs.filter { it.amount < 0 }.sumOf { abs(it.amount) }
    val netSavings = totalInflow - totalOutflow

    // Group outflows by category to locate the largest expenditure driver
    val categoryOutflows = mutableMapOf<ExpenseCategory, Double>()
    weeklyTxs.filter { it.amount < 0 }.forEach { tx ->
      val cat = tx.expenseCategory
      val existing = categoryOutflows.getOrDefault(cat, 0.0)
      categoryOutflows[cat] = existing + abs(tx.amount)
    }

    val biggestEntry = categoryOutflows.maxByOrNull { it.value }
    val biggestCategory = biggestEntry?.key
    val biggestCategoryAmount = biggestEntry?.value ?: 0.0

    // Recommended daily pace adjustment
    val requiredDaily = pacingInfo.dailyPace
    val actualDailySavings = max(0.0, netSavings / 7.0)
    val shortfallDaily = max(0.0, requiredDaily - actualDailySavings)

    val recommendedAdjustment = if (shortfallDaily > 10.0) {
      ((shortfallDaily / 50.0).roundToInt() * 50.0).coerceAtLeast(50.0)
    } else {
      0.0
    }

    val recommendationText = when {
      pacingInfo.isCompleted -> "Goal target satisfied. Capital allocation on pace."
      recommendedAdjustment > 0.0 -> "Adjust daily run-rate by +${CurrencyFormatter.formatRupee(recommendedAdjustment)} / day to stay aligned."
      netSavings > 0 -> "Net capital generation is positive. Maintain current savings cadence."
      else -> "Maintain baseline pace of ${CurrencyFormatter.formatRupee(pacingInfo.dailyPace)} / day."
    }

    val headline = when {
      netSavings > 0 -> "NET POSITIVE CAPITAL FLOW"
      totalOutflow > totalInflow -> "CAPITAL DEFICIT DETECTED"
      else -> "BALANCED CAPITAL CADENCE"
    }

    return WeeklyAuditSummary(
      totalInflow = totalInflow,
      totalOutflow = totalOutflow,
      netSavings = netSavings,
      biggestCategory = biggestCategory,
      biggestCategoryAmount = biggestCategoryAmount,
      recommendedPaceAdjustment = recommendedAdjustment,
      recommendationText = recommendationText,
      dateRangeLabel = dateRangeLabel,
      totalTransactionCount = weeklyTxs.size
    )
  }
}

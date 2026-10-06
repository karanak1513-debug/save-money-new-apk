package com.example.data.repository

import com.example.data.local.GoalEntity
import com.example.data.local.SanchayDao
import com.example.data.local.TransactionEntity
import com.example.data.model.Channel
import com.example.data.model.ChannelBreakdown
import com.example.data.model.DailyBarData
import com.example.data.model.Goal
import com.example.data.model.PacingInfo
import com.example.data.model.TransactionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

class SanchayRepository(private val dao: SanchayDao) {

  val allGoals: Flow<List<Goal>> = dao.getAllGoals().map { entities ->
    entities.map { it.toDomain() }
  }

  val primaryGoal: Flow<Goal?> = dao.getPrimaryGoal().map { entity ->
    entity?.toDomain()
  }

  val allTransactions: Flow<List<TransactionItem>> = dao.getAllTransactions().map { entities ->
    entities.map { it.toDomain() }
  }

  suspend fun insertGoal(goal: Goal): Long {
    return dao.insertGoal(GoalEntity.fromDomain(goal))
  }

  suspend fun updateGoal(goal: Goal) {
    dao.updateGoal(GoalEntity.fromDomain(goal))
  }

  suspend fun deleteGoal(goal: Goal) {
    dao.deleteGoal(GoalEntity.fromDomain(goal))
  }

  suspend fun setPrimaryGoal(goalId: Long) {
    dao.setPrimaryGoal(goalId)
  }

  suspend fun addTransaction(item: TransactionItem) {
    dao.addTransactionWithGoalUpdate(TransactionEntity.fromDomain(item))
  }

  suspend fun updateTransaction(item: TransactionItem) {
    dao.updateTransaction(TransactionEntity.fromDomain(item))
  }

  suspend fun deleteTransaction(item: TransactionItem) {
    dao.removeTransactionWithGoalUpdate(TransactionEntity.fromDomain(item))
  }

  suspend fun clearAllTransactions() {
    dao.clearAllTransactions()
  }

  fun calculatePacing(goal: Goal?): PacingInfo {
    if (goal == null) {
      return PacingInfo(dailyPace = 450.0, weeklyPace = 3150.0, monthlyPace = 13500.0, daysRemaining = 200)
    }
    val todayEpoch = LocalDate.now().toEpochDay()
    val daysRemaining = max(1L, goal.deadlineEpochDay - todayEpoch)
    val remaining = max(0.0, goal.targetAmount - goal.savedAmount)
    val daily = if (daysRemaining > 0) remaining / daysRemaining else 0.0
    val weekly = daily * 7
    val monthly = daily * 30
    return PacingInfo(
      dailyPace = daily,
      weeklyPace = weekly,
      monthlyPace = monthly,
      daysRemaining = daysRemaining
    )
  }

  fun calculateChannelBreakdown(transactions: List<TransactionItem>): List<ChannelBreakdown> {
    var upiTotal = 0.0
    var cashTotal = 0.0
    var otherTotal = 0.0

    transactions.forEach { tx ->
      val amt = tx.amount
      when (tx.channel) {
        Channel.UPI -> upiTotal += amt
        Channel.CASH -> cashTotal += amt
        Channel.OTHER -> otherTotal += amt
      }
    }

    // Guard against negative balances in channel view
    val safeUpi = max(0.0, upiTotal)
    val safeCash = max(0.0, cashTotal)
    val safeOther = max(0.0, otherTotal)
    val grandTotal = safeUpi + safeCash + safeOther

    return listOf(
      ChannelBreakdown(
        channel = Channel.UPI,
        amount = safeUpi,
        percentage = if (grandTotal > 0) (safeUpi / grandTotal).toFloat() else 0.33f
      ),
      ChannelBreakdown(
        channel = Channel.CASH,
        amount = safeCash,
        percentage = if (grandTotal > 0) (safeCash / grandTotal).toFloat() else 0.33f
      ),
      ChannelBreakdown(
        channel = Channel.OTHER,
        amount = safeOther,
        percentage = if (grandTotal > 0) (safeOther / grandTotal).toFloat() else 0.34f
      )
    )
  }

  fun calculateWeeklyBars(transactions: List<TransactionItem>): List<DailyBarData> {
    val today = LocalDate.now()
    val result = mutableListOf<DailyBarData>()
    val dayFormat = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)

    for (i in 6 downTo 0) {
      val day = today.minusDays(i.toLong())
      val dayEpoch = day.toEpochDay()
      val dayName = day.dayOfWeek.name.take(1) // "M", "T", "W", "T", "F", "S", "S"

      val sumForDay = transactions
        .filter { it.dateEpochDay == dayEpoch }
        .sumOf { max(0.0, it.amount) } // Show manual additions

      result.add(
        DailyBarData(
          dayLabel = dayName,
          dateStr = day.format(dayFormat),
          amount = sumForDay,
          isToday = (i == 0)
        )
      )
    }
    return result
  }
}

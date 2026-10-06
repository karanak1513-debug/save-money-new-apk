package com.example.data.repository

import com.example.data.local.GoalEntity
import com.example.data.local.SanchayDao
import com.example.data.local.TransactionEntity
import com.example.data.model.Channel
import com.example.data.model.ChannelBreakdown
import com.example.data.model.DailyBarData
import com.example.data.model.FrequencyPref
import com.example.data.model.Goal
import com.example.data.model.PacingInfo
import com.example.data.model.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

  suspend fun ensureDefaultGoalExists() = withContext(Dispatchers.IO) {
    if (dao.getGoalCount() == 0) {
      val today = LocalDate.now()
      val todayEpochDay = today.toEpochDay()
      val primaryGoal = GoalEntity(
        id = 1L,
        title = "Reserve Target",
        targetAmount = 100000.0,
        savedAmount = 0.0,
        deadlineEpochDay = todayEpochDay + 180,
        frequencyPref = FrequencyPref.DAILY.name,
        isPrimary = true,
        createdAtEpochDay = todayEpochDay
      )
      dao.insertGoal(primaryGoal)
    }
  }

  suspend fun insertGoal(goal: Goal): Long = withContext(Dispatchers.IO) {
    dao.insertGoal(GoalEntity.fromDomain(goal))
  }

  suspend fun updateGoal(goal: Goal) = withContext(Dispatchers.IO) {
    dao.updateGoal(GoalEntity.fromDomain(goal))
  }

  suspend fun deleteGoal(goal: Goal) = withContext(Dispatchers.IO) {
    dao.deleteGoal(GoalEntity.fromDomain(goal))
  }

  suspend fun setPrimaryGoal(goalId: Long) = withContext(Dispatchers.IO) {
    dao.setPrimaryGoal(goalId)
  }

  suspend fun addTransaction(item: TransactionItem) = withContext(Dispatchers.IO) {
    dao.addTransactionWithGoalUpdate(TransactionEntity.fromDomain(item))
  }

  suspend fun updateTransaction(item: TransactionItem) = withContext(Dispatchers.IO) {
    dao.updateTransaction(TransactionEntity.fromDomain(item))
  }

  suspend fun deleteTransaction(item: TransactionItem) = withContext(Dispatchers.IO) {
    dao.removeTransactionWithGoalUpdate(TransactionEntity.fromDomain(item))
  }

  suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
    dao.clearAllTransactions()
  }

  fun calculatePacing(goal: Goal?): PacingInfo {
    if (goal == null) {
      return PacingInfo(
        dailyPace = 0.0,
        weeklyPace = 0.0,
        monthlyPace = 0.0,
        daysRemaining = 0,
        isExpired = false,
        isCompleted = false
      )
    }
    val currentMillis = System.currentTimeMillis()
    val deadlineMillis = goal.deadlineEpochDay * 86400000L
    val remainingMillis = deadlineMillis - currentMillis
    val isExpired = remainingMillis <= 0
    val remainingDays = max(1L, remainingMillis / (1000L * 60L * 60L * 24L))
    val remainingDeficit = max(0.0, goal.targetAmount - goal.savedAmount)
    val isCompleted = goal.targetAmount > 0 && goal.savedAmount >= goal.targetAmount

    val requiredDaily = if (!isExpired && !isCompleted) remainingDeficit / remainingDays.toDouble() else 0.0
    val requiredWeekly = requiredDaily * 7.0
    val requiredMonthly = requiredDaily * 30.0

    return PacingInfo(
      dailyPace = requiredDaily,
      weeklyPace = requiredWeekly,
      monthlyPace = requiredMonthly,
      daysRemaining = if (isExpired) 0L else remainingDays,
      isExpired = isExpired,
      isCompleted = isCompleted
    )
  }

  fun calculateChannelBreakdown(transactions: List<TransactionItem>): List<ChannelBreakdown> {
    var upiTotal = 0.0
    var cashTotal = 0.0
    var tacticalOtherTotal = 0.0

    transactions.forEach { tx ->
      val amt = tx.amount
      when (tx.channel) {
        Channel.UPI -> upiTotal += amt
        Channel.CASH -> cashTotal += amt
        Channel.OTHER -> tacticalOtherTotal += amt
      }
    }

    val safeUpi = max(0.0, upiTotal)
    val safeCash = max(0.0, cashTotal)
    val safeOther = max(0.0, tacticalOtherTotal)
    val grandTotal = safeUpi + safeCash + safeOther

    return listOf(
      ChannelBreakdown(
        channel = Channel.UPI,
        amount = safeUpi,
        percentage = if (grandTotal > 0) (safeUpi / grandTotal).toFloat() else 0f
      ),
      ChannelBreakdown(
        channel = Channel.CASH,
        amount = safeCash,
        percentage = if (grandTotal > 0) (safeCash / grandTotal).toFloat() else 0f
      ),
      ChannelBreakdown(
        channel = Channel.OTHER,
        amount = safeOther,
        percentage = if (grandTotal > 0) (safeOther / grandTotal).toFloat() else 0f
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

  fun calculateStreak(transactions: List<TransactionItem>): Int {
    val depositDates = transactions
      .filter { it.amount > 0 }
      .map { it.dateEpochDay }
      .toSet()

    if (depositDates.isEmpty()) return 0

    val today = LocalDate.now().toEpochDay()
    var currentStreak = 0

    var checkDay = if (depositDates.contains(today)) {
      today
    } else if (depositDates.contains(today - 1)) {
      today - 1
    } else {
      return 0
    }

    while (depositDates.contains(checkDay)) {
      currentStreak++
      checkDay--
    }

    return currentStreak
  }

  suspend fun restoreBackup(payload: BackupPayload) = withContext(Dispatchers.IO) {
    if (payload.goals.isNotEmpty()) {
      dao.insertGoals(payload.goals.map { GoalEntity.fromDomain(it) })
    }
    if (payload.transactions.isNotEmpty()) {
      dao.insertTransactions(payload.transactions.map { TransactionEntity.fromDomain(it) })
    }
  }
}

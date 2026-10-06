package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Channel
import com.example.data.model.FrequencyPref
import com.example.data.model.Goal
import com.example.data.model.TransactionItem

@Entity(tableName = "goals")
data class GoalEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val targetAmount: Double,
  val savedAmount: Double,
  val deadlineEpochDay: Long,
  val frequencyPref: String = FrequencyPref.DAILY.name,
  val isPrimary: Boolean = false,
  val createdAtEpochDay: Long = 0
) {
  fun toDomain(): Goal = Goal(
    id = id,
    title = title,
    targetAmount = targetAmount,
    savedAmount = savedAmount,
    deadlineEpochDay = deadlineEpochDay,
    frequencyPref = runCatching { FrequencyPref.valueOf(frequencyPref) }.getOrDefault(FrequencyPref.DAILY),
    isPrimary = isPrimary,
    createdAtEpochDay = createdAtEpochDay
  )

  companion object {
    fun fromDomain(goal: Goal): GoalEntity = GoalEntity(
      id = goal.id,
      title = goal.title,
      targetAmount = goal.targetAmount,
      savedAmount = goal.savedAmount,
      deadlineEpochDay = goal.deadlineEpochDay,
      frequencyPref = goal.frequencyPref.name,
      isPrimary = goal.isPrimary,
      createdAtEpochDay = goal.createdAtEpochDay
    )
  }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val goalId: Long? = null,
  val amount: Double,
  val channel: String,
  val note: String,
  val timestamp: Long,
  val dateEpochDay: Long,
  val isAutoCaptured: Boolean = false,
  val upiAppName: String? = null,
  val merchantOrSender: String? = null,
  val upiRefId: String? = null,
  val isConfirmed: Boolean = true,
  val category: String = "GENERAL"
) {
  fun toDomain(): TransactionItem = TransactionItem(
    id = id,
    goalId = goalId,
    amount = amount,
    channel = runCatching { Channel.valueOf(channel) }.getOrDefault(Channel.UPI),
    note = note,
    timestamp = timestamp,
    dateEpochDay = dateEpochDay,
    isAutoCaptured = isAutoCaptured,
    upiAppName = upiAppName,
    merchantOrSender = merchantOrSender,
    upiRefId = upiRefId,
    isConfirmed = isConfirmed,
    category = category
  )

  companion object {
    fun fromDomain(item: TransactionItem): TransactionEntity = TransactionEntity(
      id = item.id,
      goalId = item.goalId,
      amount = item.amount,
      channel = item.channel.name,
      note = item.note,
      timestamp = item.timestamp,
      dateEpochDay = item.dateEpochDay,
      isAutoCaptured = item.isAutoCaptured,
      upiAppName = item.upiAppName,
      merchantOrSender = item.merchantOrSender,
      upiRefId = item.upiRefId,
      isConfirmed = item.isConfirmed,
      category = item.category
    )
  }
}

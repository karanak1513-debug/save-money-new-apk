package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SanchayDao {

  @Query("SELECT * FROM goals ORDER BY isPrimary DESC, id ASC")
  fun getAllGoals(): Flow<List<GoalEntity>>

  @Query("SELECT * FROM goals WHERE isPrimary = 1 LIMIT 1")
  fun getPrimaryGoal(): Flow<GoalEntity?>

  @Query("SELECT COUNT(*) FROM goals")
  suspend fun getGoalCount(): Int

  @Query("SELECT COUNT(*) FROM transactions")
  suspend fun getTransactionCount(): Int

  @Query("SELECT * FROM goals WHERE id = :id")
  suspend fun getGoalById(id: Long): GoalEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGoal(goal: GoalEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGoals(goals: List<GoalEntity>): List<Long>

  @Update
  suspend fun updateGoal(goal: GoalEntity)

  @Delete
  suspend fun deleteGoal(goal: GoalEntity)

  @Query("UPDATE goals SET isPrimary = CASE WHEN id = :goalId THEN 1 ELSE 0 END")
  suspend fun setPrimaryGoal(goalId: Long)

  @Query("UPDATE goals SET savedAmount = savedAmount + :delta WHERE id = :goalId")
  suspend fun adjustGoalSavedAmount(goalId: Long, delta: Double)

  @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
  fun getAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY timestamp DESC")
  fun getTransactionsByGoal(goalId: Long): Flow<List<TransactionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(tx: TransactionEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransactions(txs: List<TransactionEntity>): List<Long>

  @Update
  suspend fun updateTransaction(tx: TransactionEntity)

  @Delete
  suspend fun deleteTransaction(tx: TransactionEntity)

  @Query("DELETE FROM transactions")
  suspend fun clearAllTransactions()

  @Query("DELETE FROM goals")
  suspend fun clearAllGoals()

  @Transaction
  suspend fun addTransactionWithGoalUpdate(tx: TransactionEntity) {
    insertTransaction(tx)
    if (tx.goalId != null) {
      adjustGoalSavedAmount(tx.goalId, tx.amount)
    }
  }

  @Transaction
  suspend fun removeTransactionWithGoalUpdate(tx: TransactionEntity) {
    deleteTransaction(tx)
    if (tx.goalId != null) {
      adjustGoalSavedAmount(tx.goalId, -tx.amount)
    }
  }
}

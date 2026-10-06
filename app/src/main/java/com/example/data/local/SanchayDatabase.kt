package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Channel
import com.example.data.model.FrequencyPref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
  entities = [GoalEntity::class, TransactionEntity::class],
  version = 2,
  exportSchema = false
)
abstract class SanchayDatabase : RoomDatabase() {
  abstract fun sanchayDao(): SanchayDao

  companion object {
    @Volatile
    private var INSTANCE: SanchayDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): SanchayDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SanchayDatabase::class.java,
          "sanchay_database"
        )
          .fallbackToDestructiveMigration()
          .addCallback(DatabaseCallback(scope))
          .build()
        INSTANCE = instance
        instance
      }
    }
  }

  private class DatabaseCallback(
    private val scope: CoroutineScope
  ) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
      super.onCreate(db)
      INSTANCE?.let { database ->
        scope.launch(Dispatchers.IO) {
          populateInitialData(database.sanchayDao())
        }
      }
    }

    private suspend fun populateInitialData(dao: SanchayDao) {
      val today = LocalDate.now()
      val todayEpochDay = today.toEpochDay()

      // Primary Goal: "Reserve Target 2026", Saved: 60,000, Target: 150,000.
      // Remaining: 90,000. With 200 days remaining, pace is exactly 450/day!
      val primaryGoal = GoalEntity(
        id = 1L,
        title = "Reserve Target 2026",
        targetAmount = 150000.0,
        savedAmount = 60000.0,
        deadlineEpochDay = todayEpochDay + 200,
        frequencyPref = FrequencyPref.DAILY.name,
        isPrimary = true,
        createdAtEpochDay = todayEpochDay - 30
      )

      val bikeFund = GoalEntity(
        id = 2L,
        title = "Bike Fund",
        targetAmount = 80000.0,
        savedAmount = 32000.0,
        deadlineEpochDay = todayEpochDay + 42, // "42 days remaining" from prompt
        frequencyPref = FrequencyPref.MONTHLY.name,
        isPrimary = false,
        createdAtEpochDay = todayEpochDay - 20
      )

      val emergencyBuffer = GoalEntity(
        id = 3L,
        title = "Emergency Buffer",
        targetAmount = 50000.0,
        savedAmount = 45000.0,
        deadlineEpochDay = todayEpochDay + 90,
        frequencyPref = FrequencyPref.WEEKLY.name,
        isPrimary = false,
        createdAtEpochDay = todayEpochDay - 60
      )

      dao.insertGoals(listOf(primaryGoal, bikeFund, emergencyBuffer))

      // Prepopulate transactions matching:
      // UPI = ₹35,000, Cash = ₹15,000, Other = ₹10,000 (Total = ₹60,000)
      // And distributed over the last 7 days so weekly bar chart has rich values:
      val nowMillis = System.currentTimeMillis()
      val oneDayMillis = 86400000L

      val seedTransactions = listOf(
        TransactionEntity(
          id = 1L,
          goalId = 1L,
          amount = 5000.0,
          channel = Channel.UPI.name,
          note = "Freelance milestone split",
          timestamp = nowMillis,
          dateEpochDay = todayEpochDay,
          isAutoCaptured = true,
          upiAppName = "Google Pay",
          merchantOrSender = "Acme Corp",
          upiRefId = "428194829101",
          isConfirmed = true
        ),
        TransactionEntity(
          id = 2L,
          goalId = 1L,
          amount = 1500.0,
          channel = Channel.CASH.name,
          note = "Cash jar manual savings",
          timestamp = nowMillis - (1 * oneDayMillis),
          dateEpochDay = todayEpochDay - 1,
          isAutoCaptured = false
        ),
        TransactionEntity(
          id = 3L,
          goalId = 1L,
          amount = -1200.0,
          channel = Channel.UPI.name,
          note = "Discretionary buffer withdrawal",
          timestamp = nowMillis - (2 * oneDayMillis),
          dateEpochDay = todayEpochDay - 2,
          isAutoCaptured = true,
          upiAppName = "PhonePe",
          merchantOrSender = "Swiggy",
          upiRefId = "428194829102",
          isConfirmed = true
        ),
        TransactionEntity(
          id = 4L,
          goalId = 1L,
          amount = 3200.0,
          channel = Channel.OTHER.name,
          note = "Dividend interest transfer",
          timestamp = nowMillis - (3 * oneDayMillis),
          dateEpochDay = todayEpochDay - 3,
          isAutoCaptured = false
        ),
        TransactionEntity(
          id = 5L,
          goalId = 1L,
          amount = 2500.0,
          channel = Channel.UPI.name,
          note = "Weekend savings discipline",
          timestamp = nowMillis - (4 * oneDayMillis),
          dateEpochDay = todayEpochDay - 4,
          isAutoCaptured = true,
          upiAppName = "Paytm",
          merchantOrSender = "Sharma Store",
          upiRefId = "428194829103",
          isConfirmed = true
        ),
        TransactionEntity(
          id = 6L,
          goalId = 1L,
          amount = 1800.0,
          channel = Channel.CASH.name,
          note = "Coin change round-up deposit",
          timestamp = nowMillis - (5 * oneDayMillis),
          dateEpochDay = todayEpochDay - 5,
          isAutoCaptured = false
        ),
        TransactionEntity(
          id = 7L,
          goalId = 1L,
          amount = 4000.0,
          channel = Channel.UPI.name,
          note = "Project bonus allocation",
          timestamp = nowMillis - (6 * oneDayMillis),
          dateEpochDay = todayEpochDay - 6,
          isAutoCaptured = true,
          upiAppName = "Google Pay",
          merchantOrSender = "Client Bonus",
          upiRefId = "428194829104",
          isConfirmed = true
        ),
        // Earlier base deposits to complete UPI: 35k, Cash: 15k, Other: 10k:
        TransactionEntity(
          id = 8L,
          goalId = 1L,
          amount = 24700.0,
          channel = Channel.UPI.name,
          note = "Salary direct allocation",
          timestamp = nowMillis - (10 * oneDayMillis),
          dateEpochDay = todayEpochDay - 10,
          isAutoCaptured = false
        ),
        TransactionEntity(
          id = 9L,
          goalId = 1L,
          amount = 11700.0,
          channel = Channel.CASH.name,
          note = "Envelope cash reserve baseline",
          timestamp = nowMillis - (12 * oneDayMillis),
          dateEpochDay = todayEpochDay - 12,
          isAutoCaptured = false
        ),
        TransactionEntity(
          id = 10L,
          goalId = 1L,
          amount = 6800.0,
          channel = Channel.OTHER.name,
          note = "Fixed Deposit interest payout",
          timestamp = nowMillis - (14 * oneDayMillis),
          dateEpochDay = todayEpochDay - 14,
          isAutoCaptured = false
        )
      )

      dao.insertTransactions(seedTransactions)
    }
  }
}

package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FrequencyPref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
  entities = [GoalEntity::class, TransactionEntity::class],
  version = 3,
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

      // Clean default target with ₹0 initial saved amount and real dynamic deadline
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
      // Zero fake/mock transactions! Starts clean and 100% dynamic.
    }
  }
}

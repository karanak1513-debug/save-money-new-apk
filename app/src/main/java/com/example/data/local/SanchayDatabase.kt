package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope

@Database(
  entities = [GoalEntity::class, TransactionEntity::class],
  version = 3,
  exportSchema = false
)
abstract class SanchayDatabase : RoomDatabase() {
  abstract fun sanchayDao(): SanchayDao

  companion object {
    private const val TAG = "SanchayDatabase"

    @Volatile
    private var INSTANCE: SanchayDatabase? = null

    fun getDatabase(context: Context): SanchayDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = try {
          Room.databaseBuilder(
            context.applicationContext,
            SanchayDatabase::class.java,
            "sanchay_database"
          )
            .fallbackToDestructiveMigration(true)
            .build()
        } catch (e: Throwable) {
          Log.e(TAG, "Error building room database on disk, falling back safely to in-memory: ${e.message}", e)
          Room.inMemoryDatabaseBuilder(context.applicationContext, SanchayDatabase::class.java)
            .fallbackToDestructiveMigration(true)
            .build()
        }
        INSTANCE = instance
        instance
      }
    }

    fun getDatabase(context: Context, scope: CoroutineScope): SanchayDatabase {
      return getDatabase(context)
    }
  }
}

package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<WorkoutHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: WorkoutHistoryItem)

    @Query("DELETE FROM workout_history")
    suspend fun clearHistory()
}

@Database(entities = [WorkoutHistoryItem::class], version = 1, exportSchema = false)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract val dao: WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class WorkoutRepository(private val dao: WorkoutDao) {
    val history: Flow<List<WorkoutHistoryItem>> = dao.getAllHistory()

    suspend fun saveHistory(item: WorkoutHistoryItem) {
        dao.insertHistory(item)
    }

    suspend fun resetAll() {
        dao.clearHistory()
    }
}

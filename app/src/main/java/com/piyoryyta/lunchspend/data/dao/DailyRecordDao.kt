package com.piyoryyta.lunchspend.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piyoryyta.lunchspend.data.entity.DailyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(dailyRecord: DailyRecord): Long

    @Update
    suspend fun update(dailyRecord: DailyRecord)

    @Delete
    suspend fun delete(dailyRecord: DailyRecord)

    @Query("SELECT * FROM daily_records WHERE date = :date")
    suspend fun getByDate(date: String): DailyRecord?

    @Query("SELECT * FROM daily_records WHERE date = :date")
    fun observeByDate(date: String): Flow<DailyRecord?>

    @Query(
        "SELECT * FROM daily_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC",
    )
    fun observeBetween(startDate: String, endDate: String): Flow<List<DailyRecord>>
}

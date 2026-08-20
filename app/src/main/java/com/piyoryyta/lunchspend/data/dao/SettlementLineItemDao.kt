package com.piyoryyta.lunchspend.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piyoryyta.lunchspend.data.entity.SettlementLineItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementLineItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: SettlementLineItem): Long

    @Update
    suspend fun update(item: SettlementLineItem)

    @Delete
    suspend fun delete(item: SettlementLineItem)

    @Query("SELECT * FROM settlement_line_items WHERE id = :id")
    suspend fun getById(id: Long): SettlementLineItem?

    @Query("SELECT * FROM settlement_line_items WHERE dailyRecordId = :dailyRecordId")
    fun observeByDailyRecord(dailyRecordId: Long): Flow<List<SettlementLineItem>>

    @Query("SELECT * FROM settlement_line_items WHERE productId = :productId ORDER BY createdAt DESC")
    fun observeByProduct(productId: Long): Flow<List<SettlementLineItem>>
}

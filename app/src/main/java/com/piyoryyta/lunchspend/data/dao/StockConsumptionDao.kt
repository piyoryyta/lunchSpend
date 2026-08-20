package com.piyoryyta.lunchspend.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.piyoryyta.lunchspend.data.entity.StockConsumption

@Dao
interface StockConsumptionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(consumption: StockConsumption): Long

    @Delete
    suspend fun delete(consumption: StockConsumption)

    @Query("SELECT * FROM stock_consumptions WHERE settlementLineItemId = :settlementLineItemId")
    suspend fun getByLineItem(settlementLineItemId: Long): List<StockConsumption>

    @Query("DELETE FROM stock_consumptions WHERE settlementLineItemId = :settlementLineItemId")
    suspend fun deleteByLineItem(settlementLineItemId: Long)

    @Query("SELECT * FROM stock_consumptions WHERE stockLotId = :stockLotId")
    suspend fun getByStockLot(stockLotId: Long): List<StockConsumption>
}

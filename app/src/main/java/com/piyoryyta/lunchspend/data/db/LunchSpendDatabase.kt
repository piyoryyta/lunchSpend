package com.piyoryyta.lunchspend.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.piyoryyta.lunchspend.data.dao.DailyRecordDao
import com.piyoryyta.lunchspend.data.dao.ProductDao
import com.piyoryyta.lunchspend.data.dao.SettlementLineItemDao
import com.piyoryyta.lunchspend.data.dao.StockConsumptionDao
import com.piyoryyta.lunchspend.data.dao.StockLotDao
import com.piyoryyta.lunchspend.data.entity.DailyRecord
import com.piyoryyta.lunchspend.data.entity.Product
import com.piyoryyta.lunchspend.data.entity.SettlementLineItem
import com.piyoryyta.lunchspend.data.entity.StockConsumption
import com.piyoryyta.lunchspend.data.entity.StockLot

/**
 * アプリのRoomデータベース (SPEC.md 3章)。
 *
 * バージョンは破壊的変更を避け、スキーマ変更時は Migration を追加していく方針とする (6章)。
 */
@Database(
    entities = [
        Product::class,
        StockLot::class,
        DailyRecord::class,
        SettlementLineItem::class,
        StockConsumption::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LunchSpendDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun stockLotDao(): StockLotDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun settlementLineItemDao(): SettlementLineItemDao
    abstract fun stockConsumptionDao(): StockConsumptionDao

    companion object {
        const val DATABASE_NAME = "lunchspend.db"
    }
}

package com.piyoryyta.lunchspend

import android.app.Application
import androidx.room.Room
import com.piyoryyta.lunchspend.data.db.LunchSpendDatabase
import com.piyoryyta.lunchspend.data.repository.ProductRepository
import com.piyoryyta.lunchspend.data.repository.SettlementRepository
import com.piyoryyta.lunchspend.data.repository.StockRepository

/**
 * シンプルな手動DIコンテナ。
 *
 * 単一ユーザー・ローカル動作のみのアプリ (SPEC.md 1.2) のため、Hilt 等の DI フレームワークは
 * 導入せず、Application クラスで DB とリポジトリを遅延生成して各画面から参照する。
 */
class LunchSpendApplication : Application() {

    val database: LunchSpendDatabase by lazy {
        Room.databaseBuilder(this, LunchSpendDatabase::class.java, LunchSpendDatabase.DATABASE_NAME)
            .build()
    }

    val productRepository: ProductRepository by lazy {
        ProductRepository(database.productDao())
    }

    val stockRepository: StockRepository by lazy {
        StockRepository(database, database.stockLotDao(), database.productDao())
    }

    val settlementRepository: SettlementRepository by lazy {
        SettlementRepository(
            database,
            database.dailyRecordDao(),
            database.settlementLineItemDao(),
            database.stockConsumptionDao(),
            stockRepository,
        )
    }
}

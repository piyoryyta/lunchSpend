package com.piyoryyta.lunchspend.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.piyoryyta.lunchspend.data.db.LunchSpendDatabase
import com.piyoryyta.lunchspend.data.entity.StockLot
import com.piyoryyta.lunchspend.data.repository.ProductRepository
import com.piyoryyta.lunchspend.data.repository.SettlementRepository
import com.piyoryyta.lunchspend.data.repository.StockRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SettlementRepository.addProductLineItem の FIFO消費 (SPEC.md 4.1, 4.4) を
 * in-memory Room DB 経由で検証する。ProductRepositoryTest と同じRobolectricパターンを使う。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettlementRepositoryFifoTest {
    private lateinit var db: LunchSpendDatabase
    private lateinit var productRepository: ProductRepository
    private lateinit var stockRepository: StockRepository
    private lateinit var settlementRepository: SettlementRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LunchSpendDatabase::class.java,
        ).allowMainThreadQueries().build()
        productRepository = ProductRepository(db.productDao())
        stockRepository = StockRepository(db, db.stockLotDao(), db.productDao())
        settlementRepository = SettlementRepository(
            db,
            db.dailyRecordDao(),
            db.settlementLineItemDao(),
            db.stockConsumptionDao(),
            stockRepository,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** テスト用にロットを直接挿入する (remainingPieces を任意の値に固定できるようにするため)。 */
    private suspend fun insertLot(
        productId: Long,
        purchasedAt: Long,
        unitPriceAtPurchase: Int,
        unitsPerPackageAtPurchase: Int,
        totalPieces: Int,
        remainingPieces: Int = totalPieces,
    ): Long = db.stockLotDao().insert(
        StockLot(
            productId = productId,
            purchasedAt = purchasedAt,
            packageCount = 1,
            unitPriceAtPurchase = unitPriceAtPurchase,
            unitsPerPackageAtPurchase = unitsPerPackageAtPurchase,
            totalPieces = totalPieces,
            remainingPieces = remainingPieces,
        ),
    )

    /** SPEC.md 4.9 ステップ4: お茶6本入りを8本消費 → 760円、A→0本、B→4本。 */
    @Test
    fun fifoAcrossTwoLots_matchesSpecExample() = runBlocking {
        val productId = productRepository.create(name = "お茶 6本入り", defaultUnitPrice = 600, unitsPerPackage = 6)
        val lotAId = insertLot(productId, purchasedAt = 1_000, unitPriceAtPurchase = 600, unitsPerPackageAtPurchase = 6, totalPieces = 6)
        val lotBId = insertLot(productId, purchasedAt = 2_000, unitPriceAtPurchase = 480, unitsPerPackageAtPurchase = 6, totalPieces = 6)
        val dailyRecord = settlementRepository.getOrCreateDailyRecord("2026-08-21")

        val result = settlementRepository.addProductLineItem(dailyRecord.id, productId, quantity = 8)

        assertEquals(760, result.totalCost)
        assertEquals(0, result.shortagePieces)

        assertEquals(0, db.stockLotDao().getById(lotAId)?.remainingPieces)
        assertEquals(4, db.stockLotDao().getById(lotBId)?.remainingPieces)

        val consumptions = db.stockConsumptionDao().getByLineItem(result.lineItemId)
        assertEquals(2, consumptions.size)
        assertTrue(consumptions.any { it.stockLotId == lotAId && it.consumedPieces == 6 && it.unitCostPerPieceSnapshot == 100 })
        assertTrue(consumptions.any { it.stockLotId == lotBId && it.consumedPieces == 2 && it.unitCostPerPieceSnapshot == 80 })

        val lineItem = db.settlementLineItemDao().getById(result.lineItemId)
        assertEquals(productId, lineItem?.productId)
        assertEquals(8, lineItem?.quantity)
        assertEquals(760, lineItem?.totalCost)
    }

    /** SPEC.md 4.9 ステップ5: 残りB由来4本のみのところへ10本消費 → 920円、登録はブロックされない。 */
    @Test
    fun shortage_matchesSpecExample_andDoesNotBlockRegistration() = runBlocking {
        val productId = productRepository.create(name = "お茶 6本入り", defaultUnitPrice = 600, unitsPerPackage = 6)
        val lotBId = insertLot(
            productId,
            purchasedAt = 2_000,
            unitPriceAtPurchase = 480,
            unitsPerPackageAtPurchase = 6,
            totalPieces = 6,
            remainingPieces = 4,
        )
        val dailyRecord = settlementRepository.getOrCreateDailyRecord("2026-08-21")

        val result = settlementRepository.addProductLineItem(dailyRecord.id, productId, quantity = 10)

        assertEquals(920, result.totalCost)
        assertEquals(6, result.shortagePieces)
        assertEquals(0, db.stockLotDao().getById(lotBId)?.remainingPieces)

        val consumptions = db.stockConsumptionDao().getByLineItem(result.lineItemId)
        assertEquals(1, consumptions.size)
        assertEquals(lotBId, consumptions.single().stockLotId)
        assertEquals(4, consumptions.single().consumedPieces)
        assertEquals(80, consumptions.single().unitCostPerPieceSnapshot)
    }

    @Test
    fun noStockAtAll_isFullyShortageButStillRegisters() = runBlocking {
        val productId = productRepository.create(name = "都度買いしがちな商品", defaultUnitPrice = 150, unitsPerPackage = 1)
        val dailyRecord = settlementRepository.getOrCreateDailyRecord("2026-08-21")

        val result = settlementRepository.addProductLineItem(dailyRecord.id, productId, quantity = 3)

        assertEquals(450, result.totalCost)
        assertEquals(3, result.shortagePieces)
        assertTrue(db.stockConsumptionDao().getByLineItem(result.lineItemId).isEmpty())
    }

    @Test
    fun consumptionIsIsolatedPerProduct() = runBlocking {
        val productAId = productRepository.create(name = "商品A", defaultUnitPrice = 100, unitsPerPackage = 1)
        val productBId = productRepository.create(name = "商品B", defaultUnitPrice = 200, unitsPerPackage = 1)
        val lotAId = insertLot(productAId, purchasedAt = 1_000, unitPriceAtPurchase = 100, unitsPerPackageAtPurchase = 1, totalPieces = 5)
        val lotBId = insertLot(productBId, purchasedAt = 1_000, unitPriceAtPurchase = 200, unitsPerPackageAtPurchase = 1, totalPieces = 5)
        val dailyRecord = settlementRepository.getOrCreateDailyRecord("2026-08-21")

        settlementRepository.addProductLineItem(dailyRecord.id, productAId, quantity = 3)

        assertEquals(2, db.stockLotDao().getById(lotAId)?.remainingPieces)
        assertEquals(5, db.stockLotDao().getById(lotBId)?.remainingPieces)
    }

    /** SPEC.md 4.3: DB経由でも、ロットごとではなく明細単位で1回だけ丸められること。 */
    @Test
    fun roundsOnceAtLineItemLevel_throughRepository() = runBlocking {
        val productId = productRepository.create(name = "端数商品", defaultUnitPrice = 10, unitsPerPackage = 3)
        insertLot(productId, purchasedAt = 1_000, unitPriceAtPurchase = 10, unitsPerPackageAtPurchase = 3, totalPieces = 1)
        insertLot(productId, purchasedAt = 2_000, unitPriceAtPurchase = 10, unitsPerPackageAtPurchase = 3, totalPieces = 1)
        val dailyRecord = settlementRepository.getOrCreateDailyRecord("2026-08-21")

        val result = settlementRepository.addProductLineItem(dailyRecord.id, productId, quantity = 2)

        assertEquals(7, result.totalCost)
    }

    @Test
    fun invalidQuantity_throwsAndPersistsNothing() = runBlocking {
        val productId = productRepository.create(name = "商品", defaultUnitPrice = 100, unitsPerPackage = 1)
        insertLot(productId, purchasedAt = 1_000, unitPriceAtPurchase = 100, unitsPerPackageAtPurchase = 1, totalPieces = 5)
        val dailyRecord = settlementRepository.getOrCreateDailyRecord("2026-08-21")

        try {
            settlementRepository.addProductLineItem(dailyRecord.id, productId, quantity = 0)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }

        assertTrue(db.settlementLineItemDao().observeByDailyRecord(dailyRecord.id).first().isEmpty())
        assertEquals(5, db.stockLotDao().observeByProduct(productId).first().single().remainingPieces)
    }

    @Test
    fun unknownProduct_rollsBackAndThrows() = runBlocking {
        val dailyRecord = settlementRepository.getOrCreateDailyRecord("2026-08-21")

        try {
            settlementRepository.addProductLineItem(dailyRecord.id, productId = 999_999L, quantity = 3)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }

        assertTrue(db.settlementLineItemDao().observeByDailyRecord(dailyRecord.id).first().isEmpty())
    }
}

package com.piyoryyta.lunchspend.data.repository

import androidx.room.withTransaction
import com.piyoryyta.lunchspend.data.dao.ProductDao
import com.piyoryyta.lunchspend.data.dao.StockLotDao
import com.piyoryyta.lunchspend.data.db.LunchSpendDatabase
import com.piyoryyta.lunchspend.data.entity.StockLot
import com.piyoryyta.lunchspend.domain.fifo.FifoConsumptionCalculator
import com.piyoryyta.lunchspend.domain.fifo.FifoConsumptionPlan
import kotlinx.coroutines.flow.Flow

/**
 * 購入 (仕入れ) と在庫ロットの管理 (SPEC.md 2.2, 4.8)。
 */
class StockRepository(
    private val database: LunchSpendDatabase,
    private val stockLotDao: StockLotDao,
    private val productDao: ProductDao,
) {
    fun observeAllLots(): Flow<List<StockLot>> = stockLotDao.observeAll()

    fun observeLotsForProduct(productId: Long): Flow<List<StockLot>> =
        stockLotDao.observeByProduct(productId)

    fun observeRemainingPiecesForProduct(productId: Long): Flow<Int> =
        stockLotDao.observeRemainingPiecesForProduct(productId)

    /**
     * 購入を記録し、在庫ロットを1件生成する (SPEC.md 2.2)。
     *
     * @param overrideUnitPrice 臨時単価。指定しなければ商品の基準価格を使う。
     */
    suspend fun recordPurchase(
        productId: Long,
        packageCount: Int,
        overrideUnitPrice: Int? = null,
        purchasedAt: Long = System.currentTimeMillis(),
        memo: String? = null,
    ): Long {
        val product = requireNotNull(productDao.getById(productId)) {
            "Product $productId not found"
        }
        val unitPrice = overrideUnitPrice ?: product.defaultUnitPrice
        val unitsPerPackage = product.unitsPerPackage
        val totalPieces = packageCount * unitsPerPackage

        val lot = StockLot(
            productId = productId,
            purchasedAt = purchasedAt,
            packageCount = packageCount,
            unitPriceAtPurchase = unitPrice,
            unitsPerPackageAtPurchase = unitsPerPackage,
            totalPieces = totalPieces,
            remainingPieces = totalPieces,
            memo = memo,
        )
        return stockLotDao.insert(lot)
    }

    /**
     * 購入履歴 (StockLot) を削除する (SPEC.md 4.8)。
     *
     * remainingPieces が残っている分は在庫からそのまま減る。既に消費済みの分は
     * StockConsumption (CASCADE) ごと削除されるため、どのロット由来だったかの
     * トレーサビリティは失われるが、SettlementLineItem.totalCost 自体は変わらない。
     *
     * 呼び出し側は削除前に影響範囲をユーザーへ警告すること。
     */
    suspend fun deleteLot(lot: StockLot) {
        stockLotDao.delete(lot)
    }

    /**
     * 商品を FIFO で消費し、StockLot.remainingPieces を更新する (SPEC.md 4.1, 4.2, 4.4)。
     *
     * 在庫だけで賄えない場合はブロックせず、不足分は商品の現在の基準価格ベースで計上できるよう
     * [FifoConsumptionPlan.shortagePieces] / totalCost に反映して返す (4.4)。
     * StockConsumption / SettlementLineItem の作成は行わない (settlementLineItemId がまだ存在しないため) —
     * これは呼び出し側の [SettlementRepository.addProductLineItem] の責務。
     *
     * [database] 上でトランザクションを開始する。同一 database インスタンス上で呼び出し側が既に
     * withTransaction 中であれば、その外側トランザクションにネストして合流する (Room の仕様)。
     */
    suspend fun consumeFifo(productId: Long, quantity: Int): FifoConsumptionPlan {
        require(quantity > 0) { "quantity must be > 0" }
        return database.withTransaction {
            val product = requireNotNull(productDao.getById(productId)) {
                "Product $productId not found"
            }
            val lots = stockLotDao.getConsumableLotsForFifo(productId)
            val plan = FifoConsumptionCalculator.plan(
                lots = lots,
                quantity = quantity,
                fallbackUnitPrice = product.defaultUnitPrice,
                fallbackUnitsPerPackage = product.unitsPerPackage,
            )

            val lotsById = lots.associateBy { it.id }
            plan.lotConsumptions.forEach { consumption ->
                val lot = lotsById.getValue(consumption.stockLotId)
                stockLotDao.update(lot.copy(remainingPieces = lot.remainingPieces - consumption.consumedPieces))
            }
            plan
        }
    }
}

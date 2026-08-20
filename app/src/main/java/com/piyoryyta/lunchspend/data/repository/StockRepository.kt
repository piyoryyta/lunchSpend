package com.piyoryyta.lunchspend.data.repository

import com.piyoryyta.lunchspend.data.dao.ProductDao
import com.piyoryyta.lunchspend.data.dao.StockConsumptionDao
import com.piyoryyta.lunchspend.data.dao.StockLotDao
import com.piyoryyta.lunchspend.data.entity.StockLot
import kotlinx.coroutines.flow.Flow

/**
 * 購入 (仕入れ) と在庫ロットの管理 (SPEC.md 2.2, 4.8)。
 */
class StockRepository(
    private val stockLotDao: StockLotDao,
    private val stockConsumptionDao: StockConsumptionDao,
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

    // TODO(SPEC.md 4.1/4.2/4.4): FIFO消費本体 (consumeForSettlement) は精算機能の実装時に
    // SettlementRepository と合わせて追加する。ここでは購入・在庫照会のみを提供する。
}

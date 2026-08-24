package com.piyoryyta.lunchspend.data.repository

import androidx.room.withTransaction
import com.piyoryyta.lunchspend.data.dao.DailyRecordDao
import com.piyoryyta.lunchspend.data.dao.SettlementLineItemDao
import com.piyoryyta.lunchspend.data.dao.StockConsumptionDao
import com.piyoryyta.lunchspend.data.db.LunchSpendDatabase
import com.piyoryyta.lunchspend.data.entity.DailyRecord
import com.piyoryyta.lunchspend.data.entity.LineItemType
import com.piyoryyta.lunchspend.data.entity.SettlementLineItem
import com.piyoryyta.lunchspend.data.entity.StockConsumption
import kotlinx.coroutines.flow.Flow

/**
 * 精算 (日次記録) の読み書き (SPEC.md 2.3)。
 *
 * 商品ベース明細の登録 (4.1, 4.4) は StockRepository の FIFO消費と同一トランザクションで行う。
 * 明細の編集・削除 (4.7) は本クラスではまだ未実装 (TODO)。
 */
class SettlementRepository(
    private val database: LunchSpendDatabase,
    private val dailyRecordDao: DailyRecordDao,
    private val settlementLineItemDao: SettlementLineItemDao,
    private val stockConsumptionDao: StockConsumptionDao,
    private val stockRepository: StockRepository,
) {
    fun observeByDate(date: String): Flow<DailyRecord?> = dailyRecordDao.observeByDate(date)

    fun observeBetween(startDate: String, endDate: String): Flow<List<DailyRecord>> =
        dailyRecordDao.observeBetween(startDate, endDate)

    fun observeLineItems(dailyRecordId: Long): Flow<List<SettlementLineItem>> =
        settlementLineItemDao.observeByDailyRecord(dailyRecordId)

    suspend fun getOrCreateDailyRecord(date: String, now: Long = System.currentTimeMillis()): DailyRecord {
        dailyRecordDao.getByDate(date)?.let { return it }
        val record = DailyRecord(date = date, createdAt = now, updatedAt = now)
        val id = dailyRecordDao.insert(record)
        return record.copy(id = id)
    }

    /**
     * 都度アイテム (ad-hoc) 明細を追加する (SPEC.md 2.3)。
     * 単価×数量の概念を持たず、支払った金額をそのまま totalCost として保存する。
     */
    suspend fun addAdHocLineItem(
        dailyRecordId: Long,
        name: String,
        paidAmount: Int,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val item = SettlementLineItem(
            dailyRecordId = dailyRecordId,
            type = LineItemType.AD_HOC,
            adHocName = name,
            adHocPaidAmount = paidAmount,
            totalCost = paidAmount,
            createdAt = now,
        )
        return settlementLineItemDao.insert(item)
    }

    /**
     * 商品ベース明細を追加する (SPEC.md 2.3, 4.1, 4.4)。
     *
     * [StockRepository.consumeFifo] で在庫を FIFO 消費した上で、その結果を SettlementLineItem /
     * StockConsumption として永続化する。すべて同一 [database] 上の1トランザクションで行われ、
     * 途中で失敗した場合は全てロールバックされる。
     *
     * 在庫が不足していても登録はブロックしない (4.4)。不足があったかどうかは
     * [AddProductLineItemResult.shortagePieces] で呼び出し側 (UI層) に伝え、一過性の警告表示の
     * 判断に使ってもらう想定で、明細データ自体には永続化しない。
     */
    suspend fun addProductLineItem(
        dailyRecordId: Long,
        productId: Long,
        quantity: Int,
        now: Long = System.currentTimeMillis(),
    ): AddProductLineItemResult = database.withTransaction {
        val plan = stockRepository.consumeFifo(productId, quantity)

        val item = SettlementLineItem(
            dailyRecordId = dailyRecordId,
            type = LineItemType.PRODUCT,
            productId = productId,
            quantity = quantity,
            totalCost = plan.totalCost,
            createdAt = now,
        )
        val lineItemId = settlementLineItemDao.insert(item)

        plan.lotConsumptions.forEach { consumption ->
            stockConsumptionDao.insert(
                StockConsumption(
                    settlementLineItemId = lineItemId,
                    stockLotId = consumption.stockLotId,
                    consumedPieces = consumption.consumedPieces,
                    unitCostPerPieceSnapshot = consumption.unitCostPerPieceSnapshot,
                ),
            )
        }

        AddProductLineItemResult(
            lineItemId = lineItemId,
            totalCost = plan.totalCost,
            shortagePieces = plan.shortagePieces,
        )
    }

    // TODO(SPEC.md 4.7): 明細の編集・削除時は関連する StockConsumption を取り消し、
    // StockLot.remainingPieces を復元してから再計算する処理が必要。
}

/**
 * [SettlementRepository.addProductLineItem] の結果。
 *
 * @property shortagePieces 在庫不足だった数量 (SPEC.md 4.4)。0 より大きい場合、
 *   呼び出し側 (UI層) はその場限りの警告ダイアログを出すかどうかの判断に使う。
 */
data class AddProductLineItemResult(
    val lineItemId: Long,
    val totalCost: Int,
    val shortagePieces: Int,
)

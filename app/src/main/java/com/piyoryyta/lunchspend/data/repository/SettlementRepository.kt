package com.piyoryyta.lunchspend.data.repository

import com.piyoryyta.lunchspend.data.dao.DailyRecordDao
import com.piyoryyta.lunchspend.data.dao.SettlementLineItemDao
import com.piyoryyta.lunchspend.data.entity.DailyRecord
import com.piyoryyta.lunchspend.data.entity.SettlementLineItem
import kotlinx.coroutines.flow.Flow

/**
 * 精算 (日次記録) の読み書き (SPEC.md 2.3)。
 *
 * FIFO在庫消費を伴う商品ベース明細の登録・編集・削除 (4.1, 4.4, 4.7) はコアロジックであり、
 * StockRepository と合わせてトランザクション付きで実装する必要があるため、本スキャフォールドでは
 * 未実装 (TODO) とし、DailyRecord / SettlementLineItem の素朴な CRUD のみを提供する。
 */
class SettlementRepository(
    private val dailyRecordDao: DailyRecordDao,
    private val settlementLineItemDao: SettlementLineItemDao,
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
            type = com.piyoryyta.lunchspend.data.entity.LineItemType.AD_HOC,
            adHocName = name,
            adHocPaidAmount = paidAmount,
            totalCost = paidAmount,
            createdAt = now,
        )
        return settlementLineItemDao.insert(item)
    }

    // TODO(SPEC.md 4.1): 商品ベース明細の追加は StockRepository の FIFO消費と
    // 同一トランザクションで行う必要があるため未実装。
    // TODO(SPEC.md 4.7): 明細の編集・削除時は関連する StockConsumption を取り消し、
    // StockLot.remainingPieces を復元してから再計算する処理が必要。
}

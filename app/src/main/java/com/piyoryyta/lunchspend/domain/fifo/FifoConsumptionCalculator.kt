package com.piyoryyta.lunchspend.domain.fifo

import com.piyoryyta.lunchspend.data.entity.StockLot

/**
 * FIFO消費で1ロットから消し込まれた量 (SPEC.md 3.5 / 4.1)。
 *
 * [unitCostPerPieceSnapshot] は表示・監査用に丸めた値であり、原価集計には使わない
 * (集計は [FifoConsumptionPlan.totalCost] が明細単位で1回だけ丸めた値を持つ, SPEC.md 4.3)。
 */
data class FifoLotConsumption(
    val stockLotId: Long,
    val consumedPieces: Int,
    val unitCostPerPieceSnapshot: Int,
)

/**
 * [FifoConsumptionCalculator.plan] の結果。
 *
 * @property shortagePieces 在庫だけでは賄えなかった数量 (SPEC.md 4.4)。0 なら在庫のみで完結。
 */
data class FifoConsumptionPlan(
    val lotConsumptions: List<FifoLotConsumption>,
    val shortagePieces: Int,
    private val totalCostMilliYen: Long,
) {
    /** SPEC.md 4.3: ミリ円単位で積算した原価を、明細単位で最後に1回だけ四捨五入する。 */
    val totalCost: Int get() = roundHalfUpDiv(totalCostMilliYen, 1000L).toInt()
}

/**
 * FIFO在庫消費の計算ロジック (SPEC.md 4.1〜4.4)。
 *
 * Room/Android に依存しない純粋関数として実装し、[StockLot] のみを入力に取る。
 * DBへの副作用 (StockLot.remainingPieces の更新、StockConsumption/SettlementLineItem の作成) は
 * 呼び出し側 (StockRepository / SettlementRepository) の責務とする。
 */
object FifoConsumptionCalculator {
    /**
     * @param lots 消費対象ロット。`remainingPieces > 0` のものを `purchasedAt ASC, id ASC` で渡すこと
     *   ([com.piyoryyta.lunchspend.data.dao.StockLotDao.getConsumableLotsForFifo] がこの順序・条件で返す)。
     *   念のためこの関数自身も `remainingPieces <= 0` のロットは無視する。
     * @param quantity 消費したい数量 (個)。0以上。
     * @param fallbackUnitPrice 在庫不足分の計上に使う、商品の現在の基準価格 (SPEC.md 4.4)。
     * @param fallbackUnitsPerPackage 同上、商品の現在の入り数。
     */
    fun plan(
        lots: List<StockLot>,
        quantity: Int,
        fallbackUnitPrice: Int,
        fallbackUnitsPerPackage: Int,
    ): FifoConsumptionPlan {
        require(quantity >= 0) { "quantity must be >= 0" }

        var remainingNeeded = quantity
        val lotConsumptions = mutableListOf<FifoLotConsumption>()
        var totalMilliYen = 0L

        for (lot in lots) {
            if (remainingNeeded <= 0) break
            if (lot.remainingPieces <= 0) continue
            val consume = minOf(lot.remainingPieces, remainingNeeded)

            totalMilliYen += roundHalfUpDiv(
                consume.toLong() * lot.unitPriceAtPurchase * 1000L,
                lot.unitsPerPackageAtPurchase.toLong(),
            )
            lotConsumptions += FifoLotConsumption(
                stockLotId = lot.id,
                consumedPieces = consume,
                unitCostPerPieceSnapshot = roundHalfUpDiv(
                    lot.unitPriceAtPurchase.toLong(),
                    lot.unitsPerPackageAtPurchase.toLong(),
                ).toInt(),
            )
            remainingNeeded -= consume
        }

        val shortagePieces = remainingNeeded.coerceAtLeast(0)
        if (shortagePieces > 0) {
            totalMilliYen += roundHalfUpDiv(
                shortagePieces.toLong() * fallbackUnitPrice * 1000L,
                fallbackUnitsPerPackage.toLong(),
            )
        }

        return FifoConsumptionPlan(
            lotConsumptions = lotConsumptions,
            shortagePieces = shortagePieces,
            totalCostMilliYen = totalMilliYen,
        )
    }

    /** 非負の n, d (d > 0) に対する四捨五入(round-half-up)除算。 */
    private fun roundHalfUpDiv(n: Long, d: Long): Long = (2 * n + d) / (2 * d)
}

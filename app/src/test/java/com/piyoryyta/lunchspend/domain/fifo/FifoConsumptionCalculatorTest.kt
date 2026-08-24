package com.piyoryyta.lunchspend.domain.fifo

import com.piyoryyta.lunchspend.data.entity.StockLot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FifoConsumptionCalculator の純粋関数としてのユニットテスト。
 * Room/Robolectric に依存しないため通常のJVMユニットテストとして高速に実行できる。
 */
class FifoConsumptionCalculatorTest {

    private fun lot(
        id: Long,
        remainingPieces: Int,
        unitPriceAtPurchase: Int,
        unitsPerPackageAtPurchase: Int,
        productId: Long = 1,
        purchasedAt: Long = id,
    ) = StockLot(
        id = id,
        productId = productId,
        purchasedAt = purchasedAt,
        packageCount = 1,
        unitPriceAtPurchase = unitPriceAtPurchase,
        unitsPerPackageAtPurchase = unitsPerPackageAtPurchase,
        totalPieces = remainingPieces,
        remainingPieces = remainingPieces,
    )

    @Test
    fun singleLot_exactFit() {
        val plan = FifoConsumptionCalculator.plan(
            lots = listOf(lot(id = 1, remainingPieces = 5, unitPriceAtPurchase = 100, unitsPerPackageAtPurchase = 1)),
            quantity = 5,
            fallbackUnitPrice = 100,
            fallbackUnitsPerPackage = 1,
        )

        assertEquals(listOf(FifoLotConsumption(1, 5, 100)), plan.lotConsumptions)
        assertEquals(0, plan.shortagePieces)
        assertEquals(500, plan.totalCost)
    }

    /** SPEC.md 4.9 ステップ4: お茶6本入り、8本消費 → 760円。 */
    @Test
    fun spec4_9_step4_consumeAcrossTwoLots() {
        val lotA = lot(id = 1, remainingPieces = 6, unitPriceAtPurchase = 600, unitsPerPackageAtPurchase = 6, purchasedAt = 1)
        val lotB = lot(id = 2, remainingPieces = 6, unitPriceAtPurchase = 480, unitsPerPackageAtPurchase = 6, purchasedAt = 2)

        val plan = FifoConsumptionCalculator.plan(
            lots = listOf(lotA, lotB),
            quantity = 8,
            fallbackUnitPrice = 600,
            fallbackUnitsPerPackage = 6,
        )

        assertEquals(
            listOf(FifoLotConsumption(1, 6, 100), FifoLotConsumption(2, 2, 80)),
            plan.lotConsumptions,
        )
        assertEquals(0, plan.shortagePieces)
        assertEquals(760, plan.totalCost)
    }

    /** SPEC.md 4.9 ステップ5: 残りB由来4本のみのところへ10本消費 → 920円 (6本は基準価格で補完)。 */
    @Test
    fun spec4_9_step5_shortageFallsBackToDefaultPrice() {
        val lotB = lot(id = 2, remainingPieces = 4, unitPriceAtPurchase = 480, unitsPerPackageAtPurchase = 6)

        val plan = FifoConsumptionCalculator.plan(
            lots = listOf(lotB),
            quantity = 10,
            fallbackUnitPrice = 600,
            fallbackUnitsPerPackage = 6,
        )

        assertEquals(listOf(FifoLotConsumption(2, 4, 80)), plan.lotConsumptions)
        assertEquals(6, plan.shortagePieces)
        assertEquals(920, plan.totalCost)
    }

    @Test
    fun exhaustedLotsAheadInListAreSkipped() {
        // Defensive: even if the caller passes an already-exhausted lot (remainingPieces=0),
        // the calculator should not choke on it or try to consume from it.
        val exhaustedA = lot(id = 1, remainingPieces = 0, unitPriceAtPurchase = 600, unitsPerPackageAtPurchase = 6, purchasedAt = 1)
        val lotB = lot(id = 2, remainingPieces = 4, unitPriceAtPurchase = 480, unitsPerPackageAtPurchase = 6, purchasedAt = 2)

        val plan = FifoConsumptionCalculator.plan(
            lots = listOf(exhaustedA, lotB),
            quantity = 10,
            fallbackUnitPrice = 600,
            fallbackUnitsPerPackage = 6,
        )

        assertEquals(listOf(FifoLotConsumption(2, 4, 80)), plan.lotConsumptions)
        assertEquals(6, plan.shortagePieces)
        assertEquals(920, plan.totalCost)
    }

    @Test
    fun noStockAtAll_fullyShortage() {
        val plan = FifoConsumptionCalculator.plan(
            lots = emptyList(),
            quantity = 5,
            fallbackUnitPrice = 100,
            fallbackUnitsPerPackage = 1,
        )

        assertTrue(plan.lotConsumptions.isEmpty())
        assertEquals(5, plan.shortagePieces)
        assertEquals(500, plan.totalCost)
    }

    @Test
    fun spansThreeLotsExactly() {
        val lotX = lot(id = 1, remainingPieces = 2, unitPriceAtPurchase = 30, unitsPerPackageAtPurchase = 3, purchasedAt = 1) // 10/piece
        val lotY = lot(id = 2, remainingPieces = 3, unitPriceAtPurchase = 60, unitsPerPackageAtPurchase = 3, purchasedAt = 2) // 20/piece
        val lotZ = lot(id = 3, remainingPieces = 10, unitPriceAtPurchase = 90, unitsPerPackageAtPurchase = 3, purchasedAt = 3) // 30/piece

        val plan = FifoConsumptionCalculator.plan(
            lots = listOf(lotX, lotY, lotZ),
            quantity = 6,
            fallbackUnitPrice = 90,
            fallbackUnitsPerPackage = 3,
        )

        assertEquals(
            listOf(
                FifoLotConsumption(1, 2, 10),
                FifoLotConsumption(2, 3, 20),
                FifoLotConsumption(3, 1, 30),
            ),
            plan.lotConsumptions,
        )
        assertEquals(0, plan.shortagePieces)
        assertEquals(110, plan.totalCost)
    }

    @Test
    fun zeroQuantity_returnsEmptyPlan() {
        val plan = FifoConsumptionCalculator.plan(
            lots = listOf(lot(id = 1, remainingPieces = 5, unitPriceAtPurchase = 100, unitsPerPackageAtPurchase = 1)),
            quantity = 0,
            fallbackUnitPrice = 100,
            fallbackUnitsPerPackage = 1,
        )

        assertTrue(plan.lotConsumptions.isEmpty())
        assertEquals(0, plan.shortagePieces)
        assertEquals(0, plan.totalCost)
    }

    /**
     * SPEC.md 4.3: ロットごとに丸めてから合計するのではなく、明細単位で最後に1回だけ丸める必要がある。
     * 2ロットとも 10/3 = 3.333...円/個。ロットごとに丸める誤った実装なら 3+3=6円になってしまうが、
     * ミリ円のまま合計してから1回だけ四捨五入すると 6.666... → 7円になる。
     */
    @Test
    fun roundsOnceAtLineItemLevel_notPerLot() {
        val lotP = lot(id = 1, remainingPieces = 1, unitPriceAtPurchase = 10, unitsPerPackageAtPurchase = 3, purchasedAt = 1)
        val lotQ = lot(id = 2, remainingPieces = 1, unitPriceAtPurchase = 10, unitsPerPackageAtPurchase = 3, purchasedAt = 2)

        val plan = FifoConsumptionCalculator.plan(
            lots = listOf(lotP, lotQ),
            quantity = 2,
            fallbackUnitPrice = 10,
            fallbackUnitsPerPackage = 3,
        )

        assertEquals(7, plan.totalCost)
    }
}

package com.piyoryyta.lunchspend.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 在庫ロット＝購入記録 (SPEC.md 3.2)。
 *
 * 1回の購入で1件生成される。数量の内部単位は常に「個 (piece)」で統一する。
 * Product 側の入り数が将来変更されても過去ロットの計算に影響しないよう、
 * 購入時点の入り数を [unitsPerPackageAtPurchase] にスナップショットする。
 *
 * Product は関連する StockLot がある限り物理削除できない (4.8) ため、
 * onDelete は明示的な CASCADE にせず、削除はリポジトリ層で制御する。
 */
@Entity(
    tableName = "stock_lots",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
        ),
    ],
    indices = [Index("productId")],
)
data class StockLot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    /** 購入日時。FIFO順序の基準 (purchasedAt ASC, id ASC)。 */
    val purchasedAt: Long,
    /** 購入したパッケージ数。 */
    val packageCount: Int,
    /** このロットの実効パッケージ単価。臨時変更が無ければ購入時点の商品基準価格をコピーする。 */
    val unitPriceAtPurchase: Int,
    /** 購入時点の入り数のスナップショット。 */
    val unitsPerPackageAtPurchase: Int,
    /** packageCount × unitsPerPackageAtPurchase */
    val totalPieces: Int,
    /** 未消費の個数。消費のたびに減算する。 */
    val remainingPieces: Int,
    val memo: String? = null,
) {
    /**
     * ロットごとに固定される個あたり原価。FIFO原価計算のキモ (SPEC.md 3.2, 4.2)。
     * 端数処理は SettlementLineItem 単位で最後に1回だけ丸める (4.3) ため、
     * ここでは丸めずに Double のまま返す。
     */
    val unitCostPerPiece: Double
        get() = unitPriceAtPurchase.toDouble() / unitsPerPackageAtPurchase
}

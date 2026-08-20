package com.piyoryyta.lunchspend.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * FIFO消費の内訳、監査・トレーサビリティ用 (SPEC.md 3.5)。
 *
 * 精算明細の削除・編集時にどのロットへ在庫を戻せばよいかを特定するために必要。
 * StockLot が削除されると、その消費を裏付ける行も一緒に削除される (CASCADE, 4.8)。
 * SettlementLineItem が削除・再計算される際も、対応する行は削除される (CASCADE, 4.7)。
 */
@Entity(
    tableName = "stock_consumptions",
    foreignKeys = [
        ForeignKey(
            entity = SettlementLineItem::class,
            parentColumns = ["id"],
            childColumns = ["settlementLineItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StockLot::class,
            parentColumns = ["id"],
            childColumns = ["stockLotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("settlementLineItemId"), Index("stockLotId")],
)
data class StockConsumption(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val settlementLineItemId: Long,
    val stockLotId: Long,
    /** このロットから消費した個数。 */
    val consumedPieces: Int,
    /**
     * 消費時点のそのロットの個あたり単価 (= StockLot.unitCostPerPiece)。
     * 冗長だが、ロット自体が後で削除されても履歴を保護するために保存する。
     */
    val unitCostPerPieceSnapshot: Int,
)

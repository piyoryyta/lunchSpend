package com.piyoryyta.lunchspend.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 精算明細の種別 (SPEC.md 3.4)。 */
enum class LineItemType {
    /** 商品ベースの明細。在庫を FIFO で消費する。 */
    PRODUCT,

    /** 都度アイテム。商品未登録のその場買いを名前と金額でそのまま記録する。 */
    AD_HOC,
}

/**
 * 精算明細 (SPEC.md 3.4)。DailyRecord 1件に対して複数の明細を持つ。
 *
 * 在庫不足時の警告は登録操作中の一過性のダイアログ表示のみで、
 * このエンティティにはその情報を永続化するフィールドを持たせない (4.4)。
 */
@Entity(
    tableName = "settlement_line_items",
    foreignKeys = [
        ForeignKey(
            entity = DailyRecord::class,
            parentColumns = ["id"],
            childColumns = ["dailyRecordId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
        ),
    ],
    indices = [Index("dailyRecordId"), Index("productId")],
)
data class SettlementLineItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dailyRecordId: Long,
    val type: LineItemType,
    /** PRODUCT 時のみ。 */
    val productId: Long? = null,
    /** PRODUCT 時のみ。消費数量 (個数)。 */
    val quantity: Int? = null,
    /** AD_HOC 時のみ。その場買い品の名前。 */
    val adHocName: String? = null,
    /** AD_HOC 時のみ。その場で払った金額そのもの (単価×数量ではない)。 */
    val adHocPaidAmount: Int? = null,
    /**
     * この明細の確定原価。
     * PRODUCT: FIFO計算結果 (+在庫不足分の基準価格計上額) の合計を丸めた値。
     * AD_HOC: adHocPaidAmount と同値。
     */
    val totalCost: Int,
    val createdAt: Long,
)

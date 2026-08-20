package com.piyoryyta.lunchspend.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 商品マスタ (SPEC.md 3.1).
 *
 * [defaultUnitPrice] はあくまで「次に購入する時のデフォルト値」であり、実際の原価計算は
 * 各購入 ([StockLot]) ごとの実売価格を使う (SPEC.md 4.5)。
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** 円。デフォルトのパッケージ単価。 */
    val defaultUnitPrice: Int,
    /** 1パッケージあたりの個数 (例: 6個入り)。 */
    val unitsPerPackage: Int,
    val category: String? = null,
    val memo: String? = null,
    /** 論理削除フラグ。関連する StockLot / SettlementLineItem がある場合は物理削除できない (4.8)。 */
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /**
     * 表示用の派生値。DBには保存しない。
     */
    val defaultUnitCostPerPiece: Double
        get() = defaultUnitPrice.toDouble() / unitsPerPackage
}

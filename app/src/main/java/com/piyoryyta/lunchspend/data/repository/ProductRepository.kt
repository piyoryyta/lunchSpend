package com.piyoryyta.lunchspend.data.repository

import com.piyoryyta.lunchspend.data.dao.ProductDao
import com.piyoryyta.lunchspend.data.entity.Product
import kotlinx.coroutines.flow.Flow

/**
 * 商品マスタの CRUD (SPEC.md 2.1)。
 *
 * 基本的な読み書きのみを提供する。FIFO在庫消費など精算まわりのコアロジックは
 * [SettlementRepository] / [StockRepository] 側の責務とする (4章)。
 */
class ProductRepository(
    private val productDao: ProductDao,
) {
    fun observeActive(): Flow<List<Product>> = productDao.observeByArchivedState(isArchived = false)

    fun observeArchived(): Flow<List<Product>> = productDao.observeByArchivedState(isArchived = true)

    fun observeAll(): Flow<List<Product>> = productDao.observeAll()

    fun observeById(id: Long): Flow<Product?> = productDao.observeById(id)

    suspend fun getById(id: Long): Product? = productDao.getById(id)

    suspend fun create(
        name: String,
        defaultUnitPrice: Int,
        unitsPerPackage: Int,
        category: String? = null,
        memo: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val product = Product(
            name = name,
            defaultUnitPrice = defaultUnitPrice,
            unitsPerPackage = unitsPerPackage,
            category = category,
            memo = memo,
            createdAt = now,
            updatedAt = now,
        )
        return productDao.insert(product)
    }

    /** Product編集時は updatedAt を更新するのみで、価格履歴は保持しない (SPEC.md 4.5)。 */
    suspend fun update(product: Product, now: Long = System.currentTimeMillis()) {
        productDao.update(product.copy(updatedAt = now))
    }

    suspend fun archive(product: Product, now: Long = System.currentTimeMillis()) {
        update(product.copy(isArchived = true), now)
    }

    suspend fun unarchive(product: Product, now: Long = System.currentTimeMillis()) {
        update(product.copy(isArchived = false), now)
    }

    /**
     * 関連する StockLot / SettlementLineItem が存在する場合は物理削除できない (SPEC.md 4.8)。
     * その場合は archive() を使うこと。
     *
     * @return 実際に削除できたら true。関連レコードが存在して削除できなければ false。
     */
    suspend fun deleteIfUnused(product: Product): Boolean {
        val hasStockLots = productDao.countStockLots(product.id) > 0
        val hasSettlementLineItems = productDao.countSettlementLineItems(product.id) > 0
        if (hasStockLots || hasSettlementLineItems) return false
        productDao.delete(product)
        return true
    }
}

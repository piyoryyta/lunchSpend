package com.piyoryyta.lunchspend.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piyoryyta.lunchspend.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    /** Product は関連レコードが無い場合のみ物理削除できる (SPEC.md 4.8)。呼び出し側で判定する。 */
    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<Product?>

    @Query(
        "SELECT * FROM products WHERE isArchived = :isArchived ORDER BY name ASC",
    )
    fun observeByArchivedState(isArchived: Boolean): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<Product>>

    @Query(
        """
        SELECT COUNT(*) FROM stock_lots WHERE productId = :productId
        """,
    )
    suspend fun countStockLots(productId: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM settlement_line_items WHERE productId = :productId
        """,
    )
    suspend fun countSettlementLineItems(productId: Long): Int
}

package com.piyoryyta.lunchspend.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piyoryyta.lunchspend.data.entity.StockLot
import kotlinx.coroutines.flow.Flow

@Dao
interface StockLotDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(stockLot: StockLot): Long

    @Update
    suspend fun update(stockLot: StockLot)

    /** SPEC.md 4.8: 購入履歴はユーザーがいつでも削除できる。 */
    @Delete
    suspend fun delete(stockLot: StockLot)

    @Query("SELECT * FROM stock_lots WHERE id = :id")
    suspend fun getById(id: Long): StockLot?

    /**
     * FIFO消費対象のロットを購入日時の古い順に取得する (SPEC.md 4.1)。
     * 同時刻の場合は id ASC (挿入順) で一意に順序付けする。
     */
    @Query(
        """
        SELECT * FROM stock_lots
        WHERE productId = :productId AND remainingPieces > 0
        ORDER BY purchasedAt ASC, id ASC
        """,
    )
    suspend fun getConsumableLotsForFifo(productId: Long): List<StockLot>

    @Query(
        "SELECT * FROM stock_lots WHERE productId = :productId ORDER BY purchasedAt DESC, id DESC",
    )
    fun observeByProduct(productId: Long): Flow<List<StockLot>>

    @Query("SELECT * FROM stock_lots ORDER BY purchasedAt DESC, id DESC")
    fun observeAll(): Flow<List<StockLot>>

    @Query(
        "SELECT COALESCE(SUM(remainingPieces), 0) FROM stock_lots WHERE productId = :productId",
    )
    fun observeRemainingPiecesForProduct(productId: Long): Flow<Int>
}

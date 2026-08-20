package com.piyoryyta.lunchspend.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.piyoryyta.lunchspend.data.db.LunchSpendDatabase
import com.piyoryyta.lunchspend.data.repository.ProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * in-memory Room DB を使った基本的な CRUD の検証 (Robolectric によりJVM上で実行)。
 *
 * FIFO消費ロジック (SPEC.md 4.1〜4.4) を同じ方式でテストする際は、このクラスを
 * 参考に StockRepository / SettlementRepository のテストを追加していくこと。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductRepositoryTest {
    private lateinit var db: LunchSpendDatabase
    private lateinit var repository: ProductRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LunchSpendDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ProductRepository(db.productDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createAndFetchProduct() = runBlocking {
        val id = repository.create(name = "お茶 6本入り", defaultUnitPrice = 600, unitsPerPackage = 6)

        val product = repository.getById(id)

        assertEquals("お茶 6本入り", product?.name)
        assertEquals(600, product?.defaultUnitPrice)
        assertEquals(6, product?.unitsPerPackage)
        assertEquals(100.0, product?.defaultUnitCostPerPiece)
    }

    @Test
    fun archivedProductsAreExcludedFromActiveList() = runBlocking {
        val id = repository.create(name = "廃盤商品", defaultUnitPrice = 100, unitsPerPackage = 1)
        val product = requireNotNull(repository.getById(id))

        repository.archive(product)

        val active = repository.observeActive().first()
        val archived = repository.observeArchived().first()
        assertTrue(active.none { it.id == id })
        assertTrue(archived.any { it.id == id })
    }
}

package com.piyoryyta.lunchspend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.piyoryyta.lunchspend.LunchSpendApplication
import com.piyoryyta.lunchspend.data.entity.LineItemType
import com.piyoryyta.lunchspend.data.entity.Product
import com.piyoryyta.lunchspend.data.entity.SettlementLineItem
import com.piyoryyta.lunchspend.data.repository.ProductRepository
import com.piyoryyta.lunchspend.data.repository.SettlementRepository
import com.piyoryyta.lunchspend.ui.common.viewModelFactory
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeLineItemUi(
    val id: Long,
    val label: String,
    val quantity: Int?,
    val cost: Int,
)

data class HomeUiState(
    val date: String,
    val lineItems: List<HomeLineItemUi> = emptyList(),
    val total: Int = 0,
    val activeProducts: List<Product> = emptyList(),
)

/**
 * ホーム / 今日の精算 (SPEC.md 5.1, 5.2フロー3)。
 */
class HomeViewModel(
    private val settlementRepository: SettlementRepository,
    productRepository: ProductRepository,
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    /**
     * 今日の DailyRecord を必要なら作成しつつ、その明細一覧を購読する。
     */
    private val lineItemsFlow: Flow<List<SettlementLineItem>> = flow {
        val record = settlementRepository.getOrCreateDailyRecord(today)
        emitAll(settlementRepository.observeLineItems(record.id))
    }

    val uiState: StateFlow<HomeUiState> = combine(
        lineItemsFlow,
        productRepository.observeActive(),
    ) { items, products ->
        val nameById = products.associate { it.id to it.name }
        HomeUiState(
            date = today,
            lineItems = items.map { item ->
                HomeLineItemUi(
                    id = item.id,
                    label = when (item.type) {
                        LineItemType.PRODUCT -> nameById[item.productId] ?: "(削除済み商品)"
                        LineItemType.AD_HOC -> item.adHocName.orEmpty()
                    },
                    quantity = item.quantity,
                    cost = item.totalCost,
                )
            },
            total = items.sumOf { it.totalCost },
            activeProducts = products,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(date = today))

    private val _shortagePieces = MutableStateFlow<Int?>(null)
    /** SPEC.md 4.4: 在庫不足があった場合、一過性の警告ダイアログをUI側が出すためのイベント。 */
    val shortagePieces: StateFlow<Int?> = _shortagePieces

    /** 商品ベースの明細を追加する (SPEC.md 4.1, 4.4)。 */
    fun addProductLine(productId: Long, quantity: Int) {
        if (quantity <= 0) return
        viewModelScope.launch {
            val record = settlementRepository.getOrCreateDailyRecord(today)
            val result = settlementRepository.addProductLineItem(record.id, productId, quantity)
            if (result.shortagePieces > 0) {
                _shortagePieces.value = result.shortagePieces
            }
        }
    }

    /** 都度アイテムを追加する (SPEC.md 2.3)。 */
    fun addAdHocLine(name: String, paidAmount: Int) {
        if (name.isBlank() || paidAmount <= 0) return
        viewModelScope.launch {
            val record = settlementRepository.getOrCreateDailyRecord(today)
            settlementRepository.addAdHocLineItem(record.id, name.trim(), paidAmount)
        }
    }

    fun dismissShortageDialog() {
        _shortagePieces.value = null
    }

    companion object {
        fun factory(app: LunchSpendApplication): ViewModelProvider.Factory =
            viewModelFactory { HomeViewModel(app.settlementRepository, app.productRepository) }
    }
}

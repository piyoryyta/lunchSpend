package com.piyoryyta.lunchspend.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.piyoryyta.lunchspend.LunchSpendApplication
import com.piyoryyta.lunchspend.data.entity.Product
import com.piyoryyta.lunchspend.data.repository.ProductRepository
import com.piyoryyta.lunchspend.data.repository.StockRepository
import com.piyoryyta.lunchspend.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PurchaseViewModel(
    private val stockRepository: StockRepository,
    productRepository: ProductRepository,
) : ViewModel() {

    val activeProducts: StateFlow<List<Product>> = productRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    /** 購入を記録する (SPEC.md 2.2)。 */
    fun purchase(productId: Long, packageCount: Int, overrideUnitPrice: Int?) {
        if (packageCount <= 0) return
        viewModelScope.launch {
            stockRepository.recordPurchase(
                productId = productId,
                packageCount = packageCount,
                overrideUnitPrice = overrideUnitPrice,
            )
            _saved.value = true
        }
    }

    companion object {
        fun factory(app: LunchSpendApplication): ViewModelProvider.Factory =
            viewModelFactory { PurchaseViewModel(app.stockRepository, app.productRepository) }
    }
}

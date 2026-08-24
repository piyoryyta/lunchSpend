package com.piyoryyta.lunchspend.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.piyoryyta.lunchspend.LunchSpendApplication
import com.piyoryyta.lunchspend.data.entity.Product
import com.piyoryyta.lunchspend.data.repository.ProductRepository
import com.piyoryyta.lunchspend.ui.common.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val productRepository: ProductRepository,
) : ViewModel() {

    val products: StateFlow<List<Product>> = productRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 商品を登録する (SPEC.md 2.1)。 */
    fun addProduct(name: String, defaultUnitPrice: Int, unitsPerPackage: Int) {
        if (name.isBlank() || defaultUnitPrice <= 0 || unitsPerPackage <= 0) return
        viewModelScope.launch {
            productRepository.create(
                name = name.trim(),
                defaultUnitPrice = defaultUnitPrice,
                unitsPerPackage = unitsPerPackage,
            )
        }
    }

    companion object {
        fun factory(app: LunchSpendApplication): ViewModelProvider.Factory =
            viewModelFactory { ProductListViewModel(app.productRepository) }
    }
}

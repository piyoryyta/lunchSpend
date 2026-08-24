package com.piyoryyta.lunchspend.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Hilt等のDIフレームワークを使わない簡易DI用の [ViewModelProvider.Factory] 生成ヘルパー。
 *
 * 各ViewModelの companion object から以下のように使う:
 * ```
 * companion object {
 *     fun factory(app: LunchSpendApplication): ViewModelProvider.Factory =
 *         viewModelFactory { MyViewModel(app.someRepository) }
 * }
 * ```
 */
fun <VM : ViewModel> viewModelFactory(creator: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }

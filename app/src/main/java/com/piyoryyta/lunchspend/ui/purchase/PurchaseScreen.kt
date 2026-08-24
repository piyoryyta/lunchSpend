package com.piyoryyta.lunchspend.ui.purchase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piyoryyta.lunchspend.LunchSpendApplication

/**
 * 購入登録 (SPEC.md 5.1, 5.2フロー2)。
 *
 * @param preselectedProductId 商品一覧から遷移してきた場合の商品ID。null の場合は自分で選択する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    preselectedProductId: Long?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as LunchSpendApplication
    val viewModel: PurchaseViewModel = viewModel(factory = PurchaseViewModel.factory(app))
    val products by viewModel.activeProducts.collectAsState()
    val saved by viewModel.saved.collectAsState()

    var selectedProductId by remember(preselectedProductId) { mutableStateOf(preselectedProductId) }
    var productMenuExpanded by remember { mutableStateOf(false) }
    var packageCountText by remember { mutableStateOf("1") }
    var overridePriceText by remember { mutableStateOf("") }

    LaunchedEffect(saved) {
        if (saved) onSaved()
    }

    val selectedProduct = products.firstOrNull { it.id == selectedProductId }
    val packageCount = packageCountText.toIntOrNull()
    val overridePrice = overridePriceText.toIntOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("購入登録") },
                navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Text("商品", style = MaterialTheme.typography.titleMedium)
            Box {
                Button(onClick = { productMenuExpanded = true }) {
                    Text(selectedProduct?.name ?: "商品を選択")
                }
                DropdownMenu(expanded = productMenuExpanded, onDismissRequest = { productMenuExpanded = false }) {
                    products.forEach { product ->
                        DropdownMenuItem(
                            text = { Text(product.name) },
                            onClick = {
                                selectedProductId = product.id
                                productMenuExpanded = false
                            },
                        )
                    }
                }
            }
            selectedProduct?.let {
                Text(
                    "基準価格: ${it.defaultUnitPrice}円 / ${it.unitsPerPackage}個入り",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = packageCountText,
                onValueChange = { packageCountText = it.filter(Char::isDigit) },
                label = { Text("パッケージ数") },
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = overridePriceText,
                onValueChange = { overridePriceText = it.filter(Char::isDigit) },
                label = { Text("臨時単価 (円・任意。空欄なら基準価格)") },
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val productId = selectedProductId
                    if (productId != null && packageCount != null && packageCount > 0) {
                        viewModel.purchase(productId, packageCount, overridePrice)
                    }
                },
                enabled = selectedProductId != null && packageCount != null && packageCount > 0,
            ) {
                Text("保存")
            }
        }
    }
}

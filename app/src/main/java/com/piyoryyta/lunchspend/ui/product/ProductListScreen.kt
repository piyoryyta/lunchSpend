package com.piyoryyta.lunchspend.ui.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piyoryyta.lunchspend.LunchSpendApplication

/**
 * 商品一覧 (SPEC.md 5.1) — 商品登録と、商品を選んで購入登録画面へ進む導線を兼ねる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    onSelectProduct: (productId: Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as LunchSpendApplication
    val viewModel: ProductListViewModel = viewModel(factory = ProductListViewModel.factory(app))
    val products by viewModel.products.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price, units ->
                viewModel.addProduct(name, price, units)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("商品一覧") },
                navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") }
        },
    ) { paddingValues ->
        if (products.isEmpty()) {
            Box(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("商品がまだありません。右下の + から登録してください。")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                items(products, key = { it.id }) { product ->
                    ListItem(
                        headlineContent = { Text(product.name) },
                        supportingContent = {
                            Text("基準価格: ${product.defaultUnitPrice}円 / ${product.unitsPerPackage}個入り")
                        },
                        trailingContent = {
                            TextButton(onClick = { onSelectProduct(product.id) }) { Text("購入") }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, defaultUnitPrice: Int, unitsPerPackage: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var unitsText by remember { mutableStateOf("1") }

    val price = priceText.toIntOrNull()
    val units = unitsText.toIntOrNull()
    val isValid = name.isNotBlank() && price != null && price > 0 && units != null && units > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("商品を登録") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("商品名") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter(Char::isDigit) },
                    label = { Text("基準価格 (円)") },
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { unitsText = it.filter(Char::isDigit) },
                    label = { Text("1パッケージあたりの入り数") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (isValid) onConfirm(name, price!!, units!!) }, enabled = isValid) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

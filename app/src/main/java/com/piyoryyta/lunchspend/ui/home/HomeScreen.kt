package com.piyoryyta.lunchspend.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piyoryyta.lunchspend.LunchSpendApplication
import com.piyoryyta.lunchspend.R

/**
 * ホーム / 今日の精算 (SPEC.md 5.1, 5.2フロー3)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToProducts: () -> Unit) {
    val app = LocalContext.current.applicationContext as LunchSpendApplication
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val uiState by viewModel.uiState.collectAsState()
    val shortagePieces by viewModel.shortagePieces.collectAsState()

    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    var productMenuExpanded by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf("") }
    var adHocName by remember { mutableStateOf("") }
    var adHocAmountText by remember { mutableStateOf("") }

    if (shortagePieces != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissShortageDialog,
            confirmButton = {
                TextButton(onClick = viewModel::dismissShortageDialog) { Text("OK") }
            },
            title = { Text("在庫が不足しています") },
            text = { Text("${shortagePieces}個は在庫がなかったため、基準価格で計上しました。") },
        )
    }

    val selectedProduct = uiState.activeProducts.firstOrNull { it.id == selectedProductId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${stringResource(R.string.screen_home)} (${uiState.date})") },
                actions = {
                    TextButton(onClick = onNavigateToProducts) { Text("商品一覧") }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Text("合計: ${uiState.total}円", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.lineItems, key = { it.id }) { item ->
                    val quantity = item.quantity
                    ListItem(
                        headlineContent = { Text(item.label) },
                        supportingContent = if (quantity != null) {
                            { Text("数量: $quantity") }
                        } else {
                            null
                        },
                        trailingContent = { Text("${item.cost}円") },
                    )
                    HorizontalDivider()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text("商品から追加", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Button(onClick = { productMenuExpanded = true }) {
                        Text(selectedProduct?.name ?: "商品を選択")
                    }
                    DropdownMenu(expanded = productMenuExpanded, onDismissRequest = { productMenuExpanded = false }) {
                        uiState.activeProducts.forEach { product ->
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
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter(Char::isDigit) },
                    label = { Text("数量") },
                    modifier = Modifier.width(100.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val quantity = quantityText.toIntOrNull()
                Button(
                    onClick = {
                        val productId = selectedProductId
                        if (productId != null && quantity != null && quantity > 0) {
                            viewModel.addProductLine(productId, quantity)
                            quantityText = ""
                        }
                    },
                    enabled = selectedProductId != null && quantity != null && quantity > 0,
                ) { Text("追加") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("都度アイテムを追加", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = adHocName,
                    onValueChange = { adHocName = it },
                    label = { Text("品名") },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = adHocAmountText,
                    onValueChange = { adHocAmountText = it.filter(Char::isDigit) },
                    label = { Text("金額") },
                    modifier = Modifier.width(100.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val adHocAmount = adHocAmountText.toIntOrNull()
                Button(
                    onClick = {
                        if (adHocName.isNotBlank() && adHocAmount != null && adHocAmount > 0) {
                            viewModel.addAdHocLine(adHocName, adHocAmount)
                            adHocName = ""
                            adHocAmountText = ""
                        }
                    },
                    enabled = adHocName.isNotBlank() && adHocAmount != null && adHocAmount > 0,
                ) { Text("追加") }
            }
        }
    }
}

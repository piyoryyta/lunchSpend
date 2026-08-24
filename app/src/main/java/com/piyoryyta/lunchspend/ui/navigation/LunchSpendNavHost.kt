package com.piyoryyta.lunchspend.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.piyoryyta.lunchspend.R
import com.piyoryyta.lunchspend.ui.common.PlaceholderScreen
import com.piyoryyta.lunchspend.ui.home.HomeScreen
import com.piyoryyta.lunchspend.ui.product.ProductListScreen
import com.piyoryyta.lunchspend.ui.purchase.PurchaseScreen

/**
 * アプリ全体のナビゲーション (SPEC.md 5.1 の9画面)。
 *
 * ホーム(今日の精算)・商品一覧・購入登録の3画面は実装済み。それ以外はプレースホルダーであり、
 * 機能実装時に個別のComposableへ置き換える。
 */
@Composable
fun LunchSpendNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destination.Home.route) {
        composable(Destination.Home.route) {
            HomeScreen(onNavigateToProducts = { navController.navigate(Destination.ProductList.route) })
        }
        composable(Destination.ProductList.route) {
            ProductListScreen(
                onBack = { navController.popBackStack() },
                onSelectProduct = { productId ->
                    navController.navigate(Destination.Purchase.createRoute(productId))
                },
            )
        }
        composable(
            route = Destination.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.LongType }),
        ) {
            PlaceholderScreen(title = stringRes(R.string.screen_product_detail))
        }
        composable(
            route = Destination.Purchase.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: -1L
            PurchaseScreen(
                preselectedProductId = productId.takeIf { it >= 0 },
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Destination.SettlementEntry.route,
            arguments = listOf(
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            PlaceholderScreen(title = stringRes(R.string.screen_settlement_entry))
        }
        composable(
            route = Destination.DailyDetail.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(title = stringRes(R.string.screen_daily_detail))
        }
        composable(Destination.MonthlySummary.route) {
            PlaceholderScreen(title = stringRes(R.string.screen_monthly_summary))
        }
        composable(Destination.Inventory.route) {
            PlaceholderScreen(title = stringRes(R.string.screen_inventory))
        }
        composable(Destination.PurchaseHistory.route) {
            PlaceholderScreen(title = stringRes(R.string.screen_purchase_history))
        }
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

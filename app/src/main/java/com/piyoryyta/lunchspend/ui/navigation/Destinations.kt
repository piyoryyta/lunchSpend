package com.piyoryyta.lunchspend.ui.navigation

/**
 * 画面一覧 (SPEC.md 5.1)。
 *
 * 商品詳細・購入登録・精算入力・日次詳細は対象IDを引数に取るため、
 * NavHost 側でルート引数付きのパスを定義する。
 */
sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object ProductList : Destination("products")
    data object ProductDetail : Destination("products/{productId}") {
        fun createRoute(productId: Long) = "products/$productId"
    }
    data object Purchase : Destination("purchase?productId={productId}") {
        fun createRoute(productId: Long? = null) =
            if (productId != null) "purchase?productId=$productId" else "purchase"
    }
    data object SettlementEntry : Destination("settlement?date={date}") {
        fun createRoute(date: String? = null) =
            if (date != null) "settlement?date=$date" else "settlement"
    }
    data object DailyDetail : Destination("daily/{date}") {
        fun createRoute(date: String) = "daily/$date"
    }
    data object MonthlySummary : Destination("monthly-summary")
    data object Inventory : Destination("inventory")
    data object PurchaseHistory : Destination("purchase-history")
}

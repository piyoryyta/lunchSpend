package com.piyoryyta.lunchspend.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 未実装画面の仮表示。今後、各画面 (SPEC.md 5章) の実装に置き換える。
 */
@Composable
fun PlaceholderScreen(title: String, description: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        if (description != null) {
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

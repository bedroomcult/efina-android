package com.efina.app.ui.cashflow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.ui.components.charts.CashFlowLineChart
import com.efina.app.ui.components.charts.CashFlowPoint
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.LocalMoneyPalette
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.RevealLatch
import com.efina.app.ui.components.LoadingList
import com.efina.app.ui.components.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowScreen(viewModel: CashFlowViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    RevealLatch.currentScreen = "cashflow"

    WidthCapped(ContentWidth.List) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            if (state.loading) {
                LoadingList(rows = 6, modifier = Modifier.fillMaxWidth())
            } else if (state.error != null) {
                Column(Modifier.fillMaxSize()) {
                    ErrorState(message = state.error!!, onRetry = { viewModel.load() })
                }
            } else if (state.points.isEmpty()) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transaction data for the last 30 days.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            "Income", state.totalIncome,
                            LocalMoneyPalette.current.income,
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                        StatChip(
                            "Expense", state.totalExpense,
                            LocalMoneyPalette.current.expense,
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = EfinaCorners.menu,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Cash Flow", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            CashFlowLineChart(
                                points = state.points,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    amount: Double,
    color: Color,
    container: Color,
) {
    Surface(
        modifier = Modifier.padding(16.dp),
        shape = EfinaCorners.menu,
        color = container
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val abs = Math.abs(amount)
    return when {
        abs >= 1_000_000 -> "${(abs / 1_000_000).toInt()}M"
        abs >= 1_000 -> "${(abs / 1_000).toInt()}K"
        else -> amount.toInt().toString()
    }
}

package com.efina.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.TransactionType
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.Error
import com.efina.app.ui.theme.Success
import com.efina.app.ui.theme.Warning

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadDashboard() }
            )
            else -> {
                // Balance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Total Balance", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(uiState.totalBalance, uiState.currency),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Success
                        )
                        if (uiState.healthScore != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Health Score: ", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "${uiState.healthScore}/100",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = when {
                                        uiState.healthScore >= 80 -> Success
                                        uiState.healthScore >= 60 -> Warning
                                        else -> Error
                                    }
                                )
                            }
                            LinearProgressIndicator(
                                progress = { uiState.healthScore / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .padding(top = 4.dp),
                                color = when {
                                    uiState.healthScore >= 80 -> Success
                                    uiState.healthScore >= 60 -> Warning
                                    else -> Error
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                // Quick Actions
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(Icons.Default.Add, "Income", onNavigateToTransactions, Modifier.weight(1f))
                    QuickActionCard(Icons.Default.Remove, "Expense", onNavigateToTransactions, Modifier.weight(1f))
                    QuickActionCard(Icons.Default.SwapHoriz, "Transfer", onNavigateToTransactions, Modifier.weight(1f))
                }

                // Recent Transactions
                if (uiState.recentTransactions.isNotEmpty()) {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
                    uiState.recentTransactions.forEach { tx ->
                        TransactionItem(
                            description = tx.description ?: "",
                            categoryName = tx.categoryName,
                            categoryColor = tx.categoryColor?.let {
                                try { Color(android.graphics.Color.parseColor(it)) }
                                catch (_: Exception) { null }
                            },
                            amount = tx.amount,
                            isExpense = tx.type == TransactionType.EXPENSE,
                            date = tx.date.take(10),
                            currency = uiState.currency,
                            onClick = { }
                        )
                    }
                }

                // Menu
                Text("Manage", style = MaterialTheme.typography.titleMedium)
                MenuListItem(Icons.Default.AccountBalanceWallet, "Accounts",
                    "${uiState.accounts.size} accounts", onNavigateToAccounts)
                MenuListItem(Icons.Default.Receipt, "Transactions",
                    "View all transactions", onNavigateToTransactions)
                MenuListItem(Icons.Default.PieChart, "Budgets",
                    "${uiState.budgets.size} active budgets", onNavigateToBudgets)
                MenuListItem(Icons.Default.Flag, "Goals",
                    "${uiState.goals.size} goals", onNavigateToGoals)
                MenuListItem(Icons.Default.BarChart, "Statistics",
                    "View insights", onNavigateToStats)
            }
        }
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun MenuListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

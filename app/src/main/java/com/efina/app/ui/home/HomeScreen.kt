package com.efina.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.Transaction
import com.efina.app.domain.model.TransactionType
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaSemantic
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.LocalMoneyPalette
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.RevealLatch
import com.efina.app.ui.theme.springClick
import androidx.compose.foundation.lazy.rememberLazyListState
import com.efina.app.ui.components.ScrollToTopFab
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons

/**
 * Dashboard. Assembled from accounts + transactions + budgets + goals, per
 * HomeViewModel, because the backend has no single dashboard endpoint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    // Marks this screen as having played its entrance, so returning to the tab
    // does not replay the staggered reveal.
    RevealLatch.currentScreen = "home"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("efina", style = EfinaType.titleLargeEmphasized) },
                actions = {
                    TextButton(onClick = onNavigateToSettings) { Text("Settings") }
                }
            )
        },
        floatingActionButton = { ScrollToTopFab(listState) },
    ) { padding ->

        WidthCapped(ContentWidth.List) {        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (state.error != null) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorState(message = state.error!!, onRetry = { viewModel.load() })
                }
                return@PullToRefreshBox
            }

            if (state.loading) {
                Column(Modifier.padding(16.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                EfinaCorners.hero
                            )
                    )
                    Spacer(Modifier.height(16.dp))
                    LoadingList(rows = 4)
                }
            } else {
                val data = state.data
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { BalanceHero(data.totalBalance, data.accounts.count()) }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FlowStat(
                                "Income", data.monthlyIncome, MaterialTheme.colorScheme.surfaceContainerLow,
                                LocalMoneyPalette.current.income, Modifier.weight(1f)
                            )
                            FlowStat(
                                "Expense", data.monthlyExpense, MaterialTheme.colorScheme.surfaceContainerLow,
                                LocalMoneyPalette.current.expense, Modifier.weight(1f)
                            )
                        }
                    }

                    if (data.accounts.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Accounts",
                                action = {
                                    TextButton(onClick = onNavigateToAccounts) { Text("See all") }
                                }
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                data.accounts.take(4).forEachIndexed { index, account ->
                                    val last = index == minOf(3, data.accounts.lastIndex)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = if (index == 0 && last) EfinaCorners.groupOuter
                                        else if (index == 0) androidx.compose.foundation.shape.RoundedCornerShape(
                                            topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp
                                        )
                                        else if (last) androidx.compose.foundation.shape.RoundedCornerShape(
                                            topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp
                                        )
                                        else EfinaCorners.groupInner,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        onClick = onNavigateToAccounts
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AccountIconTile(account, size = 34)
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        account.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (account.isShared || account.role != null) {
                                                        Spacer(Modifier.width(6.dp))
                                                        SharedAccountBadge(account)
                                                    }
                                                }
                                                Text(
                                                    account.type.displayName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                formatMoney(account.balance, account.currency),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.healthScore != null) {
                        item { HealthCard(state.healthScore!!) }
                    }

                    if (data.budgets.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Budgets",
                                action = { TextButton(onClick = onNavigateToBudgets) { Text("See all") } }
                            )
                        }
                        items(minOf(3, data.budgets.size)) { i ->
                            val b = data.budgets[i]
                            Surface(
                                modifier = Modifier.fillMaxWidth()
                                    .springClick(EfinaCorners.menu, onClick = onNavigateToBudgets),
                                shape = EfinaCorners.menu,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row {
                                        Text(
                                            b.categoryName ?: "Category",
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${formatMoney(b.spent)} / ${formatMoney(b.amount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { (b.percentage / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = if (b.isOverBudget) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }

                    if (data.goals.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Goals",
                                action = { TextButton(onClick = onNavigateToGoals) { Text("See all") } }
                            )
                        }
                        items(minOf(2, data.goals.size)) { i ->
                            val g = data.goals[i]
                            Surface(
                                modifier = Modifier.fillMaxWidth()
                                    .springClick(EfinaCorners.menu, onClick = onNavigateToGoals),
                                shape = EfinaCorners.menu,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(g.name, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { g.progressPercentage / 100f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${formatMoney(g.currentAmount)} of ${formatMoney(g.targetAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Recent",
                            action = { TextButton(onClick = onNavigateToTransactions) { Text("See all") } }
                        )
                    }
                    if (data.recentTransactions.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Nothing this month",
                                message = "Record a transaction to see it here."
                            )
                        }
                    } else {
                        items(data.recentTransactions.size) { i ->
                            val currencyByAccount = data.accounts.associate { it.id to it.currency }
                            RecentRow(
                                tx = data.recentTransactions[i],
                                currency = currencyByAccount[data.recentTransactions[i].accountId]
                                    ?: "IDR",
                                onClick = onNavigateToTransactions
                            )
                        }
                    }
                }
            }
        }
    
        }
    }
}

@Composable
private fun BalanceHero(total: Double, accountCount: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, EfinaCorners.hero)
            .padding(22.dp)
    ) {
        Text(
            "Total balance",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(6.dp))
        AnimatedMoney(
            amount = total,
            style = EfinaType.amountLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            "$accountCount account${if (accountCount == 1) "" else "s"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun FlowStat(
    label: String,
    amount: Double,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .background(container, EfinaCorners.menu)
            .padding(14.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(formatMoney(amount), style = EfinaType.amountSmall, color = content)
    }
}

@Composable
private fun HealthCard(health: com.efina.app.domain.model.HealthScore) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = EfinaCorners.menu,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Financial health", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${health.factors.size} factors tracked",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${health.score}",
                style = EfinaType.amountMedium,
                color = when {
                    health.score >= 70 -> EfinaSemantic.safe
                    health.score >= 40 -> EfinaSemantic.caution
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun RecentRow(tx: Transaction, currency: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .springClick(EfinaCorners.menu, onClick = onClick),
        shape = EfinaCorners.groupInner,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Same direction icon as the full transaction list, so the two lists read
            // the same way.
            val money = LocalMoneyPalette.current
            val (tint, container, glyph) = when (tx.type) {
                TransactionType.INCOME ->
                    Triple(money.income, money.onIncome, Icons.Filled.ArrowDownward)
                TransactionType.EXPENSE ->
                    Triple(money.expense, money.onExpense, Icons.Filled.ArrowUpward)
                TransactionType.TRANSFER ->
                    Triple(money.transfer, money.onTransfer, Icons.Filled.SwapHoriz)
                TransactionType.ADJUSTMENT -> Triple(
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    Icons.Filled.Tune
                )
            }
            Box(
                Modifier.padding(end = 12.dp).size(36.dp).background(container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(glyph, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    tx.description ?: tx.categoryName ?: "Untitled",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    formatDate(tx.date) + (tx.accountName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            AmountText(tx.amount, tx.type, currency = currency)
        }
    }
}

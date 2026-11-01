package com.efina.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaSemantic
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.LocalMoneyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", style = EfinaType.titleLargeEmphasized) }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingList(rows = 6, modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        "Income", state.income, LocalMoneyPalette.current.income,
                        MaterialTheme.colorScheme.surfaceContainerLow, Modifier.weight(1f)
                    )
                    StatTile(
                        "Expense", state.expense, LocalMoneyPalette.current.expense,
                        MaterialTheme.colorScheme.surfaceContainerLow, Modifier.weight(1f)
                    )
                }
            }

            item {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = EfinaCorners.menu,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Net this month", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatMoney(state.net),
                            style = EfinaType.amountMedium,
                            color = if (state.net >= 0) LocalMoneyPalette.current.income
                            else LocalMoneyPalette.current.expense
                        )
                        Text(
                            "Savings rate ${"%.0f".format(state.savingsRate)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.health != null) {
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = EfinaCorners.menu,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Financial health", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${state.health!!.score}/100",
                                style = EfinaType.amountMedium,
                                color = when {
                                    state.health!!.score >= 70 -> EfinaSemantic.safe
                                    state.health!!.score >= 40 -> EfinaSemantic.caution
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            state.health!!.factors.entries.take(4).forEach { (key, factor) ->
                                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                    Text(
                                        key.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        factor.value ?: factor.label ?: "${factor.score}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.byCategory.isNotEmpty()) {
                item { SectionHeader("Where it went") }
                items(state.byCategory) { slice ->
                    val max = state.byCategory.maxOf { it.total }.coerceAtLeast(1.0)
                    Column(Modifier.fillMaxWidth()) {
                        Row {
                            Text(
                                slice.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                formatMoney(slice.total),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (slice.total / max).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }

            if (state.forecast != null && state.forecast!!.points.isNotEmpty()) {
                item { SectionHeader("Forecast") }
                items(state.forecast!!.points.take(3)) { point ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(point.month, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f))
                        Text(
                            formatMoney(point.predictedExpense),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.achievements.isNotEmpty()) {
                item { SectionHeader("Achievements") }
                items(state.achievements.take(5)) { a ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            a.description?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        if (a.achieved) PillTag("Unlocked")
                        else Text(
                            "${a.progress}/${a.target}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.leaderboard.isNotEmpty()) {
                item { SectionHeader("Leaderboard") }
                items(state.leaderboard) { entry ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(
                            "#${entry.rank}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(40.dp)
                        )
                        Text(
                            entry.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            "${entry.xp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    amount: Double,
    contentColor: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier.background(container, EfinaCorners.menu).padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(formatMoney(amount), style = EfinaType.amountSmall, color = contentColor)
    }
}

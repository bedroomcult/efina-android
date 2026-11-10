package com.efina.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.ui.components.charts.CashFlowLineChart
import com.efina.app.ui.components.charts.CashFlowPoint
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaSemantic
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.LocalMoneyPalette
import androidx.compose.foundation.layout.BoxWithConstraints
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.theme.RevealLatch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val scrollState = remember { ScrollState() }
    val coroutineScope = rememberCoroutineScope()
    // Marks this screen as having played its entrance, so returning to the tab
    // does not replay the staggered reveal.
    RevealLatch.currentScreen = "stats"

    // Width cap from design.md §11, applied to the Scaffold's own modifier so the
    // content lambda and its early returns are untouched.
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val side = ((maxWidth - ContentWidth.List.max.dp) / 2).coerceAtLeast(0.dp)
    Scaffold(
        modifier = Modifier.padding(horizontal = side),
        topBar = {
            TopAppBar(
                title = { Text("Statistics", style = EfinaType.titleLargeEmphasized, color = MaterialTheme.colorScheme.onSurface) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { coroutineScope.launch { scrollState.animateScrollTo(0) } },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Default.KeyboardArrowUp, contentDescription = "Scroll to top")
            }
        },
    ) { padding ->
        if (state.error != null) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                ErrorState(message = state.error!!, onRetry = { viewModel.load(state.timeRange) })
            }
            return@Scaffold
        }

        if (state.loading) {
            LoadingList(rows = 6, modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
        ) {
            // Time-range segmented button filter (pill style matching expense|income|transfer)
            val selectedIndex = TimeRange.entries.indexOf(state.timeRange)
            val moneyColors = listOf(
                LocalMoneyPalette.current.income,
                LocalMoneyPalette.current.expense,
                LocalMoneyPalette.current.transfer
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TimeRange.entries.forEachIndexed { index, range ->
                    val isSelected = index == selectedIndex
                    val accent = moneyColors[index]
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { viewModel.load(TimeRange.entries[index]) },
                        shape = SegmentedButtonDefaults.itemShape(index, TimeRange.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = accent,
                            activeContentColor = Color.White,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = range.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = EfinaCorners.menu,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Cash flow", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    CashFlowLineChart(
                        points = state.chartPoints,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            }

            if (state.health != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = EfinaCorners.menu,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Financial health", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
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

            if (state.byCategory.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Where it went")
                state.byCategory.forEach { slice ->
                    val max = state.byCategory.maxOf { it.total }.coerceAtLeast(1.0)
                    Column(Modifier.fillMaxWidth()) {
                        Row {
                            Text(
                                slice.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                formatMoney(slice.total),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Spacer(Modifier.height(12.dp))
                SectionHeader("Forecast")
                state.forecast!!.points.take(3).forEach { point ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(point.month, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Spacer(Modifier.height(12.dp))
                SectionHeader("Achievements")
                state.achievements.take(5).forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
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
                Spacer(Modifier.height(12.dp))
                SectionHeader("Leaderboard")
                state.leaderboard.forEach { entry ->
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
                            color = MaterialTheme.colorScheme.onSurface,
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
        AnimatedMoney(amount = amount, style = EfinaType.amountSmall, color = contentColor)
    }
}

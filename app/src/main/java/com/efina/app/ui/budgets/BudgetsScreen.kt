package com.efina.app.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.Budget
import com.efina.app.domain.model.CreateBudgetRequest
import com.efina.app.domain.model.PeriodType
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Budget?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Budgets", style = EfinaType.titleLargeEmphasized) },
                actions = {
                    IconButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add budget")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> LoadingList(rows = 5, modifier = Modifier.padding(padding).padding(16.dp))
            state.budgets.isEmpty() -> EmptyState(
                title = "No budgets yet",
                message = "Set a limit per category to keep spending in check.",
                modifier = Modifier.padding(padding),
                action = {
                    Button(onClick = { creating = true }, shape = EfinaCorners.menu) {
                        Text("Create budget")
                    }
                }
            )
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    BudgetSummaryCard(
                        total = state.totalBudget,
                        spent = state.totalSpent,
                        over = state.overBudgetCount
                    )
                }
                itemsIndexed(state.budgets, key = { _, b -> b.id }) { index, budget ->
                    val (alpha, offsetY) = revealState(index)
                    BudgetCard(
                        budget = budget,
                        modifier = Modifier.graphicsLayerCompat(alpha, offsetY),
                        onDelete = { pendingDelete = budget }
                    )
                }
            }
        }
    }

    if (creating) {
        CreateBudgetDialog(
            categories = state.categories,
            saving = state.saving,
            onDismiss = { creating = false },
            onCreate = { request ->
                viewModel.create(request) { creating = false }
            }
        )
    }

    pendingDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete budget?") },
            text = { Text("Spending in this category will no longer be tracked against a limit.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(b.id); pendingDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private fun Modifier.graphicsLayerCompat(alpha: Float, offsetY: Float): Modifier =
    this.then(androidx.compose.ui.graphics.graphicsLayer { this.alpha = alpha; translationY = offsetY })

@Composable
private fun BudgetSummaryCard(total: Double, spent: Double, over: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, EfinaCorners.hero)
            .padding(20.dp)
    ) {
        Text(
            "This period",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${formatMoney(spent)} of ${formatMoney(total)}",
            style = EfinaType.amountMedium
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (total <= 0.0) 0f else (spent / total).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (over > 0) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        if (over > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "$over budget${if (over == 1) "" else "s"} over the limit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun BudgetCard(
    budget: Budget,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit
) {
    val pct = budget.percentage / 100f
    val barColor = when {
        budget.isOverBudget -> MaterialTheme.colorScheme.error
        pct > 0.85f -> com.efina.app.ui.theme.EfinaSemantic.caution
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = EfinaCorners.menu,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        budget.categoryName ?: "Category",
                        style = EfinaType.titleMediumEmphasized,
                        maxLines = 1
                    )
                    Text(
                        budget.periodType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete budget",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    formatMoney(budget.spent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (budget.isOverBudget) "Over by ${formatMoney(-budget.remaining)}"
                    else "${formatMoney(budget.remaining)} left",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (budget.isOverBudget) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateBudgetDialog(
    categories: List<com.efina.app.domain.model.Category>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (CreateBudgetRequest) -> Unit
) {
    var categoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(PeriodType.MONTHLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (categories.isEmpty()) {
                    Text(
                        "Create a category first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.take(8).forEach { cat ->
                            FilterChip(
                                selected = categoryId == cat.id,
                                onClick = { categoryId = cat.id },
                                label = { Text(cat.name) },
                                shape = EfinaCorners.pill
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d{0,15}(\\.\\d{0,2})?$"))) {
                            amount = input
                        }
                    },
                    label = { Text("Limit") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = EfinaCorners.menu
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodType.entries.forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p.label) },
                            shape = EfinaCorners.pill
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && categoryId != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = {
                    onCreate(
                        CreateBudgetRequest(
                            categoryId = categoryId!!,
                            amount = amount.toDouble(),
                            periodType = period
                        )
                    )
                }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

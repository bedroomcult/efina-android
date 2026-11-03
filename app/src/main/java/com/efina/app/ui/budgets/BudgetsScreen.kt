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
import com.efina.app.ui.theme.revealModifier
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.RevealLatch
import com.efina.app.ui.theme.Haptic
import com.efina.app.ui.theme.rememberHaptics
import com.efina.app.ui.theme.springClick
import androidx.compose.foundation.lazy.rememberLazyListState
import com.efina.app.ui.components.ScrollToTopFab

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val haptics = rememberHaptics()
    // Marks this screen as having played its entrance, so returning to the tab
    // does not replay the staggered reveal.
    RevealLatch.currentScreen = "budgets"
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Budget?>(null) }
    var editing by remember { mutableStateOf<Budget?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editPeriod by remember { mutableStateOf(com.efina.app.domain.model.PeriodType.MONTHLY) }

    LaunchedEffect(state.error) {
        state.error?.let {
            // A failed save is the one moment a user needs to notice without
            // reading; the snackbar alone is easy to miss mid-scroll.
            haptics(Haptic.Reject)
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
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
        floatingActionButton = { ScrollToTopFab(listState) },
    ) { padding ->

        WidthCapped(ContentWidth.List) {        when {
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
                state = listState,
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
                    BudgetCard(
                        budget = budget,
                        modifier = revealModifier(index),
                        onDelete = { pendingDelete = budget },
                        onEdit = {
                            editing = budget
                            editAmount = budget.amount.toString()
                            editPeriod = budget.periodType
                        }
                    )
                }
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

    editing?.let { b ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d{0,15}(\\.\\d{0,2})?$"))) editAmount = input
                        },
                        label = { Text("Limit") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = EfinaCorners.menu
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.efina.app.domain.model.PeriodType.entries.forEach { p ->
                            FilterChip(
                                selected = editPeriod == p,
                                onClick = { editPeriod = p },
                                label = { Text(p.label) },
                                shape = EfinaCorners.pill
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.saving && (editAmount.toDoubleOrNull() ?: 0.0) > 0.0,
                    onClick = {
                        viewModel.update(
                            b.id,
                            com.efina.app.domain.model.UpdateBudgetRequest(
                                amount = editAmount.toDouble(),
                                periodType = editPeriod
                            )
                        ) { editing = null }
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }

    pendingDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete budget?") },
            text = { Text("Spending in this category will no longer be tracked against a limit.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(b.id); haptics(Haptic.Confirm); pendingDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

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
        // Fills from zero on entry, so the bar reads as progress being made rather than
        // as a value that was always there.
        val target = if (total <= 0.0) 0f else (spent / total).toFloat().coerceIn(0f, 1f)
        val filled by androidx.compose.animation.core.animateFloatAsState(
            targetValue = target,
            animationSpec = if (com.efina.app.ui.theme.EfinaMotion.enabled)
                androidx.compose.animation.core.tween(
                    durationMillis = 900,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            else androidx.compose.animation.core.tween(durationMillis = 0),
            label = "budgetFill"
        )
        LinearProgressIndicator(
            progress = { filled },
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
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val pct = budget.percentage / 100f
    val barColor = when {
        budget.isOverBudget -> MaterialTheme.colorScheme.error
        pct > 0.85f -> com.efina.app.ui.theme.EfinaSemantic.caution
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth()
            .springClick(EfinaCorners.menu, onClick = onEdit),
        shape = EfinaCorners.menu,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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

package com.efina.app.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.CreateDebtRequest
import com.efina.app.domain.model.Debt
import com.efina.app.domain.model.DebtType
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.LocalMoneyPalette
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.RevealLatch
import com.efina.app.ui.theme.Haptic
import com.efina.app.ui.theme.rememberHaptics
import androidx.compose.foundation.lazy.rememberLazyListState
import com.efina.app.ui.components.ScrollToTopFab

/**
 * Debts and receivables.
 *
 * `type` decides the direction: BORROW is money I owe, LEND is money owed to me.
 * The backend stores 'borrow' and 'lend' and books the payment as an expense for
 * the former and income for the latter.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DebtsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val haptics = rememberHaptics()
    // Marks this screen as having played its entrance, so returning to the tab
    // does not replay the staggered reveal.
    RevealLatch.currentScreen = "debts"
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<Debt?>(null) }
    var pendingDelete by remember { mutableStateOf<Debt?>(null) }

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
                title = { Text("Debts", style = EfinaType.titleLargeEmphasized) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add debt")
                    }
                }
            )
        },
        floatingActionButton = { ScrollToTopFab(listState) },
    ) { padding ->

        WidthCapped(ContentWidth.List) {        when {
            state.loading -> LoadingList(rows = 4, modifier = Modifier.padding(padding).padding(16.dp))
            state.debts.isEmpty() -> EmptyState(
                title = "Nothing tracked",
                message = "Record money you owe or money owed to you.",
                modifier = Modifier.padding(padding),
                action = {
                    Button(onClick = { creating = true }, shape = EfinaCorners.menu) {
                        Text("Add debt")
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DebtTile(
                            "I owe", state.owedByMe,
                            LocalMoneyPalette.current.expense,
                            Modifier.weight(1f)
                        )
                        DebtTile(
                            "Owed to me", state.owedToMe,
                            LocalMoneyPalette.current.income,
                            Modifier.weight(1f)
                        )
                    }
                }
                itemsIndexed(state.debts, key = { _, d -> d.id }) { index, debt ->
                    DebtCard(
                        debt = debt,
                        modifier = com.efina.app.ui.theme.revealModifier(index),
                        onPay = { paying = debt },
                        onDelete = { pendingDelete = debt }
                    )
                }
            }
        }
    
        }
    }

    if (creating) {
        CreateDebtDialog(
            saving = state.saving,
            onDismiss = { creating = false },
            onCreate = { viewModel.create(it) { creating = false } }
        )
    }

    paying?.let { debt ->
        PayDebtDialog(
            debt = debt,
            accounts = state.accounts.filter { it.canWrite },
            saving = state.saving,
            onDismiss = { paying = null },
            onPay = { amount, accountId -> viewModel.pay(debt.id, amount, accountId) { paying = null } }
        )
    }

    pendingDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${d.name}?") },
            text = { Text("Payment history on this debt is removed with it.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(d.id); haptics(Haptic.Confirm); pendingDelete = null }) {
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
private fun DebtTile(
    label: String,
    amount: Double,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, EfinaCorners.menu)
            .padding(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(formatMoney(amount), style = EfinaType.amountSmall, color = contentColor)
    }
}

@Composable
private fun DebtCard(
    debt: Debt,
    modifier: Modifier = Modifier,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    val isBorrow = debt.type == DebtType.BORROW

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = EfinaCorners.menu,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(debt.name, style = EfinaType.titleMediumEmphasized, maxLines = 1)
                    Text(
                        if (isBorrow) "I owe" else "Owed to me",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBorrow) LocalMoneyPalette.current.expense
                        else LocalMoneyPalette.current.income
                    )
                }
                if (debt.isSettled) {
                    PillTag("Settled")
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete debt",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { debt.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (debt.isSettled) com.efina.app.ui.theme.EfinaSemantic.safe
                else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatMoney(debt.remainingAmount)} of ${formatMoney(debt.amount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                debt.dueDate?.let {
                    Text(
                        formatDate(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!debt.isSettled) {
                    TextButton(onClick = onPay) { Text("Pay") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateDebtDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (CreateDebtRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DebtType.BORROW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add debt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DebtType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) },
                            shape = EfinaCorners.pill
                        )
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true, shape = EfinaCorners.menu
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d{0,15}(\\.\\d{0,2})?$"))) amount = it
                    },
                    label = { Text("Amount") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = EfinaCorners.menu
                )
                OutlinedTextField(
                    value = dueDate, onValueChange = { dueDate = it },
                    label = { Text("Due date (optional, YYYY-MM-DD)") },
                    singleLine = true, shape = EfinaCorners.menu
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = {
                    onCreate(
                        CreateDebtRequest(
                            name = name.trim(),
                            amount = amount.toDouble(),
                            dueDate = dueDate.takeIf { it.length == 10 }?.let { "${it}T00:00:00" },
                            type = type
                        )
                    )
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PayDebtDialog(
    debt: Debt,
    accounts: List<com.efina.app.domain.model.Account>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onPay: (Double, Long?) -> Unit
) {
    var amount by remember { mutableStateOf(debt.remainingAmount.toString()) }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay ${debt.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d{0,15}(\\.\\d{0,2})?$"))) amount = it
                    },
                    label = { Text("Amount") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = EfinaCorners.menu
                )
                Text("Book against account", style = MaterialTheme.typography.labelLarge)
                if (accounts.isEmpty()) {
                    Text(
                        "Optional. With no account, only the debt balance changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = accountId == acc.id,
                                onClick = { accountId = acc.id },
                                label = { Text(acc.name) },
                                shape = EfinaCorners.pill
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = { onPay(amount.toDouble(), accountId) }
            ) { Text("Pay") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

package com.efina.app.ui.scheduled

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
import com.efina.app.domain.model.CreateScheduledRequest
import com.efina.app.domain.model.ScheduledTransaction
import com.efina.app.domain.model.TransactionType
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.revealModifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.RevealLatch
import com.efina.app.ui.theme.Haptic
import com.efina.app.ui.theme.rememberHaptics
import androidx.compose.foundation.lazy.rememberLazyListState
import com.efina.app.ui.components.ScrollToTopFab

/**
 * Recurring transactions.
 *
 * The backend has a `process` cron that materialises due rows; this screen only
 * creates and cancels them. `next_date` is required for a schedule to fire.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduledScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScheduledViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val haptics = rememberHaptics()
    // Marks this screen as having played its entrance, so returning to the tab
    // does not replay the staggered reveal.
    RevealLatch.currentScreen = "scheduled"
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ScheduledTransaction?>(null) }

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
                title = { Text("Scheduled", style = EfinaType.titleLargeEmphasized) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add schedule")
                    }
                }
            )
        }
        floatingActionButton = { ScrollToTopFab(listState) }
    ) { padding ->

        WidthCapped(ContentWidth.List) {        when {
            state.loading -> LoadingList(rows = 4, modifier = Modifier.padding(padding).padding(16.dp))
            state.scheduled.isEmpty() -> EmptyState(
                title = "No schedules",
                message = "Set up rent, subscriptions or salary so they are recorded for you.",
                modifier = Modifier.padding(padding),
                action = {
                    Button(onClick = { creating = true }, shape = EfinaCorners.menu) {
                        Text("Add schedule")
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
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = EfinaCorners.menu,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "${state.activeCount} active",
                                style = EfinaType.titleMediumEmphasized
                            )
                            Text(
                                "About ${formatMoney(state.monthlyTotal)} a month in recurring expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                itemsIndexed(state.scheduled, key = { _, s -> s.id }) { index, item ->
                    ScheduleCard(
                        item = item,
                        modifier = revealModifier(index),
                        onDelete = { pendingDelete = item }
                    )
                }
            }
        }
    
        }
    }

    if (creating) {
        CreateScheduleDialog(
            accounts = state.accounts.filter { it.canWrite },
            saving = state.saving,
            onDismiss = { creating = false },
            onCreate = { viewModel.create(it) { creating = false } }
        )
    }

    pendingDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Cancel this schedule?") },
            text = { Text("Transactions already generated by it are kept.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(s.id); haptics(Haptic.Confirm); pendingDelete = null }) {
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
private fun ScheduleCard(
    item: ScheduledTransaction,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = EfinaCorners.menu,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.description ?: "Scheduled transaction",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    "${item.frequency} · next ${formatDate(item.nextDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            AmountText(item.amount, item.type)
            if (item.isActive) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete schedule",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                PillTag("Paused")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateScheduleDialog(
    accounts: List<com.efina.app.domain.model.Account>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (CreateScheduledRequest) -> Unit
) {
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var frequency by remember { mutableStateOf("monthly") }
    var nextDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (accounts.isEmpty()) {
                    Text(
                        "You need an account you can write to first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text("Account", style = MaterialTheme.typography.labelLarge)
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TransactionType.EXPENSE to "Expense",
                        TransactionType.INCOME to "Income"
                    ).forEach { (t, label) ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(label) },
                            shape = EfinaCorners.pill
                        )
                    }
                }
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
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") }, singleLine = true,
                    shape = EfinaCorners.menu
                )
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("daily", "weekly", "monthly", "yearly").forEach { f ->
                        FilterChip(
                            selected = frequency == f,
                            onClick = { frequency = f },
                            label = { Text(f.replaceFirstChar { it.uppercase() }) },
                            shape = EfinaCorners.pill
                        )
                    }
                }
                OutlinedTextField(
                    value = nextDate, onValueChange = { nextDate = it },
                    label = { Text("First occurrence (YYYY-MM-DD)") },
                    singleLine = true, shape = EfinaCorners.menu
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && accountId != null &&
                    (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = {
                    onCreate(
                        CreateScheduledRequest(
                            accountId = accountId!!,
                            amount = amount.toDouble(),
                            type = type,
                            description = description.ifBlank { null },
                            frequency = frequency,
                            nextDate = "${nextDate}T00:00:00"
                        )
                    )
                }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

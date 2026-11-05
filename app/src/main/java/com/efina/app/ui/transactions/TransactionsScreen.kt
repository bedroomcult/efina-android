package com.efina.app.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.Transaction
import com.efina.app.domain.model.TransactionType
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.revealModifier
import com.efina.app.ui.theme.springClick
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.RevealLatch
import com.efina.app.ui.theme.Haptic
import com.efina.app.ui.theme.rememberHaptics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import com.efina.app.ui.theme.LocalMoneyPalette
import com.efina.app.ui.components.BadgeToastHost
import com.efina.app.ui.components.BadgeTone
import com.efina.app.ui.components.DockClearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val haptics = rememberHaptics()
    // Marks this screen as having played its entrance, so returning to the tab
    // does not replay the staggered reveal.
    RevealLatch.currentScreen = "transactions"
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var detail by remember { mutableStateOf<Transaction?>(null) }
    val listState = rememberLazyListState()

    // A failed save is the one moment a user needs to notice without reading.
    var badge by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val firstLine = error.lineSequence().firstOrNull() ?: error.take(1)
            haptics(Haptic.Reject)
            badge = firstLine.take(48)
            viewModel.clearError()
        }
    }

    // Infinite scroll: request the next page when the last item is visible.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.transactions.size - 3 && state.hasMore && !state.loadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        snackbarHost = { BadgeToastHost(badge, BadgeTone.Error) { badge = null } },
        topBar = {
            if (searchOpen) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it; viewModel.search(it) },
                    onSearch = { viewModel.search(it) },
                    active = false,
                    onActiveChange = {},
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search transactions") },
                    trailingIcon = {
                        TextButton(onClick = {
                            searchOpen = false; query = ""; viewModel.search("")
                        }) { Text("Cancel") }
                    }
                ) {}
            } else {
                TopAppBar(
                    title = { Text("Transactions", style = EfinaType.titleLargeEmphasized) },
                    actions = {
                        IconButton(onClick = { searchOpen = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        },
        // Lifted clear of the floating dock, which is drawn over the NavHost.
        floatingActionButton = {
            // Clears the floating dock, which is drawn over the NavHost.
            Box(Modifier.padding(bottom = DockClearance)) {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    shape = EfinaCorners.menu
                ) { Icon(Icons.Default.Add, contentDescription = "Add transaction") }
            }
        }
    ) { padding ->

        WidthCapped(ContentWidth.List) {        Column(Modifier.fillMaxSize().padding(padding)) {
            TypeFilterRow(selected = state.filter.type) { viewModel.setType(it) }

            when {
                state.loading -> LoadingList(rows = 6, modifier = Modifier.padding(16.dp))
                state.isEmpty -> EmptyState(
                    title = "No transactions yet",
                    message = "Record your first income or expense to see it here."
                )
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(state.transactions, key = { _, t -> t.id }) { index, tx ->
                        TransactionRow(
                            tx = tx,
                            modifier = revealModifier(index),
                            // Tap opens the detail sheet rather than the edit form: most
                            // taps are "what was this?", and jumping into the form made
                            // looking cost an accidental edit.
                            onClick = { detail = tx },
                            onLongClick = { pendingDelete = tx },
                            currentUserId = state.currentUserId
                        )
                    }
                    if (state.loadingMore) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
                        }
                    }
                    item {
                        Text(
                            text = "Showing ${state.transactions.size} of ${state.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    
        }
    }

    detail?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            onDismiss = { detail = null },
            onEdit = {
                detail = null
                onNavigateToEdit(tx.id)
            },
            onDelete = {
                // Reuse the existing confirmation instead of deleting straight from the
                // sheet: a transaction is financial history.
                detail = null
                pendingDelete = tx
            }
        )
    }

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("This reverses the balance change and cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(tx.id); haptics(Haptic.Confirm); pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeFilterRow(selected: TransactionType?, onSelect: (TransactionType?) -> Unit) {
    val options = listOf(
        null to "All",
        TransactionType.EXPENSE to "Expense",
        TransactionType.INCOME to "Income",
        TransactionType.TRANSFER to "Transfer"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (type, label) ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(label) },
                shape = EfinaCorners.pill
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionRow(
    tx: Transaction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    currentUserId: Long? = null
) {
    // springClick rather than Surface(onClick): the latter keeps its interaction
    // source private, so the press scale and focus ring cannot hook in. Long-press
    // stays on the combinedClickable inside springClick's caller.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .springClick(EfinaCorners.groupInner, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = EfinaCorners.groupInner
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction icon on the left, in the money colour for the type. The row
            // previously carried no visual marker, so income and expense were only
            // distinguishable by reading the sign on the amount at the far right.
            val money = LocalMoneyPalette.current
            val (tint, container, glyph) = when (tx.type) {
                TransactionType.INCOME ->
                    Triple(money.income, money.onIncome, Icons.Filled.ArrowDownward)
                TransactionType.EXPENSE ->
                    Triple(money.expense, money.onExpense, Icons.Filled.ArrowUpward)
                TransactionType.TRANSFER ->
                    Triple(money.transfer, money.onTransfer, Icons.Filled.SwapHoriz)
                TransactionType.ADJUSTMENT ->
                    Triple(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        Icons.Filled.Tune
                    )
                else -> Triple(
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    Icons.Filled.Help
                )
            }
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .size(36.dp)
                    .background(container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = glyph,
                    contentDescription = when (tx.type) {
                        TransactionType.INCOME -> "Income"
                        TransactionType.EXPENSE -> "Expense"
                        TransactionType.TRANSFER -> "Transfer"
                        TransactionType.ADJUSTMENT -> "Adjustment"
                        else -> "Transaction"
                    },
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    tx.description ?: tx.categoryName ?: "Untitled",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDate(tx.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // A row recorded by another household member, so a shared budget's
                    // spending is distinguishable from your own.
                    val fromFamily = currentUserId != null && tx.userId != 0L &&
                        tx.userId != currentUserId
                    val meta = tx.categoryName ?: tx.accountName
                    if (fromFamily) {
                        Text(
                            " · Family",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    } else if (meta != null) {
                        Text(
                            " · $meta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            AmountText(
                amount = tx.amount,
                type = tx.type,
                currency = tx.currency
            )
        }
    }
}

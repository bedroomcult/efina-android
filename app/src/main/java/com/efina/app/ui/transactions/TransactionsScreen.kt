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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
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
        snackbarHost = { SnackbarHost(snackbar) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                shape = EfinaCorners.menu
            ) { Icon(Icons.Default.Add, contentDescription = "Add transaction") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
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
                            onClick = { onNavigateToEdit(tx.id) },
                            onLongClick = { pendingDelete = tx }
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

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("This reverses the balance change and cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(tx.id); pendingDelete = null
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
    onLongClick: () -> Unit
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = EfinaCorners.groupInner,
        onClick = onClick
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    val meta = tx.categoryName ?: tx.accountName
                    if (meta != null) {
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

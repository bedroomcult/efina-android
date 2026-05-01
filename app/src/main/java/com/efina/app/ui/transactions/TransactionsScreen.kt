package com.efina.app.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    
    // Infinite scroll detection
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= uiState.transactions.size - 5) {
                    viewModel.loadMore()
                }
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                    IconButton(onClick = { viewModel.loadTransactions() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.search(it)
                },
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.search("")
                        }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Type Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter.type == null,
                    onClick = { viewModel.filterByType(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.filter.type == TransactionType.INCOME,
                    onClick = { viewModel.filterByType(TransactionType.INCOME) },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = uiState.filter.type == TransactionType.EXPENSE,
                    onClick = { viewModel.filterByType(TransactionType.EXPENSE) },
                    label = { Text("Expense") }
                )
            }
            
            // Transaction Count
            Text(
                text = "${uiState.total} transactions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Transactions List
            when {
                uiState.isLoading && uiState.transactions.isEmpty() -> LoadingIndicator()
                uiState.error != null && uiState.transactions.isEmpty() -> ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadTransactions() }
                )
                uiState.transactions.isEmpty() -> EmptyState(
                    title = "No transactions",
                    description = "Your transactions will appear here.",
                    icon = Icons.Default.Receipt
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.transactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            description = transaction.description ?: "",
                            categoryName = transaction.categoryName,
                            categoryColor = transaction.categoryColor?.let {
                                try { Color(android.graphics.Color.parseColor(it)) }
                                catch (e: Exception) { null }
                            },
                            amount = transaction.amount,
                            isExpense = transaction.type == TransactionType.EXPENSE,
                            date = transaction.date.take(10),
                            currency = "IDR",
                            onClick = { onNavigateToEdit(transaction.id) }
                        )
                    }
                    
                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            FilterBottomSheet(
                accounts = uiState.accounts,
                categories = uiState.categories,
                selectedAccountId = uiState.filter.accountId,
                selectedCategoryId = uiState.filter.categoryId,
                onAccountSelected = {
                    viewModel.filterByAccount(it)
                    showFilterSheet = false
                },
                onCategorySelected = {
                    viewModel.filterByCategory(it)
                    showFilterSheet = false
                },
                onClearFilters = {
                    viewModel.updateFilter(TransactionFilter())
                    showFilterSheet = false
                }
            )
        }
    }
    
    // Delete Dialog
    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FilterBottomSheet(
    accounts: List<com.efina.app.domain.model.Account>,
    categories: List<com.efina.app.domain.model.Category>,
    selectedAccountId: Long?,
    selectedCategoryId: Long?,
    onAccountSelected: (Long?) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Account",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        accounts.forEach { account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedAccountId == account.id,
                    onClick = { onAccountSelected(account.id) }
                )
                Text(
                    text = account.name,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        categories.take(10).forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) }
                )
                Text(
                    text = category.name,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onClearFilters,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear All Filters")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

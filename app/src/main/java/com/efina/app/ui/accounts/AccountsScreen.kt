package com.efina.app.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.AccountType
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                actions = {
                    IconButton(onClick = { viewModel.loadAccounts() }) {
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
                Icon(Icons.Default.Add, "Add Account")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Total Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatCurrency(uiState.totalBalance, uiState.currency),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Success
                    )
                }
            }
            
            // Accounts List
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadAccounts() }
                )
                uiState.accounts.isEmpty() -> EmptyState(
                    title = "No accounts yet",
                    description = "Add your first account to start tracking your finances.",
                    icon = Icons.Default.AccountBalanceWallet
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.accounts, key = { it.id }) { account ->
                        AccountCard(
                            name = account.name,
                            balance = account.balance,
                            currency = account.currency,
                            type = account.type.displayName,
                            onClick = { onNavigateToEdit(account.id) }
                        )
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { accountId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure? This will also delete all transactions in this account.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(accountId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
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

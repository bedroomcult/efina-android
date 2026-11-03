package com.efina.app.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.Account
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
import androidx.compose.foundation.lazy.rememberLazyListState
import com.efina.app.ui.components.ScrollToTopFab
import com.efina.app.ui.components.BadgeToastHost
import com.efina.app.ui.components.BadgeTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToShare: (Long) -> Unit = {},
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val haptics = rememberHaptics()
    // Marks this screen as having played its entrance, so returning to the tab
    // does not replay the staggered reveal.
    RevealLatch.currentScreen = "accounts"
    var pendingDelete by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let {
            badge = (it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { BadgeToastHost(badge, BadgeTone.Error) { badge = null } },
        topBar = {
            TopAppBar(
                title = { Text("Accounts", style = EfinaType.titleLargeEmphasized) },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Default.Add, contentDescription = "Add account")
                    }
                }
            )
        },
        // Lifted clear of the floating dock: the dock is an overlay drawn after the
        // NavHost, so an un-padded FAB sits underneath it and cannot be tapped.
        floatingActionButton = {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.End
            ) {
            ScrollToTopFab(listState)
            // The dock is an overlay drawn after the NavHost, so an un-padded FAB ends
            // up underneath it and cannot be tapped. 78dp clears the 52dp items plus
            // the dock's 6dp padding, 12dp inset and the navigation-bar inset.
            Box(Modifier.padding(bottom = 78.dp)) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCreate,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Account") },
                    shape = EfinaCorners.menu
                )
            }
            }
        }
    ) { padding ->

        WidthCapped(ContentWidth.List) {        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding)) {
                LoadingList(rows = 4, modifier = Modifier.padding(16.dp))
            }
            state.accounts.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    title = "No accounts yet",
                    message = "Add a bank account, e-wallet or cash balance to start tracking.",
                    action = {
                        Button(onClick = onNavigateToCreate, shape = EfinaCorners.menu) {
                            Text("Create account")
                        }
                    }
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    TotalCard(state.totalBalance, state.accounts.firstOrNull()?.currency ?: "IDR")
                }
                itemsIndexed(state.accounts, key = { _, a -> a.id }) { index, account ->
                    AccountRow(
                        account = account,
                        modifier = revealModifier(index),
                        onClick = { onNavigateToEdit(account.id) },
                        onShare = { onNavigateToShare(account.id) },
                        onDelete = { pendingDelete = account }
                    )
                }
            }
        }
    
        }
    }

    pendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${account.name}?") },
            text = {
                Text("This removes the account and every transaction on it. It cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(account.id); haptics(Haptic.Confirm)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}


@Composable
private fun TotalCard(total: Double, currency: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, EfinaCorners.hero)
            .padding(20.dp)
    ) {
        Text(
            "Total balance",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(4.dp))
        AnimatedMoney(
            amount = total,
            currency = currency,
            style = EfinaType.amountLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun AccountRow(
    account: Account,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    // Plain Surface + springClick rather than Surface(onClick): the latter keeps its
    // InteractionSource private, so the press scale and focus ring cannot hook in.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .springClick(EfinaCorners.menu, onClick = onClick),
        shape = EfinaCorners.menu,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountIconTile(account)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        account.name,
                        style = EfinaType.titleMediumEmphasized,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (account.isShared || account.role != null) {
                        Spacer(Modifier.width(6.dp))
                        SharedAccountBadge(account)
                    }
                }
                Text(
                    account.type.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                formatMoney(account.balance, account.currency),
                style = EfinaType.amountSmall,
                color = if (account.balance < 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            if (account.canAdmin) {
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Account actions")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = EfinaCorners.menu
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (account.isShared) "Manage sharing" else "Share with family") },
                            onClick = { menuOpen = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

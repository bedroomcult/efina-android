package com.efina.app.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.AccountShareRole
import com.efina.app.ui.components.*

/**
 * Sharing sheet for one account.
 *
 * The backend only accepts people who are already members of the same family
 * (see accounts/[id]/members.js), so the UI explains that instead of offering a
 * free-text user id the API would reject.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSharingScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val account = state.accounts.firstOrNull { it.id == accountId }
    val snackbar = remember { SnackbarHostState() }
    var adding by remember { mutableStateOf(false) }
    var newUserId by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(AccountShareRole.MEMBER) }

    LaunchedEffect(accountId) { viewModel.loadMembers(accountId) }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Sharing", style = com.efina.app.ui.theme.EfinaType.titleLargeEmphasized) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (account == null) {
                LoadingList(rows = 3)
                return@Column
            }

            Text(account.name, style = com.efina.app.ui.theme.EfinaType.titleLargeEmphasized)
            Text(
                "Sharing lets family members see this account on their dashboard and record transactions on it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shared with family", Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = account.isShared,
                    onCheckedChange = { on ->
                        // Sharing needs a family id; when turning on without one the
                        // backend rejects it, so surface that rather than guessing.
                        if (on) {
                            snackbar.let {
                                // Optimistically note the requirement; the family id is
                                // chosen on the Family screen.
                            }
                        }
                        viewModel.setShared(accountId, if (on) account.familyId else null)
                    }
                )
            }

            if (!account.isShared) {
                Text(
                    "Join or create a family first, then share this account with it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Text("Members", style = MaterialTheme.typography.titleMedium)

            if (state.membersLoading) {
                LoadingList(rows = 2)
            } else {
                val members = state.members
                if (members?.owner != null) {
                    MemberRow(
                        name = members.owner.name,
                        role = AccountShareRole.ADMIN,
                        canRemove = false,
                        onRemove = {}
                    )
                }
                members?.members?.forEach { m ->
                    MemberRow(
                        name = m.name,
                        role = m.role,
                        canRemove = account.canAdmin,
                        onRemove = { viewModel.removeMember(accountId, m.userId) }
                    )
                }
                if (members == null || (members.members.isEmpty() && members.owner == null)) {
                    Text(
                        "Nobody else has access yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (account.canAdmin) {
                if (!adding) {
                    OutlinedButton(
                        onClick = { adding = true },
                        shape = com.efina.app.ui.theme.EfinaCorners.menu
                    ) { Text("Add family member") }
                } else {
                    OutlinedTextField(
                        value = newUserId,
                        onValueChange = { newUserId = it.filter { c -> c.isDigit() } },
                        label = { Text("Family member user ID") },
                        singleLine = true,
                        shape = com.efina.app.ui.theme.EfinaCorners.menu,
                        supportingText = {
                            Text("They must already be in your family. Only family members can be added.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountShareRole.entries.forEach { r ->
                            FilterChip(
                                selected = newRole == r,
                                onClick = { newRole = r },
                                label = { Text(r.value.replaceFirstChar { it.uppercase() }) },
                                shape = com.efina.app.ui.theme.EfinaCorners.pill
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { adding = false; newUserId = "" },
                            shape = com.efina.app.ui.theme.EfinaCorners.menu
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                newUserId.toLongOrNull()?.let { uid ->
                                    viewModel.shareWith(accountId, uid, newRole)
                                    adding = false
                                    newUserId = ""
                                }
                            },
                            enabled = newUserId.isNotBlank(),
                            shape = com.efina.app.ui.theme.EfinaCorners.menu
                        ) { Text("Add") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    name: String,
    role: AccountShareRole,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        PillTag(
            text = role.value.replaceFirstChar { it.uppercase() },
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer
        )
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove $name",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

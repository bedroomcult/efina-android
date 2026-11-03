package com.efina.app.ui.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.FamilyGroup
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.RevealLatch
import com.efina.app.ui.theme.Haptic
import com.efina.app.ui.theme.rememberHaptics
import androidx.compose.foundation.lazy.rememberLazyListState
import com.efina.app.ui.components.ScrollToTopFab
import com.efina.app.ui.components.BadgeToastHost
import com.efina.app.ui.components.BadgeTone

/**
 * Families and their invite codes.
 *
 * The backend caps free plans at one family (FAMILY_LIMIT_REACHED) and only
 * owner/admin can invite. Both are enforced server-side; this screen mirrors them
 * so the user is not offered an action that returns 403.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    onNavigateBack: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val haptics = rememberHaptics()
    // Marks this screen as having played its entrance, so returning to the tab
    // does not replay the staggered reveal.
    RevealLatch.currentScreen = "family"
    var creating by remember { mutableStateOf(false) }
    var joining by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var pendingRemove by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    // A failed save is the one moment a user needs to notice without reading.
    var badge by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.error) {
        state.error?.let {
            haptics(Haptic.Reject)
            badge = it.lineSequence().first().take(48)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { BadgeToastHost(badge, BadgeTone.Error) { badge = null } },
        topBar = {
            TopAppBar(
                title = { Text("Family", style = EfinaType.titleLargeEmphasized) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.atLimit) {
                        IconButton(onClick = { creating = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create family")
                        }
                    }
                }
            )
        },
        floatingActionButton = { ScrollToTopFab(listState) },
    ) { padding ->

        WidthCapped(ContentWidth.List) {        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "Share bank accounts with your household. Family members can see a shared account and record transactions on it, depending on their role.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.atLimit) {
                    Button(onClick = { creating = true }, shape = EfinaCorners.menu) {
                        Text("Create family")
                    }
                }
                OutlinedButton(onClick = { joining = true }, shape = EfinaCorners.menu) {
                    Text("Join with code")
                }
            }
            Spacer(Modifier.height(16.dp))

            when {
                state.loading -> LoadingList(rows = 3)
                state.families.isEmpty() -> EmptyState(
                    title = "No family yet",
                    message = "Create one, or join with a code someone shared."
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.families, key = { it.id }) { family ->
                        FamilyCard(
                            family = family,
                            onInvite = { viewModel.createInvite(family.id) },
                            onRemove = { userId -> pendingRemove = family.id to userId }
                        )
                    }
                }
            }
        }
    
        }
    }

    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false; newName = "" },
            title = { Text("Create family") },
            text = {
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("Family name") }, singleLine = true,
                    shape = EfinaCorners.menu
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank() && !state.saving,
                    onClick = { viewModel.create(newName.trim()) { creating = false; newName = "" } }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { creating = false; newName = "" }) { Text("Cancel") }
            }
        )
    }

    if (joining) {
        AlertDialog(
            onDismissRequest = { joining = false; inviteCodeInput = "" },
            title = { Text("Join a family") },
            text = {
                OutlinedTextField(
                    value = inviteCodeInput,
                    onValueChange = { inviteCodeInput = it.uppercase().take(8) },
                    label = { Text("Invite code") },
                    singleLine = true,
                    shape = EfinaCorners.menu,
                    supportingText = { Text("8 characters. Codes expire after 72 hours.") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = inviteCodeInput.isNotBlank() && !state.saving,
                    onClick = {
                        viewModel.acceptInvite(inviteCodeInput) {
                            joining = false; inviteCodeInput = ""
                        }
                    }
                ) { Text("Join") }
            },
            dismissButton = {
                TextButton(onClick = { joining = false; inviteCodeInput = "" }) { Text("Cancel") }
            }
        )
    }

    state.inviteCode?.let { code ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissInvite() },
            title = { Text("Invite code") },
            text = {
                Column {
                    Text(
                        "Share this with a family member. It works once and expires in 72 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        code,
                        style = EfinaType.code.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissInvite() }) { Text("Done") }
            }
        )
    }

    pendingRemove?.let { (familyId, userId) ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("Remove member?") },
            text = { Text("They lose access to accounts shared with this family.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeMember(familyId, userId); pendingRemove = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FamilyCard(
    family: FamilyGroup,
    onInvite: () -> Unit,
    onRemove: (Long) -> Unit
) {
    val canManage = family.memberRole == "owner" || family.memberRole == "admin"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = EfinaCorners.menu,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(family.name, style = EfinaType.titleMediumEmphasized)
                    Text(
                        "${family.members.size} member${if (family.members.size == 1) "" else "s"} · ${family.memberRole}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canManage) {
                    TextButton(onClick = onInvite) { Text("Invite") }
                }
            }
            if (family.members.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                family.members.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                m.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            m.email?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        PillTag(m.role.replaceFirstChar { it.uppercase() })
                        if (canManage && m.role != "owner") {
                            IconButton(onClick = { onRemove(m.userId) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove ${m.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

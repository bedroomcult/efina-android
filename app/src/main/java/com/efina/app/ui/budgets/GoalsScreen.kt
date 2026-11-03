package com.efina.app.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.CreateGoalRequest
import com.efina.app.domain.model.Goal
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.efina.app.ui.theme.revealModifier
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var funding by remember { mutableStateOf<Goal?>(null) }
    var pendingDelete by remember { mutableStateOf<Goal?>(null) }
    var editing by remember { mutableStateOf<Goal?>(null) }
    var editName by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf("") }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Goals", style = EfinaType.titleLargeEmphasized) },
                actions = {
                    IconButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add goal")
                    }
                }
            )
        }
    ) { padding ->

        WidthCapped(ContentWidth.List) {        when {
            state.loading -> LoadingList(rows = 4, modifier = Modifier.padding(padding).padding(16.dp))
            state.goals.isEmpty() -> EmptyState(
                title = "No goals yet",
                message = "Set a target and fund it from one of your accounts.",
                modifier = Modifier.padding(padding),
                action = {
                    Button(onClick = { creating = true }, shape = EfinaCorners.menu) {
                        Text("Create goal")
                    }
                }
            )
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    GoalsSummary(state.totalSaved, state.totalTarget, state.completedCount)
                }
                itemsIndexed(state.goals, key = { _, g -> g.id }) { index, goal ->
                    GoalCard(
                        goal = goal,
                        modifier = revealModifier(index),
                        onFund = { funding = goal },
                        onDelete = { pendingDelete = goal },
                        onEdit = {
                            editing = goal
                            editName = goal.name
                            editTarget = goal.targetAmount.toString()
                        }
                    )
                }
            }
        }
    
        }
    }

    if (creating) {
        CreateGoalDialog(
            saving = state.saving,
            onDismiss = { creating = false },
            onCreate = { viewModel.create(it) { creating = false } }
        )
    }

    editing?.let { g ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName, onValueChange = { editName = it },
                        label = { Text("Name") }, singleLine = true, shape = EfinaCorners.menu
                    )
                    OutlinedTextField(
                        value = editTarget,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d{0,15}(\\.\\d{0,2})?$"))) editTarget = it
                        },
                        label = { Text("Target amount") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = EfinaCorners.menu
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.saving && editName.isNotBlank() &&
                        (editTarget.toDoubleOrNull() ?: 0.0) > 0.0,
                    onClick = {
                        viewModel.update(
                            g.id,
                            com.efina.app.domain.model.UpdateGoalRequest(
                                name = editName.trim(),
                                targetAmount = editTarget.toDouble(),
                                // goals/[id].js binds these straight into the UPDATE;
                                // leaving them null makes D1 reject the statement (500).
                                startDate = g.startDate,
                                targetDate = g.targetDate,
                                periodType = g.periodType,
                                linkedAccountId = g.linkedAccountId
                            )
                        ) { editing = null }
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }

    funding?.let { goal ->
        FundGoalDialog(
            goal = goal,
            accounts = state.accounts,
            saving = state.saving,
            onDismiss = { funding = null },
            onFund = { amount, accountId ->
                viewModel.addFunds(goal.id, amount, accountId) { funding = null }
            }
        )
    }

    pendingDelete?.let { g ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${g.name}?") },
            text = { Text("Money already moved into this goal is not returned automatically.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(g.id); pendingDelete = null }) {
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
private fun GoalsSummary(saved: Double, target: Double, completed: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, EfinaCorners.hero)
            .padding(20.dp)
    ) {
        Text(
            "Saved towards goals",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            formatMoney(saved),
            style = EfinaType.amountMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            "of ${formatMoney(target)}${
                if (completed > 0) " · $completed completed" else ""
            }",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    modifier: Modifier = Modifier,
    onFund: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = EfinaCorners.menu,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onEdit
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(goal.name, style = EfinaType.titleMediumEmphasized, maxLines = 1)
                    goal.targetDate?.let {
                        Text(
                            "Target ${formatDate(it)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (goal.isComplete) {
                    PillTag("Done")
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete goal",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { goal.progressPercentage / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (goal.isComplete) com.efina.app.ui.theme.EfinaSemantic.safe
                else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatMoney(goal.currentAmount)} / ${formatMoney(goal.targetAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (!goal.isComplete) {
                    TextButton(onClick = onFund) { Text("Add funds") }
                }
            }
        }
    }
}

@Composable
private fun CreateGoalDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (CreateGoalRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var targetDate by remember {
        mutableStateOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Date(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000))
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true, shape = EfinaCorners.menu
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d{0,15}(\\.\\d{0,2})?$"))) target = it
                    },
                    label = { Text("Target amount") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = EfinaCorners.menu
                )
                OutlinedTextField(
                    value = targetDate, onValueChange = { targetDate = it },
                    label = { Text("Target date (YYYY-MM-DD)") },
                    singleLine = true, shape = EfinaCorners.menu
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && name.isNotBlank() && (target.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = {
                    val start = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    onCreate(
                        CreateGoalRequest(
                            name = name.trim(),
                            targetAmount = target.toDouble(),
                            // goals/index.js validates /^\d{4}-\d{2}-\d{2}$/, so a
                            // time component makes every create fail with 400.
                            startDate = start,
                            targetDate = targetDate,
                            periodType = "monthly"
                        )
                    )
                }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FundGoalDialog(
    goal: Goal,
    accounts: List<com.efina.app.domain.model.Account>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onFund: (Double, Long) -> Unit
) {
    val writable = accounts.filter { it.canWrite }
    var amount by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf(writable.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add funds to ${goal.name}") },
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
                Text("From account", style = MaterialTheme.typography.labelLarge)
                if (writable.isEmpty()) {
                    Text(
                        "No account you can write to.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        writable.forEach { acc ->
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
                enabled = !saving && accountId != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = { onFund(amount.toDouble(), accountId!!) }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

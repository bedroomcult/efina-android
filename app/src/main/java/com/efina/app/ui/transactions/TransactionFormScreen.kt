package com.efina.app.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.*
import com.efina.app.ui.accounts.AccountsViewModel
import com.efina.app.ui.components.ErrorState
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.categories.CategoriesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Create / edit a transaction, or create a transfer pair.
 *
 * A transfer is not a transaction with type=transfer: the backend has a separate
 * POST /transfers that writes a linked expense+income pair and moves both
 * balances atomically. Sending type=transfer to /transactions would not move the
 * destination account, so the form routes that case to the transfer endpoint.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(
    existing: Transaction?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
    accountsViewModel: AccountsViewModel = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val accountsState by accountsViewModel.state.collectAsState()
    val categoriesState by categoriesViewModel.state.collectAsState()

    val isEdit = existing != null
    var mode by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember(existing?.id) {
        mutableStateOf(existing?.amount?.toString().orEmpty())
    }
    var accountId by remember(existing?.id) { mutableStateOf(existing?.accountId) }
    var toAccountId by remember(existing?.id) { mutableStateOf<Long?>(null) }
    var categoryId by remember(existing?.id) { mutableStateOf(existing?.categoryId) }
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var date by remember(existing?.id) {
        mutableStateOf(existing?.date?.take(10) ?: today())
    }
    var localError by remember { mutableStateOf<String?>(null) }

    val accounts = accountsState.accounts
    val writableAccounts = accounts.filter { it.canWrite }
    val categories = categoriesState.categories

    LaunchedEffect(accounts) {
        if (accountId == null) accountId = writableAccounts.firstOrNull()?.id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "Edit transaction" else "New transaction",
                        style = EfinaType.titleLargeEmphasized
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isEdit) {
                SegmentedButtonRow(
                    selected = mode,
                    onSelect = { mode = it }
                )
            }

            AmountField(amount) { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d{0,15}(\\.\\d{0,2})?$"))) {
                    amount = input
                }
            }

            // Account picker
            Text("Account", style = MaterialTheme.typography.labelLarge)
            if (writableAccounts.isEmpty()) {
                Text(
                    "No accounts you can write to. Create one, or ask the owner for access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    writableAccounts.forEach { acc ->
                        FilterChip(
                            selected = accountId == acc.id,
                            onClick = { accountId = acc.id },
                            label = { Text(acc.name) },
                            shape = EfinaCorners.pill
                        )
                    }
                }
            }

            if (mode == TransactionType.TRANSFER && !isEdit) {
                Text("To account", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    writableAccounts.filter { it.id != accountId }.forEach { acc ->
                        FilterChip(
                            selected = toAccountId == acc.id,
                            onClick = { toAccountId = acc.id },
                            label = { Text(acc.name) },
                            shape = EfinaCorners.pill
                        )
                    }
                }
            }

            if (mode != TransactionType.TRANSFER) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                if (categories.isEmpty()) {
                    Text(
                        "No categories yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.take(12).forEach { cat ->
                            FilterChip(
                                selected = categoryId == cat.id,
                                onClick = { categoryId = cat.id },
                                label = { Text(cat.name) },
                                shape = EfinaCorners.pill
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                shape = EfinaCorners.menu,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") },
                shape = EfinaCorners.menu,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            localError?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val parsed = amount.toDoubleOrNull()
                    localError = when {
                        parsed == null || parsed <= 0.0 -> "Enter an amount greater than zero"
                        accountId == null -> "Choose an account"
                        mode == TransactionType.TRANSFER && !isEdit && toAccountId == null ->
                            "Choose a destination account"
                        mode == TransactionType.TRANSFER && toAccountId == accountId ->
                            "Source and destination must differ"
                        else -> null
                    }
                    if (localError == null) {
                        val acc = accountId!!
                        if (mode == TransactionType.TRANSFER && !isEdit) {
                            viewModel.transfer(
                                CreateTransferRequest(
                                    fromAccountId = acc,
                                    toAccountId = toAccountId!!,
                                    amount = parsed!!,
                                    date = normalizeDate(date),
                                    description = description.ifBlank { null }
                                )
                            ) { onSaved() }
                        } else if (isEdit) {
                            viewModel.update(
                                existing!!.id,
                                UpdateTransactionRequest(
                                    accountId = acc,
                                    categoryId = categoryId,
                                    amount = parsed!!,
                                    type = existing.type,
                                    description = description.ifBlank { null },
                                    date = normalizeDate(date),
                                    mood = existing.mood
                                )
                            ) { onSaved() }
                        } else {
                            viewModel.create(
                                CreateTransactionRequest(
                                    accountId = acc,
                                    categoryId = categoryId,
                                    amount = parsed!!,
                                    type = mode,
                                    description = description.ifBlank { null },
                                    date = normalizeDate(date)
                                )
                            ) { onSaved() }
                        }
                    }
                },
                enabled = !state.saving,
                shape = EfinaCorners.menu,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isEdit) "Save changes" else "Save transaction")
                }
            }

            state.error?.let { ErrorState(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedButtonRow(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit
) {
    val options = listOf(
        TransactionType.EXPENSE to "Expense",
        TransactionType.INCOME to "Income",
        TransactionType.TRANSFER to "Transfer"
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) { Text(label) }
        }
    }
}

@Composable
private fun AmountField(value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text("Amount") },
        singleLine = true,
        textStyle = EfinaType.amountMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = EfinaCorners.menu,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun today(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

/**
 * The backend validates the date as ISO 8601 (`YYYY-MM-DDTHH:mm:ss`); a bare date
 * is accepted by `new Date()`, but normalising keeps list sorting stable.
 */
private fun normalizeDate(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.length == 10) "${trimmed}T00:00:00" else trimmed
}

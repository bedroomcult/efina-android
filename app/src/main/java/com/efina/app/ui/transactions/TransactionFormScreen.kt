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
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.efina.app.ui.theme.LocalIsDarkTheme
import com.efina.app.ui.theme.MoneyColors
import com.efina.app.ui.components.CategoryPickerSheet
import com.efina.app.ui.components.DateTimeField
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.Haptic
import com.efina.app.ui.theme.rememberHaptics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.efina.app.ui.components.PickerOption
import com.efina.app.ui.components.PickerSheet
import com.efina.app.ui.theme.parseHexColor
import com.efina.app.ui.components.formatMoney

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
    // transactions.date is a DATETIME and the API accepts YYYY-MM-DDTHH:mm:ss, but
    // the form only ever sent a date and hardcoded midnight. Editing an existing
    // transaction now preserves its recorded time instead of resetting it.
    var time by remember(existing?.id) {
        mutableStateOf(existing?.date?.drop(11)?.take(8) ?: nowTime())
    }
    val haptics = rememberHaptics()
    var localError by remember { mutableStateOf<String?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showDestPicker by remember { mutableStateOf(false) }

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

        WidthCapped(ContentWidth.Form) {        Column(
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
                val chosenAccount = writableAccounts.firstOrNull { it.id == accountId }
                OutlinedButton(
                    onClick = { showAccountPicker = true },
                    shape = EfinaCorners.menu,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .padding(end = 10.dp)
                            .size(12.dp)
                            .background(
                                parseHexColor(chosenAccount?.color, MaterialTheme.colorScheme.primary),
                                CircleShape
                            )
                    )
                    Text(
                        text = chosenAccount?.name ?: "Choose an account",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (chosenAccount == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Show accounts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showAccountPicker) {
                    PickerSheet(
                        title = "Account",
                        options = writableAccounts.map {
                            PickerOption(
                                id = it.id,
                                label = it.name,
                                color = it.color,
                                subtitle = formatMoney(it.balance, it.currency)
                            )
                        },
                        selectedId = accountId,
                        onSelect = { accountId = it.id; showAccountPicker = false },
                        onDismiss = { showAccountPicker = false }
                    )
                }
            }

            if (mode == TransactionType.TRANSFER && !isEdit) {
                val destinations = writableAccounts.filter { it.id != accountId }
                val chosenDest = destinations.firstOrNull { it.id == toAccountId }
                OutlinedButton(
                    onClick = { showDestPicker = true },
                    shape = EfinaCorners.menu,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .padding(end = 10.dp)
                            .size(12.dp)
                            .background(
                                parseHexColor(chosenDest?.color, MaterialTheme.colorScheme.primary),
                                CircleShape
                            )
                    )
                    Text(
                        text = chosenDest?.name ?: "Choose a destination",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (chosenDest == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Show accounts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showDestPicker) {
                    PickerSheet(
                        title = "To account",
                        options = destinations.map {
                            PickerOption(
                                id = it.id,
                                label = it.name,
                                color = it.color,
                                subtitle = formatMoney(it.balance, it.currency)
                            )
                        },
                        selectedId = toAccountId,
                        onSelect = { toAccountId = it.id; showDestPicker = false },
                        onDismiss = { showDestPicker = false }
                    )
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
                    // Two-level sheet, not a flat dropdown: parents expand and are
                    // themselves selectable, children are indented, and search
                    // flattens the tree. Mirrors the web's CategorySelectionModal.
                    val selectable = categories
                        .filter { it.type == null || it.type.equals(mode.value, true) }
                    val chosen = selectable.firstNotNullOfOrNull { root ->
                        when {
                            root.id == categoryId -> root
                            else -> root.children.firstOrNull { it.id == categoryId }
                        }
                    }

                    OutlinedButton(
                        onClick = { showCategoryPicker = true },
                        shape = EfinaCorners.menu,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = chosen?.let { pickerLabel(selectable, it) } ?: "Choose a category",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (chosen == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Show categories",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showCategoryPicker) {
                        CategoryPickerSheet(
                            roots = selectable,
                            selectedId = categoryId,
                            onSelect = {
                                categoryId = it.id
                                showCategoryPicker = false
                            },
                            onDismiss = { showCategoryPicker = false }
                        )
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

            // Platform pickers rather than text fields: typed input let users submit
            // impossible dates and times, which the API then rejected.
            DateTimeField(
                date = date,
                time = time,
                onDate = { date = it },
                onTime = { time = it }
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
                                    date = normalizeDate(date, time),
                                    description = description.ifBlank { null }
                                )
                            ) { haptics(Haptic.Confirm); onSaved() }
                        } else if (isEdit) {
                            viewModel.update(
                                existing!!.id,
                                UpdateTransactionRequest(
                                    accountId = acc,
                                    categoryId = categoryId,
                                    amount = parsed!!,
                                    type = existing.type,
                                    description = description.ifBlank { null },
                                    date = normalizeDate(date, time),
                                    mood = existing.mood
                                )
                            ) { haptics(Haptic.Confirm); onSaved() }
                        } else {
                            viewModel.create(
                                CreateTransactionRequest(
                                    accountId = acc,
                                    categoryId = categoryId,
                                    amount = parsed!!,
                                    type = mode,
                                    description = description.ifBlank { null },
                                    date = normalizeDate(date, time)
                                )
                            ) { haptics(Haptic.Confirm); onSaved() }
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
    val isDark = LocalIsDarkTheme.current
    // The web app fills these three buttons with emerald/red/blue and white text
    // (TransactionModal.jsx). Match that: the selected chip carries the money colour
    // for its type, so income and expense are distinguishable at a glance.
    // A local `fun` cannot call @Composable functions, so the adjustment colour is
    // read once here and the sealed expression stays composable-free.
    val adjustmentColor = MaterialTheme.colorScheme.surfaceContainerHighest
    fun accentFor(type: TransactionType) = when (type) {
        TransactionType.INCOME -> if (isDark) MoneyColors.incomeDark else MoneyColors.incomeLight
        TransactionType.EXPENSE -> if (isDark) MoneyColors.expenseDark else MoneyColors.expenseLight
        TransactionType.TRANSFER -> if (isDark) MoneyColors.transferDark else MoneyColors.transferLight
        // A reconciliation entry, not a direction of money.
        TransactionType.ADJUSTMENT -> adjustmentColor
    }

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label) ->
            val accent = accentFor(type)
            val isSelected = selected == type
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = accent,
                    activeContentColor = Color.White,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) }
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
private fun nowTime(): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

/**
 * Joins the date and time fields into the ISO 8601 form the backend validates
 * (`/^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2})?$/`). A missing or malformed time
 * falls back to midnight rather than sending a string the server will reject.
 */
private fun normalizeDate(datePart: String, timePart: String): String {
    val d = datePart.trim()
    if (d.length != 10) return d
    val raw = timePart.trim()
    val withSeconds = when {
        raw.matches(Regex("^\\d{2}:\\d{2}$")) -> "$raw:00"
        raw.matches(Regex("^\\d{2}:\\d{2}:\\d{2}$")) -> raw
        else -> "00:00:00"
    }
    return "${d}T$withSeconds"
}

/**
 * A dropdown for picking one value from a list.
 *
 * Chips were used before and did not scale: the account and category lists are
 * unbounded, and categories were additionally capped at twelve, so most of the
 * seeded tree could not be selected at all.
 */
/** "Food · Cafe" for a child, or the plain name for a root. */
private fun pickerLabel(roots: List<com.efina.app.domain.model.Category>,
                        chosen: com.efina.app.domain.model.Category): String {
    if (chosen.parentId == null) return chosen.name
    val parent = roots.firstOrNull { it.id == chosen.parentId }
    return if (parent != null) "${parent.name} · ${chosen.name}" else chosen.name
}

@Composable
private fun PickerField(
    label: String,
    selectedLabel: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = EfinaCorners.menu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Show $label options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nothing available") },
                    onClick = { expanded = false },
                    enabled = false
                )
            }
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

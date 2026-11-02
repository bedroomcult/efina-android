package com.efina.app.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.Account
import com.efina.app.domain.model.AccountType
import com.efina.app.domain.model.CreateAccountRequest
import com.efina.app.domain.model.UpdateAccountRequest
import com.efina.app.ui.components.ErrorState
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType

/**
 * Create and edit share one form: the backend's create and update bodies differ
 * only in that update requires the full field set, which the form always has.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountFormScreen(
    account: Account?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isEdit = account != null

    // Keyed on the id: the account arrives asynchronously, so a plain `remember`
    // captured null on first composition and never re-initialised. Editing then
    // submitted empty fields, and because the blank balance parsed to 0.0 the PUT
    // silently zeroed the account's balance.
    var name by remember(account?.id) { mutableStateOf(account?.name.orEmpty()) }
    var type by remember(account?.id) { mutableStateOf(account?.type ?: AccountType.BANK) }
    var balance by remember(account?.id) {
        mutableStateOf(account?.balance?.let { if (it == 0.0) "" else it.toString() }.orEmpty())
    }
    var currency by remember(account?.id) { mutableStateOf(account?.currency ?: "IDR") }
    var description by remember(account?.id) { mutableStateOf(account?.description.orEmpty()) }
    var localError by remember { mutableStateOf<String?>(null) }

    val currencies = listOf("IDR", "USD", "EUR", "GBP", "SGD", "MYR", "JPY")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "Edit account" else "New account",
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account name") },
                singleLine = true,
                shape = EfinaCorners.menu,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountType.entries.forEach { candidate ->
                    FilterChip(
                        selected = type == candidate,
                        onClick = { type = candidate },
                        label = { Text(candidate.displayName) },
                        shape = EfinaCorners.pill
                    )
                }
            }

            OutlinedTextField(
                value = balance,
                onValueChange = { input ->
                    // Digits and one decimal separator only; the field is a decimal
                    // string because the API takes a number, not a formatted string.
                    if (input.isEmpty() || input.matches(Regex("^\\d{0,15}(\\.\\d{0,2})?$"))) {
                        balance = input
                    }
                },
                label = { Text(if (isEdit) "Balance" else "Starting balance") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = EfinaCorners.menu,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    if (isEdit) Text("Changing this creates an adjustment transaction.")
                }
            )

            Text("Currency", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                currencies.forEach { code ->
                    FilterChip(
                        selected = currency == code,
                        onClick = { currency = code },
                        label = { Text(code) },
                        shape = EfinaCorners.pill
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                shape = EfinaCorners.menu,
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            localError?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    localError = when {
                        name.isBlank() -> "Name is required"
                        balance.isNotBlank() && balance.toDoubleOrNull() == null ->
                            "Balance must be a number"
                        // An empty field must not be coerced to 0.0 on save: a PUT
                        // with balance 0 silently wipes the account's money.
                        isEdit && balance.isBlank() -> "Enter a balance"
                        else -> null
                    }
                    if (localError == null) {
                        val amount = balance.toDoubleOrNull() ?: 0.0
                        if (isEdit) {
                            viewModel.update(
                                account!!.id,
                                UpdateAccountRequest(
                                    name = name.trim(),
                                    type = type,
                                    balance = amount,
                                    currency = currency,
                                    description = description.ifBlank { null }
                                )
                            ) { onSaved() }
                        } else {
                            viewModel.create(
                                CreateAccountRequest(
                                    name = name.trim(),
                                    type = type,
                                    balance = amount,
                                    currency = currency,
                                    description = description.ifBlank { null }
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
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isEdit) "Save changes" else "Create account")
                }
            }

            if (state.error != null) {
                ErrorState(state.error!!)
            }
        }
    }
}

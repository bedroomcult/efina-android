package com.efina.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.UpdateSettingsRequest
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType

/**
 * Settings.
 *
 * The backend's PUT /settings takes a partial body and COALESCEs the rest, so
 * every control sends only the field it changes. Sending the whole object would
 * be harmless but would also overwrite fields this screen does not show.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToDebts: () -> Unit = {},
    onNavigateToScheduled: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = EfinaType.titleLargeEmphasized) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingList(rows = 6, modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        val settings = state.settings
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.user?.let { user ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = EfinaCorners.menu,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(user.name, style = EfinaType.titleMediumEmphasized)
                        Text(
                            user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PillTag(if (user.isPro) "Pro" else "Free")
                            if (user.isAdmin) PillTag("Admin")
                        }
                    }
                }
            }

            if (settings != null) {
                Text("Preferences", style = MaterialTheme.typography.titleMedium)
                Text("Currency", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("IDR", "USD", "EUR", "GBP", "SGD").forEach { code ->
                        FilterChip(
                            selected = settings.currency == code,
                            onClick = { viewModel.update(UpdateSettingsRequest(currency = code)) },
                            label = { Text(code) },
                            shape = EfinaCorners.pill
                        )
                    }
                }

                Text("Language", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("id" to "Indonesian", "en" to "English").forEach { (code, label) ->
                        FilterChip(
                            selected = settings.language == code,
                            onClick = { viewModel.update(UpdateSettingsRequest(language = code)) },
                            label = { Text(label) },
                            shape = EfinaCorners.pill
                        )
                    }
                }

                Text("Theme", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // `mono` and `dynamic` are handled by EfinaTheme; the twelve
                    // seeded palettes derive their scheme from a single accent.
                    val themes = listOf("light" to "Light", "dark" to "Dark", "mono" to "Mono") +
                        com.efina.app.ui.theme.GeneratedPalettes.keys.map {
                            it to it.replaceFirstChar(Char::uppercase)
                        }

                    themes.forEach { (code, label) ->
                        FilterChip(
                            selected = settings.theme == code,
                            onClick = { viewModel.update(UpdateSettingsRequest(theme = code)) },
                            label = { Text(label) },
                            shape = EfinaCorners.pill
                        )
                    }
                }

                HorizontalDivider()

                Text("Data", style = MaterialTheme.typography.titleMedium)
                SegmentedColumn {
                    SettingRow("Categories", "Manage income and expense categories",
                        first = true, onClick = onNavigateToCategories)
                    SettingRow("Debts", "Money you owe, and money owed to you",
                        onClick = onNavigateToDebts)
                    SettingRow("Scheduled", "Recurring income and expenses",
                        onClick = onNavigateToScheduled)
                    SettingRow("Family", "Share accounts with your household",
                        last = true, onClick = onNavigateToFamily)
                }


                Text(
                    "efina ${com.efina.app.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            // Outside the settings != null guard on purpose: if GET /settings
            // fails (offline), the user must still be able to sign out.
            HorizontalDivider()
            Text("Account", style = MaterialTheme.typography.titleMedium)
            SegmentedColumn {
                SettingRow("Change password", "Update your sign-in password",
                    first = true, onClick = { showPasswordDialog = true })
                SettingRow("Sign out", "End this session",
                    last = true, destructive = true, onClick = { confirmLogout = true })
            }
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            saving = state.saving,
            onDismiss = { showPasswordDialog = false },
            onSubmit = { current, next ->
                viewModel.changePassword(current, next) { showPasswordDialog = false }
            }
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again on this device.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("Sign out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    first: Boolean = false,
    last: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    SegmentedRow(first = first, last = last, onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("›", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChangePasswordDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = current, onValueChange = { current = it },
                    label = { Text("Current password") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = EfinaCorners.menu
                )
                OutlinedTextField(
                    value = next, onValueChange = { next = it },
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = EfinaCorners.menu,
                    supportingText = { Text("At least 6 characters") }
                )
                localError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    localError = when {
                        current.isBlank() -> "Enter your current password"
                        next.length < 6 -> "New password must be at least 6 characters"
                        else -> null
                    }
                    if (localError == null) onSubmit(current, next)
                }
            ) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

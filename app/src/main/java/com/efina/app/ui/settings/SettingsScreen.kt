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
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.Haptic
import com.efina.app.ui.theme.rememberHaptics
import com.efina.app.ui.components.BadgeToastHost
import com.efina.app.ui.components.BadgeTone
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
    // Short badge instead of a snackbar: one phrase, self-dismissing.
    var badge by remember { mutableStateOf<String?>(null) }
    val haptics = rememberHaptics()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let { badge = (it); viewModel.clearMessages() }
    }
    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { badge = (it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost = { BadgeToastHost(badge, BadgeTone.Error) { badge = null } },
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

        WidthCapped(ContentWidth.List) {
        if (state.loading) {
            LoadingList(rows = 6, modifier = Modifier.padding(padding).padding(16.dp))
            return@WidthCapped
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
                SettingsDropdown(
                    selected = settings.currency,
                    options = listOf("IDR", "USD", "EUR", "GBP", "SGD").map { it to it },
                    onSelect = { viewModel.update(UpdateSettingsRequest(currency = it)) }
                )

                Text("Language", style = MaterialTheme.typography.labelLarge)
                SettingsDropdown(
                    selected = settings.language,
                    options = listOf("id" to "Indonesian", "en" to "English"),
                    onSelect = { viewModel.update(UpdateSettingsRequest(language = it)) }
                )

                Text("Theme", style = MaterialTheme.typography.labelLarge)
                // A dropdown rather than a chip row: thirteen palettes do not fit a chip
                // wrap without turning into three ragged lines, and a dropdown can show
                // each palette's actual colour as a swatch.
                ThemeDropdown(
                    selected = settings.theme,
                    onSelect = { viewModel.update(UpdateSettingsRequest(theme = it)) }
                )

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

/** A one-line dropdown for picking a single value. */
@Composable
private fun SettingsDropdown(
    selected: String?,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second ?: "Not set"

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { open = true },
            shape = EfinaCorners.menu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                current,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Choose",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            options.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = {
                        if (code == selected) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected")
                        }
                    },
                    onClick = { onSelect(code); open = false }
                )
            }
        }
    }
}

/**
 * Theme picker showing each palette's primary colour as a swatch.
 *
 * The names are meaningless on their own — nothing tells you what "Grape" looks like —
 * so the swatch is the actual affordance. Light and dark are listed as modifiers of the
 * current palette rather than as palettes, which is how the theme value is stored.
 */
@Composable
private fun ThemeDropdown(selected: String?, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }

    // entry: stored value, label, swatch colour (null = follow the palette/system)
    data class Entry(val code: String, val label: String, val swatch: androidx.compose.ui.graphics.Color?)

    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val entries = buildList {
        add(Entry("system", "Follow system", null))
        add(Entry("dynamic", "Material You", null))
        add(Entry("mono", "Mono", MaterialTheme.colorScheme.onSurfaceVariant))
        com.efina.app.ui.theme.GeneratedPalettes.forEach { (id, set) ->
            add(
                Entry(
                    code = id,
                    label = set.label,
                    swatch = if (dark) set.dark.primary else set.light.primary
                )
            )
        }
        val current = selected?.takeIf { s -> com.efina.app.ui.theme.GeneratedPalettes.containsKey(s) }
        if (current != null) {
            add(Entry("light", "Light", null))
            add(Entry("dark", "Dark", null))
        }
    }

    val current = entries.firstOrNull { it.code == selected }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { open = true },
            shape = EfinaCorners.menu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Swatch(current?.swatch)
            Text(
                current?.label ?: "Choose a theme",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Choose a theme",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    leadingIcon = { Swatch(entry.swatch) },
                    text = { Text(entry.label) },
                    trailingIcon = {
                        if (entry.code == selected) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected")
                        }
                    },
                    onClick = { onSelect(entry.code); open = false }
                )
            }
        }
    }
}

/** A colour dot, or a hollow ring when the palette has no single representative hue. */
@Composable
private fun Swatch(color: androidx.compose.ui.graphics.Color?) {
    Box(
        Modifier
            .padding(end = 10.dp)
            .size(14.dp)
            .background(color ?: MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
    )
}

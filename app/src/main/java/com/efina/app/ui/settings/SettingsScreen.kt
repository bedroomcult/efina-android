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
import com.efina.app.domain.model.User
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
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import com.efina.app.ui.components.PickerOption
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api

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
                ProfileCard(
                    user = user,
                    onClick = { /* open profile edit screen when wired */ }
                )
            }

            if (settings != null) {
                Text("Preferences", style = MaterialTheme.typography.titleMedium)
                Text("Currency", style = MaterialTheme.typography.labelLarge)
                SettingsDropdown(
                    label = "Currency",
                    selected = settings.currency,
                    options = listOf("IDR", "USD", "EUR", "GBP", "SGD")
                        .map { Triple(it, it, null) },
                    onSelect = { viewModel.update(UpdateSettingsRequest(currency = it)) }
                )

                Text("Language", style = MaterialTheme.typography.labelLarge)
                SettingsDropdown(
                    label = "Language",
                    selected = settings.language,
                    options = listOf(
                        Triple("id", "Indonesian", null),
                        Triple("en", "English", null)
                    ),
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

                Text("Updates", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Notify new updates",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "Check GitHub for new versions on startup",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    Switch(
                        checked = state.updateNotifications,
                        onCheckedChange = { viewModel.setUpdateNotifications(it) }
                    )
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

/**
 * A settings picker that opens as a bottom sheet.
 *
 * Same interaction as the category and account pickers: tap the field, the sheet slides
 * up, pick a row, it slides away. An anchored DropdownMenu was the odd one out — it
 * appears wherever the field happens to sit, is awkward to reach one-handed, and behaves
 * differently from every other single-choice control in the app.
 */
@Composable
private fun SettingsDropdown(
    label: String,
    selected: String?,
    options: List<Triple<String, String, androidx.compose.ui.graphics.Color?>>,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }

    OutlinedButton(
        onClick = { open = true },
        shape = EfinaCorners.menu,
        modifier = Modifier.fillMaxWidth()
    ) {
        current?.third?.let { Swatch(it) }
        Text(
            current?.second ?: "Not set",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = "Choose $label",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (open) {
        ChoiceSheet(
            title = label,
            options = options.map { PickerOption(0L, it.second, null) }
                .mapIndexed { i, o -> o.copy(id = i.toLong()) },
            selectedIndex = options.indexOfFirst { it.first == selected },
            onSelectIndex = { idx ->
                onSelect(options[idx].first)
                open = false
            },
            onDismiss = { open = false }
        )
    }
}

/**
 * Theme picker, shown as a bottom sheet with each palette's primary colour.
 *
 * The names are meaningless on their own — nothing tells you what "Grape" looks like —
 * so the swatch is the actual affordance.
 */
@Composable
private fun ThemeDropdown(selected: String?, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }

    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val entries = buildList {
        add(Triple("system", "Follow system", null))
        add(Triple("dynamic", "Material You", null))
        add(Triple("mono", "Mono", MaterialTheme.colorScheme.onSurfaceVariant))
        com.efina.app.ui.theme.GeneratedPalettes.forEach { (id, set) ->
            add(
                Triple(
                    id,
                    set.label,
                    if (dark) set.dark.primary else set.light.primary
                )
            )
        }
        // Light/dark are modifiers of a chosen palette, not palettes themselves.
        if (selected != null && com.efina.app.ui.theme.GeneratedPalettes.containsKey(selected)) {
            add(Triple("light", "Light", null))
            add(Triple("dark", "Dark", null))
        }
    }

    val current = entries.firstOrNull { it.first == selected }

    OutlinedButton(
        onClick = { open = true },
        shape = EfinaCorners.menu,
        modifier = Modifier.fillMaxWidth()
    ) {
        current?.third?.let { Swatch(it) }
        Text(
            current?.second ?: "Choose a theme",
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

    if (open) {
        ThemeSheet(
            entries = entries,
            selected = selected,
            onSelect = { onSelect(it); open = false },
            onDismiss = { open = false }
        )
    }
}

/** Bottom sheet of plain choices, matching the category picker's rows. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSheet(
    title: String,
    options: List<PickerOption>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 420.dp)
            ) {
                itemsIndexed(options) { index, option ->
                    val chosen = index == selectedIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectIndex(index) }
                            .background(
                                if (chosen) MaterialTheme.colorScheme.primaryContainer
                                else androidx.compose.ui.graphics.Color.Transparent,
                                EfinaCorners.menu
                            )
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (chosen) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                item { Box(Modifier.height(16.dp)) }
            }
        }
    }
}

/** Bottom sheet of themes, each row led by its palette colour. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSheet(
    entries: List<Triple<String, String, androidx.compose.ui.graphics.Color?>>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                "Theme",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 480.dp)
            ) {
                items(entries) { entry ->
                    val chosen = entry.first == selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(entry.first) }
                            .background(
                                if (chosen) MaterialTheme.colorScheme.primaryContainer
                                else androidx.compose.ui.graphics.Color.Transparent,
                                EfinaCorners.menu
                            )
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Swatch(entry.third)
                        Text(
                            entry.second,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        if (chosen) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                item { Box(Modifier.height(16.dp)) }
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

/**
 * Profile card showing the signed-in user's avatar, name, email and role badges.
 *
 * MD3 pattern: leading avatar (48dp touch target), two-line text block, trailing
 * chevron. The whole card is tappable so the row itself is the interaction
 * surface rather than relying on a tiny icon target.
 *
 * @param user the signed-in user
 * @param onClick invoked when the card is tapped
 */
@Composable
private fun ProfileCard(
    user: User,
    onClick: () -> Unit
) {
    val initials = user.name
        .split(" ")
        .map { it.firstOrNull()?.uppercase() ?: "" }
        .joinToString("")
        .take(2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = EfinaCorners.menu,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .heightIn(min = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar: 48dp minimum touch target per MD3 accessibility guidance.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = EfinaType.titleLargeEmphasized,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillTag(
                        text = if (user.isPro) "Pro" else "Free",
                        modifier = Modifier
                    )
                    if (user.isAdmin) {
                        PillTag(
                            text = "Admin",
                            modifier = Modifier
                        )
                    }
                    Text(
                        text = "${user.accountCount} account${if (user.accountCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

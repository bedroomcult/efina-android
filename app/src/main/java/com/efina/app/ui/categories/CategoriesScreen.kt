package com.efina.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.Category
import com.efina.app.domain.model.CreateCategoryRequest
import com.efina.app.ui.components.*
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.LocalIsDarkTheme
import com.efina.app.ui.theme.parseHexColor
import com.efina.app.ui.theme.tonal
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.RevealLatch
import com.efina.app.ui.theme.Haptic
import com.efina.app.ui.theme.rememberHaptics

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val haptics = rememberHaptics()
    // Marks this screen as having played its entrance, so returning to the tab
    // does not replay the staggered reveal.
    RevealLatch.currentScreen = "categories"
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(com.efina.app.domain.model.CategoryType.EXPENSE) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var editName by remember { mutableStateOf("") }

    LaunchedEffect(state.error) {
        state.error?.let {
            // A failed save is the one moment a user needs to notice without
            // reading; the snackbar alone is easy to miss mid-scroll.
            haptics(Haptic.Reject)
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Categories", style = EfinaType.titleLargeEmphasized) },
                actions = {
                    IconButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add category")
                    }
                }
            )
        }
    ) { padding ->

        WidthCapped(ContentWidth.List) {        when {
            state.loading -> LoadingList(rows = 6, modifier = Modifier.padding(padding).padding(16.dp))
            state.categories.isEmpty() -> EmptyState(
                title = "No categories",
                message = "Add one to group your spending.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(state.categories, key = { _, c -> c.id }) { index, category ->
                    CategoryRow(
                        category = category,
                        first = index == 0,
                        last = index == state.categories.lastIndex,
                        onDelete = { pendingDelete = category },
                        onEdit = {
                            editing = category
                            editName = category.name
                        }
                    )
                }
            }
        }
    
        }
    }

    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false; newName = "" },
            title = { Text("New category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.efina.app.domain.model.CategoryType.entries.forEach { t ->
                            FilterChip(
                                selected = newType == t,
                                onClick = { newType = t },
                                label = { Text(t.label) },
                                shape = EfinaCorners.pill
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = EfinaCorners.menu
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank() && !state.saving,
                    onClick = {
                        viewModel.create(CreateCategoryRequest(newName.trim(), type = newType, color = "#6366f1")) {
                            creating = false
                            newName = ""
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { creating = false; newName = "" }) { Text("Cancel") }
            }
        )
    }

    editing?.let { cat ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Rename category") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = EfinaCorners.menu
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editName.isNotBlank() && !state.saving,
                    onClick = {
                        viewModel.update(cat.id, com.efina.app.domain.model.UpdateCategoryRequest(name = editName.trim())) {
                            editing = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }

    pendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${cat.name}?") },
            text = { Text("Transactions using it keep their amount but lose the category.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(cat.id); haptics(Haptic.Confirm); pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    first: Boolean,
    last: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dark = LocalIsDarkTheme.current
    val accent = parseHexColor(category.color, MaterialTheme.colorScheme.primary)
    val (container, onContainer) = tonal(accent, dark)

    val rowShape = when {
        first && last -> EfinaCorners.groupOuter
        first -> androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp
        )
        last -> androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp
        )
        else -> EfinaCorners.groupInner
    }
    Surface(
        modifier = Modifier.fillMaxWidth().springClick(rowShape, onClick = onEdit),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = rowShape
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .then(Modifier.background(container, EfinaCorners.icon(32))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category.name.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleSmall)
                if (category.isSystem) {
                    Text(
                        "Default",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!category.isSystem) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete ${category.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

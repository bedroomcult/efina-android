package com.efina.app.ui.categories

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
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
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
        when {
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
                        onDelete = { pendingDelete = category }
                    )
                }
            }
        }
    }

    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false; newName = "" },
            title = { Text("New category") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = EfinaCorners.menu
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank() && !state.saving,
                    onClick = {
                        viewModel.create(CreateCategoryRequest(newName.trim(), color = "#6366f1")) {
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

    pendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${cat.name}?") },
            text = { Text("Transactions using it keep their amount but lose the category.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(cat.id); pendingDelete = null
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
    onDelete: () -> Unit
) {
    val dark = LocalIsDarkTheme.current
    val accent = parseHexColor(category.color, MaterialTheme.colorScheme.primary)
    val (container, onContainer) = tonal(accent, dark)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = when {
            first && last -> EfinaCorners.groupOuter
            first -> androidx.compose.foundation.shape.RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp
            )
            last -> androidx.compose.foundation.shape.RoundedCornerShape(
                topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp
            )
            else -> EfinaCorners.groupInner
        }
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

private fun Modifier.background(color: Color, shape: androidx.compose.ui.graphics.Shape) =
    this.then(androidx.compose.foundation.background(color, shape))

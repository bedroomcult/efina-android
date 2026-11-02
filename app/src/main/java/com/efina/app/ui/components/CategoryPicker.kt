package com.efina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.efina.app.domain.model.Category
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.parseHexColor

/**
 * Two-level category picker, mirroring the web's CategorySelectionModal.
 *
 * Rules taken from that component so the two feel the same:
 *  - a parent with children expands on tap; tapping it again selects the parent
 *  - a parent with no children selects immediately
 *  - typing a search flattens the tree, and tapping a match selects it directly
 *    (no expand step, since the structure is already filtered away)
 *  - children are indented behind a left rule and inherit the parent's colour
 *
 * A single flat dropdown cannot express this: the tree is ~90 leaves across a dozen
 * roots, parents are themselves valid choices, and two branches commonly share a
 * child name like "Other".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    roots: List<Category>,
    selectedId: Long?,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<Long?>(null) }

    val searching = query.isNotBlank()
    val visible = remember(roots, query, searching) {
        if (!searching) roots
        else roots.mapNotNull { root ->
            val selfMatch = root.name.contains(query, ignoreCase = true)
            val kids = root.children.filter { it.name.contains(query, ignoreCase = true) }
            when {
                // A root that matches keeps all its children; a root whose children
                // match keeps only those, so the result shows why it matched.
                selfMatch -> root
                kids.isNotEmpty() -> root.copy(children = kids)
                else -> null
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                "Choose a category",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search categories") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = EfinaCorners.menu,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                if (visible.isEmpty()) {
                    item {
                        Text(
                            "No categories match \"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }

                items(visible, key = { it.id }) { root ->
                    val expanded = expandedId == root.id
                    val isSelected = selectedId == root.id
                    val dotColor = parseHexColor(root.color, MaterialTheme.colorScheme.primary)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Search results are already flattened, so a tap
                                // means select. Otherwise: expand first, then select.
                                when {
                                    searching -> onSelect(root)
                                    root.children.isEmpty() -> onSelect(root)
                                    expanded -> onSelect(root)
                                    else -> expandedId = root.id
                                }
                            }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                EfinaCorners.menu
                            )
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            when {
                                searching || root.children.isEmpty() -> Unit
                                expanded -> Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Collapse ${root.name}"
                                )
                                else -> Icon(
                                    Icons.Filled.KeyboardArrowRight,
                                    contentDescription = "Expand ${root.name}"
                                )
                            }
                        }
                        Box(
                            Modifier
                                .padding(start = 4.dp, end = 12.dp)
                                .size(12.dp)
                                .background(dotColor, CircleShape)
                        )
                        Text(
                            root.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if ((expanded && !searching) || (searching && root.children.isNotEmpty())) {
                        root.children.forEach { child ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(child) }
                                    .background(
                                        if (selectedId == child.id) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                        EfinaCorners.menu
                                    )
                                    .padding(start = 44.dp, top = 10.dp, bottom = 10.dp, end = 8.dp)
                            ) {
                                Box(
                                    Modifier
                                        .padding(end = 12.dp)
                                        .size(8.dp)
                                        .background(dotColor.copy(alpha = 0.7f), CircleShape)
                                )
                                Text(
                                    child.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedId == child.id) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item { Box(Modifier.height(16.dp)) }
            }
        }
    }
}

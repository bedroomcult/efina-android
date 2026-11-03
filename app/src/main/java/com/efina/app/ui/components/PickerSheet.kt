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
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.parseHexColor

/** One selectable row in [PickerSheet]. */
data class PickerOption(
    val id: Long,
    val label: String,
    /** Dot colour. Null falls back to the theme's primary. */
    val color: String? = null,
    /** Optional second line, e.g. the account balance. */
    val subtitle: String? = null
)

/**
 * Bottom-sheet picker with colour dots, matching the category picker.
 *
 * Accounts had a plain dropdown while categories got a sheet. The sheet is the better
 * control here for the same reasons: it carries a colour dot and a second line, it is
 * thumb-reachable, and it scales past a handful of rows. Two pickers that behave
 * differently for the same kind of choice is the inconsistency worth removing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSheet(
    title: String,
    options: List<PickerOption>,
    selectedId: Long?,
    onSelect: (PickerOption) -> Unit,
    onDismiss: () -> Unit,
    searchable: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val visible = remember(options, query) {
        if (query.isBlank()) options
        else options.filter { it.label.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // A search field over two or three accounts is noise; only show it when the
            // list is long enough that scanning becomes work.
            if (searchable && options.size > 6) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = EfinaCorners.menu,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                if (visible.isEmpty()) {
                    item {
                        Text(
                            "Nothing matches",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }

                items(visible, key = { it.id }) { option ->
                    val selected = option.id == selectedId
                    val dot = parseHexColor(option.color, MaterialTheme.colorScheme.primary)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                EfinaCorners.menu
                            )
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Box(
                            Modifier
                                .padding(end = 14.dp)
                                .size(14.dp)
                                .background(dot, CircleShape)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            option.subtitle?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        if (selected) {
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

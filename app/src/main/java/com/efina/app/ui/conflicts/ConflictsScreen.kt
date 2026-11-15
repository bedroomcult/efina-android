package com.efina.app.ui.conflicts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.efina.app.ui.components.BadgeTone
import com.efina.app.ui.components.BadgeToastHost
import com.efina.app.ui.components.ContentWidth
import com.efina.app.ui.components.EmptyState
import com.efina.app.ui.components.WidthCapped
import com.efina.app.ui.theme.EfinaSemantic
import com.efina.app.ui.theme.EfinaType

/**
 * Changes the server refused because the row had moved on.
 *
 * A conflict is a *lost update*, not a duplicate row: the id is unique the whole
 * time, but the edit was based on a version that no longer exists. Both payloads
 * are shown so the choice is informed rather than a coin flip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConflictsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync conflicts", style = EfinaType.titleLargeEmphasized) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        WidthCapped(ContentWidth.List, Modifier.padding(inner)) {
            when {
                state.loading -> Unit

                state.isEmpty -> EmptyState(
                    title = "No conflicts",
                    message = "Changes made offline that collide with a newer edit on " +
                        "another device appear here, so you can choose which to keep.",
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.conflicts, key = { it.id }) { row ->
                        ConflictCard(
                            row = row,
                            busy = state.busy,
                            onKeepMine = { viewModel.keepMine(row) },
                            onKeepServer = { viewModel.keepServer(row) },
                        )
                    }
                }
            }
        }

        BadgeToastHost(
            message = state.message,
            tone = BadgeTone.Info,
            onDismissed = viewModel::clearMessage,
        )
    }
}

@Composable
private fun ConflictCard(
    row: ConflictRow,
    busy: Boolean,
    onKeepMine: () -> Unit,
    onKeepServer: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.title,
                    style = EfinaType.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Changed elsewhere",
                    style = MaterialTheme.typography.labelSmall,
                    color = EfinaSemantic.danger,
                )
            }

            Spacer(Modifier.height(12.dp))

            PayloadBlock("Your change", row.localPayload, EfinaSemantic.info)
            Spacer(Modifier.height(8.dp))
            PayloadBlock("On the server", row.serverPayload, EfinaSemantic.caution)

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onKeepServer, enabled = !busy) {
                    Text("Keep server")
                }
                TextButton(onClick = onKeepMine, enabled = !busy && row.serverSyncSeq != null) {
                    Text("Keep mine")
                }
            }
        }
    }
}

@Composable
private fun PayloadBlock(label: String, payload: String, accent: androidx.compose.ui.graphics.Color) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(8.dp),
                )
                .padding(10.dp)
        ) {
            // Border-left style accent without needing a Canvas.
            Text(
                text = payload,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = accent,
                maxLines = 12,
            )
        }
    }
}

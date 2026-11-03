package com.efina.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Appears once the reader is past the eighth item, and jumps back to the top.
 *
 * The bottom inset keeps it clear of the floating dock, which occupies the same corner.
 */
@Composable
fun ScrollToTopFab(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val visible by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
            info.totalItemsCount > 8 && first > 8
        }
    }

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        FilledTonalIconButton(
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            // Clears the floating dock, which is drawn over the NavHost.
            modifier = modifier.padding(bottom = DockClearance)
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll to top")
        }
    }
}

package com.efina.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.dp
import com.efina.app.ui.theme.EfinaType

/**
 * Content width caps from design.md §11.
 *
 * Nothing in this app was capped, so on a tablet every screen stretched edge to edge:
 * a transaction row ran the full width of the display with its amount a hand-span from
 * its label. The spec gives three values and they cover every case here.
 */
enum class ContentWidth(val max: Int) {
    /** Wide grids. Unused today; kept so the scale is complete. */
    Grid(1200),

    /** Tab roots and long scrolling lists. */
    List(840),

    /** Create/edit forms, where an over-wide field is a usability problem. */
    Form(560)
}

/**
 * A 64dp title row (design.md §11), transparent so content scrolls beneath it.
 *
 * Positioned above the scrolling content rather than inside it: putting it in the
 * scrollable would make it scroll away on the tab roots, and every screen here keeps a
 * fixed title.
 */
@Composable
fun EfinaTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = EfinaType.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

/**
 * Caps and centres a screen's content.
 *
 * Applied to the caller's content rather than to a Scaffold, because the Scaffold's own
 * surfaces (snackbar, FAB) must keep spanning the full display.
 */
@Composable
fun WidthCapped(
    width: ContentWidth,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val side = maxOf(0.dp, (maxWidth - width.max.dp) / 2)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = side)
        ) { content() }
    }
}

/** Convenience: `PaddingValues` equivalent for content that clears the floating dock. */
val DockClearance: PaddingValues = PaddingValues(bottom = 96.dp)

package com.efina.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaMotion

/** One tab in the dock or rail. */
data class DockItem(val route: String, val icon: ImageVector, val label: String)

/** The width at which the dock becomes a rail (design.md §11). */
const val WIDE_LAYOUT_BREAKPOINT_DP = 600

/**
 * Floating pill dock, per design.md §7.3's "Pill (default)".
 *
 * An overlay rather than `Scaffold.bottomBar`: the spec insets it 12dp above the system
 * bar with content scrolling underneath, and a bottomBar slot would reserve that space
 * instead of letting the list run behind it.
 *
 * Only the selected tab shows its label. Five labelled tabs do not fit a 320dp screen,
 * and the expanding capsule is the point of this style.
 */
@Composable
fun FloatingDock(
    items: List<DockItem>,
    currentRoute: String?,
    onSelect: (DockItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = EfinaCorners.pill,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                DockTab(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.DockTab(item: DockItem, selected: Boolean, onClick: () -> Unit) {
    // Animating the width is why this is hand-built rather than a NavigationBarItem:
    // the capsule has to grow to admit the label, and no stock item does that.
    val capsuleWidth by animateDpAsState(
        targetValue = if (selected) 132.dp else 52.dp,
        animationSpec = EfinaMotion.spatial(),
        label = "dockCapsuleWidth"
    )

    Box(
        modifier = Modifier
            .width(capsuleWidth)
            .height(52.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selected) {
                Spacer4()
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun Spacer4() {
    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
}

/**
 * Wide-screen navigation, per design.md §7.4: at 600dp the dock becomes a rail, so a
 * tablet does not get a phone dock stretched across the bottom.
 */
@Composable
fun EfinaSideRail(
    items: List<DockItem>,
    currentRoute: String?,
    onSelect: (DockItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(modifier = modifier) {
        items.forEach { item ->
            NavigationRailItem(
                selected = currentRoute == item.route,
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, maxLines = 1) }
            )
        }
    }
}

package com.efina.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.efina.app.ui.theme.EfinaCorners
import androidx.compose.animation.core.tween
import com.efina.app.ui.theme.EfinaMotion
import com.efina.app.ui.theme.EfinaSemantic

/**
 * The offline strip.
 *
 * A slim bar under the status bar rather than a dialog or a toast: being offline
 * is a persistent state, not an event, and the user needs to know that what they
 * see is saved data rather than live data. It is a live region so a screen
 * reader announces the change once instead of on every recomposition.
 */
@Composable
fun OfflineBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
    pendingCount: Int = 0,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
        exit = shrinkVertically(tween(160)) + fadeOut(tween(160)),
        modifier = modifier,
    ) {
        val message = if (pendingCount > 0) {
            "Offline · $pendingCount change${if (pendingCount == 1) "" else "s"} will sync"
        } else {
            "Offline · showing saved data"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EfinaSemantic.caution.copy(alpha = 0.16f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics {
                    contentDescription = message
                    liveRegion = LiveRegionMode.Polite
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = EfinaSemantic.caution,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * The session died with a queue still waiting.
 *
 * Distinct from the offline banner: the network is fine, the token is not, and
 * the queued writes will not move until the user signs in again. Saying so is
 * the difference between "my changes are safe" and "my changes vanished".
 */
@Composable
fun ReauthBanner(
    visible: Boolean,
    pendingCount: Int,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
        exit = shrinkVertically(tween(160)) + fadeOut(tween(160)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EfinaSemantic.danger.copy(alpha = 0.16f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = EfinaSemantic.danger,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Sign in again to sync $pendingCount change${if (pendingCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSignIn) { Text("Sign in") }
        }
    }
}

/**
 * The top-bar sync chip: pending count, and a spinner while a flush is running.
 *
 * Tapping it opens the Sync section, which is where the detail lives. The chip
 * exists so the count is visible from any screen without opening Settings.
 */
@Composable
fun SyncChip(
    pendingCount: Int,
    syncing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pendingCount == 0 && !syncing) return
    val label = if (syncing) "Syncing" else "$pendingCount"
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, EfinaCorners.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics {
                contentDescription = if (syncing) "Syncing" else "$pendingCount changes waiting to sync"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (syncing) Icons.Default.Sync else Icons.Default.CloudQueue,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Marks a row that has not reached the server yet.
 *
 * A clock for "queued", a warning for "the server refused it". The second is
 * deliberately louder: a queued write will land on its own, a rejected one needs
 * the user.
 */
@Composable
fun PendingBadge(
    pending: Boolean,
    failed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (!pending && !failed) return
    val tint = if (failed) EfinaSemantic.danger else EfinaSemantic.caution
    val description = if (failed) "Not saved: needs your review" else "Waiting to sync"
    Box(
        modifier = modifier
            .size(16.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (failed) Icons.Default.WarningAmber else Icons.Default.Schedule,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
    }
}




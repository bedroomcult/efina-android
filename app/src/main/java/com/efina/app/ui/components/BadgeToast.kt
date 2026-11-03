package com.efina.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.efina.app.ui.theme.EfinaCorners

/** Tone of a [BadgeToast]; drives its icon and colour. */
enum class BadgeTone { Success, Info, Error }

/**
 * A short, self-dismissing badge — the toast replacement.
 *
 * The M3 `Snackbar` was the wrong shape for this app twice over: it is a full-width bar
 * with a message and optional action, and it sat under the floating dock, so its lower
 * edge was hidden. Here the badge is a small pill centred above the dock, carries one
 * short phrase, and is non-interactive — nothing in this app has ever used a snackbar
 * action, so the extra machinery was weight without benefit.
 *
 * Positioned by the caller (screens put it in the Scaffold's snackbarHost slot, whose
 * default placement already clears the bottom inset).
 */
@Composable
fun BadgeToastHost(
    message: String?,
    tone: BadgeTone = BadgeTone.Success,
    modifier: Modifier = Modifier,
    durationMillis: Long = 1800,
    onDismissed: () -> Unit = {}
) {
    // Keep the last message visible through the exit animation, otherwise the badge
    // disappears a frame before it slides out.
    var shown by remember { mutableStateOf(message) }
    var visible by remember { mutableStateOf(message != null) }

    LaunchedEffect(message) {
        if (message != null) {
            shown = message
            visible = true
            delay(durationMillis)
            visible = false
            onDismissed()
        } else {
            visible = false
        }
    }

    // Lifted clear of the floating dock, which is drawn over the NavHost and would
    // otherwise cover the badge's lower half.
    Box(
        modifier.fillMaxWidth().padding(bottom = 78.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(140)) + slideInVertically(tween(180)) { it / 2 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 2 }
        ) {
            val text = shown
            if (text != null) Badge(text = text, tone = tone)
        }
    }
}

@Composable
private fun Badge(text: String, tone: BadgeTone) {
    val (container, content, icon) = when (tone) {
        BadgeTone.Success -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Filled.CheckCircle
        )
        BadgeTone.Info -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurface,
            Icons.Filled.Info
        )
        BadgeTone.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Filled.Warning
        )
    }

    Surface(
        shape = EfinaCorners.pill,
        color = container,
        shadowElevation = 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1
            )
        }
    }
}

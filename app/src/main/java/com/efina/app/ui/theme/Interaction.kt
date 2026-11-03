package com.efina.app.ui.theme

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Interaction feedback: press squash, keyboard focus ring, and a haptic tick.
 *
 * Adapted from Orion-Store's `springClick` / `focusRing` (DESIGN_SYSTEM.md §9 rule
 * 7: "every tappable card/tile squashes on press with a ripple, focus ring and
 * haptic"). Efina had none of the three, which meant no tactile confirmation on a
 * tap and no visible focus for keyboard or D-pad users.
 *
 * Deliberately narrower than Orion's: their signature carries a long-press and a
 * ViewModel-owned haptic switch, while this app has no haptics setting yet, so the
 * tick is unconditional and goes through the system haptic constants rather than a
 * waveform table.
 */

/** Haptic strengths, named the way Orion's `H` enum names them. */
enum class Haptic { Selection, Confirm, Reject }

@Composable
fun rememberHaptics(): (Haptic) -> Unit {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(android.os.VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }
    return remember(vibrator) {
        { kind ->
            if (vibrator != null && vibrator.hasVibrator()) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val effect = when (kind) {
                            Haptic.Selection ->
                                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                            Haptic.Confirm ->
                                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                            Haptic.Reject ->
                                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                        }
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(if (kind == Haptic.Reject) 30L else 12L)
                    }
                }
            }
        }
    }
}

/**
 * A tappable surface that squashes on press, lifts slightly on focus, and ticks.
 *
 * `clip(shape)` comes first so the ripple is contained; the scale is applied through
 * `graphicsLayer` so it does not affect layout.
 */
fun Modifier.springClick(
    shape: Shape,
    haptic: Haptic? = Haptic.Selection,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = composed {
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val focused by src.collectIsFocusedAsState()
    val tick = rememberHaptics()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else if (focused) 1.04f else 1f,
        animationSpec = EfinaMotion.spatial(),
        label = "springClickScale"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .focusRing(src, shape)
        .clip(shape)
        .clickable(
            interactionSource = src,
            indication = LocalIndication.current,
            enabled = enabled
        ) {
            if (haptic != null) tick(haptic)
            onClick()
        }
}

/** A primary ring around whatever holds keyboard or D-pad focus. */
@Composable
fun Modifier.focusRing(source: InteractionSource, shape: Shape): Modifier {
    val focused by source.collectIsFocusedAsState()
    val width by animateDpAsState(
        targetValue = if (focused) 3.dp else 0.dp,
        animationSpec = EfinaMotion.spatial(),
        label = "focusRingWidth"
    )
    return if (width > 0.dp) border(width, MaterialTheme.colorScheme.primary, shape) else this
}

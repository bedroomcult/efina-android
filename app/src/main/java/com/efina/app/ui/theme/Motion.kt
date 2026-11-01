package com.efina.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Motion from design.md section 5, reimplemented without depending on the
 * Material 3 Expressive APIs.
 *
 * design.md specifies `MotionScheme.expressive()`, which only exists in
 * material3 1.4+. This project is pinned to the Compose BOM in build.gradle.kts,
 * so the *feel* is reproduced with plain spring specs instead: the same squash on
 * press, the same staggered entrance, the same press haptic hook. DESIGN-PARITY.md
 * records the API gap.
 *
 * All values respect the user's animation setting via [EfinaMotion.enabled].
 */

object EfinaMotion {

    /** Global switch, set from the theme/settings layer. "Disable animations". */
    var enabled: Boolean = true

    private fun <T> spec(expressive: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (enabled) expressive else tween(durationMillis = 0)

    /** Movement: springy, spatial. */
    fun <T> spatial(): FiniteAnimationSpec<T> = spec(
        spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    /** Colour and alpha: gentler, so fades do not wobble. */
    fun <T> effects(): FiniteAnimationSpec<T> = spec(
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    )

    /** Staggered entrance: 45ms per item, first 9 items only, per design.md. */
    fun staggerDelay(index: Int): Int =
        if (!enabled || index >= 9) 0 else index * 45
}

/**
 * `springClick` from design.md: scale to 0.94 while pressed, paired with a
 * selection haptic. Applied to cards and tiles.
 */
fun Modifier.springClick(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f,
    onPress: (() -> Unit)? = null
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressed = true
                    onPress?.invoke()
                }
                is PressInteraction.Release,
                is PressInteraction.Cancel -> pressed = false
            }
        }
    }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed && EfinaMotion.enabled) pressedScale else 1f,
        animationSpec = EfinaMotion.spatial(),
        label = "springClickScale"
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * `reveal(index)` from design.md: 28dp rise plus fade in, staggered. Returns the
 * modifier to apply and the alpha/offset pair via graphicsLayer.
 */
@Composable
fun revealState(index: Int): Pair<Float, Float> {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (EfinaMotion.enabled) {
            kotlinx.coroutines.delay(EfinaMotion.staggerDelay(index).toLong())
        }
        shown = true
    }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = EfinaMotion.effects(),
        label = "revealAlpha"
    )
    val offset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 0f else 28f,
        animationSpec = EfinaMotion.spatial(),
        label = "revealOffset"
    )
    return alpha to offset
}

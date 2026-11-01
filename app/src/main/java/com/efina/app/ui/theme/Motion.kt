package com.efina.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.MotionScheme
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
 * Motion from design.md section 5.
 *
 * design.md specifies `MotionScheme.expressive()` with `MaterialExpressiveTheme`.
 * Both now exist: material3 1.4.0 (BOM 2026.09.00) ships [MotionScheme] and the
 * expressive theme. [EfinaMotion.scheme] is the real thing, not an approximation,
 * and [EfinaTheme] passes it into `MaterialTheme`.
 *
 * design.md also asks for a "Disable Animations" switch that swaps in an instant
 * scheme where every spec is snap(). [EfinaMotion.enabled] does that by giving
 * [spec] a zero-duration tween when off, which is equivalent for every call site
 * and does not require a second MotionScheme instance.
 *
 * The custom pieces below (springClick, revealModifier) sit on top of the scheme
 * because design.md specifies exact numbers for them: 0.94 press scale, 28dp
 * rise, 45ms stagger, first 9 items.
 */

object EfinaMotion {

    /** Global switch, set from the theme/settings layer. "Disable animations". */
    var enabled: Boolean = true

    /**
     * The Material 3 Expressive motion scheme, straight from the library.
     * Spatial specs drive movement and shape; effect specs drive colour and
     * alpha, which is why they are separate.
     */
    val scheme: MotionScheme = MotionScheme.expressive()

    private fun <T> spec(expressive: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (enabled) expressive else snap()

    /** Movement and shape. */
    fun <T> spatial(): FiniteAnimationSpec<T> = spec(scheme.defaultSpatialSpec())

    /** Colour and alpha. */
    fun <T> effects(): FiniteAnimationSpec<T> = spec(scheme.defaultEffectsSpec())

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
 * `reveal(index)` from design.md: 28dp rise plus fade in, staggered.
 *
 * Returns a Modifier rather than a (alpha, offset) pair so it can be applied
 * directly inside a LazyColumn item lambda, which is not itself a @Composable
 * scope for destructuring.
 */
@Composable
fun revealModifier(index: Int): Modifier {
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
    return Modifier.graphicsLayer {
        this.alpha = alpha
        translationY = offset
    }
}

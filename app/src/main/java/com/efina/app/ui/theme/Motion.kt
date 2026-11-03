package com.efina.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.saveable.rememberSaveable

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

    /**
     * design.md asks for an instant MotionScheme where every spec is snap().
     * A zero-duration tween is equivalent here and works for every call site;
     * `snap()` only satisfies DurationBasedAnimationSpec, so using it directly
     * would not type-check against the FiniteAnimationSpec return type.
     */
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
 * design.md's `springClick` (0.94 press scale) is NOT implemented.
 *
 * Every tappable surface in this app is `Surface(onClick = ...)`, which keeps its
 * InteractionSource private and exposes no hook for an external modifier. The
 * helper that existed here had zero call sites for exactly that reason, so it was
 * deleted rather than left as dead code that reads like a feature.
 *
 * To add it later, the rows must move to `Modifier.clickable(interactionSource =
 * remember { MutableInteractionSource() })` so the press stream can be observed.
 */

/**
 * `reveal(index)` from design.md: 28dp rise plus fade in, staggered.
 *
 * Returns a Modifier rather than a (alpha, offset) pair so it can be applied
 * directly inside a LazyColumn item lambda, which is not itself a @Composable
 * scope for destructuring.
 */
@Composable
fun revealModifier(index: Int): Modifier {
    // rememberSaveable, not remember: inside a LazyColumn the composable is disposed
    // when it scrolls off screen, so `remember` reset the flag and every row
    // re-animated each time it recycled back into view.
    var shown by rememberSaveable { mutableStateOf(index > 8) }
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

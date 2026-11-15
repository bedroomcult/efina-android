package com.efina.app.ui.theme

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.LazyItemScope
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

    /** Duration that collapses to zero when animations are disabled. */
    private fun duration(millis: Int): Int = if (enabled) millis else 0

    /** Tab switch: fade-through with a slight scale-in, per motion-and-assets.md §1.3. */
    fun tabEnter(): EnterTransition =
        fadeIn(tween(duration(180))) + scaleIn(
            animationSpec = effects(),
            initialScale = 0.98f
        )

    fun tabExit(): ExitTransition = fadeOut(tween(duration(120)))

    /** Detail screens slide in from the right; pop reverses the direction. */
    fun detailEnter(): EnterTransition =
        slideInHorizontally(animationSpec = spatial()) { it / 4 } +
            fadeIn(tween(duration(120)))

    fun detailExit(): ExitTransition =
        slideOutHorizontally(animationSpec = spatial()) { -it / 4 } +
            fadeOut(tween(duration(120)))

    fun detailPopEnter(): EnterTransition =
        slideInHorizontally(animationSpec = spatial()) { -it / 4 } +
            fadeIn(tween(duration(120)))

    fun detailPopExit(): ExitTransition =
        slideOutHorizontally(animationSpec = spatial()) { it / 4 } +
            fadeOut(tween(duration(120)))
}

/**
 * Shared-element bounds for an account icon, keyed by account id.
 *
 * Call inside a [SharedTransitionLayout] and pass the composable's own
 * [AnimatedVisibilityScope]. The same key must be used on the list row and the
 * detail header for the icon to fly between them.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.accountIconBounds(
    accountId: Long,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = Modifier.sharedBounds(
    sharedContentState = rememberSharedContentState(key = "account-icon-$accountId"),
    animatedVisibilityScope = animatedVisibilityScope,
    enter = fadeIn(tween(if (EfinaMotion.enabled) 120 else 0)),
    exit = fadeOut(tween(if (EfinaMotion.enabled) 120 else 0)),
    boundsTransform = { _, _ -> EfinaMotion.spatial() }
)

/**
 * Lazy-list item motion that obeys the "Disable animations" setting.
 *
 * `animateItem()` falls back to its own spring specs when a spec is null, so
 * passing null for all three is what actually turns the add/remove/placement
 * animation off.
 */
fun LazyItemScope.efinaAnimateItem(modifier: Modifier = Modifier): Modifier = modifier.animateItem(
    fadeInSpec = if (EfinaMotion.enabled) EfinaMotion.effects() else null,
    placementSpec = if (EfinaMotion.enabled) EfinaMotion.spatial() else null,
    fadeOutSpec = if (EfinaMotion.enabled) EfinaMotion.effects() else null
)

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
    // Once per screen, across navigations.
    //
    // rememberSaveable alone fixed recycling but not navigation: each NavHost entry
    // gets its own saveable registry, so returning to a tab replayed the whole
    // staggered entrance. That is the flash. Rows past the first screenful skip it
    // entirely, and the per-screen latch below is keyed by route name so a tab
    // animates the first time and not on every return.
    if (index > 8) return Modifier
    var shown by remember { mutableStateOf(RevealLatch.isDone(RevealLatch.currentScreen)) }
    LaunchedEffect(Unit) {
        if (EfinaMotion.enabled) {
            kotlinx.coroutines.delay(EfinaMotion.staggerDelay(index).toLong())
        }
        shown = true
        RevealLatch.markDone(RevealLatch.currentScreen)
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

/**
 * Remembers which screens have already played their entrance.
 *
 * A process-lifetime set, keyed by route name. Deliberately not persisted to disk: a
 * cold start should animate, a tab switch should not.
 */
object RevealLatch {
    /** Set by each screen root before composing its list. */
    var currentScreen: String = ""

    private val done = mutableSetOf<String>()

    fun isDone(screen: String): Boolean = screen.isNotEmpty() && screen in done

    fun markDone(screen: String) {
        if (screen.isNotEmpty()) done += screen
    }
}

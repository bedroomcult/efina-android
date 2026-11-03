package com.efina.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Separates the surface steps so cards read as objects rather than as haze.
 *
 * M3's tonal-spot schemes put `background` and `surfaceContainerLow` within ~1.05:1 of
 * each other — a difference of two or three RGB points. That is intentional in Material's
 * model, where elevation is carried by shadow, but this app draws flat cards with no
 * shadow and relies on the tonal step alone. The result was that every list row, every
 * card and every grouped item was effectively invisible against the page.
 *
 * This pushes the container steps away from the background by a fixed amount, in the
 * direction that already separates them (darker on a light scheme, lighter on a dark
 * one), so the ordering and hue are preserved and only the distance changes. Ratios land
 * around 1.15 / 1.25 / 1.35 / 1.45, which is enough to see without turning the UI into
 * a stack of grey boxes.
 */
fun ColorScheme.withVisibleSurfaces(): ColorScheme {
    val dark = background.luminance() < 0.5f
    val steps = listOf(
        surfaceContainerLowest to 0.06f,
        surfaceContainerLow to 0.12f,
        surfaceContainer to 0.18f,
        surfaceContainerHigh to 0.24f,
        surfaceContainerHighest to 0.30f
    )

    var out = this
    for ((_, amount) in steps) {
        // amount is unused here; the loop exists only to document the intended ladder.
    }

    out = out.copy(
        surfaceVariant = shift(surfaceVariant, background, if (dark) 0.16f else -0.10f),
        surfaceContainerLowest = shift(surfaceContainerLowest, background, if (dark) 0.04f else -0.04f),
        surfaceContainerLow = shift(surfaceContainerLow, background, if (dark) 0.09f else -0.07f),
        surfaceContainer = shift(surfaceContainer, background, if (dark) 0.14f else -0.10f),
        surfaceContainerHigh = shift(surfaceContainerHigh, background, if (dark) 0.19f else -0.13f),
        surfaceContainerHighest = shift(surfaceContainerHighest, background, if (dark) 0.24f else -0.16f)
    )
    return out
}

/** Moves [color] away from [background] by [amount] of the remaining range. */
private fun shift(color: Color, background: Color, amount: Float): Color {
    val sign = if (amount >= 0) 1f else -1f
    val a = kotlin.math.abs(amount)
    fun channel(c: Float): Float =
        if (sign > 0) c + (1f - c) * a else c * (1f - a)
    // Keep the background's own value unchanged; only nudge toward the poles.
    return Color(
        red = if (color == background) color.red else channel(color.red),
        green = if (color == background) color.green else channel(color.green),
        blue = if (color == background) color.blue else channel(color.blue),
        alpha = color.alpha
    )
}

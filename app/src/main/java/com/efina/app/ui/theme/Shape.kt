package com.efina.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale from design.md section 4.
 *
 * The M3 corner scale (4, 8, 12, 16, 20, 28, full) is mapped onto the M3 [Shapes]
 * slots. The extra constants below cover the patterns design.md calls out that
 * have no M3 slot: hero cards at 28dp, grouped/segmented rows at 20dp outer with
 * 4dp inner and 2dp gaps, and pill-shaped chips.
 */
val EfinaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object EfinaCorners {
    /** Hero cards, carousel cards, bottom action bars (top corners only). */
    val hero = RoundedCornerShape(28.dp)

    /** Dropdown menus and sheets. */
    val menu = RoundedCornerShape(20.dp)

    /** Grouped list: 20dp outer, 4dp inner, 2dp gap between rows. */
    val groupOuter = RoundedCornerShape(20.dp)
    val groupInner = RoundedCornerShape(4.dp)

    /** Screenshots and media. */
    val media = RoundedCornerShape(16.dp)

    /** Search pill, chips, tags, badges. */
    val pill = RoundedCornerShape(percent = 50)

    /** Account/category icon tiles: ~24% of size, per design.md. */
    fun icon(size: Int) = RoundedCornerShape((size * 0.24f).dp)

    /** Amount entry keypad and its keys. */
    val keypad = RoundedCornerShape(20.dp)
}

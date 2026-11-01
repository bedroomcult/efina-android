package com.efina.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale from design.md section 3.
 *
 * The doc specifies Space Grotesk for the reference app and the standard M3 scale
 * plus "Emphasized" variants. Efina uses the platform default family (no font
 * download, no first-frame flash) and reproduces the scale and the emphatic
 * variants as named extras. DESIGN-PARITY.md records the font substitution.
 *
 * Money amounts use [tabular] so digits do not jitter when a balance updates.
 */

private val Default = FontFamily.Default

val Typography = Typography(
    displayLarge = TextStyle(Default, FontWeight.Bold, 57.sp, 64.sp, (-0.25).sp),
    displayMedium = TextStyle(Default, FontWeight.Bold, 45.sp, 52.sp, 0.sp),
    displaySmall = TextStyle(Default, FontWeight.Bold, 36.sp, 44.sp, 0.sp),

    headlineLarge = TextStyle(Default, FontWeight.SemiBold, 32.sp, 40.sp, 0.sp),
    headlineMedium = TextStyle(Default, FontWeight.SemiBold, 28.sp, 36.sp, 0.sp),
    headlineSmall = TextStyle(Default, FontWeight.SemiBold, 24.sp, 32.sp, 0.sp),

    titleLarge = TextStyle(Default, FontWeight.SemiBold, 22.sp, 28.sp, 0.sp),
    titleMedium = TextStyle(Default, FontWeight.Medium, 16.sp, 24.sp, 0.15.sp),
    titleSmall = TextStyle(Default, FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),

    bodyLarge = TextStyle(Default, FontWeight.Normal, 16.sp, 24.sp, 0.5.sp),
    bodyMedium = TextStyle(Default, FontWeight.Normal, 14.sp, 20.sp, 0.25.sp),
    bodySmall = TextStyle(Default, FontWeight.Normal, 12.sp, 16.sp, 0.4.sp),

    labelLarge = TextStyle(Default, FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
    labelMedium = TextStyle(Default, FontWeight.Medium, 12.sp, 16.sp, 0.5.sp),
    labelSmall = TextStyle(Default, FontWeight.Medium, 11.sp, 16.sp, 0.5.sp)
)

/**
 * "Emphasized" styles from design.md: same sizes, heavier weight. Used only for
 * section titles, screen titles and primary actions, per the doc's rule that
 * emphasis is rationed.
 */
object EfinaType {
    val displaySmallEmphasized = TextStyle(Default, FontWeight.Black, 36.sp, 44.sp, 0.sp)
    val headlineSmallEmphasized = TextStyle(Default, FontWeight.Bold, 24.sp, 32.sp, 0.sp)
    val titleLargeEmphasized = TextStyle(Default, FontWeight.Bold, 22.sp, 28.sp, 0.sp)
    val titleMediumEmphasized = TextStyle(Default, FontWeight.SemiBold, 16.sp, 24.sp, 0.15.sp)
    val bodyMediumEmphasized = TextStyle(Default, FontWeight.Medium, 14.sp, 20.sp, 0.25.sp)

    /** Uppercase eyebrow labels above sections. */
    val eyebrow = TextStyle(Default, FontWeight.Black, 11.sp, 16.sp, 0.8.sp)

    /** Monospaced, used for account numbers and IDs. */
    val code = TextStyle(FontFamily.Monospace, FontWeight.Medium, 13.sp, 18.sp, 0.sp)

    /** Money figures. Tabular by default via the caller's fontFeatureSettings. */
    val amountLarge = TextStyle(Default, FontWeight.Black, 32.sp, 38.sp, (-0.5).sp)
    val amountMedium = TextStyle(Default, FontWeight.Bold, 20.sp, 26.sp, (-0.2).sp)
    val amountSmall = TextStyle(Default, FontWeight.Bold, 15.sp, 20.sp, 0.sp)
}

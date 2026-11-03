package com.efina.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Keeps the status- and navigation-bar icons legible against the active palette.
 *
 * `enableEdgeToEdge()` with no arguments installs a fixed light scrim and never
 * revisits it, so the bars kept whatever style the launch theme implied. With a light
 * palette the icons stayed light-on-light and effectively disappeared; with OLED they
 * were dark-on-black. Neither followed a theme change at runtime.
 *
 * This reads the *actual* surface colour from the composed theme, so it tracks palette
 * switches, Dusk/OLED, and Material You without knowing any of them by name. The
 * luminance test is the same one Material uses to decide `onSurface` polarity.
 */
@Composable
fun SyncSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return

    val background = MaterialTheme.colorScheme.background
    val darkIcons = background.luminance() > 0.5f

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        // true = dark icons, for a light background.
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
    }
}

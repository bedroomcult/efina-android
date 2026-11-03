package com.efina.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.efina.app.ui.EfinaAppRoot
import com.efina.app.ui.routeForUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * The route an inbound link should land on.
     *
     * A StateFlow, not a plain field, because the activity is `singleTop`: a link
     * tapped while the app is already running arrives through onNewIntent, and
     * Compose has to observe that change. Assigning to a var would update the
     * value but not recompose, so the link would be silently ignored — the app
     * would just come to the front on whatever screen it was already showing.
     */
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        pendingRoute.value = routeForUri(intent?.dataString)
        super.onCreate(savedInstanceState)
        // No arguments: enableEdgeToEdge() defaults to SystemBarStyle.auto with a fixed
        // light scrim, so the status-bar icons never follow the app's palette. On a
        // light theme the icons stay light-on-light and vanish. EfinaAppRoot re-applies
        // the correct style once the theme is known (see SyncSystemBars there).
        enableEdgeToEdge()

        // EfinaAppRoot owns the theme, because the palette is driven by the
        // server-side settings row (GET /settings). Nothing here may read
        // MaterialTheme.colorScheme: there is no theme above this point.
        setContent {
            EfinaAppRoot(deepLinkRoute = pendingRoute.asStateFlow())
        }
    }

    /**
     * Called instead of onCreate when a deep link arrives at a running instance,
     * because the activity is singleTop. Without this the link is dropped.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeForUri(intent.dataString)?.let { pendingRoute.value = it }
    }
}

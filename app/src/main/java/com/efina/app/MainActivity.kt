package com.efina.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.efina.app.ui.EfinaAppRoot
import com.efina.app.ui.routeForUri
import com.efina.app.util.NotificationHelper
import com.efina.app.util.PreferencesManager
import com.efina.app.util.UpdateChecker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var updateChecker: UpdateChecker

    @Inject
    lateinit var preferences: PreferencesManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val _updateAvailable = MutableStateFlow<UpdateChecker.UpdateInfo?>(null)
    val updateAvailable: StateFlow<UpdateChecker.UpdateInfo?> = _updateAvailable.asStateFlow()

    /** Holds an update waiting for notification permission to be granted. */
    private var pendingUpdateInfo: UpdateChecker.UpdateInfo? = null

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

    /** Launcher for the POST_NOTIFICATIONS runtime permission (API 33+). */
    private val requestNotificationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingUpdateInfo?.let { updateInfo ->
                    notificationHelper.notifyUpdate(updateInfo)
                    preferences.setLastNotifiedVersion(updateInfo.version)
                    pendingUpdateInfo = null
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        pendingRoute.value = routeForUri(intent?.dataString)
        super.onCreate(savedInstanceState)
        // No arguments: enableEdgeToEdge() defaults to SystemBarStyle.auto with a fixed
        // light scrim, so the status-bar icons never follow the app's palette. On a
        // light theme the icons stay light-on-light and vanish. EfinaAppRoot re-applies
        // the correct style once the theme is known (see SyncSystemBars there).
        enableEdgeToEdge()

        // Check for updates on startup if notifications are enabled
        lifecycleScope.launch {
            val updateInfo = updateChecker.checkForUpdatesIfEnabled(BuildConfig.VERSION_NAME)
            _updateAvailable.value = updateInfo

            if (updateInfo != null) {
                val lastNotified = preferences.lastNotifiedVersion.first()
                if (lastNotified == updateInfo.version) {
                    // Already notified about this version — skip duplicate notification.
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Need runtime permission before posting; store the update info
                    // and request it from the user.
                    pendingUpdateInfo = updateInfo
                    requestNotificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    // API < 33 or permission already granted — post straight away.
                    notificationHelper.notifyUpdate(updateInfo)
                    preferences.setLastNotifiedVersion(updateInfo.version)
                }
            }
        }

        // EfinaAppRoot owns the theme, because the palette is driven by the
        // server-side settings row (GET /settings). Nothing here may read
        // MaterialTheme.colorScheme: there is no theme above this point.
        setContent {
            EfinaAppRoot(
                deepLinkRoute = pendingRoute.asStateFlow(),
                updateAvailable = updateAvailable
            )
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

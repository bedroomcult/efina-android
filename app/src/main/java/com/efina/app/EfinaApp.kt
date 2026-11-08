package com.efina.app

import android.app.Application
import com.efina.app.util.CrashReporter
import com.efina.app.ui.theme.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EfinaApp : Application() {

    @Inject
    lateinit var preferences: com.efina.app.util.PreferencesManager

    override fun onCreate() {
        super.onCreate()
        // Installed before anything else runs, so a crash during Hilt graph
        // construction or the first composition still reaches the server.
        CrashReporter.install(this)

        // Restore the saved theme before any composition, so the first frame uses
        // the user's palette instead of flashing the default.
        runBlocking {
            ThemePreference.apply(preferences.theme.first())
        }
    }
}

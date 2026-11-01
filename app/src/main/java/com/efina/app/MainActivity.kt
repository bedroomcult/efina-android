package com.efina.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.efina.app.ui.EfinaAppRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // EfinaAppRoot owns the theme, because the palette is driven by the
        // server-side settings row (GET /settings). Nothing here may read
        // MaterialTheme.colorScheme: there is no theme above this point.
        setContent {
            EfinaAppRoot()
        }
    }
}

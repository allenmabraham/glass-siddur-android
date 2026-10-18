package com.rowshr.siddur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rowshr.siddur.fold.LocalFoldInfo
import com.rowshr.siddur.fold.rememberFoldInfo
import com.rowshr.siddur.ui.LocalAppContainer
import com.rowshr.siddur.ui.SiddurApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SiddurApplication).container
        setContent {
            val fold by rememberFoldInfo(this)
            CompositionLocalProvider(LocalAppContainer provides container, LocalFoldInfo provides fold) {
                // `adb shell am start -n com.rowshr.siddur/.MainActivity --es route reader/ASHKENAZ/0.0.0` opens a screen directly (screenshots, tests).
                SiddurApp(initialRoute = intent?.getStringExtra("route"))
            }
        }
    }
}

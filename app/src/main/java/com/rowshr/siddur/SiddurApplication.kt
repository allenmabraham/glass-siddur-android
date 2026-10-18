package com.rowshr.siddur

import android.app.Application
import com.rowshr.siddur.clock.LocationService
import com.rowshr.siddur.data.BundledTexts
import com.rowshr.siddur.data.SiddurLibrary
import com.rowshr.siddur.data.TextRepository
import com.rowshr.siddur.settings.ReadingSettingsStore
import com.rowshr.siddur.settings.UserLibrary

/** Single place that owns the app's services; small enough not to need a DI framework. */
class SiddurApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val settings = ReadingSettingsStore(app)
    val library = UserLibrary(app)
    val location = LocationService(app)
    val siddur = SiddurLibrary(app)
    val bundled = BundledTexts(app)
    val texts = TextRepository(app, bundled)
}

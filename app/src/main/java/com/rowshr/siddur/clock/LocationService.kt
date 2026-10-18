package com.rowshr.siddur.clock

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Opt-in, one-shot coarse location so sunrise/sunset can be computed locally. Nothing leaves the device. */
class LocationService(private val context: Context) {
    private val _coordinate = MutableStateFlow<Coordinate?>(null)
    val coordinate: StateFlow<Coordinate?> = _coordinate

    val isAuthorized: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Reads the last known or a fresh coarse fix when permission has been granted. */
    fun refresh() {
        if (!isAuthorized) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }
        try {
            manager.getLastKnownLocation(provider)?.let { _coordinate.value = Coordinate(it.latitude, it.longitude) }
            if (Build.VERSION.SDK_INT >= 30) {
                manager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                    location?.let { _coordinate.value = Coordinate(it.latitude, it.longitude) }
                }
            }
        } catch (_: SecurityException) {
        }
    }
}

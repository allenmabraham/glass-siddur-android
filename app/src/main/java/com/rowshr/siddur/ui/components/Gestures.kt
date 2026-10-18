package com.rowshr.siddur.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Reports pinch zoom without claiming single-finger drags, so a list underneath still scrolls.
 * [onZoomStart] fires when a second finger lands; [onZoom] with the cumulative factor since then.
 */
fun Modifier.pinchToZoom(onZoomStart: () -> Unit, onZoom: (Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var zoom = 1f
        var pinching = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break
            if (pressed.size >= 2) {
                if (!pinching) { pinching = true; onZoomStart() }
                val z = event.calculateZoom()
                if (abs(z - 1f) > 0.001f) {
                    zoom *= z
                    onZoom(zoom)
                }
                event.changes.forEach { it.consume() }
            }
        }
    }
}

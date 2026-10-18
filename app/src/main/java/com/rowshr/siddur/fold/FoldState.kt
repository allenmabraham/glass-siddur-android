package com.rowshr.siddur.fold

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo

/**
 * The fold as the app sees it. Jetpack WindowManager reports a [FoldingFeature] only while
 * the app's window spans a hinge, which on a Galaxy Z Fold or Pixel Fold means the inner
 * display is in use; on the cover display, and on every ordinary phone, there is none.
 */
data class FoldInfo(
    val feature: FoldingFeature? = null,
    /** The hardware has a hinge-angle sensor, i.e. it is a foldable at all. */
    val hasHinge: Boolean = false,
    /** Width of the window in dp. */
    val windowWidthDp: Int = 0,
) {
    /**
     * The device is opened: the window spans the hinge of the inner display. Some devices
     * (and the emulator) only report the hinge after it first moves, so a wide window on
     * hinge hardware counts as open too; the cover display of every foldable is narrower.
     */
    val isOpen: Boolean get() = feature != null || (hasHinge && windowWidthDp >= 600)

    /** The crease runs top to bottom, like the spine of an open book. */
    val isVerticalHinge: Boolean get() = feature?.orientation == FoldingFeature.Orientation.VERTICAL

    /** Half open on a table, the crease horizontal, like a laptop. */
    val isTabletop: Boolean
        get() = feature?.state == FoldingFeature.State.HALF_OPENED && feature.orientation == FoldingFeature.Orientation.HORIZONTAL

    companion object {
        fun from(info: WindowLayoutInfo, hasHinge: Boolean, windowWidthDp: Int): FoldInfo =
            FoldInfo(info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull(), hasHinge, windowWidthDp)
    }
}

val LocalFoldInfo = compositionLocalOf { FoldInfo() }

/** Observes the window's folding features for the lifetime of the activity. */
@Composable
fun rememberFoldInfo(activity: Activity): State<FoldInfo> {
    val hasHinge = remember(activity) {
        val pm = activity.packageManager
        val sensors = activity.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        android.os.Build.VERSION.SDK_INT >= 30 && (
            pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_HINGE_ANGLE) ||
                sensors?.getDefaultSensor(android.hardware.Sensor.TYPE_HINGE_ANGLE) != null
            )
    }
    val widthDp = LocalConfiguration.current.screenWidthDp
    return produceState(initialValue = FoldInfo(null, hasHinge, widthDp), activity, hasHinge, widthDp) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect {
            Log.i("Siddur.Fold", "window layout: ${it.displayFeatures} hinge=$hasHinge width=${widthDp}dp")
            value = FoldInfo.from(it, hasHinge, widthDp)
        }
    }
}

/** Two facing pages with the crease between them. Widths are content widths: outer page margins and the margins either side of the fold are taken out. */
data class SpreadPages(val left: Dp, val gutter: Dp, val right: Dp)

/** How a screen lays itself out around the fold. */
sealed interface ReadingArrangement {
    /** One column: every flat phone, and a foldable's cover display. */
    data object Page : ReadingArrangement

    /** The device open like a book, the fold vertical: two pages, the crease as the gutter. */
    data class Spread(val pages: SpreadPages) : ReadingArrangement

    /** The device half open on a table, the fold horizontal: reading surface above, controls below. */
    data class Tabletop(val top: Dp, val gap: Dp) : ReadingArrangement

    companion object {
        /**
         * @param widthDp,heightDp size of the composable that will be laid out.
         * @param originX,originY that composable's position in the window, in dp: the fold
         *   bounds are window coordinates, and a navigation rail may sit to the left of the content.
         */
        fun resolve(fold: FoldInfo, widthDp: Float, heightDp: Float, density: Float, inset: Dp, originX: Float = 0f, originY: Float = 0f): ReadingArrangement {
            val feature = fold.feature ?: return Page
            val b = feature.bounds
            val left = b.left / density - originX
            val top = b.top / density - originY
            val foldWidth = b.width() / density
            val foldHeight = b.height() / density
            val right = widthDp - (b.right / density - originX)
            val bottom = heightDp - (b.bottom / density - originY)
            if (feature.orientation == FoldingFeature.Orientation.VERTICAL && left > 140 && right > 140) {
                return Spread(SpreadPages(left = (left - 2 * inset.value).dp, gutter = (foldWidth + 2 * inset.value).dp, right = (right - 2 * inset.value).dp))
            }
            if (fold.isTabletop && top > 180 && bottom > 140) {
                return Tabletop(top = top.dp, gap = foldHeight.dp)
            }
            return Page
        }
    }
}

/** A box that knows how the fold crosses it: [BoxWithConstraints] plus the arrangement to use. */
@Composable
fun FoldAwareBox(
    inset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(ReadingArrangement) -> Unit,
) {
    val fold = LocalFoldInfo.current
    val density = LocalDensity.current.density
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }) {
        val arrangement = remember(fold, maxWidth, maxHeight, origin, inset) {
            ReadingArrangement.resolve(fold, maxWidth.value, maxHeight.value, density, inset, origin.x / density, origin.y / density)
        }
        content(arrangement)
    }
}

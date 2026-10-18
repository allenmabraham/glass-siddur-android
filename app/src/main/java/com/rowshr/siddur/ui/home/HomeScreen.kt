package com.rowshr.siddur.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.rowshr.siddur.clock.JewishClock
import com.rowshr.siddur.fold.FoldAwareBox
import com.rowshr.siddur.fold.LocalFoldInfo
import com.rowshr.siddur.fold.ReadingArrangement
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.model.ServiceSlot
import com.rowshr.siddur.model.SiddurNode
import com.rowshr.siddur.settings.ReadingSettings
import com.rowshr.siddur.ui.LocalAppContainer
import com.rowshr.siddur.ui.SettingsRoute
import com.rowshr.siddur.ui.components.Backdrop
import com.rowshr.siddur.ui.components.BackdropStyle
import com.rowshr.siddur.ui.components.LanguageModePicker
import com.rowshr.siddur.ui.components.NusachPicker
import com.rowshr.siddur.ui.components.SectionHeading
import com.rowshr.siddur.ui.components.SurfaceCard
import com.rowshr.siddur.ui.components.glassBorder
import com.rowshr.siddur.ui.components.glassColor
import com.rowshr.siddur.ui.openReader
import com.rowshr.siddur.ui.openSection
import com.rowshr.siddur.ui.theme.Brand
import com.rowshr.siddur.ui.theme.BrandDeep
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Front screen: what to daven now, quick language/nusach switches, and the services. */
@Composable
fun HomeScreen(nav: NavHostController) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val library by container.library.state.collectAsStateWithLifecycle()
    val coordinate by container.location.coordinate.collectAsStateWithLifecycle()
    val fold = LocalFoldInfo.current

    // The clock ticks once a minute so the suggested service follows the day.
    val now by produceState(ZonedDateTime.now()) {
        while (true) { value = ZonedDateTime.now(); delay(60_000) }
    }
    val moment = remember(now, coordinate, settings.useLocation) {
        JewishClock(now, if (settings.useLocation) coordinate else null).moment()
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) container.location.refresh() }
    LaunchedEffect(settings.useLocation) {
        if (settings.useLocation) {
            if (container.location.isAuthorized) container.location.refresh() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val inset = 18.dp
    Box(Modifier.fillMaxSize()) {
        Backdrop(BackdropStyle.HOME)
        FoldAwareBox(inset) { arrangement ->
            val safe = WindowInsets.safeDrawing.asPaddingValues()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(top = safe.calculateTopPadding(), bottom = safe.calculateBottomPadding() + 32.dp)
                    .padding(horizontal = inset),
            ) {
                TitleBar(subtitle = "${moment.weekdayName} · ${moment.hebrewDateEnglish}", onSettings = { nav.navigate(SettingsRoute) })
                val today: @Composable () -> Unit = {
                    NowCard(moment, settings, onBegin = { node -> nav.openReader(settings.nusach, node.id) })
                }
                val pickers: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LanguageModePicker(
                            mode = settings.languageMode(fold.isOpen),
                            onSelect = { container.settings.setLanguageMode(it, fold.isOpen) },
                            enabled = settings.nusach.hasEnglish,
                            modifier = Modifier.then(if (settings.nusach.hasEnglish) Modifier else Modifier.alpha55()),
                        )
                        NusachPicker(settings.nusach, onSelect = { n -> container.settings.update { it.copy(nusach = n) } })
                    }
                }
                val continueCard: @Composable () -> Unit = {
                    val last = library.lastRead
                    if (last != null && last.nusach == settings.nusach) {
                        container.siddur.root(last.nusach).node(last.nodeId)?.let { node ->
                            ContinueCard(node) { nav.openReader(last.nusach, node.id) }
                        }
                    }
                }
                val services: @Composable () -> Unit = {
                    ServicesGrid(settings.nusach, moment.slot, onOpen = { node -> nav.openReader(settings.nusach, node.id) })
                }
                val browse: @Composable () -> Unit = {
                    BrowseAllCard(settings.nusach) { nav.openSection(settings.nusach, "root") }
                }
                when (arrangement) {
                    is ReadingArrangement.Spread -> {
                        // Open like a book: today on the left page, the siddur on the right.
                        Row(horizontalArrangement = Arrangement.spacedBy(arrangement.pages.gutter), verticalAlignment = Alignment.Top) {
                            Column(Modifier.width(arrangement.pages.left), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                                today(); continueCard(); browse(); Footer()
                            }
                            Column(Modifier.width(arrangement.pages.right), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                                pickers(); services()
                            }
                        }
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        today(); pickers(); continueCard(); services(); browse(); Footer()
                    }
                }
            }
        }
    }
}

private fun Modifier.alpha55(): Modifier = this.alpha(0.55f)

@Composable
private fun TitleBar(subtitle: String, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("Siddur", style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier.shadow(4.dp, CircleShape).clip(CircleShape).background(glassColor()).border(glassBorder(), CircleShape),
        ) {
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Brand) }
        }
    }
}

/** The hero: the service for this moment, with the Hebrew date. */
@Composable
fun NowCard(moment: JewishClock.Moment, settings: ReadingSettings, onBegin: (SiddurNode) -> Unit) {
    val container = LocalAppContainer.current
    val node = remember(moment.slot, settings.nusach) { container.siddur.node(moment.slot, settings.nusach) }
    SurfaceCard(Modifier.fillMaxWidth(), padding = 22.dp, cornerRadius = 30.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        dayLabel(moment).uppercase(), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp,
                    )
                    Text(moment.hebrewDateHebrew, fontFamily = settings.hebrewFont.family, fontSize = 22.sp, lineHeight = 30.sp)
                }
                Icon(moment.slot.icon, contentDescription = null, tint = Brand, modifier = Modifier.size(32.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Now", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        moment.slot.title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(moment.slot.hebrewTitle, fontFamily = settings.hebrewFont.family, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (node != null) BrandButton(text = "Begin", icon = Icons.Filled.MenuBook) { onBegin(node) }
                if (moment.isRoshChodesh) Badge("Rosh Chodesh")
                Spacer(Modifier.weight(1f))
                moment.sunset?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.WbTwilight, contentDescription = "Sunset", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text(it.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun dayLabel(moment: JewishClock.Moment) = when (moment.dayType) {
    JewishClock.DayType.WEEKDAY -> moment.weekdayName
    JewishClock.DayType.EREV_SHABBAT -> "Erev Shabbat"
    JewishClock.DayType.SHABBAT -> "Shabbat Kodesh"
    JewishClock.DayType.MOTZAEI_SHABBAT -> "Motza'ei Shabbat"
}

val ServiceSlot.icon: ImageVector
    get() = when (this) {
        ServiceSlot.SHACHARIT, ServiceSlot.SHABBAT_SHACHARIT -> Icons.Outlined.Brightness5
        ServiceSlot.MINCHA, ServiceSlot.SHABBAT_MINCHA -> Icons.Filled.WbTwilight
        ServiceSlot.MAARIV, ServiceSlot.SHABBAT_MAARIV -> Icons.Filled.NightsStay
        ServiceSlot.KABBALAT_SHABBAT -> Icons.Filled.LocalFireDepartment
        ServiceSlot.SHABBAT_MUSAF -> Icons.Filled.WbSunny
        ServiceSlot.HAVDALAH -> Icons.Filled.LocalFireDepartment
        ServiceSlot.BEDTIME_SHEMA -> Icons.Filled.Bedtime
    }

/** Prominent call-to-action: brand-filled capsule. */
@Composable
fun BrandButton(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .shadow(6.dp, CircleShape, spotColor = Brand.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Brand, BrandDeep)))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = Brand,
        modifier = Modifier.clip(CircleShape).background(Brand.copy(alpha = 0.14f)).border(glassBorder(), CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun ContinueCard(node: SiddurNode, onClick: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth(), padding = 16.dp, cornerRadius = 22.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = Brand, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("CONTINUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(node.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(node.heTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ServicesGrid(nusach: Nusach, current: ServiceSlot, onOpen: (SiddurNode) -> Unit) {
    val container = LocalAppContainer.current
    val slots = if (nusach.hasShabbat) listOf(
        ServiceSlot.SHACHARIT, ServiceSlot.MINCHA, ServiceSlot.MAARIV, ServiceSlot.KABBALAT_SHABBAT, ServiceSlot.SHABBAT_SHACHARIT,
        ServiceSlot.SHABBAT_MUSAF, ServiceSlot.SHABBAT_MINCHA, ServiceSlot.HAVDALAH, ServiceSlot.BEDTIME_SHEMA,
    ) else listOf(ServiceSlot.SHACHARIT, ServiceSlot.MINCHA, ServiceSlot.MAARIV, ServiceSlot.BEDTIME_SHEMA)
    val tiles = remember(nusach) { slots.mapNotNull { slot -> container.siddur.node(slot, nusach)?.let { slot to it } } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("Services", "תפילות")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false,
            modifier = Modifier.heightIn(max = 2000.dp),
        ) {
            items(tiles, key = { it.first.name }) { (slot, node) ->
                ServiceTile(slot, node, highlighted = slot == current) { onOpen(node) }
            }
        }
    }
}

@Composable
private fun ServiceTile(slot: ServiceSlot, node: SiddurNode, highlighted: Boolean, onClick: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(24.dp)
    val fg = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface
    val secondary = if (highlighted) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .shadow(if (highlighted) 14.dp else 6.dp, shape, spotColor = Color.Black.copy(alpha = if (highlighted) 0.25f else 0.1f))
            .clip(shape)
            .then(if (highlighted) Modifier.background(Brush.linearGradient(listOf(Brand, BrandDeep))) else Modifier.background(glassColor()))
            .border(glassBorder(), shape)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .heightIn(min = 118.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(slot.icon, contentDescription = null, tint = if (highlighted) Color.White else Brand, modifier = Modifier.size(24.dp))
            Spacer(Modifier.weight(1f))
            Text(slot.hebrewTitle, fontFamily = settings.hebrewFont.family, fontSize = 17.sp, color = secondary, maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(slot.title, style = MaterialTheme.typography.titleMedium, color = fg)
            Text("${node.leafCount} ${if (node.leafCount == 1) "prayer" else "prayers"}", style = MaterialTheme.typography.labelSmall, color = secondary)
        }
    }
}

@Composable
private fun BrowseAllCard(nusach: Nusach, onClick: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth(), padding = 16.dp, cornerRadius = 22.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.LocalLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Text("Browse the whole siddur", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(nusach.hebrewName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Footer() {
    Text(
        "Texts from Sefaria · Translation based on the Metsudah Siddur",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

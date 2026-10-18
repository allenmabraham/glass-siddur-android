package com.rowshr.siddur.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.rowshr.siddur.clock.JewishClock
import com.rowshr.siddur.fold.LocalFoldInfo
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.settings.BilingualLayout
import com.rowshr.siddur.settings.LanguageMode
import com.rowshr.siddur.settings.ReaderTheme
import com.rowshr.siddur.ui.LocalAppContainer
import com.rowshr.siddur.ui.browse.InlineTopBar
import com.rowshr.siddur.ui.components.Backdrop
import com.rowshr.siddur.ui.components.BackdropStyle
import com.rowshr.siddur.ui.components.SurfaceCard
import com.rowshr.siddur.ui.components.readableWidth
import com.rowshr.siddur.ui.reader.ToggleRow
import com.rowshr.siddur.ui.theme.Brand
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(nav: NavHostController) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val coordinate by container.location.coordinate.collectAsStateWithLifecycle()
    val fold = LocalFoldInfo.current
    val uriHandler = LocalUriHandler.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) container.location.refresh() }
    val counts = remember { Nusach.entries.associateWith { container.bundled.count(it) } }

    Box(Modifier.fillMaxSize()) {
        Backdrop(BackdropStyle.HOME)
        Column(Modifier.fillMaxSize().padding(top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())) {
            InlineTopBar("Settings", null, onBack = { nav.popBackStack() })
            val bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = bottom + 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Group("Nusach") {
                    Nusach.entries.forEachIndexed { i, n ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth().clickable { container.settings.update { it.copy(nusach = n) } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${n.displayName} · ${n.hebrewName}", style = MaterialTheme.typography.bodyLarge)
                                Text(n.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (settings.nusach == n) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Brand)
                        }
                    }
                }

                Group(
                    "Reading",
                    footer = if (fold.isOpen || fold.feature != null) "Closed, the siddur shows the Hebrew alone, as on paper. Opened, the two pages have room for the translation beside it. Turn the separate choice off to read the same way on both screens."
                    else "Hebrew alone by default, as on paper. Add the English here or with the language switch on the Today screen. A foldable, once opened, keeps its own choice.",
                ) {
                    ChoiceRow("Language when closed", LanguageMode.entries, settings.languageMode, { it.label }) { m -> container.settings.update { it.copy(languageMode = m) } }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ToggleRow("Choose separately when opened", settings.separateOpenLanguage) { v -> container.settings.update { it.copy(separateOpenLanguage = v) } }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ChoiceRow("Language when opened", LanguageMode.entries, settings.openLanguageMode, { it.label }, enabled = settings.separateOpenLanguage) { m -> container.settings.update { it.copy(openLanguageMode = m) } }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ChoiceRow("Bilingual layout", BilingualLayout.entries, settings.bilingualLayout, { it.label }) { l -> container.settings.update { it.copy(bilingualLayout = l) } }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ChoiceRow("Appearance", ReaderTheme.entries, settings.theme, { it.label }) { t -> container.settings.update { it.copy(theme = t) } }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ToggleRow("Keep screen awake while reading", settings.keepScreenAwake) { v -> container.settings.update { it.copy(keepScreenAwake = v) } }
                }

                Group("Prayer times", footer = "Without location, the app assumes sunrise at 6:00 and sunset at 18:30 when choosing the service to suggest. Location never leaves the device.") {
                    ToggleRow("Use location for sunrise & sunset", settings.useLocation) { on ->
                        container.settings.update { it.copy(useLocation = on) }
                        if (on) { if (container.location.isAuthorized) container.location.refresh() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                    }
                    if (settings.useLocation) {
                        val moment = remember(coordinate) { JewishClock(ZonedDateTime.now(), coordinate).moment() }
                        val fmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        if (moment.sunrise != null && moment.sunset != null) {
                            LabeledRow("Sunrise", moment.sunrise.format(fmt))
                            LabeledRow("Sunset", moment.sunset.format(fmt))
                        } else if (!container.location.isAuthorized) {
                            Text("Location access is off. Allow it to compute local times.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            Text("Waiting for location…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }

                Group("Offline", footer = "Every prayer of every nusach is built into the app. Nothing needs to be downloaded, and no connection is required.") {
                    LabeledRow("Prayers included", counts.values.sum().toString())
                    Nusach.entries.forEach { n -> HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); LabeledRow(n.displayName, (counts[n] ?: 0).toString()) }
                }

                Group("About") {
                    LabeledRow("Texts", "Sefaria", onClick = { uriHandler.openUri("https://www.sefaria.org") })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LabeledRow("Hebrew", "Metsudah Siddur (1981)")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LabeledRow("Translation", "Based on the Metsudah linear siddur, Avrohom Davis")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LabeledRow("Hebrew fonts", "Frank Ruhl Libre, David Libre, Noto Serif Hebrew (OFL)")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val context = LocalContext.current
                    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—" }
                    LabeledRow("Version", version)
                }
            }
        }
    }
}

@Composable
private fun Group(title: String, footer: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().readableWidth(640.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 16.dp, cornerRadius = 22.dp) { Column { content() } }
        if (footer != null) Text(footer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun LabeledRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = if (onClick != null) Brand else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun <T> ChoiceRow(label: String, options: List<T>, selected: T, text: (T) -> String, enabled: Boolean = true, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { open = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text(selected) + "  ⌄", style = MaterialTheme.typography.bodyLarge, color = if (enabled) Brand else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(text(option)) }, onClick = { onSelect(option); open = false }, trailingIcon = { if (option == selected) Icon(Icons.Filled.Check, contentDescription = null) })
            }
        }
    }
}

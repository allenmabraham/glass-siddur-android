package com.rowshr.siddur.ui.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rowshr.siddur.fold.LocalFoldInfo
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.model.PrayerParagraph
import com.rowshr.siddur.settings.BilingualLayout
import com.rowshr.siddur.settings.FontCatalog
import com.rowshr.siddur.settings.FontChoice
import com.rowshr.siddur.settings.LanguageMode
import com.rowshr.siddur.settings.ReaderTheme
import com.rowshr.siddur.settings.ReadingSettings
import com.rowshr.siddur.ui.LocalAppContainer
import com.rowshr.siddur.ui.components.GlassChip
import com.rowshr.siddur.ui.components.LanguageModePicker
import com.rowshr.siddur.ui.components.SurfaceCard
import com.rowshr.siddur.ui.home.BrandButton
import com.rowshr.siddur.ui.theme.Brand

private const val PREVIEW_HEBREW = "<b>מוֹדֶה אֲנִי</b> לְפָנֶֽיךָ מֶֽלֶךְ חַי וְקַיָּם, שֶׁהֶחֱזַֽרְתָּ בִּי נִשְׁמָתִי בְּחֶמְלָה, רַבָּה אֱמוּנָתֶֽךָ."
private const val PREVIEW_ENGLISH = "<b>I give thanks</b> before You, living and eternal King, for You have returned my soul within me with compassion; abundant is Your faithfulness."

/** Text size, fonts, spacing, layout and nikud controls with a live preview. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypographySheet(onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val fold = LocalFoldInfo.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { container.settings.resetTypography() }) { Text("Reset") }
                Spacer(Modifier.weight(1f))
                Text("Text", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                BrandButton("Done") { onDismiss() }
            }
            SurfaceCard(Modifier.fillMaxWidth(), padding = 18.dp) {
                ParagraphRow(
                    paragraph = PrayerParagraph(0, PREVIEW_HEBREW, PREVIEW_ENGLISH), nusach = Nusach.ASHKENAZ, settings = settings,
                    mode = settings.languageMode(fold.isOpen), availableWidth = 340.dp, spread = null, selected = false, onTap = {},
                )
            }
            SizeControls(settings)
            FontPicker("Hebrew typeface", "אבג", FontCatalog.hebrew, settings.hebrewFontId) { id -> container.settings.update { it.copy(hebrewFontId = id) } }
            FontPicker("English typeface", "Aa", FontCatalog.english, settings.englishFontId) { id -> container.settings.update { it.copy(englishFontId = id) } }
            LayoutControls(settings)
            HebrewControls(settings)
        }
    }
}

@Composable
private fun SizeControls(settings: ReadingSettings) {
    val container = LocalAppContainer.current
    SurfaceCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassChip(selected = false, onClick = { container.settings.decreaseFont() }) { Text("A", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Serif) }
                Slider(
                    value = settings.fontScale.toFloat(), onValueChange = { container.settings.setFontScale(it.toDouble()) },
                    valueRange = ReadingSettings.fontScaleRange.start.toFloat()..ReadingSettings.fontScaleRange.endInclusive.toFloat(),
                    modifier = Modifier.weight(1f),
                )
                GlassChip(selected = false, onClick = { container.settings.increaseFont() }) { Text("A", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Serif) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Line spacing", style = MaterialTheme.typography.bodyMedium)
                Slider(value = settings.lineSpacing.toFloat(), onValueChange = { v -> container.settings.update { it.copy(lineSpacing = v.toDouble()) } }, valueRange = 0.5f..2f, modifier = Modifier.weight(1f))
            }
            Text(
                "Tips: pinch the page to resize text. Tap any paragraph to highlight it with its translation; in Hebrew-only mode a tap reveals the English underneath.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FontPicker(title: String, sample: String, choices: List<FontChoice>, selectedId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            choices.forEach { choice ->
                val selected = choice.id == selectedId
                GlassChip(selected = selected, onClick = { onSelect(choice.id) }, shape = RoundedCornerShape(18.dp), modifier = Modifier.width(112.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        Text(sample, fontFamily = choice.family, fontSize = 26.sp, color = color, modifier = Modifier.height(34.dp))
                        Text(choice.displayName, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
                        choice.note?.let { Text(it, fontSize = 9.sp, color = color.copy(alpha = 0.7f), maxLines = 1) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayoutControls(settings: ReadingSettings) {
    val container = LocalAppContainer.current
    val fold = LocalFoldInfo.current
    SurfaceCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LanguageModePicker(mode = settings.languageMode(fold.isOpen), onSelect = { container.settings.setLanguageMode(it, fold.isOpen) })
            if (settings.languageMode(fold.isOpen) == LanguageMode.BILINGUAL) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    BilingualLayout.entries.forEachIndexed { i, layout ->
                        SegmentedButton(selected = settings.bilingualLayout == layout, onClick = { container.settings.update { it.copy(bilingualLayout = layout) } }, shape = SegmentedButtonDefaults.itemShape(i, BilingualLayout.entries.size)) { Text(layout.label) }
                    }
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ReaderTheme.entries.forEachIndexed { i, theme ->
                    SegmentedButton(selected = settings.theme == theme, onClick = { container.settings.update { it.copy(theme = theme) } }, shape = SegmentedButtonDefaults.itemShape(i, ReaderTheme.entries.size)) { Text(theme.label) }
                }
            }
        }
    }
}

@Composable
private fun HebrewControls(settings: ReadingSettings) {
    val container = LocalAppContainer.current
    SurfaceCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToggleRow("Vowels (nikud)", settings.showVowels) { v -> container.settings.update { it.copy(showVowels = v) } }
            ToggleRow("Cantillation (ta'amim)", settings.showCantillation) { v -> container.settings.update { it.copy(showCantillation = v) } }
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Brand))
    }
}

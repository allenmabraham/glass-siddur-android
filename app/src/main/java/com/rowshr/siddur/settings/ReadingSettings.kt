package com.rowshr.siddur.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rowshr.siddur.model.Nusach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

enum class LanguageMode(val label: String) {
    HEBREW("עברית"), BILINGUAL("Both"), ENGLISH("English");

    val accessibilityLabel: String
        get() = when (this) { HEBREW -> "Hebrew only"; BILINGUAL -> "Hebrew and English"; ENGLISH -> "English only" }
}

enum class BilingualLayout(val label: String) { AUTOMATIC("Auto"), SIDE_BY_SIDE("Side by side"), STACKED("Stacked") }

enum class ReaderTheme(val label: String) { SYSTEM("System"), LIGHT("Light"), SEPIA("Sepia"), DARK("Dark") }

/** Everything about how prayer text is displayed. Persisted in DataStore. */
data class ReadingSettings(
    /** Language on a single screen: every ordinary phone, and a foldable's cover display. Hebrew alone by default, as in a printed siddur. */
    val languageMode: LanguageMode = LanguageMode.HEBREW,
    /** Language once a foldable is opened, when [separateOpenLanguage] is on: the translation joins the Hebrew by default. */
    val openLanguageMode: LanguageMode = LanguageMode.BILINGUAL,
    /** Whether the open display keeps its own language choice. */
    val separateOpenLanguage: Boolean = true,
    val bilingualLayout: BilingualLayout = BilingualLayout.AUTOMATIC,
    val theme: ReaderTheme = ReaderTheme.SYSTEM,
    val nusach: Nusach = Nusach.ASHKENAZ,
    /** Multiplier on the base type size. 1.0 ≈ 21sp Hebrew / 17sp English. */
    val fontScale: Double = 1.0,
    val lineSpacing: Double = 1.0,
    val hebrewFontId: String = FontCatalog.hebrew[0].id,
    val englishFontId: String = FontCatalog.english[0].id,
    val showVowels: Boolean = true,
    val showCantillation: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val useLocation: Boolean = false,
) {
    val hebrewFont: FontChoice get() = FontCatalog.hebrew(hebrewFontId)
    val englishFont: FontChoice get() = FontCatalog.english(englishFontId)

    val hebrewPointSize: Float get() = (21 * fontScale).toFloat()
    val englishPointSize: Float get() = (17 * fontScale).toFloat()

    /** Hebrew line spacing as a multiple of the font size (extra leading). */
    val hebrewLineHeightFactor: Float get() = (1.0 + 0.28 * lineSpacing).toFloat()
    val englishLineHeightFactor: Float get() = (1.0 + 0.30 * lineSpacing).toFloat()

    /**
     * Side-by-side columns need roughly eight Hebrew glyphs per line to read well;
     * below that (large type on a phone) the pair stacks, Hebrew above English.
     */
    fun resolvedLayout(widthDp: Float): BilingualLayout = when (bilingualLayout) {
        BilingualLayout.SIDE_BY_SIDE, BilingualLayout.STACKED -> bilingualLayout
        BilingualLayout.AUTOMATIC -> {
            val column = (widthDp - 18) / 2
            if (column >= hebrewPointSize * 8) BilingualLayout.SIDE_BY_SIDE else BilingualLayout.STACKED
        }
    }

    /** The language that applies to the screen in view: [open] is true on an opened foldable. */
    fun languageMode(open: Boolean): LanguageMode = if (open && separateOpenLanguage) openLanguageMode else languageMode

    companion object {
        val fontScaleRange = 0.6..2.6
    }
}

private val Context.readingStore: DataStore<Preferences> by preferencesDataStore(name = "reading")

/** Persists [ReadingSettings] and exposes them as state. */
class ReadingSettingsStore(context: Context) {
    private val store = context.applicationContext.readingStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object Keys {
        val languageMode = stringPreferencesKey("languageMode")
        val openLanguageMode = stringPreferencesKey("openLanguageMode")
        val separateOpenLanguage = booleanPreferencesKey("separateOpenLanguage")
        val bilingualLayout = stringPreferencesKey("bilingualLayout")
        val theme = stringPreferencesKey("theme")
        val nusach = stringPreferencesKey("nusach")
        val fontScale = doublePreferencesKey("fontScale")
        val lineSpacing = doublePreferencesKey("lineSpacing")
        val hebrewFontId = stringPreferencesKey("hebrewFontId")
        val englishFontId = stringPreferencesKey("englishFontId")
        val showVowels = booleanPreferencesKey("showVowels")
        val showCantillation = booleanPreferencesKey("showCantillation")
        val keepScreenAwake = booleanPreferencesKey("keepScreenAwake")
        val useLocation = booleanPreferencesKey("useLocation")
    }

    private fun Preferences.toSettings(): ReadingSettings {
        val d = ReadingSettings()
        return ReadingSettings(
            languageMode = this[Keys.languageMode]?.let { runCatching { LanguageMode.valueOf(it) }.getOrNull() } ?: d.languageMode,
            openLanguageMode = this[Keys.openLanguageMode]?.let { runCatching { LanguageMode.valueOf(it) }.getOrNull() } ?: d.openLanguageMode,
            separateOpenLanguage = this[Keys.separateOpenLanguage] ?: d.separateOpenLanguage,
            bilingualLayout = this[Keys.bilingualLayout]?.let { runCatching { BilingualLayout.valueOf(it) }.getOrNull() } ?: d.bilingualLayout,
            theme = this[Keys.theme]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() } ?: d.theme,
            nusach = Nusach.fromName(this[Keys.nusach]) ?: d.nusach,
            fontScale = (this[Keys.fontScale] ?: d.fontScale).coerceIn(ReadingSettings.fontScaleRange),
            lineSpacing = this[Keys.lineSpacing] ?: d.lineSpacing,
            hebrewFontId = this[Keys.hebrewFontId] ?: d.hebrewFontId,
            englishFontId = this[Keys.englishFontId] ?: d.englishFontId,
            showVowels = this[Keys.showVowels] ?: d.showVowels,
            showCantillation = this[Keys.showCantillation] ?: d.showCantillation,
            keepScreenAwake = this[Keys.keepScreenAwake] ?: d.keepScreenAwake,
            useLocation = this[Keys.useLocation] ?: d.useLocation,
        )
    }

    /** Settings are read once at startup (a few bytes) so the first frame is already right. */
    val state: StateFlow<ReadingSettings> = store.data.map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, runBlocking { store.data.first().toSettings() })

    fun update(transform: (ReadingSettings) -> ReadingSettings) {
        val next = transform(state.value)
        scope.launch {
            store.edit { p ->
                p[Keys.languageMode] = next.languageMode.name
                p[Keys.openLanguageMode] = next.openLanguageMode.name
                p[Keys.separateOpenLanguage] = next.separateOpenLanguage
                p[Keys.bilingualLayout] = next.bilingualLayout.name
                p[Keys.theme] = next.theme.name
                p[Keys.nusach] = next.nusach.name
                p[Keys.fontScale] = next.fontScale.coerceIn(ReadingSettings.fontScaleRange)
                p[Keys.lineSpacing] = next.lineSpacing
                p[Keys.hebrewFontId] = next.hebrewFontId
                p[Keys.englishFontId] = next.englishFontId
                p[Keys.showVowels] = next.showVowels
                p[Keys.showCantillation] = next.showCantillation
                p[Keys.keepScreenAwake] = next.keepScreenAwake
                p[Keys.useLocation] = next.useLocation
            }
        }
    }

    fun setLanguageMode(mode: LanguageMode, open: Boolean) = update {
        if (open && it.separateOpenLanguage) it.copy(openLanguageMode = mode) else it.copy(languageMode = mode)
    }

    fun setFontScale(value: Double) = update { it.copy(fontScale = value.coerceIn(ReadingSettings.fontScaleRange)) }
    fun increaseFont() = setFontScale(state.value.fontScale + 0.1)
    fun decreaseFont() = setFontScale(state.value.fontScale - 0.1)
    fun resetTypography() = update { it.copy(fontScale = 1.0, lineSpacing = 1.0, hebrewFontId = FontCatalog.hebrew[0].id, englishFontId = FontCatalog.english[0].id) }
}

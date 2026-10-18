package com.rowshr.siddur.model

import kotlinx.serialization.Serializable

/**
 * A single paragraph with its Hebrew and English (either may be empty when a
 * version is missing that segment). Text is Sefaria HTML; see `HtmlText`.
 */
@Serializable
data class PrayerParagraph(
    val index: Int,
    val hebrewHTML: String,
    val englishHTML: String,
) {
    val hasHebrew: Boolean get() = hebrewHTML.isNotEmpty()
    val hasEnglish: Boolean get() = englishHTML.isNotEmpty()
}

/** The text of one leaf (one prayer) in a siddur. */
@Serializable
data class PrayerLeafText(
    val ref: String,
    val heRef: String,
    val title: String,
    val heTitle: String,
    val hebrewVersionTitle: String? = null,
    val englishVersionTitle: String? = null,
    val paragraphs: List<PrayerParagraph>,
)

/** A prayer as displayed in the reader: the TOC node plus its loaded text. */
data class PrayerBlock(val node: SiddurNode, val state: State) {
    sealed interface State {
        data object Loading : State
        data class Loaded(val text: PrayerLeafText) : State
        data class Failed(val message: String) : State
    }

    val id: String get() = node.id
}

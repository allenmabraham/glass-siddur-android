package com.rowshr.siddur.settings

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rowshr.siddur.R

/** A selectable typeface: a bundled Open Font License face or an Android system family. */
data class FontChoice(
    val id: String,
    val displayName: String,
    val family: FontFamily,
    /** Short note shown in the picker ("Traditional siddur face"). */
    val note: String? = null,
)

object FontCatalog {
    private val frankRuhl = FontFamily(
        Font(R.font.frankruhllibre, FontWeight.Normal),
        Font(R.font.frankruhllibre, FontWeight.Bold),
    )
    private val david = FontFamily(
        Font(R.font.davidlibre_regular, FontWeight.Normal),
        Font(R.font.davidlibre_medium, FontWeight.Medium),
        Font(R.font.davidlibre_bold, FontWeight.Bold),
    )
    private val notoSerifHebrew = FontFamily(
        Font(R.font.notoserifhebrew, FontWeight.Normal),
        Font(R.font.notoserifhebrew, FontWeight.Bold),
    )

    /** Bundled siddur faces first, then Android families with Hebrew glyphs. */
    val hebrew: List<FontChoice> = listOf(
        FontChoice("he.frank", "Frank Ruhl", frankRuhl, "Traditional siddur face"),
        FontChoice("he.david", "David", david, "Classic Israeli serif"),
        FontChoice("he.noto", "Noto Serif", notoSerifHebrew),
        FontChoice("he.system", "Noto Sans Hebrew", FontFamily.SansSerif, "Modern"),
        FontChoice("he.serif", "System serif", FontFamily.Serif),
        FontChoice("he.mono", "Monospace", FontFamily.Monospace),
    )

    val english: List<FontChoice> = listOf(
        FontChoice("en.serif", "Noto Serif", FontFamily.Serif, "Book serif"),
        FontChoice("en.system", "Roboto", FontFamily.SansSerif, "Modern"),
        FontChoice("en.frank", "Frank Ruhl", frankRuhl, "Matches the Hebrew"),
        FontChoice("en.david", "David", david),
        FontChoice("en.cursive", "Cursive", FontFamily.Cursive),
        FontChoice("en.mono", "Monospace", FontFamily.Monospace),
    )

    fun hebrew(id: String): FontChoice = hebrew.firstOrNull { it.id == id } ?: hebrew[0]
    fun english(id: String): FontChoice = english.firstOrNull { it.id == id } ?: english[0]
}

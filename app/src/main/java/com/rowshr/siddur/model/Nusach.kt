package com.rowshr.siddur.model

import kotlinx.serialization.Serializable

/** The four complete siddurim available on Sefaria. */
@Serializable
enum class Nusach(val resourceName: String, val sefariaTitle: String) {
    ASHKENAZ("ashkenaz", "Siddur Ashkenaz"),
    SEFARD("sefard", "Siddur Sefard"),
    ARI("ari", "Siddur Tehillat Hashem"),
    EDOT_HAMIZRACH("edotHaMizrach", "Siddur Edot HaMizrach");

    val displayName: String
        get() = when (this) {
            ASHKENAZ -> "Ashkenaz"
            SEFARD -> "Sefard"
            ARI -> "Ari"
            EDOT_HAMIZRACH -> "Edot HaMizrach"
        }

    val hebrewName: String
        get() = when (this) {
            ASHKENAZ -> "אשכנז"
            SEFARD -> "ספרד"
            ARI -> "אר״י"
            EDOT_HAMIZRACH -> "עדות המזרח"
        }

    val subtitle: String
        get() = when (this) {
            ASHKENAZ -> "Central & Western European rite"
            SEFARD -> "Chassidic rite"
            ARI -> "Chabad rite · Hebrew only · weekday"
            EDOT_HAMIZRACH -> "Sephardic & Mizrachi rite"
        }

    /** Sefaria has no English translation for the Chabad siddur. */
    val hasEnglish: Boolean get() = this != ARI

    /** The Chabad siddur on Sefaria covers weekdays only. */
    val hasShabbat: Boolean get() = this != ARI

    companion object {
        fun fromName(name: String?): Nusach? = entries.firstOrNull { it.name == name || it.resourceName == name }
    }
}

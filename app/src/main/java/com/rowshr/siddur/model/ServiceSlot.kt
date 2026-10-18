package com.rowshr.siddur.model

/**
 * The service that is most likely wanted right now. The Today screen turns the
 * current moment into one of these and resolves it to a node per nusach.
 */
enum class ServiceSlot {
    SHACHARIT, MINCHA, MAARIV,
    KABBALAT_SHABBAT, SHABBAT_MAARIV, SHABBAT_SHACHARIT, SHABBAT_MUSAF, SHABBAT_MINCHA, HAVDALAH,
    BEDTIME_SHEMA;

    val title: String
        get() = when (this) {
            SHACHARIT -> "Shacharit"
            MINCHA -> "Mincha"
            MAARIV -> "Maariv"
            KABBALAT_SHABBAT -> "Kabbalat Shabbat"
            SHABBAT_MAARIV -> "Shabbat Maariv"
            SHABBAT_SHACHARIT -> "Shabbat Shacharit"
            SHABBAT_MUSAF -> "Musaf"
            SHABBAT_MINCHA -> "Shabbat Mincha"
            HAVDALAH -> "Havdalah"
            BEDTIME_SHEMA -> "Bedtime Shema"
        }

    val hebrewTitle: String
        get() = when (this) {
            SHACHARIT -> "שחרית"
            MINCHA -> "מנחה"
            MAARIV -> "מעריב"
            KABBALAT_SHABBAT -> "קבלת שבת"
            SHABBAT_MAARIV -> "ערבית לשבת"
            SHABBAT_SHACHARIT -> "שחרית לשבת"
            SHABBAT_MUSAF -> "מוסף"
            SHABBAT_MINCHA -> "מנחה לשבת"
            HAVDALAH -> "הבדלה"
            BEDTIME_SHEMA -> "קריאת שמע על המיטה"
        }

    /** Path of English titles from the siddur root, per nusach. Titles are matched loosely. */
    fun path(nusach: Nusach): List<String> = when (nusach) {
        Nusach.ASHKENAZ -> when (this) {
            SHACHARIT -> listOf("Weekday", "Shacharit")
            MINCHA -> listOf("Weekday", "Minchah")
            MAARIV -> listOf("Weekday", "Maariv")
            KABBALAT_SHABBAT -> listOf("Shabbat", "Kabbalat Shabbat")
            SHABBAT_MAARIV -> listOf("Shabbat", "Maariv")
            SHABBAT_SHACHARIT -> listOf("Shabbat", "Shacharit")
            SHABBAT_MUSAF -> listOf("Shabbat", "Musaf LeShabbat")
            SHABBAT_MINCHA -> listOf("Shabbat", "Minchah")
            HAVDALAH -> listOf("Shabbat", "Havdalah")
            BEDTIME_SHEMA -> listOf("Weekday", "Maariv", "Keri'at Shema al Hamita")
        }
        Nusach.SEFARD -> when (this) {
            SHACHARIT -> listOf("Weekday Shacharit")
            MINCHA -> listOf("Weekday Mincha")
            MAARIV -> listOf("Weekday Maariv")
            KABBALAT_SHABBAT -> listOf("Kabbalat Shabbat")
            SHABBAT_MAARIV -> listOf("Shabbat Eve Maariv")
            SHABBAT_SHACHARIT -> listOf("Shabbat Morning Services")
            SHABBAT_MUSAF -> listOf("Musaf")
            SHABBAT_MINCHA -> listOf("Shabbat Mincha")
            HAVDALAH -> listOf("Motzaei Shabbat")
            BEDTIME_SHEMA -> listOf("Bedtime Shema")
        }
        Nusach.ARI -> when (this) {
            SHACHARIT, SHABBAT_SHACHARIT, SHABBAT_MUSAF -> listOf("Shacharit")
            MINCHA, SHABBAT_MINCHA -> listOf("Mincha")
            MAARIV, SHABBAT_MAARIV, KABBALAT_SHABBAT, HAVDALAH -> listOf("Maariv")
            BEDTIME_SHEMA -> listOf("Bedtime Shema")
        }
        Nusach.EDOT_HAMIZRACH -> when (this) {
            SHACHARIT -> listOf("Weekday Shacharit")
            MINCHA -> listOf("Weekday Mincha")
            MAARIV -> listOf("Weekday Arvit")
            KABBALAT_SHABBAT -> listOf("Kabbalat Shabbat")
            SHABBAT_MAARIV -> listOf("Shabbat Arvit")
            SHABBAT_SHACHARIT -> listOf("Shabbat Shacharit")
            SHABBAT_MUSAF -> listOf("Shabbat Mussaf")
            SHABBAT_MINCHA -> listOf("Shabbat Mincha")
            HAVDALAH -> listOf("Havdalah")
            BEDTIME_SHEMA -> listOf("Bedtime Shema")
        }
    }
}

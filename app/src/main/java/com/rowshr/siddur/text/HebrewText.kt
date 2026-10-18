package com.rowshr.siddur.text

object HebrewText {
    /** Hebrew vowel points (nikud). */
    private fun isNikud(c: Int) = c in 0x05B0..0x05BD || c == 0x05BF || c == 0x05C1 || c == 0x05C2 || c == 0x05C4 || c == 0x05C5 || c == 0x05C7

    /** Cantillation marks (ta'amim). */
    private fun isCantillation(c: Int) = c in 0x0591..0x05AF

    fun strip(text: String, vowels: Boolean, cantillation: Boolean): String {
        if (!vowels && !cantillation) return text
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val skip = (vowels && isNikud(cp)) || (cantillation && isCantillation(cp))
            if (!skip) out.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        return out.toString()
    }

    fun containsHebrew(text: String): Boolean = text.any { it.code in 0x0590..0x05FF }
}

package com.rowshr.siddur.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import com.rowshr.siddur.settings.ReadingSettings
import com.rowshr.siddur.text.HebrewText
import com.rowshr.siddur.text.HtmlText
import com.rowshr.siddur.text.StyledRun

/**
 * Renders [StyledRun]s as one [AnnotatedString] so a paragraph wraps as a unit
 * while opening words stay bold and instructions stay small and gray —
 * the conventions of a traditional printed siddur.
 */
object StyledText {
    fun hebrew(html: String, settings: ReadingSettings, secondary: Color): AnnotatedString {
        val runs = HtmlText.runs(html).map { it.copy(text = HebrewText.strip(it.text, vowels = !settings.showVowels, cantillation = !settings.showCantillation)) }
        return build(runs, settings.hebrewPointSize, secondary)
    }

    fun english(html: String, settings: ReadingSettings, secondary: Color): AnnotatedString =
        build(HtmlText.runs(html), settings.englishPointSize, secondary)

    /** True when a paragraph is entirely an instruction (all runs small): rendered as a rubric rather than prayer text. */
    fun isInstruction(html: String): Boolean {
        val runs = HtmlText.runs(html)
        return runs.isNotEmpty() && runs.all { it.small || it.text.all { c -> c.isWhitespace() } }
    }

    private fun build(runs: List<StyledRun>, size: Float, secondary: Color): AnnotatedString = buildAnnotatedString {
        for (run in runs) {
            val style = SpanStyle(
                fontWeight = if (run.bold) FontWeight.Bold else null,
                fontStyle = if (run.italic) FontStyle.Italic else null,
                fontSize = when {
                    run.small -> (size * 0.78f).sp
                    run.superscript -> (size * 0.6f).sp
                    else -> androidx.compose.ui.unit.TextUnit.Unspecified
                },
                color = if (run.small) secondary else Color.Unspecified,
                baselineShift = if (run.superscript) BaselineShift.Superscript else null,
            )
            pushStyle(style)
            append(run.text)
            pop()
        }
    }
}

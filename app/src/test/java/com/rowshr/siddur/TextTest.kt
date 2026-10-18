package com.rowshr.siddur

import com.rowshr.siddur.model.SiddurNode
import com.rowshr.siddur.text.HebrewText
import com.rowshr.siddur.text.HtmlText
import com.rowshr.siddur.text.StyledRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTextTest {
    @Test
    fun boldAndPlainRuns() {
        val runs = HtmlText.runs("<b>מוֹדֶה אֲנִי</b> לְפָנֶֽיךָ")
        assertEquals(listOf(StyledRun("מוֹדֶה אֲנִי", bold = true), StyledRun(" לְפָנֶֽיךָ")), runs)
    }

    @Test
    fun footnotesAreDropped() {
        val html = "Blessed<sup class=\"footnote-marker\">1</sup><i class=\"footnote\">a note</i> are You"
        assertEquals("Blessed are You", HtmlText.plainText(html))
    }

    @Test
    fun entitiesAndBreaks() {
        assertEquals("a b\nc & d", HtmlText.plainText("a&nbsp;b<br>c &amp; d"))
        assertEquals("é", HtmlText.plainText("&#233;"))
        assertEquals("é", HtmlText.plainText("&#xE9;"))
    }

    @Test
    fun smallRunsAreInstructions() {
        val runs = HtmlText.runs("<small>Stand here.</small>")
        assertTrue(runs.all { it.small })
        assertFalse(HtmlText.runs("<b>Word</b> text").all { it.small })
    }

    @Test
    fun unclosedTagDoesNotCrash() {
        assertEquals("abc", HtmlText.plainText("abc<b"))
    }
}

class HebrewTextTest {
    @Test
    fun stripsVowelsAndCantillation() {
        val pointed = "בְּרֵאשִׁ֖ית"
        assertEquals("בראשית", HebrewText.strip(pointed, vowels = true, cantillation = true))
        assertEquals(pointed, HebrewText.strip(pointed, vowels = false, cantillation = false))
        assertTrue(HebrewText.strip(pointed, vowels = false, cantillation = true).contains('ְ'))
        assertFalse(HebrewText.strip(pointed, vowels = false, cantillation = true).contains('֖'))
    }

    @Test
    fun detectsHebrew() {
        assertTrue(HebrewText.containsHebrew("abc שלום"))
        assertFalse(HebrewText.containsHebrew("abc"))
    }
}

class SiddurNodeTest {
    private val root = SiddurNode(
        id = "root", title = "Siddur", heTitle = "סידור", ref = "Siddur", heRef = "",
        children = listOf(
            SiddurNode("0", "Weekday", "חול", "Siddur, Weekday", "", children = listOf(
                SiddurNode("0.0", "Shacharit", "שחרית", "Siddur, Weekday, Shacharit", "", children = listOf(
                    SiddurNode("0.0.0", "Modeh Ani", "מודה אני", "Siddur, Weekday, Shacharit, Modeh Ani", "", depth = 1),
                    SiddurNode("0.0.1", "Keri'at Shema", "קריאת שמע", "Siddur, Weekday, Shacharit, Keri'at Shema", "", depth = 1),
                )),
            )),
        ),
    )

    @Test
    fun leavesAndCounts() {
        assertEquals(2, root.leafCount)
        assertEquals(listOf("0.0.0", "0.0.1"), root.leaves.map { it.id })
        assertTrue(root.node("0.0.1")!!.isLeaf)
        assertNull(root.node("9"))
    }

    @Test
    fun descendantMatchesLoosely() {
        assertEquals("0.0.1", root.descendant(listOf("weekday", "SHACHARIT", "Keriat Shema"))?.id)
        assertNull(root.descendant(listOf("Shabbat")))
    }

    @Test
    fun trail() {
        assertEquals(listOf("0", "0.0", "0.0.1"), root.trail("0.0.1")?.map { it.id })
    }
}

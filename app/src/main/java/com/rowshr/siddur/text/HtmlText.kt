package com.rowshr.siddur.text

import java.util.Collections

/** A run of text with inline styling derived from Sefaria's light HTML. */
data class StyledRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val small: Boolean = false,
    val superscript: Boolean = false,
)

/**
 * Converts Sefaria HTML (`<b>`, `<i>`, `<small>`, `<br>`, `<span>`, `<sup>` …)
 * into styled runs. Footnotes are dropped; unknown tags are ignored.
 */
object HtmlText {
    private val cache: MutableMap<String, List<StyledRun>> =
        Collections.synchronizedMap(object : LinkedHashMap<String, List<StyledRun>>(512, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<StyledRun>>?) = size > 4000
        })

    fun runs(html: String): List<StyledRun> = cache.getOrPut(html) { parse(html) }

    /** Plain text with all tags removed (used for search and accessibility). */
    fun plainText(html: String): String = runs(html).joinToString("") { it.text }

    private data class Style(
        val bold: Boolean = false, val italic: Boolean = false,
        val small: Boolean = false, val superscript: Boolean = false,
    )

    private fun parse(html: String): List<StyledRun> {
        val runs = ArrayList<StyledRun>()
        val buffer = StringBuilder()
        val stack = ArrayList<Pair<String, Style>>()
        var style = Style()
        var skipDepth = 0 // > 0 while inside a footnote

        fun flush() {
            if (buffer.isEmpty()) return
            val last = runs.lastOrNull()
            if (last != null && last.bold == style.bold && last.italic == style.italic &&
                last.small == style.small && last.superscript == style.superscript) {
                runs[runs.lastIndex] = last.copy(text = last.text + buffer)
            } else {
                runs.add(StyledRun(buffer.toString(), style.bold, style.italic, style.small, style.superscript))
            }
            buffer.setLength(0)
        }

        fun handleTag(raw: String) {
            var body = raw.trim()
            val isClosing = body.startsWith("/")
            if (isClosing) body = body.substring(1)
            if (body.endsWith("/")) body = body.dropLast(1)
            val name = body.split(Regex("\\s+")).firstOrNull()?.lowercase() ?: return
            val attrs = body.lowercase()
            val isFootnote = attrs.contains("footnote")

            if (isClosing) {
                val idx = stack.indexOfLast { it.first == name }
                if (idx >= 0) {
                    if ((name == "span" || name == "i" || name == "sup") && skipDepth > 0) skipDepth = maxOf(0, skipDepth - 1)
                    flush()
                    style = stack[idx].second
                    while (stack.size > idx) stack.removeAt(stack.lastIndex)
                }
                return
            }
            when (name) {
                "br" -> if (skipDepth == 0) buffer.append('\n')
                "p", "div" -> if (skipDepth == 0 && buffer.isNotEmpty() && !buffer.endsWith("\n")) buffer.append('\n')
                "b", "strong" -> { flush(); stack.add(name to style); style = style.copy(bold = true) }
                "i", "em" -> { flush(); stack.add(name to style); if (isFootnote) skipDepth++ else style = style.copy(italic = true) }
                "small" -> { flush(); stack.add(name to style); style = style.copy(small = true) }
                "big" -> { flush(); stack.add(name to style); style = style.copy(bold = true) }
                "sup" -> { flush(); stack.add(name to style); if (isFootnote) skipDepth++ else style = style.copy(superscript = true) }
                "span" -> {
                    flush(); stack.add(name to style)
                    if (isFootnote) skipDepth++
                    if (attrs.contains("mam-spi-samekh") || attrs.contains("mam-spi-pe")) style = style.copy(small = true)
                }
                else -> {}
            }
        }

        var i = 0
        val n = html.length
        while (i < n) {
            val ch = html[i]
            if (ch == '<') {
                val close = html.indexOf('>', i)
                if (close < 0) break
                handleTag(html.substring(i + 1, close))
                i = close + 1
                continue
            }
            if (ch == '&') {
                val semi = html.indexOf(';', i)
                if (semi in (i + 1)..(i + 10)) {
                    val decoded = decodeEntity(html.substring(i + 1, semi))
                    if (decoded != null) {
                        if (skipDepth == 0) buffer.append(decoded)
                        i = semi + 1
                        continue
                    }
                }
            }
            if (skipDepth == 0) buffer.append(ch)
            i++
        }
        flush()
        return runs.filter { it.text.isNotEmpty() }
    }

    private fun decodeEntity(entity: String): String? = when (entity) {
        "nbsp" -> " "
        "amp" -> "&"
        "lt" -> "<"
        "gt" -> ">"
        "quot" -> "\""
        "apos", "#39" -> "'"
        "thinsp" -> " "
        "ndash" -> "–"
        "mdash" -> "—"
        "hellip" -> "…"
        else -> when {
            entity.startsWith("#x") -> entity.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
            entity.startsWith("#") -> entity.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) }
            else -> null
        }
    }
}

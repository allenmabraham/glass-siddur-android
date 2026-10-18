package com.rowshr.siddur.model

import kotlinx.serialization.Serializable

/**
 * One node of a siddur's table of contents. Mirrors the compact JSON produced
 * from Sefaria's index (`assets/indices/<nusach>.json`), shared with the iOS app.
 */
@Serializable
data class SiddurNode(
    /** Positional id ("1.4.6"). Unique within one siddur even when titles repeat. */
    val id: String,
    val title: String,
    val heTitle: String,
    /** Full Sefaria ref, e.g. "Siddur Ashkenaz, Weekday, Shacharit, Amidah, Patriarchs". */
    val ref: String,
    val heRef: String,
    val children: List<SiddurNode>? = null,
    /** Text depth for leaves (1 = list of paragraphs, 2 = chapters of paragraphs). */
    val depth: Int? = null,
) {
    val isLeaf: Boolean get() = children == null

    /** Every leaf under this node in reading order (or the node itself if it is a leaf). */
    val leaves: List<SiddurNode>
        get() = children?.flatMap { it.leaves } ?: listOf(this)

    val leafCount: Int get() = leaves.size

    /** Depth-first search for a node by id. */
    fun node(withId: String): SiddurNode? {
        if (id == withId) return this
        children?.forEach { child -> child.node(withId)?.let { return it } }
        return null
    }

    /**
     * Finds a descendant by walking a path of English titles (case-insensitive,
     * whitespace-insensitive so minor Sefaria title edits don't break lookups).
     */
    fun descendant(titled: List<String>): SiddurNode? {
        var current = this
        for (step in titled) {
            val wanted = normalize(step)
            current = current.children?.firstOrNull { normalize(it.title) == wanted } ?: return null
        }
        return current
    }

    /** Ancestor chain (excluding root) that leads to the node with the given id. */
    fun trail(to: String): List<SiddurNode>? {
        if (id == to) return emptyList()
        children?.forEach { child -> child.trail(to)?.let { return listOf(child) + it } }
        return null
    }

    companion object {
        fun normalize(s: String): String =
            s.lowercase().filter { !it.isWhitespace() && it != '\'' && it != '’' }
    }
}

package com.rowshr.siddur.data

import android.content.Context
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.model.ServiceSlot
import com.rowshr.siddur.model.SiddurNode
import kotlinx.serialization.json.Json

/** Loads the bundled table of contents for each nusach. */
class SiddurLibrary(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = HashMap<Nusach, SiddurNode>()

    @Synchronized
    fun root(nusach: Nusach): SiddurNode = cache.getOrPut(nusach) {
        runCatching {
            context.assets.open("indices/${nusach.resourceName}.json").bufferedReader().use { json.decodeFromString(SiddurNode.serializer(), it.readText()) }
        }.getOrElse {
            SiddurNode(id = "root", title = nusach.sefariaTitle, heTitle = "", ref = nusach.sefariaTitle, heRef = "", children = emptyList())
        }
    }

    fun node(slot: ServiceSlot, nusach: Nusach): SiddurNode? = root(nusach).descendant(slot.path(nusach))
}

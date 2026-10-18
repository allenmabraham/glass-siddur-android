package com.rowshr.siddur.data

import android.content.Context
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.model.PrayerLeafText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/**
 * Every prayer of every siddur ships inside the app (`assets/texts/<nusach>-texts.json`,
 * the same files the iOS app bundles), so the siddur is fully usable with no connection
 * from the moment it is installed. Sefaria is only consulted for a ref missing from the bundle.
 */
class BundledTexts(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    /** At most two decoded bundles stay in memory (a few MB each); the rest are re-read on demand. */
    private val cache = object : LinkedHashMap<Nusach, Map<String, PrayerLeafText>>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Nusach, Map<String, PrayerLeafText>>?) = size > 2
    }
    private val mutex = Mutex()
    private val counts: Map<Nusach, Int> by lazy {
        runCatching {
            val raw = context.assets.open("texts/counts.json").bufferedReader().use { it.readText() }
            val decoded = json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), raw)
            Nusach.entries.associateWith { decoded[it.resourceName] ?: 0 }
        }.getOrDefault(emptyMap())
    }

    /** Which siddur a Sefaria ref belongs to, from its leading title. */
    fun nusach(ref: String): Nusach? =
        Nusach.entries.firstOrNull { ref == it.sefariaTitle || ref.startsWith(it.sefariaTitle + ",") }

    suspend fun leaf(ref: String): PrayerLeafText? {
        val nusach = nusach(ref) ?: return null
        return texts(nusach)[ref]
    }

    suspend fun contains(ref: String): Boolean = leaf(ref) != null

    /** Number of bundled prayers, from a tiny index written alongside the texts. */
    fun count(nusach: Nusach): Int = counts[nusach] ?: 0

    /** Decodes a bundle ahead of time so the first prayer opens without a pause. */
    suspend fun warm(nusach: Nusach) { texts(nusach) }

    /** Decodes a siddur's bundle once and keeps it in memory (a few MB each). */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    suspend fun texts(nusach: Nusach): Map<String, PrayerLeafText> = mutex.withLock {
        cache[nusach]?.let { return it }
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("texts/${nusach.resourceName}-texts.json").use { stream ->
                    json.decodeFromStream(MapSerializer(String.serializer(), PrayerLeafText.serializer()), stream)
                }
            }.getOrDefault(emptyMap())
        }
        cache[nusach] = decoded
        decoded
    }
}

package com.rowshr.siddur.data

import android.content.Context
import com.rowshr.siddur.model.PrayerLeafText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/**
 * Serves prayer text: bundled first (see [BundledTexts]), then the on-device
 * cache, then Sefaria. Liturgical text never changes, so nothing expires.
 */
class TextRepository(context: Context, private val bundled: BundledTexts, private val client: SefariaClient = SefariaClient()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val directory = File(context.cacheDir, "SefariaTexts").apply { mkdirs() }
    private val memory = HashMap<String, PrayerLeafText>()
    private val inFlight = HashMap<String, Deferred<PrayerLeafText>>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun text(ref: String): PrayerLeafText {
        mutex.withLock { memory[ref] }?.let { return it }
        bundled.leaf(ref)?.let { hit -> mutex.withLock { memory[ref] = hit }; return hit }
        readFromDisk(ref)?.let { hit -> mutex.withLock { memory[ref] = hit }; return hit }
        val task = mutex.withLock {
            inFlight.getOrPut(ref) {
                scope.async {
                    try {
                        val result = client.fetch(ref)
                        writeToDisk(result, ref)
                        mutex.withLock { memory[ref] = result }
                        result
                    } finally {
                        mutex.withLock { inFlight.remove(ref) }
                    }
                }
            }
        }
        return task.await()
    }

    private fun fileFor(ref: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(ref.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(directory, "$digest.json")
    }

    private suspend fun readFromDisk(ref: String): PrayerLeafText? = withContext(Dispatchers.IO) {
        val file = fileFor(ref)
        if (!file.exists()) null else runCatching { json.decodeFromString(PrayerLeafText.serializer(), file.readText()) }.getOrNull()
    }

    private suspend fun writeToDisk(text: PrayerLeafText, ref: String) = withContext(Dispatchers.IO) {
        runCatching { fileFor(ref).writeText(json.encodeToString(PrayerLeafText.serializer(), text)) }
    }
}

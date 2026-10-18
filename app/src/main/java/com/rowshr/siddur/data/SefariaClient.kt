package com.rowshr.siddur.data

import com.rowshr.siddur.model.PrayerLeafText
import com.rowshr.siddur.model.PrayerParagraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Thin client for Sefaria's v3 texts API: https://developers.sefaria.org/reference/get-v3-texts */
class SefariaClient {
    class ApiException(message: String) : IOException(message)

    suspend fun fetch(ref: String): PrayerLeafText = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(ref, "UTF-8").replace("+", "%20")
        val url = URL("https://www.sefaria.org/api/v3/texts/$encoded?version=hebrew&version=english&return_format=default")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/json")
            connectTimeout = 30_000
            readTimeout = 30_000
        }
        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            val element = runCatching { Json.parseToJsonElement(body) }.getOrNull()
            val obj = element as? JsonObject
            obj?.get("error")?.jsonPrimitive?.contentOrNull?.let { throw ApiException(it) }
            if (code !in 200..299) throw ApiException("Sefaria returned HTTP $code.")
            if (obj == null) throw ApiException("Unexpected reply from Sefaria.")
            makeLeafText(obj, ref)
        } finally {
            connection.disconnect()
        }
    }

    /** Sefaria returns text as a string, an array of strings, or nested arrays. */
    private fun flatten(element: JsonElement?): List<String> = when (element) {
        null, JsonNull -> emptyList()
        is JsonPrimitive -> listOf(element.contentOrNull ?: "")
        is JsonArray -> element.flatMap { flatten(it) }
        is JsonObject -> emptyList()
    }

    private fun makeLeafText(payload: JsonObject, requestedRef: String): PrayerLeafText {
        val versions = payload["versions"]?.jsonArray ?: JsonArray(emptyList())
        if (versions.isEmpty()) throw ApiException("Sefaria has no text for this prayer.")
        val he = versions.map { it.jsonObject }.firstOrNull { it["language"]?.jsonPrimitive?.contentOrNull == "he" }
        val en = versions.map { it.jsonObject }.firstOrNull { it["language"]?.jsonPrimitive?.contentOrNull == "en" }
        val heParas = flatten(he?.get("text"))
        val enParas = flatten(en?.get("text"))
        val count = maxOf(heParas.size, enParas.size)
        val paragraphs = ArrayList<PrayerParagraph>(count)
        for (i in 0 until count) {
            val h = heParas.getOrNull(i)?.trim() ?: ""
            val e = enParas.getOrNull(i)?.trim() ?: ""
            if (h.isEmpty() && e.isEmpty()) continue
            paragraphs.add(PrayerParagraph(i, h, e))
        }
        val ref = payload["ref"]?.jsonPrimitive?.contentOrNull?.ifEmpty { null } ?: requestedRef
        val heRef = payload["heRef"]?.jsonPrimitive?.contentOrNull ?: ""
        return PrayerLeafText(
            ref = ref,
            heRef = heRef,
            title = ref.substringAfterLast(',').trim(),
            heTitle = heRef.substringAfterLast(',').trim(),
            hebrewVersionTitle = he?.get("versionTitle")?.jsonPrimitive?.contentOrNull,
            englishVersionTitle = en?.get("versionTitle")?.jsonPrimitive?.contentOrNull,
            paragraphs = paragraphs,
        )
    }
}

package com.rowshr.siddur.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.model.SiddurNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** A saved place in a siddur. */
@Serializable
data class Bookmark(
    val nusach: Nusach,
    val nodeId: String,
    val title: String,
    val heTitle: String,
    val created: Long,
) {
    val id: String get() = "${nusach.name}|$nodeId"
}

data class LibraryState(val bookmarks: List<Bookmark> = emptyList(), val lastRead: Bookmark? = null)

private val Context.libraryStore: DataStore<Preferences> by preferencesDataStore(name = "library")

/** Bookmarks and the "continue reading" pointer. */
class UserLibrary(context: Context) {
    private val store = context.applicationContext.libraryStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val bookmarksKey = stringPreferencesKey("bookmarks")
    private val lastReadKey = stringPreferencesKey("lastRead")

    private fun Preferences.toState() = LibraryState(
        bookmarks = this[bookmarksKey]?.let { runCatching { json.decodeFromString(ListSerializer(Bookmark.serializer()), it) }.getOrNull() } ?: emptyList(),
        lastRead = this[lastReadKey]?.let { runCatching { json.decodeFromString(Bookmark.serializer(), it) }.getOrNull() },
    )

    val state: StateFlow<LibraryState> = store.data.map { it.toState() }
        .stateIn(scope, SharingStarted.Eagerly, runBlocking { store.data.first().toState() })

    fun isBookmarked(node: SiddurNode, nusach: Nusach): Boolean =
        state.value.bookmarks.any { it.nusach == nusach && it.nodeId == node.id }

    fun toggleBookmark(node: SiddurNode, nusach: Nusach) {
        val current = state.value.bookmarks
        val next = if (current.any { it.nusach == nusach && it.nodeId == node.id }) {
            current.filterNot { it.nusach == nusach && it.nodeId == node.id }
        } else {
            listOf(Bookmark(nusach, node.id, node.title, node.heTitle, System.currentTimeMillis())) + current
        }
        scope.launch { store.edit { it[bookmarksKey] = json.encodeToString(ListSerializer(Bookmark.serializer()), next) } }
    }

    fun remove(bookmark: Bookmark) {
        val next = state.value.bookmarks.filterNot { it.id == bookmark.id }
        scope.launch { store.edit { it[bookmarksKey] = json.encodeToString(ListSerializer(Bookmark.serializer()), next) } }
    }

    fun markRead(node: SiddurNode, nusach: Nusach) {
        val last = Bookmark(nusach, node.id, node.title, node.heTitle, System.currentTimeMillis())
        scope.launch { store.edit { it[lastReadKey] = json.encodeToString(Bookmark.serializer(), last) } }
    }
}

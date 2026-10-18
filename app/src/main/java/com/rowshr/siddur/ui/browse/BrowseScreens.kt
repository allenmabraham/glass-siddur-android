package com.rowshr.siddur.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.model.SiddurNode
import com.rowshr.siddur.settings.Bookmark
import com.rowshr.siddur.ui.LocalAppContainer
import com.rowshr.siddur.ui.components.Backdrop
import com.rowshr.siddur.ui.components.BackdropStyle
import com.rowshr.siddur.ui.components.GlassRow
import com.rowshr.siddur.ui.components.NusachPicker
import com.rowshr.siddur.ui.components.readableWidth
import com.rowshr.siddur.ui.home.BrandButton
import com.rowshr.siddur.ui.openReader
import com.rowshr.siddur.ui.openSection
import com.rowshr.siddur.ui.theme.Brand

/** The Library tab: the full table of contents of the chosen nusach. */
@Composable
fun LibraryScreen(nav: NavHostController) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val root = remember(settings.nusach) { container.siddur.root(settings.nusach) }
    Box(Modifier.fillMaxSize()) {
        Backdrop(BackdropStyle.HOME)
        Column(Modifier.fillMaxSize().padding(top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())) {
            Column(Modifier.padding(horizontal = 18.dp).padding(top = 8.dp)) {
                Text(settings.nusach.displayName, style = MaterialTheme.typography.headlineMedium)
                Text(settings.nusach.hebrewName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                NusachPicker(settings.nusach, onSelect = { n -> container.settings.update { it.copy(nusach = n) } })
            }
            SectionList(nav, settings.nusach, root, showReadAll = false)
        }
    }
}

/** Table of contents for one section of a siddur. */
@Composable
fun SectionScreen(nav: NavHostController, nusach: Nusach, id: String) {
    val container = LocalAppContainer.current
    val node = remember(nusach, id) { container.siddur.root(nusach).node(id) }
    Box(Modifier.fillMaxSize()) {
        Backdrop(BackdropStyle.HOME)
        Column(Modifier.fillMaxSize()) {
            InlineTopBar(
                title = if (node == null || node.id == "root") nusach.displayName else node.title,
                subtitle = if (node == null || node.id == "root") nusach.hebrewName else node.heTitle,
                onBack = { nav.popBackStack() },
            )
            if (node == null) {
                Missing()
            } else {
                SectionList(nav, nusach, node, showReadAll = node.id != "root")
            }
        }
    }
}

@Composable
private fun SectionList(nav: NavHostController, nusach: Nusach, node: SiddurNode, showReadAll: Boolean) {
    val bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showReadAll) {
            item {
                Box(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                    BrandButton("Read all of ${node.title}", Modifier.fillMaxWidth().readableWidth(520.dp), Icons.Filled.MenuBook) { nav.openReader(nusach, node.id) }
                }
            }
        }
        items(node.children ?: emptyList(), key = { it.id }) { child ->
            SectionRow(child) {
                if (child.isLeaf) nav.openReader(nusach, child.id) else nav.openSection(nusach, child.id)
            }
        }
    }
}

@Composable
fun SectionRow(node: SiddurNode, onClick: () -> Unit) {
    val settings by LocalAppContainer.current.settings.state.collectAsStateWithLifecycle()
    GlassRow(onClick = onClick) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(node.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (!node.isLeaf) {
                Text("${node.leafCount} ${if (node.leafCount == 1) "prayer" else "prayers"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(node.heTitle, fontFamily = settings.hebrewFont.family, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(if (node.isLeaf) Icons.Outlined.Notes else Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

/** Small inline title with a back button, matching the reader. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineTopBar(title: String, subtitle: String?, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrEmpty()) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
private fun Missing() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("This prayer is no longer in the table of contents.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The Search tab: find any prayer by English or Hebrew title. */
@Composable
fun SearchScreen(nav: NavHostController) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val root = remember(settings.nusach) { container.siddur.root(settings.nusach) }

    data class Hit(val node: SiddurNode, val trail: String)
    val hits = remember(query, root) {
        val q = query.trim()
        if (q.length < 2) emptyList() else {
            val out = ArrayList<Hit>()
            fun walk(n: SiddurNode, trail: List<String>) {
                if (n.id != "root" && (n.title.contains(q, ignoreCase = true) || n.heTitle.contains(q))) out.add(Hit(n, trail.joinToString(" › ")))
                n.children?.forEach { walk(it, if (n.id == "root") emptyList() else trail + n.title) }
            }
            walk(root, emptyList())
            out.take(80)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Backdrop(BackdropStyle.HOME)
        Column(Modifier.fillMaxSize().padding(top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())) {
            Text("Search", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Prayers, psalms, blessings…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand, unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            )
            if (hits.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (query.length < 2) "Search ${settings.nusach.displayName}" else "No matches", style = MaterialTheme.typography.titleMedium)
                        Text(if (query.length < 2) "Find any prayer by its English or Hebrew name." else "Try another spelling, or browse the Library.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                val bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
                LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = bottom + 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hits, key = { it.node.id }) { hit ->
                        GlassRow(onClick = { if (hit.node.isLeaf) nav.openReader(settings.nusach, hit.node.id) else nav.openSection(settings.nusach, hit.node.id) }) {
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(hit.node.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Text(hit.node.heTitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                if (hit.trail.isNotEmpty()) Text(hit.trail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

/** The Saved tab: bookmarks. */
@Composable
fun SavedScreen(nav: NavHostController) {
    val container = LocalAppContainer.current
    val library by container.library.state.collectAsStateWithLifecycle()
    val settings by container.settings.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        Backdrop(BackdropStyle.HOME)
        Column(Modifier.fillMaxSize().padding(top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())) {
            Text("Saved", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            if (library.bookmarks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No bookmarks yet", style = MaterialTheme.typography.titleMedium)
                        Text("Tap the bookmark in any prayer to keep it here.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                val bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
                LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottom + 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(library.bookmarks, key = { it.id }) { bookmark: Bookmark ->
                        GlassRow(onClick = { nav.openReader(bookmark.nusach, bookmark.nodeId) }) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(bookmark.title, style = MaterialTheme.typography.bodyLarge)
                                Text(bookmark.nusach.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(bookmark.heTitle, fontFamily = settings.hebrewFont.family, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            IconButton(onClick = { container.library.remove(bookmark) }) { Icon(Icons.Outlined.Delete, contentDescription = "Remove bookmark", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

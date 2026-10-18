package com.rowshr.siddur.ui.reader

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.rowshr.siddur.fold.FoldAwareBox
import com.rowshr.siddur.fold.LocalFoldInfo
import com.rowshr.siddur.fold.ReadingArrangement
import com.rowshr.siddur.fold.SpreadPages
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.model.PrayerBlock
import com.rowshr.siddur.model.PrayerLeafText
import com.rowshr.siddur.model.SiddurNode
import com.rowshr.siddur.settings.BilingualLayout
import com.rowshr.siddur.settings.LanguageMode
import com.rowshr.siddur.settings.ReadingSettings
import com.rowshr.siddur.ui.LocalAppContainer
import com.rowshr.siddur.ui.browse.InlineTopBar
import com.rowshr.siddur.ui.components.Backdrop
import com.rowshr.siddur.ui.components.BackdropStyle
import com.rowshr.siddur.ui.components.GlassChip
import com.rowshr.siddur.ui.components.LanguageModePicker
import com.rowshr.siddur.ui.components.SectionHeading
import com.rowshr.siddur.ui.components.SurfaceCard
import com.rowshr.siddur.ui.components.glassBorder
import com.rowshr.siddur.ui.components.glassColor
import com.rowshr.siddur.ui.components.pinchToZoom
import com.rowshr.siddur.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The page. Loads every prayer under `node`, renders them as one continuous
 * scroll, and floats the reading controls in a glass bar at the bottom.
 *
 * On an open foldable the reader becomes a book: bilingual text runs English on the
 * left page and Hebrew on the right with the crease as the gutter; a single
 * language keeps its page and the facing page carries the contents. Half open
 * on a table, the text sits on the upright half and the contents on the flat one.
 */
@Composable
fun ReaderScreen(nav: NavHostController, nusach: Nusach, id: String) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val library by container.library.state.collectAsStateWithLifecycle()
    val fold = LocalFoldInfo.current
    val node = remember(nusach, id) { container.siddur.root(nusach).node(id) }
    if (node == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This prayer is no longer in the table of contents.") }
        return
    }

    val blocks = remember(node) { mutableStateListOf<PrayerBlock>() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var selection by remember { mutableStateOf<String?>(null) }
    var showTypography by remember { mutableStateOf(false) }
    val pageInset = 20.dp
    val mode = if (nusach.hasEnglish) settings.languageMode(fold.isOpen) else LanguageMode.HEBREW
    val isBookmarked = library.bookmarks.any { it.nusach == nusach && it.nodeId == node.id }

    suspend fun load(index: Int, leaf: SiddurNode) {
        val state = try { PrayerBlock.State.Loaded(container.texts.text(leaf.ref)) } catch (e: Exception) { PrayerBlock.State.Failed(e.message ?: "Could not load this prayer.") }
        if (index < blocks.size) blocks[index] = blocks[index].copy(state = state)
    }

    LaunchedEffect(node) {
        container.library.markRead(node, nusach)
        val leaves = node.leaves
        blocks.clear(); blocks.addAll(leaves.map { PrayerBlock(it, PrayerBlock.State.Loading) })
        coroutineScope { leaves.mapIndexed { i, leaf -> async(Dispatchers.Default) { load(i, leaf) } }.awaitAll() }
    }

    KeepScreenAwake(settings.keepScreenAwake)

    // Pinch anywhere on the page to resize the type; one finger still scrolls.
    var pinchBase by remember { mutableStateOf(1.0) }
    val pinch = Modifier.pinchToZoom(
        onZoomStart = { pinchBase = container.settings.state.value.fontScale },
        onZoom = { zoom -> container.settings.setFontScale(pinchBase * zoom) },
    )

    Box(Modifier.fillMaxSize()) {
        Backdrop(BackdropStyle.READER)
        FoldAwareBox(pageInset) { arrangement ->
            val windowWidth = maxWidth
            val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.padding(top = top)) {
                    InlineTopBar(node.title, node.heTitle, onBack = { nav.popBackStack() })
                }
                val page: @Composable (Modifier, Dp, SpreadPages?, Dp?) -> Unit = { modifier, width, spread, cap ->
                    PrayerList(modifier.then(pinch), blocks, nusach, settings, mode, width, spread, cap, listState, pageInset, selection, onSelect = { selection = if (selection == it) null else it }, onRetry = { i, leaf -> scope.launch { load(i, leaf) } })
                }
                val facing: @Composable (Modifier) -> Unit = { modifier ->
                    FacingPage(modifier, node, nusach, blocks, settings, mode, pageInset, onJump = { blockId -> scope.launch { listState.animateScrollToItem(headingIndex(blocks, blockId)) } })
                }
                when (arrangement) {
                    is ReadingArrangement.Page -> page(Modifier.weight(1f).fillMaxWidth(), minOf(windowWidth, 800.dp) - pageInset * 2, null, 760.dp)
                    is ReadingArrangement.Spread -> {
                        val pages = arrangement.pages
                        if (mode == LanguageMode.BILINGUAL) {
                            page(Modifier.weight(1f).fillMaxWidth(), windowWidth - pageInset * 2, pages, null)
                        } else {
                            // Hebrew reads on the right-hand page, English on the left; the other page holds the contents.
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(pages.gutter - pageInset * 2)) {
                                if (mode == LanguageMode.HEBREW) {
                                    facing(Modifier.width(pages.left + pageInset * 2).fillMaxHeight())
                                    page(Modifier.width(pages.right + pageInset * 2).fillMaxHeight(), pages.right, null, null)
                                } else {
                                    page(Modifier.width(pages.left + pageInset * 2).fillMaxHeight(), pages.left, null, null)
                                    facing(Modifier.width(pages.right + pageInset * 2).fillMaxHeight())
                                }
                            }
                        }
                    }
                    is ReadingArrangement.Tabletop -> {
                        page(Modifier.fillMaxWidth().height(arrangement.top - top - 64.dp), minOf(windowWidth, 800.dp) - pageInset * 2, null, 760.dp)
                        Spacer(Modifier.height(arrangement.gap))
                        facing(Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            ReaderToolbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                nusach = nusach, settings = settings, mode = mode, blocks = blocks, isBookmarked = isBookmarked,
                onLanguage = { container.settings.setLanguageMode(it, fold.isOpen) },
                onLayout = { l -> container.settings.update { it.copy(bilingualLayout = l) } },
                onJump = { blockId -> scope.launch { listState.animateScrollToItem(headingIndex(blocks, blockId)) } },
                onBookmark = { container.library.toggleBookmark(node, nusach) },
                onTypography = { showTypography = true },
            )
            LoadingPill(blocks, Modifier.align(Alignment.TopCenter).padding(top = top + 64.dp))
        }
    }
    if (showTypography) TypographySheet(onDismiss = { showTypography = false })
}

/** Index in the flattened list of a block's heading, for "jump to". */
private fun headingIndex(blocks: List<PrayerBlock>, blockId: String): Int {
    var index = 0
    for (block in blocks) {
        if (block.id == blockId) return index
        index += 1 + ((block.state as? PrayerBlock.State.Loaded)?.text?.paragraphs?.size ?: 1)
    }
    return 0
}

@Composable
private fun PrayerList(
    modifier: Modifier,
    blocks: List<PrayerBlock>,
    nusach: Nusach,
    settings: ReadingSettings,
    mode: LanguageMode,
    width: Dp,
    spread: SpreadPages?,
    maxWidth: Dp?,
    listState: LazyListState,
    inset: Dp,
    selection: String?,
    onSelect: (String) -> Unit,
    onRetry: (Int, SiddurNode) -> Unit,
) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight().fillMaxWidth().then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier),
            contentPadding = PaddingValues(start = inset, end = inset, top = 8.dp, bottom = bottom + 110.dp),
        ) {
            if (!nusach.hasEnglish && settings.languageMode != LanguageMode.HEBREW) {
                item { NoEnglishNotice() }
            }
            blocks.forEachIndexed { blockIndex, block ->
                item(key = "h:${block.id}") {
                    PrayerHeading(block.node, settings, spread, bilingual = mode == LanguageMode.BILINGUAL && nusach.hasEnglish, modifier = Modifier.padding(top = if (blockIndex == 0) 12.dp else 40.dp, bottom = 10.dp))
                }
                when (val state = block.state) {
                    PrayerBlock.State.Loading -> item(key = "l:${block.id}") { Placeholder(settings) }
                    is PrayerBlock.State.Failed -> item(key = "f:${block.id}") { Failure(state.message) { onRetry(blockIndex, block.node) } }
                    is PrayerBlock.State.Loaded -> {
                        if (state.text.paragraphs.isEmpty()) {
                            item(key = "e:${block.id}") { Text("No text available for this prayer.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            state.text.paragraphs.forEach { paragraph ->
                                val rowId = "${block.id}#${paragraph.index}"
                                item(key = "p:$rowId") {
                                    Box(Modifier.padding(bottom = (settings.hebrewPointSize * 0.9f).dp)) {
                                        ParagraphRow(paragraph, nusach, settings, mode, width, spread, selected = selection == rowId, onTap = { onSelect(rowId) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Attribution(blocks, nusach) }
        }
    }
}


/** A prayer heading in the style of a printed siddur: the Hebrew title centred between hairlines with the English title in spaced small capitals beneath. */
@Composable
private fun PrayerHeading(node: SiddurNode, settings: ReadingSettings, spread: SpreadPages?, bilingual: Boolean, modifier: Modifier = Modifier) {
    if (spread != null && bilingual) {
        // On an open foldable the title sits on each page rather than across the crease.
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(spread.gutter), verticalAlignment = Alignment.Top) {
            Box(Modifier.width(spread.left).padding(top = 10.dp)) { EnglishHeading(node, settings) }
            Box(Modifier.width(spread.right)) { HebrewHeading(node, settings) }
        }
    } else {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HebrewHeading(node, settings)
            EnglishHeading(node, settings)
        }
    }
}

@Composable
private fun HebrewHeading(node: SiddurNode, settings: ReadingSettings) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Rule(Modifier.weight(1f))
        Text(node.heTitle, fontFamily = settings.hebrewFont.family, fontWeight = FontWeight.Bold, fontSize = (settings.hebrewPointSize * 1.15f).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Rule(Modifier.weight(1f))
    }
}

@Composable
private fun EnglishHeading(node: SiddurNode, settings: ReadingSettings) {
    if (node.title.isEmpty()) return
    Text(
        node.title.uppercase(), fontFamily = settings.englishFont.family, fontSize = maxOf(11f, settings.englishPointSize * 0.68f).sp, letterSpacing = 2.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Rule(modifier: Modifier) {
    Box(modifier.height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)))
}

@Composable
private fun Placeholder(settings: ReadingSettings) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { i ->
            Box(Modifier.padding(start = (i * 24).dp).fillMaxWidth().height((settings.hebrewPointSize * 1.1f).dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)))
        }
    }
}

@Composable
private fun Failure(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        GlassChip(selected = false, onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Try again")
        }
    }
}

@Composable
private fun NoEnglishNotice() {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(glassColor()).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Sefaria has no English translation for Nusach Ari, so this siddur is shown in Hebrew.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Attribution(blocks: List<PrayerBlock>, nusach: Nusach) {
    val first = blocks.firstNotNullOfOrNull { (it.state as? PrayerBlock.State.Loaded)?.text }
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        Text("Text courtesy of Sefaria", style = MaterialTheme.typography.labelSmall, color = color)
        first?.englishVersionTitle?.takeIf { nusach.hasEnglish }?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = color, textAlign = TextAlign.Center) }
        first?.hebrewVersionTitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = color, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun LoadingPill(blocks: List<PrayerBlock>, modifier: Modifier) {
    val loaded = blocks.count { it.state is PrayerBlock.State.Loaded }
    val loading = blocks.any { it.state is PrayerBlock.State.Loading }
    if (blocks.isNotEmpty() && loaded < blocks.size && loading) {
        Row(modifier.shadow(6.dp, CircleShape).clip(CircleShape).background(glassColor(1.6f)).border(glassBorder(), CircleShape).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Brand)
            Text("Loading ${loaded + 1} of ${blocks.size}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The page opposite the text on an open foldable, or the flat half on a table: the contents of this section, and the reading controls within thumb's reach. */
@Composable
private fun FacingPage(modifier: Modifier, node: SiddurNode, nusach: Nusach, blocks: List<PrayerBlock>, settings: ReadingSettings, mode: LanguageMode, inset: Dp, onJump: (String) -> Unit) {
    val container = LocalAppContainer.current
    val fold = LocalFoldInfo.current
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = inset, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(node.title, style = MaterialTheme.typography.titleLarge)
            Text(node.heTitle, fontFamily = settings.hebrewFont.family, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (nusach.hasEnglish) LanguageModePicker(mode = mode, onSelect = { container.settings.setLanguageMode(it, fold.isOpen) })
        SurfaceCard(Modifier.fillMaxWidth(), padding = 14.dp, cornerRadius = 20.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassChip(selected = false, onClick = { container.settings.decreaseFont() }) { Text("A", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                Slider(value = settings.fontScale.toFloat(), onValueChange = { container.settings.setFontScale(it.toDouble()) }, valueRange = ReadingSettings.fontScaleRange.start.toFloat()..ReadingSettings.fontScaleRange.endInclusive.toFloat(), modifier = Modifier.weight(1f))
                GlassChip(selected = false, onClick = { container.settings.increaseFont() }) { Text("A", fontSize = 24.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (blocks.size > 1) {
            SectionHeading("Contents", hebrew = "תוכן")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                blocks.forEach { block ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(glassColor()).border(glassBorder(), RoundedCornerShape(14.dp)).clickable { onJump(block.id) }.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(block.node.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(block.node.heTitle, fontFamily = settings.hebrewFont.family, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Floating glass bar: language, jump to, bookmark, text size. */
@Composable
private fun ReaderToolbar(
    modifier: Modifier, nusach: Nusach, settings: ReadingSettings, mode: LanguageMode, blocks: List<PrayerBlock>, isBookmarked: Boolean,
    onLanguage: (LanguageMode) -> Unit, onLayout: (BilingualLayout) -> Unit, onJump: (String) -> Unit, onBookmark: () -> Unit, onTypography: () -> Unit,
) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var languageMenu by remember { mutableStateOf(false) }
    var jumpMenu by remember { mutableStateOf(false) }
    Row(
        modifier.padding(bottom = bottom + 16.dp).padding(horizontal = 16.dp).shadow(14.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.18f)).clip(CircleShape).background(glassColor(1.7f)).border(glassBorder(), CircleShape).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Row(Modifier.clip(CircleShape).clickable(enabled = nusach.hasEnglish) { languageMenu = true }.padding(horizontal = 12.dp, vertical = 10.dp).alpha(if (nusach.hasEnglish) 1f else 0.5f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Language, contentDescription = "Language", tint = Brand)
                Text(mode.label, style = MaterialTheme.typography.labelLarge)
            }
            DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                LanguageMode.entries.forEach { m ->
                    DropdownMenuItem(text = { Text(m.accessibilityLabel) }, onClick = { onLanguage(m); languageMenu = false }, trailingIcon = { if (m == mode) Icon(Icons.Filled.Check, contentDescription = null) })
                }
                if (mode == LanguageMode.BILINGUAL) {
                    HorizontalDivider()
                    BilingualLayout.entries.forEach { l ->
                        DropdownMenuItem(text = { Text(l.label) }, onClick = { onLayout(l); languageMenu = false }, trailingIcon = { if (settings.bilingualLayout == l) Icon(Icons.Filled.Check, contentDescription = null) })
                    }
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        if (blocks.size > 1) {
            Box {
                ToolbarIcon(Icons.Outlined.FormatListBulleted, "Jump to") { jumpMenu = true }
                DropdownMenu(expanded = jumpMenu, onDismissRequest = { jumpMenu = false }) {
                    blocks.forEach { block -> DropdownMenuItem(text = { Text(block.node.title) }, onClick = { onJump(block.id); jumpMenu = false }) }
                }
            }
        }
        ToolbarIcon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark, "Bookmark", onBookmark)
        ToolbarIcon(Icons.Filled.FormatSize, "Text size and fonts", onTypography)
    }
}

@Composable
private fun ToolbarIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = description, tint = Brand) }
}

/** Keeps the screen on while a prayer is open. */
@Composable
private fun KeepScreenAwake(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = (context as? ComponentActivity)?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

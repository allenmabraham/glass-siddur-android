package com.rowshr.siddur.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rowshr.siddur.fold.SpreadPages
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.model.PrayerParagraph
import com.rowshr.siddur.settings.BilingualLayout
import com.rowshr.siddur.settings.LanguageMode
import com.rowshr.siddur.settings.ReadingSettings
import com.rowshr.siddur.ui.components.glassColor
import com.rowshr.siddur.ui.theme.Brand

/**
 * One paragraph laid out per the current language mode: Hebrew on the right,
 * English on the left, aligned at the top so the two columns track each other.
 *
 * Sync: every paragraph is one row, so its translation always sits beside it.
 * Tapping a paragraph highlights the pair; in a single-language mode a tap
 * reveals the other language beneath it for just that paragraph.
 */
@Composable
fun ParagraphRow(
    paragraph: PrayerParagraph,
    nusach: Nusach,
    settings: ReadingSettings,
    mode: LanguageMode,
    availableWidth: Dp,
    spread: SpreadPages?,
    selected: Boolean,
    onTap: () -> Unit,
) {
    val effectiveMode = if (nusach.hasEnglish) mode else LanguageMode.HEBREW
    val canReveal = paragraph.hasHebrew && paragraph.hasEnglish && nusach.hasEnglish
    val highlight by animateColorAsState(if (selected) Brand.copy(alpha = 0.12f) else Brand.copy(alpha = 0f), label = "highlight")
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .then(Modifier)
            .clip(shape)
            .background(highlight)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        if (selected) {
            Box(Modifier.align(Alignment.CenterStart).padding(vertical = 6.dp).width(3.dp).fillMaxHeight().background(Brand, RoundedCornerShape(2.dp)))
        }
        when (effectiveMode) {
            LanguageMode.HEBREW -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HebrewColumn(paragraph, settings, Modifier.fillMaxWidth())
                Revealed(visible = selected && canReveal) { EnglishColumn(paragraph, settings, Modifier.fillMaxWidth()) }
            }
            LanguageMode.ENGLISH -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (paragraph.hasEnglish) EnglishColumn(paragraph, settings, Modifier.fillMaxWidth()) else HebrewColumn(paragraph, settings, Modifier.fillMaxWidth())
                Revealed(visible = selected && canReveal) { HebrewColumn(paragraph, settings, Modifier.fillMaxWidth()) }
            }
            LanguageMode.BILINGUAL -> if (spread != null) {
                // An open foldable: English on the left page, Hebrew on the right, the crease between.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spread.gutter), verticalAlignment = Alignment.Top) {
                        EnglishColumn(paragraph, settings, Modifier.width(spread.left))
                        HebrewColumn(paragraph, settings, Modifier.width(spread.right))
                    }
                }
            } else when (settings.resolvedLayout(availableWidth.value)) {
                BilingualLayout.SIDE_BY_SIDE, BilingualLayout.AUTOMATIC -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                        EnglishColumn(paragraph, settings, Modifier.weight(1f))
                        HebrewColumn(paragraph, settings, Modifier.weight(1f))
                    }
                }
                BilingualLayout.STACKED -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HebrewColumn(paragraph, settings, Modifier.fillMaxWidth())
                    EnglishColumn(paragraph, settings, Modifier.fillMaxWidth())
                }
            }
        }
    }
}


/** The other language, shown inline under a tapped paragraph. */
@Composable
private fun Revealed(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(glassColor()).padding(12.dp)) { content() }
    }
}

@Composable
private fun HebrewColumn(paragraph: PrayerParagraph, settings: ReadingSettings, modifier: Modifier) {
    if (!paragraph.hasHebrew) { Box(modifier) ; return }
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val instruction = remember(paragraph.hebrewHTML) { StyledText.isInstruction(paragraph.hebrewHTML) }
    val text = remember(paragraph.hebrewHTML, settings.showVowels, settings.showCantillation, settings.hebrewPointSize, secondary) {
        StyledText.hebrew(paragraph.hebrewHTML, settings, secondary)
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text,
            modifier = modifier,
            fontFamily = settings.hebrewFont.family,
            fontSize = settings.hebrewPointSize.sp,
            lineHeight = (settings.hebrewPointSize * settings.hebrewLineHeightFactor).sp,
            textAlign = if (instruction) TextAlign.Center else TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EnglishColumn(paragraph: PrayerParagraph, settings: ReadingSettings, modifier: Modifier) {
    if (!paragraph.hasEnglish) { Box(modifier); return }
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val instruction = remember(paragraph.englishHTML) { StyledText.isInstruction(paragraph.englishHTML) }
    val text = remember(paragraph.englishHTML, settings.englishPointSize, secondary) { StyledText.english(paragraph.englishHTML, settings, secondary) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text,
            modifier = modifier,
            fontFamily = settings.englishFont.family,
            fontSize = settings.englishPointSize.sp,
            lineHeight = (settings.englishPointSize * settings.englishLineHeightFactor).sp,
            textAlign = if (instruction) TextAlign.Center else TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

package com.rowshr.siddur.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.settings.LanguageMode

/** Glass segmented control: עברית · Both · English. */
@Composable
fun LanguageModePicker(mode: LanguageMode, onSelect: (LanguageMode) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LanguageMode.entries.forEach { candidate ->
            val selected = mode == candidate
            GlassChip(
                selected = selected,
                onClick = { if (enabled) onSelect(candidate) },
                modifier = Modifier.weight(1f).semantics { contentDescription = candidate.accessibilityLabel },
            ) {
                Text(
                    candidate.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Glass chip row for choosing a nusach; two rows of two on a narrow page. */
@Composable
fun NusachPicker(nusach: Nusach, onSelect: (Nusach) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val perRow = if (maxWidth < 400.dp) 2 else 4
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Nusach.entries.chunked(perRow).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { candidate -> NusachChip(candidate, nusach == candidate, Modifier.weight(1f)) { onSelect(candidate) } }
                }
            }
        }
    }
}

@Composable
private fun NusachChip(candidate: Nusach, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    GlassChip(selected = selected, onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                candidate.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontSize = 12.sp,
            )
            Text(
                candidate.hebrewName,
                style = MaterialTheme.typography.labelSmall,
                color = (if (selected) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.8f),
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NusachPickerLegacy(nusach: Nusach, onSelect: (Nusach) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Nusach.entries.forEach { candidate ->
            val selected = nusach == candidate
            GlassChip(selected = selected, onClick = { onSelect(candidate) }, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        candidate.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontSize = 12.sp,
                    )
                    Text(
                        candidate.hebrewName,
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (selected) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.8f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

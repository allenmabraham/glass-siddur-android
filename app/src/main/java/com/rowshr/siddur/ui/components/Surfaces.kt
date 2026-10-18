package com.rowshr.siddur.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rowshr.siddur.ui.theme.Appearance
import com.rowshr.siddur.ui.theme.Brand
import com.rowshr.siddur.ui.theme.LocalAppearance

/** The translucent "glass" fill the cards and chips share. */
@Composable
fun glassColor(strength: Float = 1f): Color {
    val dark = LocalAppearance.current == Appearance.DARK
    return if (dark) Color.White.copy(alpha = 0.08f * strength) else Color.White.copy(alpha = 0.55f * strength)
}

@Composable
fun glassBorder(): BorderStroke {
    val dark = LocalAppearance.current == Appearance.DARK
    return BorderStroke(0.6.dp, Color.White.copy(alpha = if (dark) 0.14f else 0.6f))
}

/** A translucent content surface. Glass is reserved for floating controls; content sits on a quieter material so text stays legible. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    cornerRadius: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier
            .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.10f))
            .clip(shape)
            .background(glassColor())
            .border(glassBorder(), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Capsule chip: brand-filled when selected, glass otherwise. */
@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .semantics { this.selected = selected }
            .shadow(if (selected) 6.dp else 2.dp, shape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.12f))
            .clip(shape)
            .background(if (selected) Brand else glassColor())
            .border(glassBorder(), shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Section heading used on Today and Browse. */
@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, hebrew: String? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (hebrew != null) Text(hebrew, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A list row on the Browse screens: title, count, Hebrew title, chevron. */
@Composable
fun GlassRow(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glassColor())
            .border(glassBorder(), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** Keeps content at a readable measure on wide screens. */
fun Modifier.readableWidth(max: Dp = 760.dp): Modifier = this.widthIn(max = max)

@Composable
fun SingleLineText(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge, color: Color = Color.Unspecified) {
    Text(text, modifier, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun VerticalSpace(height: Dp) = Column(Modifier.padding(top = height)) {}
